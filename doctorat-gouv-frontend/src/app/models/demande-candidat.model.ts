/** Détail d'une demande de mise en relation vue par le candidat. */
export interface DemandeCandidat {
  id: number;
  statut: string | null;
  motivations: string | null;
  dateDemande: string | null;
  archivee: boolean;
  dateDesistement: string | null;
  propositionTheseId: number;
  titreSujet: string | null;
  dateMiseEnLigne: string | null;
  dateLimiteCandidature: string | null;
  dateDebutThese: string | null;
  etablissement: string | null;
  ecoleDoctorale: string | null;
  laboratoire: string | null;
  directeurPrenom: string | null;
  directeurNom: string | null;
  directeurPhotoUrl: string | null;
  candidatNom: string | null;
  candidatPrenom: string | null;
  candidatSituation: string | null;
  candidatPhotoUrl: string | null;
}
