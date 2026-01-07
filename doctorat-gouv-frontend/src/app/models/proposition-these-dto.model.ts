export type LocalDate = string;       // ex. "2026-02-15"
export type LocalDateTime = string;   // ex. "2026-02-15T13:45:00Z"

export interface PropositionTheseDto{
  id: number | null;
  matricule: string | null;
  typeProposition: string | null;

  dateCreation: LocalDateTime | null;
  dateMaj: LocalDateTime | null;
  dateSoumission: LocalDateTime | null;
  dateMiseEnLigne: LocalDateTime | null;
  dateLimiteCandidature: LocalDateTime | null;
  dateDebutThese: LocalDate | null;

  deposantOrcid: string | null;
  deposantNom: string | null;
  deposantPrenom: string | null;
  deposantEmail: string | null;

  anneeUniversitaire: string | null;
  ecoleDoctoraleNumero: string | null;
  ecoleDoctoraleLibelle: string | null;

  uniteRechercheRor: string | null;
  uniteRechercheLibelle: string | null;
  uniteRechercheCodePostal: string | null;
  uniteRechercheVille: string | null;

  etablissementRor: string | null;
  etablissementLibelle: string | null;
  etablissementCodePostal: string | null;
  etablissementVille: string | null;

  specialite: string | null;
  domaineScientifique: string | null;

  directionTheseOrcid: string | null;
  directionTheseNom: string | null;
  directionThesePrenom: string | null;
  directionTheseEmail: string | null;

  codirectionTheseOrcid: string | null;
  codirectionTheseNom: string | null;
  codirectionThesePrenom: string | null;
  codirectionTheseEmail: string | null;

  interdisciplinaire: string | null;
  cotutelle: string | null;
  cotutellePaysCode: string | null;

  modalitesEncadrement: string | null;
  domainesImpact: string | null;
  domainesImpactListe: string[] | null;

  objectifsDeveloppementDurable: string | null;
  objectifsDeveloppementDurableListe: string[] | null;

  theseTitre: string | null;
  motsCles: Record<string, string> | null;
  theseTitreAnglais: string | null;
  motsClesAnglais: Record<string, string> | null;

  resume: string | null;
  resumeAnglais: string | null;
  thematiqueRecherche: string | null;
  domaine: string | null;
  objectif: string | null;
  contexte: string | null;
  methodeDeTravail: string | null;
  resultatsAttendus: string | null;
  referencesBibliographiques: string | null;
  conditionsMaterielles: string | null;
  ouvertureInternationale: string | null;
  collaborationsEnvisagees: string | null;
  valorisationTravaux: string | null;

  confidentiel: string | null;
  urlInfosComplementaires: string | null;

  financementEtat: string | null;
  financementTypes: string[] | null;
  financementEmployeur: string | null;
  financementOrigine: string | null;
  financementDateDebut: LocalDate | null;
  financementDateFin: LocalDate | null;
  financementDetails: string | null;

  profilRecherche: string | null;
  profilRechercheAnglais: string | null;
  niveauAnglaisRequis: string | null;
  niveauFrancaisRequis: string | null;
  candidatureEnLignePossible: string | null;
}

