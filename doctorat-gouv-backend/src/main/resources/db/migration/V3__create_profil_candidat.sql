-- Migration V3 : Extraire le profil candidat dans une table dédiée
-- À exécuter après que Hibernate a créé la table profil_candidat (ddl-auto=update)

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

-- 3. Supprimer les anciennes colonnes de utilisateur (optionnel, à faire après vérification)
-- ALTER TABLE utilisateur DROP COLUMN civilite;
-- ALTER TABLE utilisateur DROP COLUMN situation;
-- ALTER TABLE utilisateur DROP COLUMN telephone;
-- ALTER TABLE utilisateur DROP COLUMN master_confirme;
-- ALTER TABLE utilisateur DROP COLUMN cv_filename;
-- DROP TABLE utilisateur_pieces;
