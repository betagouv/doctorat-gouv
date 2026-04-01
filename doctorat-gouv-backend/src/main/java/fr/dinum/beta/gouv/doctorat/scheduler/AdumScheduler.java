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
	 * 	Méthode planifiée pour exécuter l'import des sujets de thèse depuis ADUM selon l'expression cron définie dans les propriétés.
	 */
	@Scheduled(cron = "${adum.scheduler.cron}")
	public void runImport() {
		log.info("Début récupération des sujets de thèse depuis ADUM");
		String result = adumApiService.importAndSavePropositionsFromAdum();
		log.info("Fin de récupération des sujets de thèse depuis ADUM");
		log.trace("Résultat de l’export : " + result);
	}
	
	/**
	 * Méthode planifiée pour exécuter l'import des sujets de thèse depuis ADUM pour l'année n-1. 
	 */
	@Scheduled(cron = "${adum.scheduler.previous-year.cron}")
	public void runImportPreviousYear() {
	    int originalYear = adumApiService.getProperties().getYear();
	    int previousYear = originalYear - 1 ;

	    log.info("Début récupération des sujets de thèse depuis ADUM pour l'année {}", previousYear);

	    // On change temporairement l'année
	    adumApiService.getProperties().setYear(previousYear);

	    String result = adumApiService.importAndSavePropositionsFromAdum();

	    // On remet l'année d'origine
	    adumApiService.getProperties().setYear(originalYear);

	    log.info("Fin de récupération des sujets de thèse depuis ADUM pour l'année {}", previousYear);
	    log.trace("Résultat de l’export N-1 : " + result);
	}
	
	/**
	 * Méthode planifiée pour exécuter l'import des sujets de thèse depuis ADUM pour l'année n+1. 
	 */
	@Scheduled(cron = "${adum.scheduler.next-year.cron}")
	public void runImportNextYear() {
	    int originalYear = adumApiService.getProperties().getYear();
	    int nextYear = originalYear + 1 ;

	    log.info("Début récupération des sujets de thèse depuis ADUM pour l'année {}", nextYear);

	    // On change temporairement l'année
	    adumApiService.getProperties().setYear(nextYear);

	    String result = adumApiService.importAndSavePropositionsFromAdum();

	    // On remet l'année d'origine
	    adumApiService.getProperties().setYear(originalYear);

	    log.info("Fin de récupération des sujets de thèse depuis ADUM pour l'année {}", nextYear);
	    log.trace("Résultat de l’export N-1 : " + result);
	}

}
