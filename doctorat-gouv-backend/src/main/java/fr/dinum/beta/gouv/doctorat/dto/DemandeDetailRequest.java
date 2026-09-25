package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour le détail d'une demande de mise en relation.
 * L'identifiant transite dans le corps de la requête (POST).
 */
public class DemandeDetailRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long id;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
}
