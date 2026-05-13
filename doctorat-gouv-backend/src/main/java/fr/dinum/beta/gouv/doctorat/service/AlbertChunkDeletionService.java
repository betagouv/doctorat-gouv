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
 *  Ce service est responsable de la suppression des chunks associés à un document dans Albert.
 *  Il est utilisé lorsque les chunks d’un document doivent être mis à jour ou supprimés,
 *  afin de garantir que les données obtenues par les utilisateurs via Albert restent à jour et pertinentes.
 */
@Service
public class AlbertChunkDeletionService {
	
	private static final Logger log = LoggerFactory.getLogger(AlbertChunkDeletionService.class);

    private final RestTemplate restTemplate;

    @Value("${albert.api.key}")
    private String apiKey;

    @Value("${albert.base-url}")
    private String baseUrl;

    public AlbertChunkDeletionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Supprime tous les chunks associés à un document dans Albert en utilisant l’ID du document.
     * @param documentId
     */
    public void deleteChunks(Long documentId) {
    	log.info("Suppression des chunks du document Albert ID {}...", documentId);
        String url = baseUrl + "/documents/" + documentId + "/chunks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);

        HttpEntity<Void> request = new HttpEntity<>(headers);

        restTemplate.exchange(url, HttpMethod.DELETE, request, Void.class);
        
        log.info("Chunks du document Albert ID {} supprimés avec succès.", documentId);
    }
}

