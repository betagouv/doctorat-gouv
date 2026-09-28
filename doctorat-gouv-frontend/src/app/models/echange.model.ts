/** Un message échangé sur une demande de mise en relation acceptée. */
export interface MessageEchange {
  id: number;
  demandeId: number;
  auteurId: string | null;
  auteurNom: string | null;
  envoyeParMoi: boolean;
  contenu: string | null;
  dateEnvoi: string | null;
}
