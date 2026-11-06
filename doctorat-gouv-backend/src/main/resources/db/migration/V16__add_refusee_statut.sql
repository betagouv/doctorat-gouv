-- Autorise le statut REFUSEE sur les demandes de mise en relation.
-- Ajoute la date de refus et l'explication transmise au candidat.
ALTER TABLE demande_mise_en_relation DROP CONSTRAINT IF EXISTS demande_mise_en_relation_statut_check;
ALTER TABLE demande_mise_en_relation ADD CONSTRAINT demande_mise_en_relation_statut_check
    CHECK (statut IN ('BROUILLON', 'CREE', 'ACCEPTEE', 'REFUSEE'));
ALTER TABLE demande_mise_en_relation ADD COLUMN IF NOT EXISTS date_refus TIMESTAMP;
ALTER TABLE demande_mise_en_relation ADD COLUMN IF NOT EXISTS explication_refus TEXT;