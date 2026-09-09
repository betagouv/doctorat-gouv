export type StatutMiseEnRelation = 'brouillon' | 'en_attente' | 'mise_en_relation';

export interface MiseEnRelationDto {
  id: number;
  titreSujet: string;
  etablissement: string;
  encadrantNom: string;
  dateContact: string;
  statut: StatutMiseEnRelation;
  nbMessages?: number;
  candidatureEnvoyee?: boolean;
}
