package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.dto.ChunkWithType;
import fr.dinum.beta.gouv.doctorat.dto.TheseSemanticDocument;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

/**
 * Service centralisé pour gérer l'indexation des sujets de thèse dans Albert.
 * Il encapsule toute la logique nécessaire pour déterminer si un sujet doit être indexé, ré-indexé ou supprimé d'Albert,
 * ainsi que pour construire le texte à indexer et interagir avec les services d'Albert pour la création de documents et de chunks.
 *
 * CONFORMITÉ RGPD :
 * - Les logs se limitent aux identifiants techniques (ID proposition, ID document Albert).
 * - Le contenu textuel des sujets (titre, résumé, mots-clés, etc.) n'est pas loggé car il peut
 *   contenir des données à caractère personnel.
 * - Le matricule (identifiant personnel du doctorant) n'est jamais loggé.
 */
@Service
public class TheseIndexationService {
	
	private static final Logger log = LoggerFactory.getLogger(TheseIndexationService.class);

	private final AlbertDocumentService documentService;
	private final AlbertChunkService chunkService;
	private final AlbertDocumentDeletionService documentDeletionService;
	private final AlbertChunkDeletionService chunkDeletionService;
	private final TheseSemanticBuilder semanticBuilder;
	private final TextChunker textChunker;
	private final PropositionTheseRepository repository;

	public TheseIndexationService(AlbertDocumentService documentService, 
			AlbertChunkService chunkService,
			TheseSemanticBuilder semanticBuilder, 
			TextChunker textChunker, 
			PropositionTheseRepository repository, 
			AlbertDocumentDeletionService documentDeletionService, 
			AlbertChunkDeletionService chunkDeletionService) {

		this.documentService = documentService;
		this.chunkService = chunkService;
		this.semanticBuilder = semanticBuilder;
		this.textChunker = textChunker;
		this.repository = repository;
		this.documentDeletionService = documentDeletionService;
		this.chunkDeletionService = chunkDeletionService;
	}

	@Transactional
	public void indexerDocumentSiNecessaire(PropositionThese sujet) {
		
		log.info("Vérification de la nécessité d’indexer le sujet ID {} dans Albert...", sujet.getId());

		// Cas 1 — Nouveau sujet
		if (sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() == null) {
			log.info("Indexation du sujet ID {} dans Albert (nouveau sujet)...", sujet.getId());
			
			// Création du document dans Albert
			Long documentId = documentService.createDocument(sujet);
			sujet.setAlbertDocumentId(String.valueOf(documentId));
			sujet.setDateIndexationAlbert(LocalDateTime.now());
			repository.save(sujet);
			
			// Indexation des chunks
            indexerChunks(documentId, sujet);
			
            return;
		}

		// Cas 2 — Sujet désactivé
		if (!sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() != null) {
			log.info("Suppression du sujet ID {} de Albert (sujet désactivé)...", sujet.getId());
		    Long documentId = Long.valueOf(sujet.getAlbertDocumentId());
		    documentDeletionService.deleteDocument(documentId);
		    sujet.setAlbertDocumentId(null);
		    sujet.setDateIndexationAlbert(null);
		    repository.save(sujet);
			return;
		}

		// Cas 3 — Sujet actif + mis à jour
		if (sujet.getActive().booleanValue() && sujet.getAlbertDocumentId() != null
				&& sujet.getDateMaj().isAfter(sujet.getDateIndexationAlbert())) {
			
			log.info("Re-indexation du sujet ID {} dans Albert (sujet mis à jour)...", sujet.getId());

			// Récupérer l’ID du document Albert
			Long documentId = Long.valueOf(sujet.getAlbertDocumentId());

		    // Suppression des anciens chunks
		    chunkDeletionService.deleteChunks(documentId);

            // Re-indexation des chunks avec les nouvelles données
		    indexerChunks(documentId, sujet);
            sujet.setDateIndexationAlbert(LocalDateTime.now());
            repository.save(sujet);
			return;
		}
	}
	
	private void indexerChunks(Long documentId, PropositionThese sujet) {

		log.info("Indexation des chunks pour le sujet ID {} dans Albert (document ID {})...", sujet.getId(),
				documentId);

		// 1. Construire le texte FR/EN à partir du sujet avec TheseSemanticBuilder
		TheseSemanticDocument semantic = semanticBuilder.build(sujet);

		// 2. Chunker avec TextChunker
		List<ChunkWithType> chunks = textChunker.chunkWithTypes(semantic.texteComplet());

		// 3. Envoyer chaque chunk à Albert
		for (ChunkWithType c : chunks) {
		    chunkService.uploadChunk(documentId, c.content(), c.type(),
		            sujet.getId(), sujet.getMatricule());
		}

		log.info("Indexation des chunks terminée pour le sujet ID {} dans Albert (document ID {})", sujet.getId(), documentId);
	}
	

}
