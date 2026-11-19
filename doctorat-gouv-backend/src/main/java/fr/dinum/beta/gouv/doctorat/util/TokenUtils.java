package fr.dinum.beta.gouv.doctorat.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaires pour les tokens à usage unique (activation de compte,
 * confirmation de changement d'e-mail) : on ne stocke jamais le token brut
 * en base, uniquement son hash SHA-256, pour limiter l'impact d'une
 * compromission de la base de données.
 */
public final class TokenUtils {

    private TokenUtils() {
    }

    /**
     * Hash SHA-256 (hexadécimal minuscule) d'un token. Retourne null si null/blank.
     */
    public static String hash(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hashed.length * 2);
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}