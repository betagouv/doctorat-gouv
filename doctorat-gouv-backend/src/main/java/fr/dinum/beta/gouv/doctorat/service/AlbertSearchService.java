package fr.dinum.beta.gouv.doctorat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Service dédié à la recherche sémantique dans Albert.
 */
@Service
public class AlbertSearchService {
	
	private static final Logger log = LoggerFactory.getLogger(AlbertSearchService.class);

    private final RestTemplate restTemplate;

    @Value("${albert.api.key}")
    private String apiKey;

    @Value("${albert.collection.id}")
    private Integer collectionId;

    @Value("${albert.base-url:https://albert.api.etalab.gouv.fr/v1}")
    private String baseUrl;

    public AlbertSearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Effectue une recherche sémantique dans Albert à partir d’une question.
     * @param question
     * @return
     */
    public Map search(String question) {
    	
    	log.info("Recherche dans Albert (question={})", question);

        String url = baseUrl + "/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("query", question);
        body.put("collection_ids", List.of(collectionId));
        body.put("limit", 5);
        body.put("method", "semantic");
        body.put("score_threshold", 0.75);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        
        log.info("Réponse de Albert : {}", response);

        return response.getBody();
    }
}

