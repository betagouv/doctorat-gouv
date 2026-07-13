package fr.dinum.beta.gouv.doctorat.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FresqEtablissement {
	@JsonProperty("uai")
	private String uai;
	
	@JsonProperty("nom")
	private String nom;
	
	@JsonProperty("sigle")
	private String sigle;
	
	@JsonProperty("typeEtablissement")
	private String typeEtablissement;
	
	public String getUai() {
		return uai;
	}
	
	public void setUai(String uai) {
		this.uai = uai;
	}
	
	public String getNom() {
		return nom;
	}
	
	public void setNom(String nom) {
		this.nom = nom;
	}
	
	public String getSigle() {
		return sigle;
	}
	
	public void setSigle(String sigle) {
		this.sigle = sigle;
	}
	
	public String getTypeEtablissement() {
		return typeEtablissement;
	}
	
	public void setTypeEtablissement(String typeEtablissement) {
		this.typeEtablissement = typeEtablissement;
	}
}
