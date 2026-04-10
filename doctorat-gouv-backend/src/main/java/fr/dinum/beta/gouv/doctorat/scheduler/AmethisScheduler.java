package fr.dinum.beta.gouv.doctorat.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.AmethisApiService;

@Component
public class AmethisScheduler {

    private static final Logger log = LoggerFactory.getLogger(AmethisScheduler.class);

    private final AmethisApiService amethisApiService;

    public AmethisScheduler(AmethisApiService amethisApiService) {
        this.amethisApiService = amethisApiService;
    }

    // @Scheduled(cron = "${amethis.scheduler.cron}")
    public void runImport() {
        log.info("Début récupération des sujets de thèse depuis AMETHIS");
        String result = amethisApiService.importAndSavePropositionsFromAmethis();
        log.info("Fin de récupération des sujets de thèse depuis AMETHIS");
        log.trace("Résultat de l’import AMETHIS : " + result);
    }
}

