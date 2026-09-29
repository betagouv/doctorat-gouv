package fr.dinum.beta.gouv.doctorat.dto;

/**
 * DTO de réponse décrivant une demande de mise en relation
 * vue par le candidat : demande, sujet, direction de thèse et candidat.
 */
public class DemandeCandidatDetailResponse {

    private Long id;
    private String statut;
    private String motivations;
    private String dateDemande;

    private Long propositionTheseId;
    private String titreSujet;
    private String dateMiseEnLigne;
    private String dateLimiteCandidature;
    private String dateDebutThese;
    private String etablissement;
    private String ecoleDoctorale;
    private String laboratoire;

    private String directeurPrenom;
    private String directeurNom;
    private String directeurPhotoUrl;

    private String candidatNom;
    private String candidatPrenom;
    private String candidatSituation;

    public DemandeCandidatDetailResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMotivations() { return motivations; }
    public void setMotivations(String motivations) { this.motivations = motivations; }

    public String getDateDemande() { return dateDemande; }
    public void setDateDemande(String dateDemande) { this.dateDemande = dateDemande; }

    public Long getPropositionTheseId() { return propositionTheseId; }
    public void setPropositionTheseId(Long propositionTheseId) { this.propositionTheseId = propositionTheseId; }

    public String getTitreSujet() { return titreSujet; }
    public void setTitreSujet(String titreSujet) { this.titreSujet = titreSujet; }

    public String getDateMiseEnLigne() { return dateMiseEnLigne; }
    public void setDateMiseEnLigne(String dateMiseEnLigne) { this.dateMiseEnLigne = dateMiseEnLigne; }

    public String getDateLimiteCandidature() { return dateLimiteCandidature; }
    public void setDateLimiteCandidature(String dateLimiteCandidature) { this.dateLimiteCandidature = dateLimiteCandidature; }

    public String getDateDebutThese() { return dateDebutThese; }
    public void setDateDebutThese(String dateDebutThese) { this.dateDebutThese = dateDebutThese; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getEcoleDoctorale() { return ecoleDoctorale; }
    public void setEcoleDoctorale(String ecoleDoctorale) { this.ecoleDoctorale = ecoleDoctorale; }

    public String getLaboratoire() { return laboratoire; }
    public void setLaboratoire(String laboratoire) { this.laboratoire = laboratoire; }

    public String getDirecteurPrenom() { return directeurPrenom; }
    public void setDirecteurPrenom(String directeurPrenom) { this.directeurPrenom = directeurPrenom; }

    public String getDirecteurNom() { return directeurNom; }
    public void setDirecteurNom(String directeurNom) { this.directeurNom = directeurNom; }

    public String getDirecteurPhotoUrl() { return directeurPhotoUrl; }
    public void setDirecteurPhotoUrl(String directeurPhotoUrl) { this.directeurPhotoUrl = directeurPhotoUrl; }

    public String getCandidatNom() { return candidatNom; }
    public void setCandidatNom(String candidatNom) { this.candidatNom = candidatNom; }

    public String getCandidatPrenom() { return candidatPrenom; }
    public void setCandidatPrenom(String candidatPrenom) { this.candidatPrenom = candidatPrenom; }

    public String getCandidatSituation() { return candidatSituation; }
    public void setCandidatSituation(String candidatSituation) { this.candidatSituation = candidatSituation; }
}
