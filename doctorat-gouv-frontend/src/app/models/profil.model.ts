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

/** Données envoyées au backend pour modifier le profil. */
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
