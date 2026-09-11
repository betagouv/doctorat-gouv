export type StatutMiseEnRelation = 'brouillon' | 'en_attente' | 'mise_en_relation' | 'archive';

export type StatutDemandeBackend = 'BROUILLON' | 'CREE' | 'ARCHIVE';

export interface TableauDeBordCandidatItemBackend {
  id: number;
  propositionTheseId: number;
  titreSujet: string | null;
  etablissement: string | null;
  encadrantNom: string | null;
  dateContact: string | null;
  statut: StatutDemandeBackend;
}

export interface MiseEnRelationDto {
  id: number;
  propositionTheseId: number;
  titreSujet: string;
  etablissement: string;
  encadrantNom: string;
  dateContact: string;
  statut: StatutMiseEnRelation;
  nbMessages?: number;
  candidatureEnvoyee?: boolean;
}
