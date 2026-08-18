package fr.dinum.beta.gouv.doctorat.dto;

public class ConnexionResponse {

    private UtilisateurDto utilisateur;

    public ConnexionResponse() {
    }

    public ConnexionResponse(UtilisateurDto utilisateur) {
        this.utilisateur = utilisateur;
    }

    public UtilisateurDto getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UtilisateurDto utilisateur) {
        this.utilisateur = utilisateur;
    }
}
