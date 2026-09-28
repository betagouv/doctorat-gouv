package fr.dinum.beta.gouv.doctorat.dto;

/**
 * DTO de réponse décrivant un message échangé sur une demande acceptée.
 */
public class MessageDto {

    private Long id;
    private Long demandeId;
    private String auteurId;
    private String auteurNom;
    private boolean envoyeParMoi;
    private String contenu;
    private String dateEnvoi;

    public MessageDto() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDemandeId() { return demandeId; }
    public void setDemandeId(Long demandeId) { this.demandeId = demandeId; }

    public String getAuteurId() { return auteurId; }
    public void setAuteurId(String auteurId) { this.auteurId = auteurId; }

    public String getAuteurNom() { return auteurNom; }
    public void setAuteurNom(String auteurNom) { this.auteurNom = auteurNom; }

    public boolean isEnvoyeParMoi() { return envoyeParMoi; }
    public void setEnvoyeParMoi(boolean envoyeParMoi) { this.envoyeParMoi = envoyeParMoi; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public String getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(String dateEnvoi) { this.dateEnvoi = dateEnvoi; }
}
