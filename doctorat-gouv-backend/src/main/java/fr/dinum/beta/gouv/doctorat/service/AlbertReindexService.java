package fr.dinum.beta.gouv.doctorat.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

/**
 * Service responsable de la réindexation complète des sujets de thèse dans Albert.
 * Supprime les anciens documents et chunks, puis ré-indexe avec les données à jour.
 *
 * CONFORMITÉ RGPD :
 * - Les logs se limitent aux identifiants techniques (ID proposition, ID document Albert) et aux compteurs.
 * - Aucune donnée à caractère personnel (matricule, titre, établissement) n'est loggée.
 */
@Service
public class AlbertReindexService {

    private static final Logger log = LoggerFactory.getLogger(AlbertReindexService.class);

    private final PropositionTheseRepository repository;
    private final AlbertDocumentDeletionService documentDeletionService;
    private final AlbertChunkDeletionService chunkDeletionService;
    private final TheseIndexationService indexationService;
    private final Executor executor;

    public AlbertReindexService(PropositionTheseRepository repository,
                                 AlbertDocumentDeletionService documentDeletionService,
                                 AlbertChunkDeletionService chunkDeletionService,
                                 TheseIndexationService indexationService,
                                 @Qualifier("indexationTaskExecutor") Executor executor) {
        this.repository = repository;
        this.documentDeletionService = documentDeletionService;
        this.chunkDeletionService = chunkDeletionService;
        this.indexationService = indexationService;
        this.executor = executor;
    }

    /**
     * Supprime tous les documents Albert existants et ré-initialise les champs
     * albertDocumentId / dateIndexationAlbert dans la BDD.
     * @return nombre de documents supprimés
     */
    @Transactional
    public int deleteAllAlbertDocuments() {
        List<PropositionThese> indexed = repository.findIndexedInAlbert();
        int count = 0;

        for (PropositionThese sujet : indexed) {
            try {
                String docId = sujet.getAlbertDocumentId();
                if (docId != null) {
                    try {
                        chunkDeletionService.deleteChunks(Long.valueOf(docId));
                    } catch (Exception e) {
                        log.warn("Impossible de supprimer les chunks du document {} : {}", docId, e.getMessage());
                    }
                    try {
                        documentDeletionService.deleteDocument(Long.valueOf(docId));
                    } catch (Exception e) {
                        log.warn("Impossible de supprimer le document {} : {}", docId, e.getMessage());
                    }
                }
                sujet.setAlbertDocumentId(null);
                sujet.setDateIndexationAlbert(null);
                repository.save(sujet);
                count++;
            } catch (Exception e) {
                log.error("Erreur lors de la suppression du document Albert pour le sujet {} : {}", sujet.getId(), e.getMessage());
            }
        }

        log.info("{} documents Albert supprimés", count);
        return count;
    }

    /**
     * Supprime tous les documents Albert puis ré-indexe tous les sujets actifs.
     * @return nombre de sujets ré-indexés
     */
    public int reindexAll() {
        log.info("Début de la réindexation complète Albert...");

        int deleted = deleteAllAlbertDocuments();

        List<PropositionThese> actives = repository.findActivePropositions();
        AtomicInteger reindexed = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = actives.stream()
            .map(sujet -> CompletableFuture.runAsync(() -> {
                try {
                    indexationService.indexerDocumentSiNecessaire(sujet);
                    reindexed.incrementAndGet();
                } catch (Exception e) {
                    log.error("Erreur lors de la réindexation du sujet {}", sujet.getId(), e);
                }
            }, executor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Réindexation terminée : {} supprimés, {} ré-indexés", deleted, reindexed.get());
        return reindexed.get();
    }
}
