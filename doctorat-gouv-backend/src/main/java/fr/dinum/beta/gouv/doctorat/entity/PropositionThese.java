package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "proposition_these")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropositionThese {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(length = 11, nullable = false, unique = true)
	private String matricule;

	@Column(length = 12, nullable = false)
	private String typeProposition;

	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateCreation;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateMaj;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateSoumission;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateMiseEnLigne;
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	private LocalDateTime dateLimiteCandidature;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate dateDebutThese;

	@Column(length = 16)
	private String deposantOrcid;

	@Column(length = 50)
	private String deposantNom;

	@Column(length = 25)
	private String deposantPrenom;

	@Column(length = 100)
	private String deposantEmail;

	@Column(length = 4)
	private String anneeUniversitaire;

	@Column(length = 3)
	private String ecoleDoctoralNumero;

	@Column(length = 9)
	private String uniteRechercheRor;

	@Column(length = 300)
	private String uniteRechercheLibelle;

	@Column(length = 7)
	private String uniteRechercheCodePostal;

	@Column(length = 40)
	private String uniteRechercheVille;

	@Column(length = 9)
	private String etablissementRor;

	@Column(length = 250)
	private String etablissementLibelle;

	@Column(length = 7)
	private String etablissementCodePostal;

	@Column(length = 40)
	private String etablissementVille;

	@Column(length = 255)
	private String specialite;

	private String domaineScientifique;

	@Column(length = 16)
	private String directionTheseOrcid;

	@Column(length = 50)
	private String directionTheseNom;

	@Column(length = 25)
	private String directionThesePrenom;

	@Column(length = 100)
	private String directionTheseEmail;

	@Column(length = 16)
	private String codirectionTheseOrcid;

	@Column(length = 50)
	private String codirectionTheseNom;

	@Column(length = 25)
	private String codirectionThesePrenom;

	@Column(length = 100)
	private String codirectionTheseEmail;

	@Column(length = 3)
	private String interdisciplinaire;

	@Column(length = 3)
	private String cotutelle;

	@Column(length = 3)
	private String cotutellePaysCode;

	@Column(columnDefinition = "TEXT")
	private String modalitesEncadrement;

	@Column(length = 3)
	private String domainesImpact;

	@ElementCollection
	private List<String> domainesImpactListe;

	@Column(length = 3)
	private String objectifsDeveloppementDurable;

	@ElementCollection
	private List<String> objectifsDeveloppementDurableListe;

	@Column(length = 255)
	private String theseTitre;

	@ElementCollection
	private Map<String, String> motsCles;

	@Column(length = 255)
	private String theseTitreAnglais;

	@ElementCollection
	private Map<String, String> motsClesAnglais;

	@Column(columnDefinition = "TEXT")
	private String resume;

	@Column(columnDefinition = "TEXT")
	private String resumeAnglais;

	@Column(columnDefinition = "TEXT")
	private String thematiqueRecherche;

	@Column(columnDefinition = "TEXT")
	private String domaine;

	@Column(columnDefinition = "TEXT")
	private String objectif;

	@Column(columnDefinition = "TEXT")
	private String contexte;

	@Column(columnDefinition = "TEXT")
	private String methodeDeTravail;

	@Column(columnDefinition = "TEXT")
	private String resultatsAttendus;

	@Column(columnDefinition = "TEXT")
	private String referencesBibliographiques;

	@Column(columnDefinition = "TEXT")
	private String conditionsMaterielles;

	@Column(columnDefinition = "TEXT")
	private String ouvertureInternationale;

	@Column(columnDefinition = "TEXT")
	private String collaborationsEnvisagees;

	@Column(columnDefinition = "TEXT")
	private String valorisationTravaux;

	@Column(length = 3)
	private String confidentiel;

	@Column(length = 255)
	private String urlInfosComplementaires;

	@Column(length = 255)
	private String financementEtat;

	@ElementCollection
	private List<String> financementTypes;

	@Column(length = 255)
	private String financementEmployeur;

	@Column(length = 255)
	private String financementOrigine;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate financementDateDebut;
	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate financementDateFin;

	@Column(columnDefinition = "TEXT")
	private String financementDetails;

	@Column(columnDefinition = "TEXT")
	private String profilRecherche;

	@Column(columnDefinition = "TEXT")
	private String profilRechercheAnglais;

	@Column(length = 5)
	private String niveauAnglaisRequis;

	@Column(length = 5)
	private String niveauFrancaisRequis;

	@Column(length = 3)
	private String candidatureEnLignePossible;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getMatricule() {
		return matricule;
	}

	public void setMatricule(String matricule) {
		this.matricule = matricule;
	}

	public String getTypeProposition() {
		return typeProposition;
	}

	public void setTypeProposition(String typeProposition) {
		this.typeProposition = typeProposition;
	}

	public LocalDateTime getDateCreation() {
		return dateCreation;
	}

	public void setDateCreation(LocalDateTime dateCreation) {
		this.dateCreation = dateCreation;
	}

	public LocalDateTime getDateMaj() {
		return dateMaj;
	}

	public void setDateMaj(LocalDateTime dateMaj) {
		this.dateMaj = dateMaj;
	}

	public LocalDateTime getDateSoumission() {
		return dateSoumission;
	}

	public void setDateSoumission(LocalDateTime dateSoumission) {
		this.dateSoumission = dateSoumission;
	}

	public LocalDateTime getDateMiseEnLigne() {
		return dateMiseEnLigne;
	}

	public void setDateMiseEnLigne(LocalDateTime dateMiseEnLigne) {
		this.dateMiseEnLigne = dateMiseEnLigne;
	}

	public LocalDateTime getDateLimiteCandidature() {
		return dateLimiteCandidature;
	}

	public void setDateLimiteCandidature(LocalDateTime dateLimiteCandidature) {
		this.dateLimiteCandidature = dateLimiteCandidature;
	}

	public LocalDate getDateDebutThese() {
		return dateDebutThese;
	}

	public void setDateDebutThese(LocalDate dateDebutThese) {
		this.dateDebutThese = dateDebutThese;
	}

	public String getDeposantOrcid() {
		return deposantOrcid;
	}

	public void setDeposantOrcid(String deposantOrcid) {
		this.deposantOrcid = deposantOrcid;
	}

	public String getDeposantNom() {
		return deposantNom;
	}

	public void setDeposantNom(String deposantNom) {
		this.deposantNom = deposantNom;
	}

	public String getDeposantPrenom() {
		return deposantPrenom;
	}

	public void setDeposantPrenom(String deposantPrenom) {
		this.deposantPrenom = deposantPrenom;
	}

	public String getDeposantEmail() {
		return deposantEmail;
	}

	public void setDeposantEmail(String deposantEmail) {
		this.deposantEmail = deposantEmail;
	}

	public String getAnneeUniversitaire() {
		return anneeUniversitaire;
	}

	public void setAnneeUniversitaire(String anneeUniversitaire) {
		this.anneeUniversitaire = anneeUniversitaire;
	}

	public String getEcoleDoctoralNumero() {
		return ecoleDoctoralNumero;
	}

	public void setEcoleDoctoralNumero(String ecoleDoctoralNumero) {
		this.ecoleDoctoralNumero = ecoleDoctoralNumero;
	}

	public String getUniteRechercheRor() {
		return uniteRechercheRor;
	}

	public void setUniteRechercheRor(String uniteRechercheRor) {
		this.uniteRechercheRor = uniteRechercheRor;
	}

	public String getUniteRechercheLibelle() {
		return uniteRechercheLibelle;
	}

	public void setUniteRechercheLibelle(String uniteRechercheLibelle) {
		this.uniteRechercheLibelle = uniteRechercheLibelle;
	}

	public String getUniteRechercheCodePostal() {
		return uniteRechercheCodePostal;
	}

	public void setUniteRechercheCodePostal(String uniteRechercheCodePostal) {
		this.uniteRechercheCodePostal = uniteRechercheCodePostal;
	}

	public String getUniteRechercheVille() {
		return uniteRechercheVille;
	}

	public void setUniteRechercheVille(String uniteRechercheVille) {
		this.uniteRechercheVille = uniteRechercheVille;
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

	public String getEtablissementCodePostal() {
		return etablissementCodePostal;
	}

	public void setEtablissementCodePostal(String etablissementCodePostal) {
		this.etablissementCodePostal = etablissementCodePostal;
	}

	public String getEtablissementVille() {
		return etablissementVille;
	}

	public void setEtablissementVille(String etablissementVille) {
		this.etablissementVille = etablissementVille;
	}

	public String getSpecialite() {
		return specialite;
	}

	public void setSpecialite(String specialite) {
		this.specialite = specialite;
	}

	public String getDomaineScientifique() {
		return domaineScientifique;
	}

	public void setDomaineScientifique(String domaineScientifique) {
		this.domaineScientifique = domaineScientifique;
	}

	public String getDirectionTheseOrcid() {
		return directionTheseOrcid;
	}

	public void setDirectionTheseOrcid(String directionTheseOrcid) {
		this.directionTheseOrcid = directionTheseOrcid;
	}

	public String getDirectionTheseNom() {
		return directionTheseNom;
	}

	public void setDirectionTheseNom(String directionTheseNom) {
		this.directionTheseNom = directionTheseNom;
	}

	public String getDirectionThesePrenom() {
		return directionThesePrenom;
	}

	public void setDirectionThesePrenom(String directionThesePrenom) {
		this.directionThesePrenom = directionThesePrenom;
	}

	public String getDirectionTheseEmail() {
		return directionTheseEmail;
	}

	public void setDirectionTheseEmail(String directionTheseEmail) {
		this.directionTheseEmail = directionTheseEmail;
	}

	public String getCodirectionTheseOrcid() {
		return codirectionTheseOrcid;
	}

	public void setCodirectionTheseOrcid(String codirectionTheseOrcid) {
		this.codirectionTheseOrcid = codirectionTheseOrcid;
	}

	public String getCodirectionTheseNom() {
		return codirectionTheseNom;
	}

	public void setCodirectionTheseNom(String codirectionTheseNom) {
		this.codirectionTheseNom = codirectionTheseNom;
	}

	public String getCodirectionThesePrenom() {
		return codirectionThesePrenom;
	}

	public void setCodirectionThesePrenom(String codirectionThesePrenom) {
		this.codirectionThesePrenom = codirectionThesePrenom;
	}

	public String getCodirectionTheseEmail() {
		return codirectionTheseEmail;
	}

	public void setCodirectionTheseEmail(String codirectionTheseEmail) {
		this.codirectionTheseEmail = codirectionTheseEmail;
	}

	public String getInterdisciplinaire() {
		return interdisciplinaire;
	}

	public void setInterdisciplinaire(String interdisciplinaire) {
		this.interdisciplinaire = interdisciplinaire;
	}

	public String getCotutelle() {
		return cotutelle;
	}

	public void setCotutelle(String cotutelle) {
		this.cotutelle = cotutelle;
	}

	public String getCotutellePaysCode() {
		return cotutellePaysCode;
	}

	public void setCotutellePaysCode(String cotutellePaysCode) {
		this.cotutellePaysCode = cotutellePaysCode;
	}

	public String getModalitesEncadrement() {
		return modalitesEncadrement;
	}

	public void setModalitesEncadrement(String modalitesEncadrement) {
		this.modalitesEncadrement = modalitesEncadrement;
	}

	public String getDomainesImpact() {
		return domainesImpact;
	}

	public void setDomainesImpact(String domainesImpact) {
		this.domainesImpact = domainesImpact;
	}

	public List<String> getDomainesImpactListe() {
		return domainesImpactListe;
	}

	public void setDomainesImpactListe(List<String> domainesImpactListe) {
		this.domainesImpactListe = domainesImpactListe;
	}

	public String getObjectifsDeveloppementDurable() {
		return objectifsDeveloppementDurable;
	}

	public void setObjectifsDeveloppementDurable(String objectifsDeveloppementDurable) {
		this.objectifsDeveloppementDurable = objectifsDeveloppementDurable;
	}

	public List<String> getObjectifsDeveloppementDurableListe() {
		return objectifsDeveloppementDurableListe;
	}

	public void setObjectifsDeveloppementDurableListe(List<String> objectifsDeveloppementDurableListe) {
		this.objectifsDeveloppementDurableListe = objectifsDeveloppementDurableListe;
	}

	public String getTheseTitre() {
		return theseTitre;
	}

	public void setTheseTitre(String theseTitre) {
		this.theseTitre = theseTitre;
	}

	public Map<String, String> getMotsCles() {
		return motsCles;
	}

	public void setMotsCles(Map<String, String> motsCles) {
		this.motsCles = motsCles;
	}

	public String getTheseTitreAnglais() {
		return theseTitreAnglais;
	}

	public void setTheseTitreAnglais(String theseTitreAnglais) {
		this.theseTitreAnglais = theseTitreAnglais;
	}

	public Map<String, String> getMotsClesAnglais() {
		return motsClesAnglais;
	}

	public void setMotsClesAnglais(Map<String, String> motsClesAnglais) {
		this.motsClesAnglais = motsClesAnglais;
	}

	public String getResume() {
		return resume;
	}

	public void setResume(String resume) {
		this.resume = resume;
	}

	public String getResumeAnglais() {
		return resumeAnglais;
	}

	public void setResumeAnglais(String resumeAnglais) {
		this.resumeAnglais = resumeAnglais;
	}

	public String getThematiqueRecherche() {
		return thematiqueRecherche;
	}

	public void setThematiqueRecherche(String thematiqueRecherche) {
		this.thematiqueRecherche = thematiqueRecherche;
	}

	public String getDomaine() {
		return domaine;
	}

	public void setDomaine(String domaine) {
		this.domaine = domaine;
	}

	public String getObjectif() {
		return objectif;
	}

	public void setObjectif(String objectif) {
		this.objectif = objectif;
	}

	public String getContexte() {
		return contexte;
	}

	public void setContexte(String contexte) {
		this.contexte = contexte;
	}

	public String getMethodeDeTravail() {
		return methodeDeTravail;
	}

	public void setMethodeDeTravail(String methodeDeTravail) {
		this.methodeDeTravail = methodeDeTravail;
	}

	public String getResultatsAttendus() {
		return resultatsAttendus;
	}

	public void setResultatsAttendus(String resultatsAttendus) {
		this.resultatsAttendus = resultatsAttendus;
	}

	public String getReferencesBibliographiques() {
		return referencesBibliographiques;
	}

	public void setReferencesBibliographiques(String referencesBibliographiques) {
		this.referencesBibliographiques = referencesBibliographiques;
	}

	public String getConditionsMaterielles() {
		return conditionsMaterielles;
	}

	public void setConditionsMaterielles(String conditionsMaterielles) {
		this.conditionsMaterielles = conditionsMaterielles;
	}

	public String getOuvertureInternationale() {
		return ouvertureInternationale;
	}

	public void setOuvertureInternationale(String ouvertureInternationale) {
		this.ouvertureInternationale = ouvertureInternationale;
	}

	public String getCollaborationsEnvisagees() {
		return collaborationsEnvisagees;
	}

	public void setCollaborationsEnvisagees(String collaborationsEnvisagees) {
		this.collaborationsEnvisagees = collaborationsEnvisagees;
	}

	public String getValorisationTravaux() {
		return valorisationTravaux;
	}

	public void setValorisationTravaux(String valorisationTravaux) {
		this.valorisationTravaux = valorisationTravaux;
	}

	public String getConfidentiel() {
		return confidentiel;
	}

	public void setConfidentiel(String confidentiel) {
		this.confidentiel = confidentiel;
	}

	public String getUrlInfosComplementaires() {
		return urlInfosComplementaires;
	}

	public void setUrlInfosComplementaires(String urlInfosComplementaires) {
		this.urlInfosComplementaires = urlInfosComplementaires;
	}

	public String getFinancementEtat() {
		return financementEtat;
	}

	public void setFinancementEtat(String financementEtat) {
		this.financementEtat = financementEtat;
	}

	public List<String> getFinancementTypes() {
		return financementTypes;
	}

	public void setFinancementTypes(List<String> financementTypes) {
		this.financementTypes = financementTypes;
	}

	public String getFinancementEmployeur() {
		return financementEmployeur;
	}

	public void setFinancementEmployeur(String financementEmployeur) {
		this.financementEmployeur = financementEmployeur;
	}

	public String getFinancementOrigine() {
		return financementOrigine;
	}

	public void setFinancementOrigine(String financementOrigine) {
		this.financementOrigine = financementOrigine;
	}

	public LocalDate getFinancementDateDebut() {
		return financementDateDebut;
	}

	public void setFinancementDateDebut(LocalDate financementDateDebut) {
		this.financementDateDebut = financementDateDebut;
	}

	public LocalDate getFinancementDateFin() {
		return financementDateFin;
	}

	public void setFinancementDateFin(LocalDate financementDateFin) {
		this.financementDateFin = financementDateFin;
	}

	public String getFinancementDetails() {
		return financementDetails;
	}

	public void setFinancementDetails(String financementDetails) {
		this.financementDetails = financementDetails;
	}

	public String getProfilRecherche() {
		return profilRecherche;
	}

	public void setProfilRecherche(String profilRecherche) {
		this.profilRecherche = profilRecherche;
	}

	public String getProfilRechercheAnglais() {
		return profilRechercheAnglais;
	}

	public void setProfilRechercheAnglais(String profilRechercheAnglais) {
		this.profilRechercheAnglais = profilRechercheAnglais;
	}

	public String getNiveauAnglaisRequis() {
		return niveauAnglaisRequis;
	}

	public void setNiveauAnglaisRequis(String niveauAnglaisRequis) {
		this.niveauAnglaisRequis = niveauAnglaisRequis;
	}

	public String getNiveauFrancaisRequis() {
		return niveauFrancaisRequis;
	}

	public void setNiveauFrancaisRequis(String niveauFrancaisRequis) {
		this.niveauFrancaisRequis = niveauFrancaisRequis;
	}

	public String getCandidatureEnLignePossible() {
		return candidatureEnLignePossible;
	}

	public void setCandidatureEnLignePossible(String candidatureEnLignePossible) {
		this.candidatureEnLignePossible = candidatureEnLignePossible;
	}

}
