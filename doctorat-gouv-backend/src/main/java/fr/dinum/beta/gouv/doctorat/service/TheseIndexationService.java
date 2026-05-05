package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

@Service
public class TheseIndexationService {

	private final AlbertDocumentService documentService;
	private final PropositionTheseRepository repository;

	public TheseIndexationService(AlbertDocumentService documentService, PropositionTheseRepository repository) {
		this.documentService = documentService;
		this.repository = repository;
	}

	@Transactional
	public void indexerDocumentSiNecessaire(PropositionThese sujet) {

		// Cas 1 — Nouveau sujet
		if (sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() == null) {
			Long idAlbert = documentService.createDocument(sujet);
			sujet.setAlbertDocumentId(String.valueOf(idAlbert));
			sujet.setDateIndexationAlbert(LocalDateTime.now());
			repository.save(sujet);
			return;
		}

		// Cas 2 — Sujet désactivé
		if (!sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() != null) {
			// suppression du document → étape 3.4
			return;
		}

		// Cas 3 — Sujet actif + mis à jour
		if (sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() != null
				&& sujet.getDateMaj().isAfter(sujet.getDateIndexationAlbert())) {

			// mise à jour → suppression + réindexation des chunks
			return;
		}
	}
}
