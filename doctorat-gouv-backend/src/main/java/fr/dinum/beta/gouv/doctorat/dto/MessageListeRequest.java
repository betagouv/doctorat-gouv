package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour lister les messages d'une demande.
 * L'identifiant transite dans le corps de la requête (POST).
 */
public class MessageListeRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long demandeId;

    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }
}
