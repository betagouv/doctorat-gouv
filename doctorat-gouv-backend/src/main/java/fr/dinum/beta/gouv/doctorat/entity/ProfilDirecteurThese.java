package fr.dinum.beta.gouv.doctorat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * Entité JPA contenant les attributs spécifiques au profil directeur de thèse.
 * Liée à Utilisateur par héritage par jointure (1:1, même ID).
 */
@Entity
@Table(name = "profil_directeur_these")
@NoArgsConstructor
@AllArgsConstructor
public class ProfilDirecteurThese {

    @Id
    private String id;

    @OneToOne
    @MapsId
    @JoinColumn(name = "id")
    private Utilisateur utilisateur;

    @Column(length = 20)
    private String civilite;

    @Column(length = 30)
    private String orcid;

    @Column(length = 255)
    private String etablissement;

    @Column(length = 255)
    private String laboratoire;

    @Column(length = 255)
    private String ecoleDoctorale;

    @Column(length = 255)
    private String employeur;

    @Column(length = 100)
    private String titre;

    @Column(length = 255)
    private String precisionTitre;

    @Column(length = 30)
    private String habilitationRecherche;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }

    public String getCivilite() { return civilite; }
    public void setCivilite(String civilite) { this.civilite = civilite; }

    public String getOrcid() { return orcid; }
    public void setOrcid(String orcid) { this.orcid = orcid; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getLaboratoire() { return laboratoire; }
    public void setLaboratoire(String laboratoire) { this.laboratoire = laboratoire; }

    public String getEcoleDoctorale() { return ecoleDoctorale; }
    public void setEcoleDoctorale(String ecoleDoctorale) { this.ecoleDoctorale = ecoleDoctorale; }

    public String getEmployeur() { return employeur; }
    public void setEmployeur(String employeur) { this.employeur = employeur; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getPrecisionTitre() { return precisionTitre; }
    public void setPrecisionTitre(String precisionTitre) { this.precisionTitre = precisionTitre; }

    public String getHabilitationRecherche() { return habilitationRecherche; }
    public void setHabilitationRecherche(String habilitationRecherche) { this.habilitationRecherche = habilitationRecherche; }
}
