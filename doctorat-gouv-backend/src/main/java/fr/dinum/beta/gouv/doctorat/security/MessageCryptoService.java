package fr.dinum.beta.gouv.doctorat.security;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.config.MessageEncryptionProperties;

/**
 * Chiffrement symétrique AES-256-GCM des contenus de messages.
 *
 * <p>Format stocké en base : {@code ENC1:<base64(iv 12o || ciphertext+tag)>}.
 * Les valeurs sans préfixe sont considérées comme des messages historiques
 * en clair et retournées telles quelles (migration paresseuse : elles sont
 * re-chiffrées au prochain {@code save} via le converter JPA).</p>
 */
@Service
public class MessageCryptoService {

    private static final Logger log = LoggerFactory.getLogger(MessageCryptoService.class);

    public static final String PREFIX = "ENC1:";

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_BYTES = 12;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public MessageCryptoService(MessageEncryptionProperties properties) {
        String base64Key = properties != null ? properties.getKey() : null;
        if (base64Key == null || base64Key.isBlank()) {
            if (properties != null && properties.isAllowEphemeralKeyForTests()) {
                byte[] ephemeral = new byte[32];
                new SecureRandom().nextBytes(ephemeral);
                this.keySpec = new SecretKeySpec(ephemeral, "AES");
                log.warn("MESSAGE_ENCRYPTION_KEY absente : clé éphémère de TEST générée. "
                    + "Données chiffrées illisibles après redémarrage. "
                    + "Ne jamais activer allow-ephemeral-key-for-tests hors tests.");
                return;
            }
            throw new IllegalStateException(
                "Clé de chiffrement des messages manquante : définissez la variable "
                + "d'environnement MESSAGE_ENCRYPTION_KEY (Base64 de 32 octets, ex: openssl rand -base64 32)");
        }
        final byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(base64Key.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("MESSAGE_ENCRYPTION_KEY n'est pas du Base64 valide", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException(
                "MESSAGE_ENCRYPTION_KEY invalide : attendu 32 octets (AES-256), reçu " + keyBytes.length);
        }
        this.keySpec = new SecretKeySpec(keyBytes, "AES");
        log.info("Chiffrement des messages actif (clé MESSAGE_ENCRYPTION_KEY configurée, {} caractères)",
            base64Key.trim().length());
    }

    /** Chiffre un texte clair. {@code null} -&gt; {@code null}. */
    public String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[IV_BYTES + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, IV_BYTES);
            System.arraycopy(ciphertext, 0, combined, IV_BYTES, ciphertext.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Échec du chiffrement du message", e);
        }
    }

    /**
     * Déchiffre une valeur base. {@code null} -&gt; {@code null}.
     * Les valeurs sans préfixe {@code ENC1:} (historique en clair) sont retournées telles quelles.
     */
    public String decrypt(String dbValue) {
        if (dbValue == null) {
            return null;
        }
        if (!dbValue.startsWith(PREFIX)) {
            log.debug("Message historique en clair détecté (sans préfixe), retour tel quel");
            return dbValue;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(dbValue.substring(PREFIX.length()));
            if (combined.length < IV_BYTES + 1) {
                throw new IllegalStateException("Payload chiffré trop court");
            }
            byte[] iv = new byte[IV_BYTES];
            byte[] ciphertext = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException(
                "Échec du déchiffrement du message (clé incorrecte ou données corrompues)", e);
        }
    }

    /** Indique si une valeur base est chiffrée (préfixe {@code ENC1:}). */
    public boolean isEncrypted(String dbValue) {
        return dbValue != null && dbValue.startsWith(PREFIX);
    }
}
