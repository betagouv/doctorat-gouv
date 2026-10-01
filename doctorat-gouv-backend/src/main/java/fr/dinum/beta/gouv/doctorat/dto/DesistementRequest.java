package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de requête pour le désistement (« Je ne suis plus intéressé »)
 * d'un candidat sur une demande acceptée.
 */
public class DesistementRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long demandeId;

    @NotBlank(message = "Le motif est obligatoire")
    @Size(max = 2000, message = "Le motif ne doit pas dépasser 2000 caractères")
    private String motif;

    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }

    public String getMotif() { return motif; }
    public void setMotif(String motif) { this.motif = motif; }
}
