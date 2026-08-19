package fr.dinum.beta.gouv.doctorat.dto;

public class ConnexionResponse {

    private UtilisateurDto utilisateur;
    private String token;
    private long expiresIn;

    public ConnexionResponse() {
    }

    public ConnexionResponse(UtilisateurDto utilisateur, String token, long expiresIn) {
        this.utilisateur = utilisateur;
        this.token = token;
        this.expiresIn = expiresIn;
    }

    public UtilisateurDto getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(UtilisateurDto utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }
}
