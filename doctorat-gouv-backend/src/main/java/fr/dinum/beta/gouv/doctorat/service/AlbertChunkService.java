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
 *
 * CONFORMITÉ RGPD :
 * - Le contenu textuel des chunks (chunkText) n'est pas loggé : il est extrait du sujet de thèse
 *   et peut contenir des données à caractère personnel (nom du doctorant, établissement, etc.).
 * - Seul l'identifiant technique du document Albert est tracé.
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
     *
     * RGPD : le contenu du chunk (chunkText) n'est pas loggé car il peut contenir
     * des données personnelles. Seul l'ID technique du document est tracé.
     *
     * @param documentId identifiant du document Albert
     * @param chunkText  contenu textuel du chunk (non loggé pour conformité RGPD)
     * @param chunkType  type sémantique du chunk (titre, resume, etc.)
     */
    public void uploadChunk(Long documentId, String chunkText, String chunkType) {

    	log.debug("Upload d'un chunk de type '{}' vers le document Albert {}", chunkType, documentId);

        String url = baseUrl + "/documents/" + documentId + "/chunks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> chunkObject = new HashMap<>();
        chunkObject.put("content", chunkText);
        chunkObject.put("metadata", Map.of(
        	    "source", "these",
        	    "type", chunkType
        	));

        Map<String, Object> body = new HashMap<>();
        body.put("chunks", List.of(chunkObject));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, Void.class);

        log.info("Chunk de type '{}' uploadé avec succès vers le document Albert {}", chunkType, documentId);
    }
}

