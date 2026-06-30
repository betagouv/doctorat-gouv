package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
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

import fr.dinum.beta.gouv.doctorat.dto.ChunkWithType;

/**
 * Service dédié à l’upload de chunks vers Albert.
 *
 * CONFORMITÉ RGPD :
 * - Le contenu textuel des chunks n'est pas loggé : il est extrait du sujet de thèse
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
     * Upload de tous les chunks d'un document Albert en une seule requête HTTP.
     *
     * RGPD : le contenu des chunks n'est pas loggé car il peut contenir
     * des données personnelles. Seul l'ID technique du document est tracé.
     *
     * @param documentId identifiant du document Albert
     * @param chunks     liste des chunks à uploader (contenu + type)
     * @param propositionTheseId identifiant interne du sujet de thèse (id_interne)
     * @param matricule  matricule du sujet de thèse
     */
    public void uploadChunks(Long documentId, List<ChunkWithType> chunks,
                             Long propositionTheseId, String matricule) {

    	log.debug("Upload de {} chunk(s) vers le document Albert {}", chunks.size(), documentId);

        String url = baseUrl + "/documents/" + documentId + "/chunks";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        List<Map<String, Object>> chunkObjects = new ArrayList<>();
        for (ChunkWithType c : chunks) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "these");
            metadata.put("type", c.type());
            if (propositionTheseId != null) {
                metadata.put("id_interne", propositionTheseId);
            }
            if (matricule != null) {
                metadata.put("matricule", matricule);
            }

            Map<String, Object> chunkObject = new HashMap<>();
            chunkObject.put("content", c.content());
            chunkObject.put("metadata", metadata);
            chunkObjects.add(chunkObject);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("chunks", chunkObjects);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(url, request, Void.class);

        log.info("{} chunk(s) uploadé(s) avec succès vers le document Albert {}", chunks.size(), documentId);
    }
}

