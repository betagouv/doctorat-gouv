package fr.dinum.beta.gouv.doctorat.config;

import org.springframework.context.annotation.Configuration;

import fr.dinum.beta.gouv.doctorat.security.MessageContenuConverter;
import fr.dinum.beta.gouv.doctorat.security.MessageCryptoService;
import jakarta.annotation.PostConstruct;

/**
 * Branche le {@code MessageCryptoService} Spring dans le converter JPA
 * (instancié par Hibernate, hors conteneur Spring).
 */
@Configuration
public class MessageEncryptionConfig {

    private final MessageCryptoService cryptoService;

    public MessageEncryptionConfig(MessageCryptoService cryptoService) {
        this.cryptoService = cryptoService;
    }

    @PostConstruct
    public void bindConverter() {
        MessageContenuConverter.bind(cryptoService);
    }
}
