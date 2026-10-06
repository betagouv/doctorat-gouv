package fr.dinum.beta.gouv.doctorat.dto;

/**
 * Données publiques du chercheur affichées sur la fiche offre d'encadrement.
 * Résolues depuis le profil du directeur de thèse (aucune colonne ajoutée
 * à proposition_these) ; repli sur les champs dénormalisés de l'offre si
 * le profil est introuvable.
 */
public class DirecteurOffreDto {

    private String civilite;
    private String prenom;
    private String nom;
    private String titre;
    private String photoUrl;
    private String orcid;
    private String expertiseMots;

    public String getCivilite() { return civilite; }
    public void setCivilite(String civilite) { this.civilite = civilite; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public String getOrcid() { return orcid; }
    public void setOrcid(String orcid) { this.orcid = orcid; }

    public String getExpertiseMots() { return expertiseMots; }
    public void setExpertiseMots(String expertiseMots) { this.expertiseMots = expertiseMots; }
}
