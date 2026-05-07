package fr.dinum.beta.gouv.doctorat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service dédié à l’upload de chunks vers Albert.
 */
@Service
public class AlbertChunkService {
	
	private static final Logger log = LoggerFactory.getLogger(AlbertChunkService.class);

    private final RestTemplate restTemplate;

    @Value("${albert.api.key}")
    private String apiKey;

    @Value("${albert.base-url}")
    private String baseUrl;

    public AlbertChunkService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Upload d’un chunk de texte vers un document Albert déjà créé.
     * @param documentId
     * @param chunkText
     */
    public void uploadChunk(Long documentId, String chunkText) {
    	
    	log.info("Upload chunk vers Albert (documentId={}, chunkText={})", documentId, chunkText);

        String url = baseUrl + "/documents/" + documentId + "/chunks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Chunk conforme à l’API Albert
        Map<String, Object> chunkObject = new HashMap<>();
        chunkObject.put("content", chunkText); // <-- obligatoire
        chunkObject.put("metadata", Map.of("source", "these")); // <-- au moins 1 champ

        Map<String, Object> body = new HashMap<>();
        body.put("chunks", List.of(chunkObject));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, Void.class);
        
        log.info("Chunk uploadé avec succès vers Albert (documentId={})", documentId);
    }
}

