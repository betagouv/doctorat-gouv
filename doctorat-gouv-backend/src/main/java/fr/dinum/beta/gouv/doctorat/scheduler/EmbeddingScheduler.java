package fr.dinum.beta.gouv.doctorat.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.EmbeddingIndexationService;

@Component
public class EmbeddingScheduler {

	private static final Logger log = LoggerFactory.getLogger(EmbeddingScheduler.class);

	private final AtomicBoolean running = new AtomicBoolean(false);

	private final EmbeddingIndexationService indexationService;

	public EmbeddingScheduler(EmbeddingIndexationService indexationService) {
		this.indexationService = indexationService;
	}

	@Scheduled(cron = "0 */2 * * * ?")
	public void indexationQuotidienne() {
		if (!running.compareAndSet(false, true)) {
			log.info("Indexation déjà en cours, exécution ignorée");
			return;
		}
		try {
			log.info("Début de l'indexation quotidienne Scaleway");
			indexationService.indexerIncremental();
		} catch (Exception e) {
			log.error("Erreur lors de l'indexation quotidienne", e);
		} finally {
			running.set(false);
		}
	}
}
