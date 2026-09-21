package fr.dinum.beta.gouv.doctorat.dto;

/**
 * DTO de réponse décrivant une demande de mise en relation faite par un
 * candidat sur un sujet rattaché au directeur de thèse connecté.
 */
public class DemandeDtResponse {

    private Long id;
    private Long propositionTheseId;
    private String titreSujet;
    private String etablissement;
    private String candidatNom;
    private String candidatEmail;
    private String dateDemande;
    private String statut;

    public DemandeDtResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropositionTheseId() { return propositionTheseId; }
    public void setPropositionTheseId(Long propositionTheseId) { this.propositionTheseId = propositionTheseId; }

    public String getTitreSujet() { return titreSujet; }
    public void setTitreSujet(String titreSujet) { this.titreSujet = titreSujet; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getCandidatNom() { return candidatNom; }
    public void setCandidatNom(String candidatNom) { this.candidatNom = candidatNom; }

    public String getCandidatEmail() { return candidatEmail; }
    public void setCandidatEmail(String candidatEmail) { this.candidatEmail = candidatEmail; }

    public String getDateDemande() { return dateDemande; }
    public void setDateDemande(String dateDemande) { this.dateDemande = dateDemande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }
}
