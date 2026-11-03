package fr.dinum.beta.gouv.doctorat.scheduler;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.InvitationDirecteurTheseService;

/**
 * Planificateur d'envoi des invitations aux directeurs de these (DT).
 * S'execute chaque jour a 20h00 (configurable via invitation.dt.cron).
 */
@Component
public class InvitationDirecteurTheseScheduler {

    private static final Logger log = LoggerFactory.getLogger(InvitationDirecteurTheseScheduler.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Value("${invitation.dt.max-per-run:0}")
    private int maxPerRun;

    private final InvitationDirecteurTheseService service;

    public InvitationDirecteurTheseScheduler(InvitationDirecteurTheseService service) {
        this.service = service;
    }

    @Scheduled(cron = "${invitation.dt.cron:0 0 20 * * *}")
    public void envoyerInvitations() {
        if (!running.compareAndSet(false, true)) {
            log.info("Envoi des invitations DT deja en cours, execution ignoree");
            return;
        }
        try {
            log.info("Debut de l'envoi des invitations aux directeurs de these");
            int envoyes = service.envoyerInvitations(maxPerRun);
            log.info("Fin de l'envoi des invitations aux directeurs de these : {} mail(s)", envoyes);
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des invitations aux directeurs de these", e);
        } finally {
            running.set(false);
        }
    }
}
