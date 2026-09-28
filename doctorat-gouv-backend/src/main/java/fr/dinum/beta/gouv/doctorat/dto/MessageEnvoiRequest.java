package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requête pour envoyer un message sur une demande acceptée.
 * Les paramètres transitent dans le corps de la requête (POST).
 */
public class MessageEnvoiRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long demandeId;

    @NotBlank(message = "Le message est obligatoire")
    @Size(max = 2000, message = "Le message ne doit pas dépasser 2000 caractères")
    private String contenu;

    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }
}
