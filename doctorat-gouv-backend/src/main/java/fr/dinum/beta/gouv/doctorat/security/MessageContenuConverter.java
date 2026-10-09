package fr.dinum.beta.gouv.doctorat.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converter JPA : chiffre {@code MessageEchange.contenu} à l'écriture
 * et déchiffre à la lecture. Transparent pour {@code EchangeService}.
 *
 * <p>Le converter est instancié par Hibernate (pas par Spring) : le
 * {@code MessageCryptoService} est injecté via un holder statique
 * alimenté par {@code MessageEncryptionConfig} au démarrage.</p>
 */
@Converter
public class MessageContenuConverter implements AttributeConverter<String, String> {

    private static volatile MessageCryptoService cryptoService;

    public static void bind(MessageCryptoService service) {
        cryptoService = service;
    }

    /** Réservé aux tests unitaires sans contexte Spring. */
    static void resetForTests() {
        cryptoService = null;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        MessageCryptoService crypto = cryptoService;
        if (crypto == null) {
            throw new IllegalStateException(
                "MessageCryptoService non initialisé : vérifiez MESSAGE_ENCRYPTION_KEY et le scan Spring");
        }
        return crypto.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        MessageCryptoService crypto = cryptoService;
        if (crypto == null) {
            throw new IllegalStateException(
                "MessageCryptoService non initialisé : vérifiez MESSAGE_ENCRYPTION_KEY et le scan Spring");
        }
        return crypto.decrypt(dbData);
    }
}
