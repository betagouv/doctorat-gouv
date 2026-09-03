package fr.dinum.beta.gouv.doctorat.dto;

import java.util.List;

/**
 * DTO de réponse contenant les coordonnées du profil candidat.
 */
public class ProfilResponse {

    private String civilite;
    private String nom;
    private String prenom;
    private String situation;
    private String email;
    private String telephone;
    private String photoUrl;
    private List<String> competences;
    private int nbCandidatures;

    public ProfilResponse() {}

    public ProfilResponse(String civilite, String nom, String prenom, String situation, String email, String telephone) {
        this.civilite = civilite;
        this.nom = nom;
        this.prenom = prenom;
        this.situation = situation;
        this.email = email;
        this.telephone = telephone;
    }

    public String getCivilite() { return civilite; }
    public void setCivilite(String civilite) { this.civilite = civilite; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getSituation() { return situation; }
    public void setSituation(String situation) { this.situation = situation; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public List<String> getCompetences() { return competences; }
    public void setCompetences(List<String> competences) { this.competences = competences; }

    public int getNbCandidatures() { return nbCandidatures; }
    public void setNbCandidatures(int nbCandidatures) { this.nbCandidatures = nbCandidatures; }
}
