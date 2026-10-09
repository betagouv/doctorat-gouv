package fr.dinum.beta.gouv.doctorat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.SecureRandom;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import fr.dinum.beta.gouv.doctorat.config.MessageEncryptionProperties;

class MessageCryptoServiceTest {

    /**
     * Clé générée à l'exécution (jamais committée) : chaque lancement utilise
     * une clé fraîche, GitGuardian ne détecte aucun secret dans le repo.
     */
    private static final String TEST_KEY = generateKey();

    private static String generateKey() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private MessageCryptoService service() {
        MessageEncryptionProperties props = new MessageEncryptionProperties();
        props.setKey(TEST_KEY);
        return new MessageCryptoService(props);
    }

    private MessageCryptoService serviceWithKey(String base64Key) {
        MessageEncryptionProperties props = new MessageEncryptionProperties();
        props.setKey(base64Key);
        return new MessageCryptoService(props);
    }

    @Test
    void encrypt_decrypt_roundTrip() {
        MessageCryptoService crypto = service();
        String clair = "Bonjour, proposition de sujet : IA & santé — caractères accentués éèê ✓";
        String chiffre = crypto.encrypt(clair);
        assertTrue(crypto.isEncrypted(chiffre));
        assertFalse(chiffre.contains(clair));
        assertEquals(clair, crypto.decrypt(chiffre));
    }

    @Test
    void encrypt_memeClair_produitDeuxCiphertextDifferents_aleatoireIV() {
        MessageCryptoService crypto = service();
        String a = crypto.encrypt("même message");
        String b = crypto.encrypt("même message");
        assertNotEquals(a, b);
        assertEquals("même message", crypto.decrypt(a));
        assertEquals("même message", crypto.decrypt(b));
    }

    @Test
    void decrypt_valeurHistoriqueEnClair_retourneeTelleQuelle() {
        MessageCryptoService crypto = service();
        assertEquals("ancien message en clair", crypto.decrypt("ancien message en clair"));
        assertFalse(crypto.isEncrypted("ancien message en clair"));
    }

    @Test
    void encrypt_decrypt_null() {
        MessageCryptoService crypto = service();
        assertNull(crypto.encrypt(null));
        assertNull(crypto.decrypt(null));
    }

    @Test
    void decrypt_payloadAlteree_echoue() {
        MessageCryptoService crypto = service();
        String chiffre = crypto.encrypt("secret");
        String alteree = chiffre.substring(0, chiffre.length() - 2) + "AA";
        assertThrows(IllegalStateException.class, () -> crypto.decrypt(alteree));
    }

    @Test
    void decrypt_avecMauvaiseCle_echoue() {
        MessageCryptoService crypto = service();
        String chiffre = crypto.encrypt("secret");
        String autreCle = Base64.getEncoder().encodeToString(new byte[32]);
        // 32 octets à zéro : clé valide en format mais différente
        MessageCryptoService autre = serviceWithKey(autreCle);
        assertThrows(IllegalStateException.class, () -> autre.decrypt(chiffre));
    }

    @Test
    void constructeur_sansCleNiRepliTest_echoueAvecMessageClair() {
        MessageEncryptionProperties props = new MessageEncryptionProperties();
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> new MessageCryptoService(props));
        assertTrue(ex.getMessage().contains("MESSAGE_ENCRYPTION_KEY"));
    }

    @Test
    void constructeur_sansCleMaisRepliTest_genereCleEphemere() {
        MessageEncryptionProperties props = new MessageEncryptionProperties();
        props.setAllowEphemeralKeyForTests(true);
        MessageCryptoService crypto = new MessageCryptoService(props);
        assertEquals("hello", crypto.decrypt(crypto.encrypt("hello")));
    }

    @Test
    void constructeur_cleTropCourte_echoue() {
        String courte = Base64.getEncoder().encodeToString(new byte[16]);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
            () -> serviceWithKey(courte));
        assertTrue(ex.getMessage().contains("32 octets"));
    }

    @Test
    void constructeur_cleNonBase64_echoue() {
        assertThrows(IllegalStateException.class, () -> serviceWithKey("!!!pas-du-base64!!!"));
    }
}
