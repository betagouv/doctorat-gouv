package fr.dinum.beta.gouv.doctorat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.service.MessageMigrationService;

/**
 * Rechiffre au démarrage les messages historiques restés en clair,
 * uniquement si {@code app.messages.encryption.migrate-legacy-on-startup=true}.
 * Activer une seule fois au déploiement puis désactiver.
 */
@Component
public class MessageMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MessageMigrationRunner.class);

    private final MessageEncryptionProperties properties;
    private final MessageMigrationService migrationService;

    public MessageMigrationRunner(MessageEncryptionProperties properties,
                                  MessageMigrationService migrationService) {
        this.properties = properties;
        this.migrationService = migrationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isMigrateLegacyOnStartup()) {
            return;
        }
        log.info("Migration des messages historiques en clair vers le format chiffré...");
        int total = migrationService.migrateLegacyMessages(200);
        log.info("Migration des messages terminée : {} message(s)", total);
    }
}
