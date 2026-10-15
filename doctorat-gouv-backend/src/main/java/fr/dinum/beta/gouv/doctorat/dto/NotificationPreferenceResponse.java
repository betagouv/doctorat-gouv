package fr.dinum.beta.gouv.doctorat.dto;

import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;

/**
 * Socle commun : fréquence du récapitulatif des messages non lus.
 * Utilisé par l'espace candidat et l'espace directeur de thèse.
 */
public class NotificationPreferenceResponse {

    private FrequenceNotification frequence;

    public NotificationPreferenceResponse() {}

    public NotificationPreferenceResponse(FrequenceNotification frequence) {
        this.frequence = frequence;
    }

    public FrequenceNotification getFrequence() { return frequence; }
    public void setFrequence(FrequenceNotification frequence) { this.frequence = frequence; }
}
