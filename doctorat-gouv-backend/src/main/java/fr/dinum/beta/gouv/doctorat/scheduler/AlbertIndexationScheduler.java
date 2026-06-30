package fr.dinum.beta.gouv.doctorat.scheduler;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.service.TheseIndexationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Planificateur d'indexation des sujets de thèse dans Albert.
 * S'exécute selon la cron configurée (albert.scheduler.cron).
 *
 * CONFORMITÉ RGPD :
 * - Les logs se limitent à des compteurs et des identifiants techniques.
 * - Aucune donnée à caractère personnel n'est tracée dans les logs du scheduler.
 */
@Component
@ConditionalOnProperty(name = "albert.scheduler.enabled", havingValue = "true", matchIfMissing = false)
public class AlbertIndexationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlbertIndexationScheduler.class);

    private final PropositionTheseRepository repository;
    private final TheseIndexationService indexationService;
    private final Executor executor;

    @PersistenceContext
    private EntityManager entityManager;

    public AlbertIndexationScheduler(PropositionTheseRepository repository,
                                     TheseIndexationService indexationService,
                                     @Qualifier("indexationTaskExecutor") Executor executor) {
        this.repository = repository;
        this.indexationService = indexationService;
        this.executor = executor;
    }

    @Scheduled(cron = "${albert.scheduler.cron}")
    public void indexerSujets() {
        List<PropositionThese> sujets = repository.findNeedingIndexation();
        log.info("Indexation planifiée : {} sujets nécessitent une mise à jour dans Albert", sujets.size());
        indexerLot(sujets);
    }

    public void indexerLot(List<PropositionThese> sujets) {
        List<CompletableFuture<Void>> futures = sujets.stream()
            .map(sujet -> CompletableFuture.runAsync(() -> {
                try {
                    indexationService.indexerDocumentSiNecessaire(sujet);
                } catch (Exception e) {
                    log.error("Erreur lors de l'indexation du sujet {} : {}",
                            sujet.getId(), e.getMessage(), e);
                }
            }, executor))
            .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

}
