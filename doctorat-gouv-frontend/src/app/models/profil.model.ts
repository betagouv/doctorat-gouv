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

/** Un axe de recherche du directeur de thèse. */
export interface AxeDeRecherche {
  titre: string;
  precisions: string;
  contactable: boolean;
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

/** Une demande de mise en relation faite par un candidat sur un sujet du DT. */
export interface DemandeDt {
  id: number;
  propositionTheseId: number;
  titreSujet: string | null;
  etablissement: string | null;
  candidatNom: string | null;
  candidatEmail: string | null;
  candidatPhotoUrl: string | null;
  dateDemande: string | null;
  statut: string | null;
  motivations?: string | null;
}
