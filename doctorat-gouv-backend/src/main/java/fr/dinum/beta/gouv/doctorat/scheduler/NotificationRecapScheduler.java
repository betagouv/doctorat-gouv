package fr.dinum.beta.gouv.doctorat.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;
import fr.dinum.beta.gouv.doctorat.service.NotificationRecapService;

@Component
@ConditionalOnProperty(name = "notifications.recap.enabled", havingValue = "true", matchIfMissing = true)
public class NotificationRecapScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationRecapScheduler.class);

    private final NotificationRecapService recapService;

    public NotificationRecapScheduler(NotificationRecapService recapService) {
        this.recapService = recapService;
    }

    @Scheduled(cron = "${notifications.recap.daily.cron:0 0 18 * * *}")
    public void runQuotidien() {
        log.info("Début récapitulatif quotidien des notifications");
        recapService.envoyerRecap(FrequenceNotification.JOURNALIERE);
        log.info("Fin récapitulatif quotidien des notifications");
    }

    @Scheduled(cron = "${notifications.recap.weekly.cron:0 0 8 * * MON}")
    public void runHebdomadaire() {
        log.info("Début récapitulatif hebdomadaire des notifications");
        recapService.envoyerRecap(FrequenceNotification.HEBDOMADAIRE);
        log.info("Fin récapitulatif hebdomadaire des notifications");
    }
}
