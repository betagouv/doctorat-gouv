package fr.dinum.beta.gouv.doctorat.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de requête pour la mise à jour des coordonnées du profil directeur de thèse.
 */
public class ProfilDtUpdateRequest {

    private String civilite;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String prenom;

    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "L'e-mail n'est pas valide")
    private String email;

    private List<String> competences;

    @Size(max = 30, message = "L'identifiant ORCID ne doit pas dépasser 30 caractères")
    private String orcid;

    @Size(max = 255, message = "L'établissement ne doit pas dépasser 255 caractères")
    private String etablissement;

    @Size(max = 255, message = "Le laboratoire ne doit pas dépasser 255 caractères")
    private String laboratoire;

    @Size(max = 255, message = "L'école doctorale ne doit pas dépasser 255 caractères")
    private String ecoleDoctorale;

    @Size(max = 255, message = "L'employeur ne doit pas dépasser 255 caractères")
    private String employeur;

    @Size(max = 100, message = "Le titre ne doit pas dépasser 100 caractères")
    private String titre;

    @Size(max = 255, message = "La précision sur le titre ne doit pas dépasser 255 caractères")
    private String precisionTitre;

    @Size(max = 30, message = "L'habilitation ne doit pas dépasser 30 caractères")
    private String habilitationRecherche;

    public String getCivilite() { return civilite; }
    public void setCivilite(String civilite) { this.civilite = civilite; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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
