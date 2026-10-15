-- Socle commun notifications : fréquence du récapitulatif des messages non lus (candidat + directeur de thèse).
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS frequence_notification VARCHAR(20) DEFAULT 'JOURNALIERE';
UPDATE utilisateur SET frequence_notification = 'JOURNALIERE' WHERE frequence_notification IS NULL;
