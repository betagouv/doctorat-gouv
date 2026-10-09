package fr.dinum.beta.gouv.doctorat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.dinum.beta.gouv.doctorat.config.MessageEncryptionProperties;

class MessageContenuConverterTest {

    /** Clé générée à l'exécution, jamais committée (GitGuardian). */
    private static final String TEST_KEY = generateKey();

    private static String generateKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    @BeforeEach
    void bind() {
        MessageEncryptionProperties props = new MessageEncryptionProperties();
        props.setKey(TEST_KEY);
        MessageContenuConverter.bind(new MessageCryptoService(props));
    }

    @AfterEach
    void keepBound() {
        // Laisse le converter bindé pour les tests d'intégration du même JVM fork.
        MessageEncryptionProperties props = new MessageEncryptionProperties();
        props.setKey(TEST_KEY);
        MessageContenuConverter.bind(new MessageCryptoService(props));
    }

    @Test
    void roundTrip_chiffreEnBaseEtDechiffreEnEntite() {
        MessageContenuConverter converter = new MessageContenuConverter();
        String clair = "Échange candidat <-> directeur";
        String colonne = converter.convertToDatabaseColumn(clair);
        assertTrue(colonne.startsWith(MessageCryptoService.PREFIX));
        assertEquals(clair, converter.convertToEntityAttribute(colonne));
    }

    @Test
    void nulls() {
        MessageContenuConverter converter = new MessageContenuConverter();
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }

    @Test
    void legacyEnClair_luTelQuel() {
        MessageContenuConverter converter = new MessageContenuConverter();
        assertEquals("historique", converter.convertToEntityAttribute("historique"));
    }

    @Test
    void sansBinding_echoueExplicitement() throws Exception {
        Method reset = MessageContenuConverter.class.getDeclaredMethod("resetForTests");
        reset.setAccessible(true);
        reset.invoke(null);
        try {
            assertThrows(IllegalStateException.class,
                () -> new MessageContenuConverter().convertToDatabaseColumn("x"));
        } finally {
            MessageEncryptionProperties props = new MessageEncryptionProperties();
            props.setKey(TEST_KEY);
            MessageContenuConverter.bind(new MessageCryptoService(props));
        }
    }
}
