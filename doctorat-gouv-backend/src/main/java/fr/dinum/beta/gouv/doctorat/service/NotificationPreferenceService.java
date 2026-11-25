package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.NotificationPreferenceRequest;
import fr.dinum.beta.gouv.doctorat.dto.NotificationPreferenceResponse;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

/**
 * Socle commun candidat / directeur de thèse pour la préférence
 * de fréquence du récapitulatif des messages non lus.
 */
@Service
public class NotificationPreferenceService {

    private static final Logger log = LoggerFactory.getLogger(NotificationPreferenceService.class);

    private final UtilisateurRepository utilisateurRepository;

    public NotificationPreferenceService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public NotificationPreferenceResponse getPreference(String userId) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        NotificationPreferenceResponse response = new NotificationPreferenceResponse(
            utilisateur.getFrequenceNotification() != null
                ? utilisateur.getFrequenceNotification()
                : FrequenceNotification.JOURNALIERE);
        response.setAlertesRechercheActive(Boolean.TRUE.equals(utilisateur.getAlertesRechercheActive()));
        response.setFrequenceAlertesRecherche(
            utilisateur.getFrequenceAlertesRecherche() != null
                ? utilisateur.getFrequenceAlertesRecherche()
                : FrequenceNotification.HEBDOMADAIRE);
        return response;
    }

    public NotificationPreferenceResponse updatePreference(String userId, NotificationPreferenceRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        utilisateur.setFrequenceNotification(request.getFrequence());
        if (request.getAlertesRechercheActive() != null) {
            utilisateur.setAlertesRechercheActive(request.getAlertesRechercheActive());
        }
        if (request.getFrequenceAlertesRecherche() != null) {
            utilisateur.setFrequenceAlertesRecherche(request.getFrequenceAlertesRecherche());
        }
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);
        log.info("Préférence de notification {} mise à jour pour l'utilisateur {}",
            request.getFrequence(), userId);
        NotificationPreferenceResponse response = new NotificationPreferenceResponse(request.getFrequence());
        response.setAlertesRechercheActive(Boolean.TRUE.equals(utilisateur.getAlertesRechercheActive()));
        response.setFrequenceAlertesRecherche(
            utilisateur.getFrequenceAlertesRecherche() != null
                ? utilisateur.getFrequenceAlertesRecherche()
                : FrequenceNotification.HEBDOMADAIRE);
        return response;
    }
}
