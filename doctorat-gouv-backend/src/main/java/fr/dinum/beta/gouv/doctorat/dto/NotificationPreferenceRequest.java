package fr.dinum.beta.gouv.doctorat.dto;

import fr.dinum.beta.gouv.doctorat.enums.FrequenceNotification;
import jakarta.validation.constraints.NotNull;

/**
 * Socle commun : mise à jour de la fréquence du récapitulatif des messages non lus.
 * Utilisé par l'espace candidat et l'espace directeur de thèse.
 */
public class NotificationPreferenceRequest {

    @NotNull(message = "La fréquence est obligatoire")
    private FrequenceNotification frequence;

    public FrequenceNotification getFrequence() { return frequence; }
    public void setFrequence(FrequenceNotification frequence) { this.frequence = frequence; }
}
