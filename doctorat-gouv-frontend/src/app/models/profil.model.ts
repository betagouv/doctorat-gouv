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
