package fr.dinum.beta.gouv.doctorat.dto;

import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;

/**
 * Socle commun : fréquence du récapitulatif des messages non lus.
 * Utilisé par l'espace candidat et l'espace directeur de thèse.
 */
public class NotificationPreferenceResponse {

    private FrequenceNotification frequence;
    private Boolean alertesRechercheActive;
    private FrequenceNotification frequenceAlertesRecherche;

    public NotificationPreferenceResponse() {}

    public NotificationPreferenceResponse(FrequenceNotification frequence) {
        this.frequence = frequence;
    }

    public FrequenceNotification getFrequence() { return frequence; }
    public void setFrequence(FrequenceNotification frequence) { this.frequence = frequence; }

    public Boolean getAlertesRechercheActive() { return alertesRechercheActive; }
    public void setAlertesRechercheActive(Boolean alertesRechercheActive) { this.alertesRechercheActive = alertesRechercheActive; }

    public FrequenceNotification getFrequenceAlertesRecherche() { return frequenceAlertesRecherche; }
    public void setFrequenceAlertesRecherche(FrequenceNotification frequenceAlertesRecherche) { this.frequenceAlertesRecherche = frequenceAlertesRecherche; }
}
