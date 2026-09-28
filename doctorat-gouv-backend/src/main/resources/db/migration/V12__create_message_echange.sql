-- Fil de discussion d'une demande de mise en relation acceptée :
-- messages échangés entre le candidat et le directeur de thèse.
CREATE TABLE IF NOT EXISTS message_echange (
    id BIGSERIAL PRIMARY KEY,
    demande_id BIGINT NOT NULL REFERENCES demande_mise_en_relation(id),
    auteur_id VARCHAR(255) NOT NULL,
    contenu TEXT NOT NULL,
    lu BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_message_echange_demande ON message_echange(demande_id);
