-- Migration V4 : Ajouter les champs du profil directeur de thèse
-- Champs optionnels (compatibles avec le profil candidat existant)

ALTER TABLE profil_candidat ADD COLUMN IF NOT EXISTS orcid VARCHAR(30);
ALTER TABLE profil_candidat ADD COLUMN IF NOT EXISTS etablissement VARCHAR(255);
ALTER TABLE profil_candidat ADD COLUMN IF NOT EXISTS laboratoire VARCHAR(255);
