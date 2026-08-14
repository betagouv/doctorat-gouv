package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ecole_doctorale")
@NoArgsConstructor
@AllArgsConstructor
public class EcoleDoctorale {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;
	
	@Column(length = 3, nullable = false, unique = true)
	private String numero;
	
	@Column(length = 250, nullable = false)
	private String libelle;
	
	@Column(length = 100)
	private String etablissementRor;
	
	@Column(length = 250)
	private String etablissementLibelle;
	
	@Column(length = 20)
	private String uai;
	
	@Column(length = 50)
	private String academie;
	
	@Column(length = 50)
	private String regionAcademie;
	
	@Column(length = 50)
	private String secteur;
	
	@Column(length = 50)
	private String specialite;
	
	private Boolean active;
	
	private LocalDateTime dateSynchronisation;
	
	@Column(length = 50)
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
