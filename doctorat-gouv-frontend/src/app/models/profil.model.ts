/** Réponse du backend contenant les coordonnées du profil candidat. */
export interface ProfilResponse {
  civilite: string | null;
  nom: string;
  prenom: string;
  situation: string;
  email: string;
  telephone: string | null;
  photoUrl: string | null;
  competences: string[];
  nbCandidatures: number;
  orcid: string | null;
  etablissement: string | null;
  laboratoire: string | null;
  /** CV du candidat (null si absent). */
  cv: FichierCandidat | null;
  /** Pièces complémentaires (jamais null). */
  pieces: FichierCandidat[];
}

/** Données envoyées au backend pour modifier le profil candidat. */
export interface ProfilUpdateRequest {
  civilite: string | null;
  nom: string;
  prenom: string;
  situation: string;
  email: string;
  telephone: string | null;
  competences: string[];
  orcid: string | null;
  etablissement: string | null;
  laboratoire: string | null;
}

/** Réponse du backend contenant les coordonnées du profil directeur de thèse. */
export interface ProfilDtResponse {
  civilite: string | null;
  nom: string;
  prenom: string;
  email: string;
  photoUrl: string | null;
  competences: string[];
  orcid: string | null;
  etablissement: string | null;
  laboratoire: string | null;
  ecoleDoctorale: string | null;
  employeur: string | null;
  titre: string | null;
  precisionTitre: string | null;
  habilitationRecherche: string | null;
  domaineScientifique: string | null;
  expertiseMots: string | null;
  motsCles: string[];
  descriptionAxes: string | null;
  axes: AxeDeRecherche[];
}

/** Données envoyées au backend pour modifier le profil directeur de thèse. */
export interface ProfilDtUpdateRequest {
  civilite: string | null;
  nom: string;
  prenom: string;
  email: string;
  competences: string[];
  orcid: string | null;
  etablissement: string | null;
  laboratoire: string | null;
  ecoleDoctorale: string | null;
  employeur: string | null;
  titre: string | null;
  precisionTitre: string | null;
  habilitationRecherche: string | null;
  domaineScientifique: string | null;
  expertiseMots: string | null;
  motsCles: string[];
  descriptionAxes: string | null;
  axes: AxeDeRecherche[];
}

/** Une option de la liste déroulante « Situation actuelle » (valeur stockée = code). */
export interface SituationOption {
  value: string;
  label: string;
}

/** Référentiel des situations actuelles du candidat (codes inscrits en base). */
export const SITUATIONS: SituationOption[] = [
  { value: 'MASTER_FRANCE', label: 'Étudiante ou étudiant en master en France' },
  { value: 'ECOLE_INGENIEURS_FRANCE', label: "Élève d'une école d'ingénieurs en France" },
  { value: 'GRANDE_ECOLE_MASTER_FRANCE', label: "Élève d'une autre catégorie de grande école française conférant le grade de master" },
  { value: 'MASTER_UE', label: "Étudiante ou étudiant en master dans un pays de l'union européenne autre que la France" },
  { value: 'MASTER_HORS_UE', label: 'Étudiante ou étudiant en master dans un pays en dehors de l\'union européenne' },
  { value: 'CHERCHEUR_ENTREPRISE', label: 'Chercheuse ou chercheur en entreprise' },
  { value: 'CHERCHEUR_ACADEMIQUE', label: 'Chercheuse ou chercheur du secteur académique' },
  { value: 'PROFESSIONNEL_ACTIF', label: 'Autre professionnel.le en activité' },
  { value: 'PARTENARIAT_ENTREPRISE', label: 'Entreprise souhaitant établir un partenariat' },
  { value: 'PARTENARIAT_ORGANISATION', label: "Organisation relevant d'une autre catégorie, souhaitant établir un partenariat" },
  { value: 'AUTRE', label: 'Autre' },
];

/**
 * Anciens codes de situation (avant le passage aux 11 options).
 * Conservés uniquement pour l'affichage des profils existants
 * via le pipe situationLibelle — jamais proposés dans les selects.
 */
export const SITUATIONS_LEGACY: SituationOption[] = [
  { value: 'ETUDIANT_MASTER', label: 'Étudiant en master' },
  { value: 'TITULAIRE_MASTER', label: "Titulaire d'un master" },
  { value: 'DOCTORANT', label: 'Doctorant' },
  { value: 'PROFESSIONNEL', label: 'Professionnel' },
];

/** Situations « partenariat » : inscription adaptée (pas de master, 2 documents spécifiques). */
export const SITUATIONS_PARTENARIAT: readonly string[] = [
  'PARTENARIAT_ENTREPRISE',
  'PARTENARIAT_ORGANISATION',
];

/** Vrai si la situation demande le parcours d'inscription partenariat. */
export function estSituationPartenaire(situation: string | null | undefined): boolean {
  return !!situation && SITUATIONS_PARTENARIAT.includes(situation);
}

/** Socle commun : fréquence du récapitulatif des messages non lus (candidat + directeur de thèse). */
export type FrequenceNotification = 'JOURNALIERE' | 'HEBDOMADAIRE';

export interface NotificationPreference {
  frequence: FrequenceNotification;
}

/** Un axe de recherche du directeur de thèse. */
export interface AxeDeRecherche {
  titre: string;
  precisions: string;
  contactable: boolean;
  /** Identifiant stable de l'axe (génère 1 offre d'accompagnement par axe contactable). */
  externalId: string | null;
}

/** Un sujet de thèse rattaché au directeur de thèse. */
export interface SujetDt {
  id: number;
  matricule: string | null;
  titre: string | null;
  etablissement: string | null;
  ecoleDoctorale: string | null;
  laboratoire: string | null;
  active: boolean | null;
  role: string | null;
  dateMiseEnLigne: string | null;
  dateLimiteCandidature: string | null;
}

/** Un fichier complémentaire du candidat (nom affichable + taille en octets). */
export interface FichierCandidat {
  filename: string | null;
  size: number | null;
}

/** Une demande de mise en relation faite par un candidat sur un sujet du DT. */
export interface DemandeDt {
  id: number;
  propositionTheseId: number;
  titreSujet: string | null;
  etablissement: string | null;
  dateMiseEnLigne?: string | null;
  dateLimiteCandidature?: string | null;
  candidatNom: string | null;
  candidatPrenom?: string | null;
  candidatCivilite?: string | null;
  candidatEmail: string | null;
  candidatPhotoUrl: string | null;
  candidatSituation?: string | null;
  candidatTelephone?: string | null;
  candidatCvFilename?: string | null;
  candidatCvSize?: number | null;
  candidatPieces?: FichierCandidat[];
  dateDemande: string | null;
  dateAcceptation?: string | null;
  nbMessagesNonLus?: number | null;
  statut: string | null;
  motivations?: string | null;
}
