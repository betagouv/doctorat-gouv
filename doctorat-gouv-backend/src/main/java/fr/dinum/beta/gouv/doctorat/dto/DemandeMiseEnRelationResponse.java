package fr.dinum.beta.gouv.doctorat.dto;

public class DemandeMiseEnRelationResponse {

    private String message;
    private boolean success;
    private Long id;
    private String motivations;
    private Boolean rgpdConsent;
    private String statut;

    public DemandeMiseEnRelationResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }
}
