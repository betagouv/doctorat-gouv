package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * Trace l'envoi d'un mail d'invitation (template Brevo 64) a un directeur de these (DT).
 * Un seul enregistrement par e-mail normalise : garantit qu'un DT ne recoit
 * qu'une seule invitation, meme s'il a plusieurs sujets actifs.
 */
@Entity
@Table(name = "invitation_directeur_these")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitationDirecteurThese {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(length = 100)
    private String nom;

    @Column(length = 100)
    private String prenom;

    @Column(length = 20)
    private String civilite;

    @Column(name = "date_envoi", nullable = false)
    private LocalDateTime dateEnvoi;

    @Column(name = "proposition_these_id")
    private Long propositionTheseId;

    @Column(name = "source_plateforme", length = 20)
    private String sourcePlateforme;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = fr.dinum.beta.gouv.doctorat.util.EmailUtils.normalize(email);
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

    public LocalDateTime getDateEnvoi() {
        return dateEnvoi;
    }

    public void setDateEnvoi(LocalDateTime dateEnvoi) {
        this.dateEnvoi = dateEnvoi;
    }

    public Long getPropositionTheseId() {
        return propositionTheseId;
    }

    public void setPropositionTheseId(Long propositionTheseId) {
        this.propositionTheseId = propositionTheseId;
    }

    public String getSourcePlateforme() {
        return sourcePlateforme;
    }

    public void setSourcePlateforme(String sourcePlateforme) {
        this.sourcePlateforme = sourcePlateforme;
    }
}
