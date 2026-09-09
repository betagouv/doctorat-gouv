package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;

@Entity
@Table(name = "demande_mise_en_relation", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "candidat_id", "proposition_these_id" })
})
public class DemandeMiseEnRelation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "candidat_id", nullable = false)
    private String candidatId;

    @Column(name = "proposition_these_id", nullable = false)
    private Long propositionTheseId;

    @Column(columnDefinition = "TEXT")
    private String motivations;

    @Column(name = "rgpd_consent", nullable = false)
    private Boolean rgpdConsent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemandeMiseEnRelation statut;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DemandeMiseEnRelation() {
    }

    public DemandeMiseEnRelation(String candidatId, Long propositionTheseId, String motivations,
            Boolean rgpdConsent, StatutDemandeMiseEnRelation statut) {
        this.candidatId = candidatId;
        this.propositionTheseId = propositionTheseId;
        this.motivations = motivations;
        this.rgpdConsent = rgpdConsent;
        this.statut = statut;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCandidatId() {
        return candidatId;
    }

    public void setCandidatId(String candidatId) {
        this.candidatId = candidatId;
    }

    public Long getPropositionTheseId() {
        return propositionTheseId;
    }

    public void setPropositionTheseId(Long propositionTheseId) {
        this.propositionTheseId = propositionTheseId;
    }

    public String getMotivations() {
        return motivations;
    }

    public void setMotivations(String motivations) {
        this.motivations = motivations;
    }

    public Boolean getRgpdConsent() {
        return rgpdConsent;
    }

    public void setRgpdConsent(Boolean rgpdConsent) {
        this.rgpdConsent = rgpdConsent;
    }

    public StatutDemandeMiseEnRelation getStatut() {
        return statut;
    }

    public void setStatut(StatutDemandeMiseEnRelation statut) {
        this.statut = statut;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
