package fr.dinum.beta.gouv.doctorat.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FresqReferentielItem {
	@JsonProperty("id")
	private String id;
	
	@JsonProperty("libelle")
	private String libelle;
	
	@JsonProperty("code")
	private String code;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public String getLibelle() {
		return libelle;
	}
	
	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
}
