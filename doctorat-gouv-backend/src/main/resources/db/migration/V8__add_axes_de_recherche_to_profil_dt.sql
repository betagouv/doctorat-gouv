ALTER TABLE profil_directeur_these ADD COLUMN description_axes VARCHAR(2000);

CREATE TABLE profil_dt_axes (
    profil_dt_id VARCHAR(255) NOT NULL,
    axe_titre VARCHAR(255),
    axe_precisions VARCHAR(2000),
    axe_contactable BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (profil_dt_id) REFERENCES profil_directeur_these(id)
);
