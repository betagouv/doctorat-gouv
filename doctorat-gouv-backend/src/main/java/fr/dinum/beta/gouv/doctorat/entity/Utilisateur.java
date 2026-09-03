package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import fr.dinum.beta.gouv.doctorat.enums.DemarcheType;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Entité JPA représentant un utilisateur de la plateforme (candidat, directeur, école doctorale, labo, admin).
 * Identifiant : UUID généré automatiquement. Le mot de passe est stocké hashé (BCrypt).
 */
@Entity
@Table(name = "utilisateur", uniqueConstraints = {
    @UniqueConstraint(columnNames = "email")
})
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 100)
    private String nom;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoleUtilisateur role;

    @Column(nullable = false)
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceAuth sourceAuth;

    @Column(name = "france_connect_id", length = 255)
    private String franceConnectId;

    // --- Coordonnées d'inscription (profil candidat) ---

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DemarcheType demarche;

    @Column(length = 20)
    private String civilite;

    @Column(length = 50)
    private String situation;

    @Column(length = 20)
    private String telephone;

    @Column(name = "master_confirme")
    private Boolean masterConfirme;

    // --- Photo ---

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    // --- Compétences ---

    @ElementCollection
    @CollectionTable(name = "utilisateur_competences", joinColumns = @JoinColumn(name = "utilisateur_id"))
    @Column(name = "competence", length = 255)
    private List<String> competences = new ArrayList<>();

    // --- Nombre de candidatures ---

    @Column(name = "nb_candidatures")
    private Integer nbCandidatures = 0;

    // --- Liens vers les fichiers uploadés ---

    @Column(name = "cv_filename", length = 500)
    private String cvFilename;

    @ElementCollection
    @CollectionTable(name = "utilisateur_pieces", joinColumns = @JoinColumn(name = "utilisateur_id"))
    @Column(name = "piece_path", length = 500)
    private List<String> piecesFilenames = new ArrayList<>();

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "date_modification")
    private LocalDateTime dateModification;

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

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
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

    public Boolean getActif() {
        return actif;
    }

    public void setActif(Boolean actif) {
        this.actif = actif;
    }

    public SourceAuth getSourceAuth() {
        return sourceAuth;
    }

    public void setSourceAuth(SourceAuth sourceAuth) {
        this.sourceAuth = sourceAuth;
    }

    public String getFranceConnectId() {
        return franceConnectId;
    }

    public void setFranceConnectId(String franceConnectId) {
        this.franceConnectId = franceConnectId;
    }

    public DemarcheType getDemarche() {
        return demarche;
    }

    public void setDemarche(DemarcheType demarche) {
        this.demarche = demarche;
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

    public String getCvFilename() {
        return cvFilename;
    }

    public void setCvFilename(String cvFilename) {
        this.cvFilename = cvFilename;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public List<String> getCompetences() {
        return competences;
    }

    public void setCompetences(List<String> competences) {
        this.competences = competences;
    }

    public Integer getNbCandidatures() {
        return nbCandidatures;
    }

    public void setNbCandidatures(Integer nbCandidatures) {
        this.nbCandidatures = nbCandidatures;
    }

    public List<String> getPiecesFilenames() {
        return piecesFilenames;
    }

    public void setPiecesFilenames(List<String> piecesFilenames) {
        this.piecesFilenames = piecesFilenames;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public LocalDateTime getDateModification() {
        return dateModification;
    }

    public void setDateModification(LocalDateTime dateModification) {
        this.dateModification = dateModification;
    }
}
