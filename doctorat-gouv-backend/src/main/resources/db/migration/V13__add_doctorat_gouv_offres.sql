-- Migration V13 : Offres d'accompagnement générées depuis l'espace directeur de thèse.
-- 1. Nouvelle valeur de source possible : 'DOCTORAT_GOUV' (enum gérée côté applicatif,
--    stockée en VARCHAR, aucune contrainte à modifier).
-- 2. Nouveau champ "Votre expertise en quelques mots" recopié du profil DT vers la proposition.
-- 3. Lien traçabilité proposition <-> axe de recherche (UUID stable généré côté espace DT).
-- 4. Identifiant stable par axe pour une synchronisation idempotente (1 axe contactable = 1 offre).

ALTER TABLE proposition_these ADD COLUMN IF NOT EXISTS expertise_mots TEXT;

ALTER TABLE proposition_these ADD COLUMN IF NOT EXISTS axe_external_id VARCHAR(36);

ALTER TABLE profil_dt_axes ADD COLUMN IF NOT EXISTS axe_external_id VARCHAR(36);
