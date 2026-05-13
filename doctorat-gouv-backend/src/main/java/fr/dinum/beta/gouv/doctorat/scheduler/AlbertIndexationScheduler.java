package fr.dinum.beta.gouv.doctorat.scheduler;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.service.TheseIndexationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Scheduler qui s’occupe de l’indexation automatique des sujets de thèse dans Albert. 
 * Il s’exécute toutes les 30 minutes et vérifie tous les sujets actifs pour déterminer s’ils doivent être indexés, 
 * ré-indexés ou supprimés d’Albert en fonction de leur état actuel et de leur état dans Albert.
 * 
 */
@Component
public class AlbertIndexationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlbertIndexationScheduler.class);

    private final PropositionTheseRepository repository;
    private final TheseIndexationService indexationService;
    

    @PersistenceContext
    private EntityManager entityManager;


    public AlbertIndexationScheduler(PropositionTheseRepository repository,
                                     TheseIndexationService indexationService) {
        this.repository = repository;
        this.indexationService = indexationService;
    }

    
    /**
     * Tâche planifiée qui s’exécute toutes les 60 minutes pour indexer les sujets de thèse actifs dans Albert.
     */
    @Scheduled(fixedDelay = 60 * 60 * 1000)
    public void indexerSujets() {

        int page = 0;
        int batchSize = 100;
        Page<PropositionThese> pageResult;

        do {
            pageResult = repository.findByActiveTrue(PageRequest.of(page, batchSize));

            indexerLot(pageResult.getContent());

            page++;

        } while (pageResult.hasNext());
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void indexerLot(List<PropositionThese> sujets) {
        for (PropositionThese sujet : sujets) {
            try {
                indexationService.indexerDocumentSiNecessaire(sujet);
            } catch (Exception e) {
                log.error("Erreur lors de l’indexation du sujet {} : {}", sujet.getId(), e.getMessage(), e);
            }
        }
    }

}

