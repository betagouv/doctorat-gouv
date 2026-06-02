package fr.dinum.beta.gouv.doctorat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration HTTP pour les appels REST vers l'API Albert.
 *
 * CONFORMITÉ RGPD :
 * Le LoggingInterceptor attaché au RestTemplate ne loggue ni le corps des requêtes/réponses
 * ni les en-têtes HTTP (qui contiennent la clé d'API Bearer). Seules les métadonnées
 * techniques (URI, méthode, code HTTP) sont conservées.
 */
@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(60_000);
        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(new LoggingInterceptor());
        return restTemplate;
    }
}
