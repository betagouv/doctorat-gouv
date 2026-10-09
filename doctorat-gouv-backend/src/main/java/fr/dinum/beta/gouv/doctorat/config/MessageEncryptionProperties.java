package fr.dinum.beta.gouv.doctorat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Clé de chiffrement des messages échangés (candidat &lt;-&gt; directeur de thèse).
 *
 * <p>La clé est une clé AES-256 encodée en Base64 (32 octets =&gt; 44 caractères Base64),
 * fournie via la variable d'environnement {@code MESSAGE_ENCRYPTION_KEY}.</p>
 *
 * <p>Génération d'une clé :
 * <pre>openssl rand -base64 32</pre></p>
 */
@Component
@ConfigurationProperties(prefix = "app.messages.encryption")
public class MessageEncryptionProperties {

    /**
     * Clé AES-256 en Base64. Obligatoire, sauf repli éphémère de test (ci-dessous).
     * Ne jamais committer de valeur : uniquement via la variable
     * d'environnement {@code MESSAGE_ENCRYPTION_KEY}.
     */
    private String key;

    /**
     * Repli de test uniquement : génère une clé éphémère aléatoire à chaque
     * démarrage quand aucune clé n'est configurée. Données illisibles après
     * redémarrage : à n'activer que dans {@code src/test/resources}.
     * Ne jamais activer en dev/rec/prod.
     */
    private boolean allowEphemeralKeyForTests = false;

    /**
     * Si vrai, rechiffre au démarrage les lignes historiques restées en clair.
     * À activer une seule fois au déploiement (env MIGRATE_LEGACY), puis désactiver.
     */
    private boolean migrateLegacyOnStartup = false;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isMigrateLegacyOnStartup() {
        return migrateLegacyOnStartup;
    }

    public void setMigrateLegacyOnStartup(boolean migrateLegacyOnStartup) {
        this.migrateLegacyOnStartup = migrateLegacyOnStartup;
    }

    public boolean isAllowEphemeralKeyForTests() {
        return allowEphemeralKeyForTests;
    }

    public void setAllowEphemeralKeyForTests(boolean allowEphemeralKeyForTests) {
        this.allowEphemeralKeyForTests = allowEphemeralKeyForTests;
    }
}
