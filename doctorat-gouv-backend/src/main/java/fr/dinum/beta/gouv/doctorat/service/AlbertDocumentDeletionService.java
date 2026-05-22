package fr.dinum.beta.gouv.doctorat.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service responsable de la suppression des documents dans Albert.
 * Utilisé lorsqu'un sujet de thèse est désactivé ou supprimé.
 *
 * CONFORMITÉ RGPD :
 * - Seul l'identifiant technique du document Albert est loggé.
 * - Aucune donnée à caractère personnel (matricule, titre, etc.) n'est tracée.
 */
@Service
public class AlbertDocumentDeletionService {
	
	private static final Logger log = LoggerFactory.getLogger(AlbertDocumentDeletionService.class);

    private final RestTemplate restTemplate;

    @Value("${albert.api.key}")
    private String apiKey;

    @Value("${albert.base-url}")
    private String baseUrl;

    public AlbertDocumentDeletionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Supprime un document d’Albert en utilisant son ID.
     * @param documentId
     */
    public void deleteDocument(Long documentId) {
    	
    	log.info("Suppression du document Albert ID {}...", documentId);
    	
        String url = baseUrl + "/documents/" + documentId;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
        
        log.info("Document Albert ID {} supprimé avec succès.", documentId);
    }
}

