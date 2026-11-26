package fr.dinum.beta.gouv.doctorat.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.AlerteDemandeMiseEnRelationService;

@Component
public class AlerteDemandeMiseEnRelationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlerteDemandeMiseEnRelationScheduler.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${alerte.demande.mise.en.relation.scheduler.cron:0 0 8 * * *}")
    private String cronExpression;

    @Value("${alerte.demande.mise.en.relation.max-per-run:0}")
    private int maxPerRun;

    private final AlerteDemandeMiseEnRelationService service;

    public AlerteDemandeMiseEnRelationScheduler(AlerteDemandeMiseEnRelationService service) {
        this.service = service;
    }

    @Scheduled(cron = "${alerte.demande.mise.en.relation.scheduler.cron:0 0 8 * * *}")
    public void envoyerAlertes() {
        if (service == null) return;
        if (!running.compareAndSet(false, true)) {
            log.info("Envoi des alertes DT sans compte deja en cours, execution ignoree");
            return;
        }
        try {
            log.info("Debut de l'envoi des alertes aux directeurs de these sans compte");
            int envoyes = service.envoyerAlertes();
            log.info("Fin de l'envoi des alertes aux directeurs de these sans compte : {} mail(s)", envoyes);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des alertes aux directeurs de these sans compte", e);
        } finally {
            running.set(false);
        }
    }


}
