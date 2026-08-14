package fr.dinum.beta.gouv.doctorat.dto;

import java.time.LocalDateTime;

public class EcoleDoctoraleDto {
	
	private Long id;
	private String numero;
	private String libelle;
	private String etablissementRor;
	private String etablissementLibelle;
	private String uai;
	private String academie;
	private String regionAcademie;
	private String secteur;
	private String specialite;
	private Boolean active;
	private LocalDateTime dateSynchronisation;
	private String fresqRecordId;
	
	public Long getId() {
		return id;
	}
	
	public void setId(Long id) {
		this.id = id;
	}
	
	public String getNumero() {
		return numero;
	}
	
	public void setNumero(String numero) {
		this.numero = numero;
	}
	
	public String getLibelle() {
		return libelle;
	}
	
	public void setLibelle(String libelle) {
		this.libelle = libelle;
	}
	
	public String getEtablissementRor() {
		return etablissementRor;
	}
	
	public void setEtablissementRor(String etablissementRor) {
		this.etablissementRor = etablissementRor;
	}
	
	public String getEtablissementLibelle() {
		return etablissementLibelle;
	}
	
	public void setEtablissementLibelle(String etablissementLibelle) {
		this.etablissementLibelle = etablissementLibelle;
	}
	
	public String getUai() {
		return uai;
	}
	
	public void setUai(String uai) {
		this.uai = uai;
	}
	
	public String getAcademie() {
		return academie;
	}
	
	public void setAcademie(String academie) {
		this.academie = academie;
	}
	
	public String getRegionAcademie() {
		return regionAcademie;
	}
	
	public void setRegionAcademie(String regionAcademie) {
		this.regionAcademie = regionAcademie;
	}
	
	public String getSecteur() {
		return secteur;
	}
	
	public void setSecteur(String secteur) {
		this.secteur = secteur;
	}
	
	public String getSpecialite() {
		return specialite;
	}
	
	public void setSpecialite(String specialite) {
		this.specialite = specialite;
	}
	
	public Boolean getActive() {
		return active;
	}
	
	public void setActive(Boolean active) {
		this.active = active;
	}
	
	public LocalDateTime getDateSynchronisation() {
		return dateSynchronisation;
	}
	
	public void setDateSynchronisation(LocalDateTime dateSynchronisation) {
		this.dateSynchronisation = dateSynchronisation;
	}
	
	public String getFresqRecordId() {
		return fresqRecordId;
	}
	
	public void setFresqRecordId(String fresqRecordId) {
		this.fresqRecordId = fresqRecordId;
	}
}
