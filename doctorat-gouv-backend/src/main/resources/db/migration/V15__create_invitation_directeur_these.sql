-- Migration V15 : suivi des invitations envoyees aux directeurs de these (DT)
-- Permet de ne jamais envoyer plus d'un mail d'invitation par DT (meme si plusieurs sujets).
CREATE TABLE invitation_directeur_these (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    nom VARCHAR(100),
    prenom VARCHAR(100),
    civilite VARCHAR(20),
    date_envoi TIMESTAMP NOT NULL,
    proposition_these_id BIGINT,
    source_plateforme VARCHAR(20),
    CONSTRAINT fk_invitation_proposition FOREIGN KEY (proposition_these_id) REFERENCES proposition_these(id)
);