package fr.dinum.beta.gouv.doctorat.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationRequest;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class DemandeMiseEnRelationService {

    private static final Logger log = LoggerFactory.getLogger(DemandeMiseEnRelationService.class);

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private final PropositionTheseService propositionTheseService;
    private final UtilisateurRepository utilisateurRepository;
    private final BrevoEmailService emailService;

    public DemandeMiseEnRelationService(
            PropositionTheseService propositionTheseService,
            UtilisateurRepository utilisateurRepository,
            BrevoEmailService emailService) {
        this.propositionTheseService = propositionTheseService;
        this.utilisateurRepository = utilisateurRepository;
        this.emailService = emailService;
    }

    public void traiterDemande(String userId, DemandeMiseEnRelationRequest request) {
        // 1 - Récupérer l'utilisateur candidat
        Utilisateur candidat = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        // 2 - Récupérer la proposition de thèse
        PropositionTheseDto these = propositionTheseService.findById(request.getIdPropositionThese());

        // 3 - Envoyer les emails
        if (mailEnabled) {
            envoyerMailEncadrant(candidat, these, request);
            envoyerMailCandidat(candidat, these);
        } else {
            log.warn("Mode DEV : mails désactivés");
        }
    }

    private void envoyerMailEncadrant(Utilisateur candidat, PropositionTheseDto these, DemandeMiseEnRelationRequest request) {
        log.info("Préparation du mail de demande de mise en relation pour l'encadrant");

        Map<String, Object> params = new HashMap<>();
        params.put("prenom_candidat", candidat.getPrenom());
        params.put("nom_candidat", candidat.getNom());
        params.put("email_candidat", candidat.getEmail());
        params.put("titre_sujet", these.getTheseTitre());
        params.put("motivation", request.getMotivations());
        params.put("nom_plateforme", "Doctorat.gouv");

        String urlSujet = String.format("https://app.doctorat.gouv.fr/proposition?id=%s", these.getId());
        params.put("url_sujet", urlSujet);

        try {
            if (isValidEmail(these.getDirectionTheseEmail())) {
                emailService.sendTemplateEmail(these.getDirectionTheseEmail(), 31, params);
                log.info("Mail de demande de mise en relation envoyé à l'encadrant {}", these.getDirectionTheseEmail());
            } else {
                log.warn("Email encadrant invalide ou absent pour la thèse {}", these.getId());
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de l'envoi du mail à l'encadrant", e);
        }
    }

    private void envoyerMailCandidat(Utilisateur candidat, PropositionTheseDto these) {
        log.info("Préparation du mail de confirmation pour le candidat");

        Map<String, Object> params = new HashMap<>();
        params.put("prenom", candidat.getPrenom());
        params.put("titre_sujet", these.getTheseTitre());
        params.put("nom_plateforme", "Doctorat.gouv");
        params.put("url_sujet", String.format("https://app.doctorat.gouv.fr/proposition?id=%s", these.getId()));

        try {
            if (isValidEmail(candidat.getEmail())) {
                emailService.sendTemplateEmail(candidat.getEmail(), 32, params);
                log.info("Mail de confirmation envoyé au candidat {}", candidat.getEmail());
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de l'envoi du mail de confirmation au candidat", e);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isBlank() && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
