package fr.dinum.beta.gouv.doctorat.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;

public class UtilisateurDto {

    private String id;
    private String email;
    private String prenom;
    private String nom;
    private RoleUtilisateur role;
    private SourceAuth sourceAuth;
    private Boolean actif;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateCreation;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public RoleUtilisateur getRole() {
        return role;
    }

    public void setRole(RoleUtilisateur role) {
        this.role = role;
    }

    public SourceAuth getSourceAuth() {
        return sourceAuth;
    }

    public void setSourceAuth(SourceAuth sourceAuth) {
        this.sourceAuth = sourceAuth;
    }

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }
}
