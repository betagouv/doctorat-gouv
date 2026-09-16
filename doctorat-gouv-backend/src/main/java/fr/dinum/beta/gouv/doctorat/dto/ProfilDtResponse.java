package fr.dinum.beta.gouv.doctorat.dto;

import java.util.List;

/**
 * DTO de réponse contenant les coordonnées du profil directeur de thèse.
 */
public class ProfilDtResponse {

    private String civilite;
    private String nom;
    private String prenom;
    private String email;
    private String photoUrl;
    private List<String> competences;
    private String orcid;
    private String etablissement;
    private String laboratoire;
    private String ecoleDoctorale;
    private String employeur;
    private String titre;
    private String precisionTitre;
    private String habilitationRecherche;

    public ProfilDtResponse() {}

    public ProfilDtResponse(String civilite, String nom, String prenom, String email) {
        this.civilite = civilite;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
    }

    public String getCivilite() { return civilite; }
    public void setCivilite(String civilite) { this.civilite = civilite; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<String> getCompetences() { return competences; }
    public void setCompetences(List<String> competences) { this.competences = competences; }

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
