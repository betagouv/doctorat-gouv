-- Migration V3 : Extraire le profil candidat dans une table dédiée
-- Phase 1 : Copier les données (à exécuter AVANT de supprimer les colonnes)

-- 1. Copier les données candidat depuis utilisateur vers profil_candidat
INSERT INTO profil_candidat (id, civilite, situation, telephone, master_confirme, cv_filename)
SELECT id, civilite, situation, telephone, master_confirme, cv_filename
FROM utilisateur
WHERE role = 'CANDIDAT'
  AND id NOT IN (SELECT id FROM profil_candidat);

-- 2. Copier les pièces jointes depuis utilisateur_pieces vers profil_candidat_pieces
INSERT INTO profil_candidat_pieces (profil_candidat_id, piece_path)
SELECT up.utilisateur_id, up.piece_path
FROM utilisateur_pieces up
INNER JOIN profil_candidat pc ON pc.id = up.utilisateur_id;

-- Phase 2 : Nettoyer l'ancien schéma (à exécuter APRÈS vérification des données)

-- 3. Supprimer les anciennes colonnes candidat de utilisateur
ALTER TABLE utilisateur DROP COLUMN IF EXISTS civilite;
ALTER TABLE utilisateur DROP COLUMN IF EXISTS situation;
ALTER TABLE utilisateur DROP COLUMN IF EXISTS telephone;
ALTER TABLE utilisateur DROP COLUMN IF EXISTS master_confirme;
ALTER TABLE utilisateur DROP COLUMN IF EXISTS cv_filename;

-- 4. Supprimer l'ancienne table de pièces jointes
DROP TABLE IF EXISTS utilisateur_pieces;
