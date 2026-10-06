package fr.dinum.beta.gouv.doctorat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor
public class AxeDeRecherche {

    @Column(name = "axe_titre", length = 255)
    private String titre;

    @Column(name = "axe_precisions", length = 2000)
    private String precisions;

    @Column(name = "axe_contactable")
    private boolean contactable;

    @Column(name = "axe_external_id", length = 36)
    private String externalId;

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getPrecisions() { return precisions; }
    public void setPrecisions(String precisions) { this.precisions = precisions; }

    public boolean isContactable() { return contactable; }
    public void setContactable(boolean contactable) { this.contactable = contactable; }

    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
}
