package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.dto.TheseSemanticDocument;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

/**
 * Service centralisé pour gérer l’indexation des sujets de thèse dans Albert.
 */
@Service
public class TheseIndexationService {

	private final AlbertDocumentService documentService;
	private final AlbertChunkService chunkService;
	private final TheseSemanticBuilder semanticBuilder;
	private final TextChunker textChunker;
	private final PropositionTheseRepository repository;

	public TheseIndexationService(AlbertDocumentService documentService, 
			AlbertChunkService chunkService,
			TheseSemanticBuilder semanticBuilder, 
			TextChunker textChunker, 
			PropositionTheseRepository repository) {

		this.documentService = documentService;
		this.chunkService = chunkService;
		this.semanticBuilder = semanticBuilder;
		this.textChunker = textChunker;
		this.repository = repository;
	}

	@Transactional
	public void indexerDocumentSiNecessaire(PropositionThese sujet) {

		// Cas 1 — Nouveau sujet
		if (sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() == null) {
			Long documentId = documentService.createDocument(sujet);
			sujet.setAlbertDocumentId(String.valueOf(documentId));
			sujet.setDateIndexationAlbert(LocalDateTime.now());
			repository.save(sujet);
			
            indexerChunks(documentId, sujet);
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

			Long documentId = Long.valueOf(sujet.getAlbertDocumentId());

            // TODO : suppression des anciens chunks si nécessaire

            indexerChunks(documentId, sujet);
            sujet.setDateIndexationAlbert(LocalDateTime.now());
            repository.save(sujet);
			return;
		}
	}
	
	 private void indexerChunks(Long documentId, PropositionThese sujet) {

	        // 1. Construire le texte FR/EN
	        TheseSemanticDocument semantic = semanticBuilder.build(sujet);

	        // 2. Chunker avec TON TextChunker
	        List<String> chunks = textChunker.chunk(semantic.texteComplet());

	        // 3. Envoyer chaque chunk à Albert
	        for (String chunk : chunks) {
	            chunkService.uploadChunk(documentId, chunk);
	        }
	    }
}
