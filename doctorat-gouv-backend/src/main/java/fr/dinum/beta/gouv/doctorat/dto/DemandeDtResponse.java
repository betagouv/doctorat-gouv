package fr.dinum.beta.gouv.doctorat.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO de réponse décrivant une demande de mise en relation faite par un
 * candidat sur un sujet rattaché au directeur de thèse connecté.
 */
public class DemandeDtResponse {

    private Long id;
    private Long propositionTheseId;
    private String titreSujet;
    private String etablissement;
    private String dateMiseEnLigne;
    private String dateLimiteCandidature;
    private String candidatNom;
    private String candidatPrenom;
    private String candidatCivilite;
    private String candidatEmail;
    private String candidatPhotoUrl;
    private String candidatSituation;
    private String candidatTelephone;
    private String candidatCvFilename;
    private Long candidatCvSize;
    private List<FichierCandidatDto> candidatPieces = new ArrayList<>();
    private String dateDemande;
    private String statut;
    private String motivations;

    /** Fichier complémentaire du candidat (nom affiché + taille en octets). */
    public static class FichierCandidatDto {
        private String filename;
        private Long size;

        public FichierCandidatDto() {}

        public FichierCandidatDto(String filename, Long size) {
            this.filename = filename;
            this.size = size;
        }

        public String getFilename() { return filename; }
        public void setFilename(String filename) { this.filename = filename; }

        public Long getSize() { return size; }
        public void setSize(Long size) { this.size = size; }
    }

    public DemandeDtResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPropositionTheseId() { return propositionTheseId; }
    public void setPropositionTheseId(Long propositionTheseId) { this.propositionTheseId = propositionTheseId; }

    public String getTitreSujet() { return titreSujet; }
    public void setTitreSujet(String titreSujet) { this.titreSujet = titreSujet; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getDateMiseEnLigne() { return dateMiseEnLigne; }
    public void setDateMiseEnLigne(String dateMiseEnLigne) { this.dateMiseEnLigne = dateMiseEnLigne; }

    public String getDateLimiteCandidature() { return dateLimiteCandidature; }
    public void setDateLimiteCandidature(String dateLimiteCandidature) { this.dateLimiteCandidature = dateLimiteCandidature; }

    public String getCandidatNom() { return candidatNom; }
    public void setCandidatNom(String candidatNom) { this.candidatNom = candidatNom; }

    public String getCandidatPrenom() { return candidatPrenom; }
    public void setCandidatPrenom(String candidatPrenom) { this.candidatPrenom = candidatPrenom; }

    public String getCandidatCivilite() { return candidatCivilite; }
    public void setCandidatCivilite(String candidatCivilite) { this.candidatCivilite = candidatCivilite; }

    public String getCandidatEmail() { return candidatEmail; }
    public void setCandidatEmail(String candidatEmail) { this.candidatEmail = candidatEmail; }

    public String getCandidatPhotoUrl() { return candidatPhotoUrl; }
    public void setCandidatPhotoUrl(String candidatPhotoUrl) { this.candidatPhotoUrl = candidatPhotoUrl; }

    public String getCandidatSituation() { return candidatSituation; }
    public void setCandidatSituation(String candidatSituation) { this.candidatSituation = candidatSituation; }

    public String getCandidatTelephone() { return candidatTelephone; }
    public void setCandidatTelephone(String candidatTelephone) { this.candidatTelephone = candidatTelephone; }

    public String getCandidatCvFilename() { return candidatCvFilename; }
    public void setCandidatCvFilename(String candidatCvFilename) { this.candidatCvFilename = candidatCvFilename; }

    public Long getCandidatCvSize() { return candidatCvSize; }
    public void setCandidatCvSize(Long candidatCvSize) { this.candidatCvSize = candidatCvSize; }

    public List<FichierCandidatDto> getCandidatPieces() { return candidatPieces; }
    public void setCandidatPieces(List<FichierCandidatDto> candidatPieces) { this.candidatPieces = candidatPieces; }

    public String getDateDemande() { return dateDemande; }
    public void setDateDemande(String dateDemande) { this.dateDemande = dateDemande; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getMotivations() { return motivations; }
    public void setMotivations(String motivations) { this.motivations = motivations; }
}
