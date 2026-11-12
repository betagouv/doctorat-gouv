-- Vérification de l'e-mail (V2)
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS email_verified BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP;
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS email_verification_token VARCHAR(255);
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS email_verification_expires_at TIMESTAMP;
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS pending_email VARCHAR(255);

-- Les comptes existants (créés avant V2) sont considérés comme vérifiés
-- (pas de rupture de service pour les utilisateurs déjà inscrits).
UPDATE utilisateur SET email_verified = TRUE, email_verified_at = date_creation WHERE email_verified = FALSE;