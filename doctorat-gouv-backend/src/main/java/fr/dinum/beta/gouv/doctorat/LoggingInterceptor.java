package fr.dinum.beta.gouv.doctorat;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Intercepteur HTTP pour les appels vers l'API Albert.
 *
 * CONFORMITÉ RGPD :
 * - Le corps des requêtes/réponses n'est PAS loggé (il peut contenir des données à caractère personnel
 *   comme des noms, matricules, ou contenus de sujets de thèse).
 * - Les en-têtes HTTP sont également exclus des logs car ils contiennent la clé d'API (Bearer token).
 * - Seules les métadonnées techniques (URI, méthode, code de statut) sont conservées.
 */
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        log.debug(">>> Appel HTTP {} {}", request.getMethod(), request.getURI());

        ClientHttpResponse response = execution.execute(request, body);

        log.info(">>> Réponse HTTP {} {} : {}", request.getMethod(), request.getURI(), response.getStatusCode());

        return response;
    }
}

