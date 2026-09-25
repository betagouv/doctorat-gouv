package fr.dinum.beta.gouv.doctorat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de requête pour télécharger un fichier du candidat (CV ou pièce).
 * Les paramètres transitent dans le corps de la requête (POST).
 */
public class DemandeFichierRequest {

    @NotNull(message = "L'identifiant de la demande est obligatoire")
    private Long demandeId;

    @NotBlank(message = "Le type de fichier est obligatoire (cv ou piece)")
    private String type;

    private Integer index;

    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Integer getIndex() { return index; }
    public void setIndex(Integer index) { this.index = index; }
}
