-- Migration V5 : Création de la table profil_directeur_these
-- Table séparée pour les attributs spécifiques au directeur de thèse

CREATE TABLE profil_directeur_these (
    id VARCHAR(36) PRIMARY KEY,
    civilite VARCHAR(20),
    orcid VARCHAR(30),
    etablissement VARCHAR(255),
    laboratoire VARCHAR(255),
    ecole_doctorale VARCHAR(255),
    employeur VARCHAR(255),
    titre VARCHAR(100),
    precision_titre VARCHAR(255),
    habilitation_recherche VARCHAR(30),
    CONSTRAINT fk_pdthese_utilisateur FOREIGN KEY (id) REFERENCES utilisateur(id)
);
