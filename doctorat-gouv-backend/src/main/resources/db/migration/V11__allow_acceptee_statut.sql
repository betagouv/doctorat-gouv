-- Autorise le statut ACCEPTEE sur les demandes de mise en relation.
-- La contrainte a été créée hors Flyway (table non gérée par les migrations V1..V9).
ALTER TABLE demande_mise_en_relation DROP CONSTRAINT IF EXISTS demande_mise_en_relation_statut_check;
ALTER TABLE demande_mise_en_relation ADD CONSTRAINT demande_mise_en_relation_statut_check
    CHECK (statut IN ('BROUILLON', 'CREE', 'ACCEPTEE'));
