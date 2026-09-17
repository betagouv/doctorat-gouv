-- Migration V7 : Ajouter les champs domaines d'expertise au profil directeur de thèse

ALTER TABLE profil_directeur_these ADD COLUMN IF NOT EXISTS domaine_scientifique VARCHAR(100);
ALTER TABLE profil_directeur_these ADD COLUMN IF NOT EXISTS expertise_mots VARCHAR(2000);

CREATE TABLE IF NOT EXISTS profil_dt_mots_cles (
    profil_dt_id VARCHAR(36) NOT NULL,
    mot_cle VARCHAR(100) NOT NULL,
    CONSTRAINT fk_pdts_mots_cles FOREIGN KEY (profil_dt_id) REFERENCES profil_directeur_these(id),
    PRIMARY KEY (profil_dt_id, mot_cle)
);
