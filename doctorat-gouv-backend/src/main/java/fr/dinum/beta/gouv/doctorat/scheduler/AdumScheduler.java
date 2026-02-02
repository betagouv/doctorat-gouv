package fr.dinum.beta.gouv.doctorat.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.AdumApiService;

/**
 * Scheduler pour l'import des sujets de thèse depuis ADUM.
 */
@Component
public class AdumScheduler {
	
	private static final Logger log = LoggerFactory.getLogger(AdumScheduler.class);

	@Value("${adum.scheduler.cron}")
	private String cronExpression;

	private final AdumApiService adumApiService;

	public AdumScheduler(AdumApiService adumApiService) {
		this.adumApiService = adumApiService;
	}

	/**
	 * 	Méthode planifiée pour exécuter l'import des sujets de thèse depuis ADUM.
	 */
	@Scheduled(cron = "${adum.scheduler.cron}")
	public void runImport() {
		log.info("Début récupération des sujets de thèse depuis ADUM");
		String result = adumApiService.importAndSavePropositionsFromAdum();
		log.info("Fin de récupération des sujets de thèse depuis ADUM");
		log.trace("Résultat de l’export : " + result);
	}
}
