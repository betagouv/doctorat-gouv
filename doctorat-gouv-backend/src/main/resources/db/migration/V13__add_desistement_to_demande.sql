-- Désistement (« Je ne suis plus intéressé ») d'un candidat sur une demande acceptée :
-- date/heure et motif conservés, demande archivée mais affichée en MISES EN RELATION (archives).
ALTER TABLE demande_mise_en_relation ADD COLUMN IF NOT EXISTS date_desistement TIMESTAMP;
ALTER TABLE demande_mise_en_relation ADD COLUMN IF NOT EXISTS motif_desistement TEXT;
