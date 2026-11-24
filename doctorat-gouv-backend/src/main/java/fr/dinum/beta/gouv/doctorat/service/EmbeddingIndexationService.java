package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

	private static final int EMBEDDING_BATCH_SIZE = 50;

	@Value("${scaleway.indexation.subject-batch-size:50}")
	private int subjectBatchSize;

	@PersistenceContext
	private EntityManager entityManager;

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
		int total = 0;
		while (true) {
			List<PropositionThese> sujets = sujetRepository.findNeedingIndexationScalewayBatch(PageRequest.of(0, subjectBatchSize));
			if (sujets.isEmpty()) break;
			indexerEtMarquer(sujets);
			total += sujets.size();
			entityManager.flush();
			entityManager.clear();
		}
		log.info("Indexation initiale terminée : {} sujets traités", total);
	}

	@Transactional
	public void indexerIncremental() {
		log.info("Début indexation incrémentale Scaleway");

		// Désindexer les sujets devenus inactifs, par lots
		int desindexes = 0;
		while (true) {
			List<PropositionThese> inactifs = sujetRepository.findIndexedInactiveInScaleway(PageRequest.of(0, subjectBatchSize));
			if (inactifs.isEmpty()) break;

			List<Long> ids = inactifs.stream().map(PropositionThese::getId).collect(Collectors.toList());
			repository.deleteByPropositionTheseIdIn(ids);
			inactifs.forEach(s -> s.setDateIndexationScaleway(null));
			sujetRepository.saveAll(inactifs);

			desindexes += inactifs.size();
			entityManager.flush();
			entityManager.clear();
		}
		if (desindexes > 0) {
			log.info("{} sujets désindexés (devenus inactifs)", desindexes);
		}

		// Indexer les sujets qui en ont besoin, par lots
		int total = 0;
		while (true) {
			List<PropositionThese> sujets = sujetRepository.findNeedingIndexationScalewayBatch(PageRequest.of(0, subjectBatchSize));
			if (sujets.isEmpty()) break;
			indexerEtMarquer(sujets);
			total += sujets.size();
			entityManager.flush();
			entityManager.clear();
		}

		if (total == 0) {
			log.info("Aucun sujet à indexer ou ré-indexer");
		} else {
			log.info("Indexation incrémentale terminée : {} sujets traités", total);
		}
	}

	@Transactional
	public void indexerSujet(Long sujetId) {
		PropositionThese sujet = sujetRepository.findById(sujetId).orElseThrow();

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
		// Nettoyer les anciens embeddings pour éviter les doublons lors des réindexations
		List<Long> ids = sujets.stream().map(PropositionThese::getId).collect(Collectors.toList());
		repository.deleteByPropositionTheseIdIn(ids);

		List<BlocSujet> tousLesBlocs = new ArrayList<>();
		for (PropositionThese sujet : sujets) {
			tousLesBlocs.addAll(extractor.extraireBlocs(sujet));
		}

		List<SujetEmbedding> embeddings = new ArrayList<>();
		for (int i = 0; i < tousLesBlocs.size(); i += EMBEDDING_BATCH_SIZE) {
			List<BlocSujet> lot = tousLesBlocs.subList(i,
				Math.min(i + EMBEDDING_BATCH_SIZE, tousLesBlocs.size()));

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

			repository.saveAll(embeddings);
			embeddings.clear();
		}

		LocalDateTime now = LocalDateTime.now();
		for (PropositionThese sujet : sujets) {
			sujet.setDateIndexationScaleway(now);
		}
		sujetRepository.saveAll(sujets);

		log.info("{} embeddings sauvegardés pour {} sujets", tousLesBlocs.size(), sujets.size());
	}
}
