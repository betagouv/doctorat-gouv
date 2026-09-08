package fr.dinum.beta.gouv.doctorat.dto;

public class DemandeMiseEnRelationRequest {

    private long idPropositionThese;
    private String motivations;
    private Boolean rgpdConsent;

    public long getIdPropositionThese() {
        return idPropositionThese;
    }

    public void setIdPropositionThese(long idPropositionThese) {
        this.idPropositionThese = idPropositionThese;
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
}
