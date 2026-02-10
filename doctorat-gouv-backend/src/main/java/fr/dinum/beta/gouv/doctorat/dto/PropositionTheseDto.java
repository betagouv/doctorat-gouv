package fr.dinum.beta.gouv.doctorat.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class PropositionTheseDto {

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

	public void setEcoleDoctoraleLibelle(String ecoleDoctoraleLibelle) {
		this.ecoleDoctoraleLibelle = ecoleDoctoraleLibelle;
	}

	public String getEcoleDoctoraleNumero() {
		return ecoleDoctoraleNumero;
	}

	public void setEcoleDoctoraleNumero(String ecoleDoctoraleNumero) {
		this.ecoleDoctoraleNumero = ecoleDoctoraleNumero;
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

	private Long id;
	private String matricule;
	private String typeProposition;

	private LocalDateTime dateCreation;
	private LocalDateTime dateMaj;
	private LocalDateTime dateSoumission;
	private LocalDateTime dateMiseEnLigne;
	private LocalDateTime dateLimiteCandidature;
	private LocalDate dateDebutThese;

	private String deposantOrcid;
	private String deposantNom;
	private String deposantPrenom;
	private String deposantEmail;

	private String anneeUniversitaire;
	private String ecoleDoctoraleNumero;
	private String ecoleDoctoraleLibelle;

	private String uniteRechercheRor;
	private String uniteRechercheLibelle;
	private String uniteRechercheCodePostal;
	private String uniteRechercheVille;

	private String etablissementRor;
	private String etablissementLibelle;
	private String etablissementCodePostal;
	private String etablissementVille;

	private String specialite;
	private String domaineScientifique;

	private String directionTheseOrcid;
	private String directionTheseNom;
	private String directionThesePrenom;
	private String directionTheseEmail;

	private String codirectionTheseOrcid;
	private String codirectionTheseNom;
	private String codirectionThesePrenom;
	private String codirectionTheseEmail;

	private String interdisciplinaire;
	private String cotutelle;
	private String cotutellePaysCode;

	private String modalitesEncadrement;
	private String domainesImpact;
	private List<String> domainesImpactListe;

	private String objectifsDeveloppementDurable;
	private List<String> objectifsDeveloppementDurableListe;

	private String theseTitre;
	private Map<String, String> motsCles;
	private String theseTitreAnglais;
	private Map<String, String> motsClesAnglais;

	private String resume;
	private String resumeAnglais;
	private String thematiqueRecherche;
	private String domaine;
	private String objectif;
	private String contexte;
	private String methodeDeTravail;
	private String resultatsAttendus;
	private String referencesBibliographiques;
	private String conditionsMaterielles;
	private String ouvertureInternationale;
	private String collaborationsEnvisagees;
	private String valorisationTravaux;

	private String confidentiel;
	private String urlInfosComplementaires;

	private String financementEtat;
	private List<String> financementTypes;
	private String financementEmployeur;
	private String financementOrigine;
	private LocalDate financementDateDebut;
	private LocalDate financementDateFin;
	private String financementDetails;

	private String profilRecherche;
	private String profilRechercheAnglais;
	private String niveauAnglaisRequis;
	private String niveauFrancaisRequis;
	private String candidatureEnLignePossible;
	private String urlPdf;
	public String getEcoleDoctoraleLibelle() {
		return ecoleDoctoraleLibelle;
	}

	public String getUrlPdf() {
		return urlPdf;
	}

	public void setUrlPdf(String urlPdf) {
		this.urlPdf = urlPdf;
	}

}
