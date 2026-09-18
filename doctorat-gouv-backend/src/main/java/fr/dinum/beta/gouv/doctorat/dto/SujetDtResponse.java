package fr.dinum.beta.gouv.doctorat.dto;

/**
 * DTO de réponse décrivant un sujet de thèse rattaché au directeur de thèse connecté.
 */
public class SujetDtResponse {

    private Long id;
    private String matricule;
    private String titre;
    private String etablissement;
    private String ecoleDoctorale;
    private String laboratoire;
    private Boolean active;
    private String role;
    private String dateMiseEnLigne;
    private String dateLimiteCandidature;

    public SujetDtResponse() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMatricule() { return matricule; }
    public void setMatricule(String matricule) { this.matricule = matricule; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getEtablissement() { return etablissement; }
    public void setEtablissement(String etablissement) { this.etablissement = etablissement; }

    public String getEcoleDoctorale() { return ecoleDoctorale; }
    public void setEcoleDoctorale(String ecoleDoctorale) { this.ecoleDoctorale = ecoleDoctorale; }

    public String getLaboratoire() { return laboratoire; }
    public void setLaboratoire(String laboratoire) { this.laboratoire = laboratoire; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getDateMiseEnLigne() { return dateMiseEnLigne; }
    public void setDateMiseEnLigne(String dateMiseEnLigne) { this.dateMiseEnLigne = dateMiseEnLigne; }

    public String getDateLimiteCandidature() { return dateLimiteCandidature; }
    public void setDateLimiteCandidature(String dateLimiteCandidature) { this.dateLimiteCandidature = dateLimiteCandidature; }
}
