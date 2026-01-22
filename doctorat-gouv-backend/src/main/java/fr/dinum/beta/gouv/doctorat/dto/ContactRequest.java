package fr.dinum.beta.gouv.doctorat.dto;

public class ContactRequest {

	public long idPropositionThese;
	
	public String nom;
	public String prenom;
	public String civilite;
	public String email;
	public String profil;
	public Integer annees;
	public String secteur;
	public Boolean confirmMaster;
	public String message;

	public String emailEncadrant;
	public String cvBase64;
	public String documentBase64;
	public String titreSujet;
	public String urlSujet;
	public String urlVitrine;

	public String getNom() {
		return nom;
	}

	public void setNom(String nom) {
		this.nom = nom;
	}

	public String getPrenom() {
		return prenom;
	}

	public void setPrenom(String prenom) {
		this.prenom = prenom;
	}

	public String getCivilite() {
		return civilite;
	}

	public void setCivilite(String civilite) {
		this.civilite = civilite;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getProfil() {
		return profil;
	}

	public void setProfil(String profil) {
		this.profil = profil;
	}

	public Integer getAnnees() {
		return annees;
	}

	public void setAnnees(Integer annees) {
		this.annees = annees;
	}

	public String getSecteur() {
		return secteur;
	}

	public void setSecteur(String secteur) {
		this.secteur = secteur;
	}

	public Boolean getConfirmMaster() {
		return confirmMaster;
	}

	public void setConfirmMaster(Boolean confirmMaster) {
		this.confirmMaster = confirmMaster;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getEmailEncadrant() {
		return emailEncadrant;
	}

	public void setEmailEncadrant(String emailEncadrant) {
		this.emailEncadrant = emailEncadrant;
	}

	public String getCvBase64() {
		return cvBase64;
	}

	public void setCvBase64(String cvBase64) {
		this.cvBase64 = cvBase64;
	}

	public String getDocumentBase64() {
		return documentBase64;
	}

	public void setDocumentBase64(String documentBase64) {
		this.documentBase64 = documentBase64;
	}

	public String getTitreSujet() {
		return titreSujet;
	}

	public void setTitreSujet(String titreSujet) {
		this.titreSujet = titreSujet;
	}

	public String getUrlSujet() {
		return urlSujet;
	}

	public void setUrlSujet(String urlSujet) {
		this.urlSujet = urlSujet;
	}

	public String getUrlVitrine() {
		return urlVitrine;
	}

	public void setUrlVitrine(String urlVitrine) {
		this.urlVitrine = urlVitrine;
	}

	public long getIdPropositionThese() {
		return idPropositionThese;
	}

	public void setIdPropositionThese(long idPropositionThese) {
		this.idPropositionThese = idPropositionThese;
	}
}
