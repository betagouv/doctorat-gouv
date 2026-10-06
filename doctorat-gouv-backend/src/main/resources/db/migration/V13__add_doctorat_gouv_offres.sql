-- Migration V13 : Offres d'accompagnement générées depuis l'espace directeur de thèse.
-- 1. Nouvelle valeur de source possible : 'DOCTORAT_GOUV'.
--    ATTENTION : les bases existantes portent un CHECK (créé hors repo) :
--      CONSTRAINT proposition_these_source_check CHECK (source IN ('ADUM', 'AMETHIS'))
--    Il DOIT être élargi, sinon l'insertion des offres échoue avec :
--      "violates check constraint proposition_these_source_check".
-- 2. Nouveau champ "Votre expertise en quelques mots" recopié du profil DT vers la proposition.
-- 3. Lien traçabilité proposition <-> axe de recherche (UUID stable généré côté espace DT).
-- 4. Identifiant stable par axe pour une synchronisation idempotente (1 axe contactable = 1 offre).

-- 1. Elargir le CHECK sur source (sans cela : DataIntegrityViolationException à la création d'offre).
ALTER TABLE proposition_these DROP CONSTRAINT IF EXISTS proposition_these_source_check;

ALTER TABLE proposition_these ADD CONSTRAINT proposition_these_source_check
  CHECK (source IN ('ADUM', 'AMETHIS', 'DOCTORAT_GOUV'));

-- 2. Nouveaux champs.
ALTER TABLE proposition_these ADD COLUMN IF NOT EXISTS expertise_mots TEXT;

ALTER TABLE proposition_these ADD COLUMN IF NOT EXISTS axe_external_id VARCHAR(36);

ALTER TABLE profil_dt_axes ADD COLUMN IF NOT EXISTS axe_external_id VARCHAR(36);
