package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requête pour refuser une demande de mise en relation.
 * L'identifiant transite dans le corps de la requête (POST).
 */
public class RefuserDemandeRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long id;

    @NotBlank(message = "L'explication du refus est obligatoire")
    @Size(max = 2000, message = "L'explication ne doit pas dépasser 2000 caractères")
    private String explication;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getExplication() { return explication; }
    public void setExplication(String explication) { this.explication = explication; }
}