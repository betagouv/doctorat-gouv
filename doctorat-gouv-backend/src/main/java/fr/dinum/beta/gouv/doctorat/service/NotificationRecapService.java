package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

/**
 * Envoi du récapitulatif : demandes en attente et messages non lus (template Brevo 66).
 * Respecte la préférence de fréquence stockée sur l'utilisateur.
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
    private final PropositionTheseRepository propositionTheseRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final BrevoEmailService emailService;

    public NotificationRecapService(UtilisateurRepository utilisateurRepository,
                                    DemandeMiseEnRelationRepository demandeRepository,
                                    MessageEchangeRepository messageRepository,
                                    PropositionTheseRepository propositionTheseRepository,
                                    ProfilCandidatRepository profilCandidatRepository,
                                    ProfilDirecteurTheseRepository profilDtRepository,
                                    BrevoEmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.demandeRepository = demandeRepository;
        this.messageRepository = messageRepository;
        this.propositionTheseRepository = propositionTheseRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.profilDtRepository = profilDtRepository;
        this.emailService = emailService;
    }

    public int envoyerRecap(FrequenceNotification frequence) {
        List<Utilisateur> destinataires = utilisateurRepository
            .findByFrequenceNotificationAndActifTrue(frequence);
        int envoyes = 0;
        for (Utilisateur u : destinataires) {
            try {
                if (envoyerRecapSiActivite(u)) {
                    envoyes++;
                }
            } catch (Exception e) {
                log.error("Erreur récapitulatif pour l'utilisateur {}", u.getId(), e);
            }
        }
        log.info("Récapitulatif {} : {} e-mail(s) envoyé(s) sur {} destinataire(s)",
            frequence, envoyes, destinataires.size());
        return envoyes;
    }

    private boolean envoyerRecapSiActivite(Utilisateur u) {
        long nbDemandesAttente;
        long nbMessagesNonLus;
        String lienTableauDeBord;
        String civilite;

        if (u.getRole() == RoleUtilisateur.CANDIDAT) {
            nbDemandesAttente = demandeRepository.countDemandesEnAttenteCandidat(u.getId());
            nbMessagesNonLus = messageRepository.countNonLusCandidat(u.getId());
            lienTableauDeBord = frontendBaseUrl + "/tableau-de-bord-candidat";
            civilite = profilCandidatRepository.findByUtilisateurId(u.getId())
                .map(p -> p.getCivilite()).orElse("");
        } else if (u.getRole() == RoleUtilisateur.DIRECTEUR_THESE) {
            // Un DT n'est pas lié directement aux demandes : on passe par ses sujets
            // rattachés (email / ORCID), puis les demandes déposées sur ces sujets.
            Map<Long, PropositionThese> sujets = findSujetsRattaches(u);
            if (sujets.isEmpty()) {
                return false;
            }
            List<DemandeMiseEnRelation> demandes = demandeRepository
                .findByPropositionTheseIdInOrderByUpdatedAtDesc(new ArrayList<>(sujets.keySet()));
            nbDemandesAttente = demandes.stream()
                .filter(d -> d.getStatut() == StatutDemandeMiseEnRelation.CREE
                    && !Boolean.TRUE.equals(d.getArchivee()))
                .count();
            List<Long> demandeIds = demandes.stream()
                .filter(d -> !Boolean.TRUE.equals(d.getArchivee()))
                .map(DemandeMiseEnRelation::getId)
                .toList();
            nbMessagesNonLus = demandeIds.isEmpty() ? 0
                : messageRepository.countByDemandeIdInAndAuteurIdNotAndLuFalse(demandeIds, u.getId());
            lienTableauDeBord = frontendBaseUrl + "/tableau-de-bord-directeur-these";
            civilite = profilDtRepository.findByUtilisateurId(u.getId())
                .map(p -> p.getCivilite()).orElse("");
        } else {
            return false;
        }

        if (nbDemandesAttente == 0 && nbMessagesNonLus == 0) {
            return false;
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("civilite", civilite != null ? civilite : "");
        params.put("nom", u.getNom() != null ? u.getNom() : "");
        params.put("nb_demandes_attente", nbDemandesAttente);
        params.put("nb_messages_non_lus", nbMessagesNonLus);
        params.put("lien_tableau_de_bord", lienTableauDeBord);

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

    private Map<Long, PropositionThese> findSujetsRattaches(Utilisateur utilisateur) {
        Map<Long, PropositionThese> sujets = new LinkedHashMap<>();
        String email = EmailUtils.normalize(utilisateur.getEmail());
        String orcid = profilDtRepository.findByUtilisateurId(utilisateur.getId())
            .map(p -> p.getOrcid() != null ? p.getOrcid().trim() : null)
            .filter(s -> !s.isEmpty())
            .orElse(null);
        if (email != null) {
            for (PropositionThese p : propositionTheseRepository.findByDirecteurEmail(email)) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }
        if (orcid != null) {
            for (PropositionThese p : propositionTheseRepository.findByDirecteurOrcid(orcid)) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }
        sujets.values().removeIf(p -> p.getSource() == SourceThese.DOCTORAT_GOUV);
        return sujets;
    }
}
