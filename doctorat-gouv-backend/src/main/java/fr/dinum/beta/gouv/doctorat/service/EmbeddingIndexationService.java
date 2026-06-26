package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.SujetEmbedding;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.SujetEmbeddingRepository;
import fr.dinum.beta.gouv.doctorat.service.BlocExtractor.BlocSujet;

@Service
public class EmbeddingIndexationService {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingIndexationService.class);

	private final ScalewayEmbeddingService scaleway;
	private final SujetEmbeddingRepository repository;
	private final BlocExtractor extractor;
	private final PropositionTheseRepository sujetRepository;

	@Value("${scaleway.model:bge-multilingual-gemma2}")
	private String versionModele;

	public EmbeddingIndexationService(ScalewayEmbeddingService scaleway,
									  SujetEmbeddingRepository repository,
									  BlocExtractor extractor,
									  PropositionTheseRepository sujetRepository) {
		this.scaleway = scaleway;
		this.repository = repository;
		this.extractor = extractor;
		this.sujetRepository = sujetRepository;
	}

	@Transactional
	public void indexerTout() {
		log.info("Début indexation initiale Scaleway");
		List<PropositionThese> sujets = sujetRepository.findNeedingIndexationScaleway();
		if (sujets.isEmpty()) {
			log.info("Tous les sujets sont déjà indexés à jour");
			return;
		}
		indexerEtMarquer(sujets);
		log.info("Indexation initiale terminée : {} sujets traités", sujets.size());
	}

	@Transactional
	public void indexerIncremental() {
		log.info("Début indexation incrémentale Scaleway");

		// Désindexer les sujets devenus inactifs
		List<PropositionThese> indexes = sujetRepository.findIndexedInScaleway();
		List<PropositionThese> inactifs = indexes.stream()
			.filter(s -> Boolean.FALSE.equals(s.getActive()))
			.collect(Collectors.toList());
		if (!inactifs.isEmpty()) {
			List<Long> ids = inactifs.stream().map(PropositionThese::getId).collect(Collectors.toList());
			repository.deleteByPropositionTheseIdIn(ids);
			inactifs.forEach(s -> s.setDateIndexationScaleway(null));
			sujetRepository.saveAll(inactifs);
			log.info("{} sujets désindexés (devenus inactifs)", inactifs.size());
		}

		// Indexer les sujets qui en ont besoin
		List<PropositionThese> sujets = sujetRepository.findNeedingIndexationScaleway();
		if (sujets.isEmpty()) {
			log.info("Aucun sujet à indexer ou ré-indexer");
			return;
		}
		indexerEtMarquer(sujets);
		log.info("Indexation incrémentale terminée : {} sujets traités", sujets.size());
	}

	@Transactional
	public void indexerSujet(Long sujetId) {
		PropositionThese sujet = sujetRepository.findById(sujetId).orElseThrow();

		// Nettoyer les anciens embeddings
		repository.deleteByPropositionTheseId(sujetId);

		if (Boolean.FALSE.equals(sujet.getActive())) {
			sujet.setDateIndexationScaleway(null);
			sujetRepository.save(sujet);
			log.info("Sujet {} désindexé (inactif)", sujetId);
			return;
		}

		indexerEtMarquer(List.of(sujet));
		log.info("Sujet {} indexé", sujetId);
	}

	private void indexerEtMarquer(List<PropositionThese> sujets) {
		List<BlocSujet> tousLesBlocs = new ArrayList<>();
		for (PropositionThese sujet : sujets) {
			tousLesBlocs.addAll(extractor.extraireBlocs(sujet));
		}

		List<SujetEmbedding> embeddings = new ArrayList<>();
		int batchSize = 50;

		for (int i = 0; i < tousLesBlocs.size(); i += batchSize) {
			List<BlocSujet> lot = tousLesBlocs.subList(i,
				Math.min(i + batchSize, tousLesBlocs.size()));

			List<String> textes = lot.stream()
				.map(BlocSujet::getContenu)
				.collect(Collectors.toList());

			List<float[]> vecteurs = scaleway.embedBatch(textes);

			for (int j = 0; j < lot.size(); j++) {
				BlocSujet bloc = lot.get(j);
				SujetEmbedding emb = new SujetEmbedding();
				emb.setPropositionTheseId(bloc.getSujetId());
				emb.setBlocType(bloc.getType());
				emb.setContenu(bloc.getContenu());
				emb.setEmbedding(vecteurs.get(j));
				emb.setDateIndexation(LocalDateTime.now());
				emb.setVersionModele(versionModele);
				embeddings.add(emb);
			}
		}

		repository.saveAll(embeddings);

		LocalDateTime now = LocalDateTime.now();
		for (PropositionThese sujet : sujets) {
			sujet.setDateIndexationScaleway(now);
		}
		sujetRepository.saveAll(sujets);

		log.info("{} embeddings sauvegardés pour {} sujets", embeddings.size(), sujets.size());
	}
}
