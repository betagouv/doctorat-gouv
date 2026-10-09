package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Message échangé entre un candidat et un directeur de thèse
 * à propos d'une demande de mise en relation acceptée.
 * Une demande acceptée correspond à un fil de discussion unique.
 */
@Entity
@Table(name = "message_echange")
public class MessageEchange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "demande_id", nullable = false)
    private Long demandeId;

    @Column(name = "auteur_id", nullable = false)
    private String auteurId;

    @Convert(converter = fr.dinum.beta.gouv.doctorat.security.MessageContenuConverter.class)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String contenu;

    @Column(nullable = false)
    private Boolean lu = Boolean.FALSE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public MessageEchange() {
    }

    public MessageEchange(Long demandeId, String auteurId, String contenu) {
        this.demandeId = demandeId;
        this.auteurId = auteurId;
        this.contenu = contenu;
        this.lu = Boolean.FALSE;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDemandeId() {
        return demandeId;
    }

    public void setDemandeId(Long demandeId) {
        this.demandeId = demandeId;
    }

    public String getAuteurId() {
        return auteurId;
    }

    public void setAuteurId(String auteurId) {
        this.auteurId = auteurId;
    }

    public String getContenu() {
        return contenu;
    }

    public void setContenu(String contenu) {
        this.contenu = contenu;
    }

    public Boolean getLu() {
        return lu;
    }

    public void setLu(Boolean lu) {
        this.lu = lu;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
