package fr.dinum.beta.gouv.doctorat.dto;

import java.time.LocalDateTime;

public class TableauDeBordCandidatItemDto {

    private Long id;
    private Long propositionTheseId;
    private String titreSujet;
    private String etablissement;
    private String encadrantNom;
    private LocalDateTime dateContact;
    private String statut;
    private boolean archivee;

    public TableauDeBordCandidatItemDto() {
    }

    public TableauDeBordCandidatItemDto(Long id, Long propositionTheseId, String titreSujet,
            String etablissement, String encadrantNom, LocalDateTime dateContact, String statut,
            boolean archivee) {
        this.id = id;
        this.propositionTheseId = propositionTheseId;
        this.titreSujet = titreSujet;
        this.etablissement = etablissement;
        this.encadrantNom = encadrantNom;
        this.dateContact = dateContact;
        this.statut = statut;
        this.archivee = archivee;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPropositionTheseId() {
        return propositionTheseId;
    }

    public void setPropositionTheseId(Long propositionTheseId) {
        this.propositionTheseId = propositionTheseId;
    }

    public String getTitreSujet() {
        return titreSujet;
    }

    public void setTitreSujet(String titreSujet) {
        this.titreSujet = titreSujet;
    }

    public String getEtablissement() {
        return etablissement;
    }

    public void setEtablissement(String etablissement) {
        this.etablissement = etablissement;
    }

    public String getEncadrantNom() {
        return encadrantNom;
    }

    public void setEncadrantNom(String encadrantNom) {
        this.encadrantNom = encadrantNom;
    }

    public LocalDateTime getDateContact() {
        return dateContact;
    }

    public void setDateContact(LocalDateTime dateContact) {
        this.dateContact = dateContact;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public boolean isArchivee() {
        return archivee;
    }

    public void setArchivee(boolean archivee) {
        this.archivee = archivee;
    }
}
