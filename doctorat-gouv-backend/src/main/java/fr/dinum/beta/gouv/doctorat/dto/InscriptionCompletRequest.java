package fr.dinum.beta.gouv.doctorat.dto;

import fr.dinum.beta.gouv.doctorat.enums.DemarcheType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO représentant les coordonnées envoyées (partie JSON) à l'endpoint
 * POST /api/inscription/complet, accompagnées des fichiers uploadés.
 */
public class InscriptionCompletRequest {

    @NotNull(message = "La démarche est obligatoire")
    private DemarcheType demarche;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100, message = "Le prénom ne doit pas dépasser 100 caractères")
    private String prenom;

    private String civilite;

    @NotBlank(message = "La situation actuelle est obligatoire")
    private String situation;

    @NotBlank(message = "L'e-mail est obligatoire")
    @Email(message = "L'e-mail n'est pas valide")
    private String email;

    @Size(max = 20, message = "Le téléphone ne doit pas dépasser 20 caractères")
    private String telephone;

    @NotNull(message = "La confirmation du master est obligatoire")
    private Boolean masterConfirme;

    /** Optionnel : si absent, un mot de passe temporaire est généré côté serveur. */
    @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères")
    private String motDePasse;

    public DemarcheType getDemarche() {
        return demarche;
    }

    public void setDemarche(DemarcheType demarche) {
        this.demarche = demarche;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getCivilite() {
        return civilite;
    }

    public void setCivilite(String civilite) {
        this.civilite = civilite;
    }

    public String getSituation() {
        return situation;
    }

    public void setSituation(String situation) {
        this.situation = situation;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public Boolean getMasterConfirme() {
        return masterConfirme;
    }

    public void setMasterConfirme(Boolean masterConfirme) {
        this.masterConfirme = masterConfirme;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }
}
