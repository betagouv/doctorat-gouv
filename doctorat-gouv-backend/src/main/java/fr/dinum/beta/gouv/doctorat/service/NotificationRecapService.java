package fr.dinum.beta.gouv.doctorat.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

/**
 * Envoi du récapitulatif candidat : demandes en attente et messages non lus (template Brevo 66).
 * Respecte la préférence de fréquence stockée sur l'utilisateur.
 * La partie directeur de thèse sera ajoutée sur le GO DT.
 */
@Service
public class NotificationRecapService {

    private static final Logger log = LoggerFactory.getLogger(NotificationRecapService.class);

    private static final int BREVO_TEMPLATE_RECAP = 66;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    private String stubMailbox;

    @Value("${app.frontend.base-url:https://app.doctorat.gouv.fr}")
    private String frontendBaseUrl;

    private final UtilisateurRepository utilisateurRepository;
    private final DemandeMiseEnRelationRepository demandeRepository;
    private final MessageEchangeRepository messageRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final BrevoEmailService emailService;

    public NotificationRecapService(UtilisateurRepository utilisateurRepository,
                                    DemandeMiseEnRelationRepository demandeRepository,
                                    MessageEchangeRepository messageRepository,
                                    ProfilCandidatRepository profilCandidatRepository,
                                    BrevoEmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.demandeRepository = demandeRepository;
        this.messageRepository = messageRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.emailService = emailService;
    }

    public int envoyerRecap(FrequenceNotification frequence) {
        List<Utilisateur> destinataires = utilisateurRepository
            .findByFrequenceNotificationAndActifTrue(frequence);
        int envoyes = 0;
        for (Utilisateur u : destinataires) {
            if (u.getRole() != RoleUtilisateur.CANDIDAT) {
                continue;
            }
            try {
                if (envoyerRecapCandidat(u)) {
                    envoyes++;
                }
            } catch (Exception e) {
                log.error("Erreur récapitulatif pour l'utilisateur {}", u.getId(), e);
            }
        }
        log.info("Récapitulatif candidat {} : {} e-mail(s) envoyé(s)",
            frequence, envoyes);
        return envoyes;
    }

    private boolean envoyerRecapCandidat(Utilisateur u) {
        long nbDemandesAttente = demandeRepository.countDemandesEnAttenteCandidat(u.getId());
        long nbMessagesNonLus = messageRepository.countNonLusCandidat(u.getId());
        if (nbDemandesAttente == 0 && nbMessagesNonLus == 0) {
            return false;
        }

        String civilite = profilCandidatRepository.findByUtilisateurId(u.getId())
            .map(p -> p.getCivilite()).orElse("");

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("civilite", civilite != null ? civilite : "");
        params.put("nom", u.getNom() != null ? u.getNom() : "");
        params.put("nb_demandes_attente", nbDemandesAttente);
        params.put("nb_messages_non_lus", nbMessagesNonLus);
        params.put("lien_tableau_de_bord", frontendBaseUrl + "/tableau-de-bord-candidat");

        String destinataire = u.getEmail();
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.debug("Mode DEV : récapitulatif non envoyé (bouchon vide) pour {}", u.getId());
                return false;
            }
            destinataire = stubMailbox.trim();
        }
        try {
            emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_RECAP, params);
            log.info("Récapitulatif envoyé à {} (user {})", destinataire, u.getId());
            return true;
        } catch (Exception e) {
            log.error("Erreur envoi récapitulatif pour l'utilisateur {}", u.getId(), e);
            return false;
        }
    }
}
