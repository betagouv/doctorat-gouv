package fr.dinum.beta.gouv.doctorat.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        
        Map<String, Object> metadataFilter = Map.of(
        	    "key", "type",
        	    "type", "eq",
        	    "value", "mots_cles"
        );

        Map<String, Object> body = new HashMap<>();
        body.put("query", question);
        body.put("collection_ids", List.of(collectionId));
        body.put("limit", 10);
        body.put("method", "semantic");
        body.put("score_threshold", 0.4);
        //body.put("metadata_filters", metadataFilter);


        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);
        
        log.info("Réponse de Albert : {}", response);

        return response.getBody();
    }
    
    public String buildAnswerFromSearchResult(Map response, int maxChunks) {

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.isEmpty()) {
            return null;
        }

        // trier par score décroissant
        data.sort((a, b) -> {
            double sa = ((Number) a.get("score")).doubleValue();
            double sb = ((Number) b.get("score")).doubleValue();
            return Double.compare(sb, sa);
        });

        StringBuilder sb = new StringBuilder();
        int count = 0;

        for (Map<String, Object> item : data) {
            if (count >= maxChunks) break;

            Map<String, Object> chunk = (Map<String, Object>) item.get("chunk");
            String content = (String) chunk.get("content");
            if (content == null || content.isBlank()) continue;

            // éviter les répétitions grossières
            String trimmed = content.trim();
            if (sb.indexOf(trimmed) >= 0) continue;

            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append(trimmed);
            count++;
        }

        return sb.length() == 0 ? null : sb.toString();
    }
    
    public String extractKeywords(String mergedContent) {
        if (mergedContent == null || mergedContent.isBlank()) return null;

        return Arrays.stream(mergedContent.split("\\R"))
                .map(String::trim)
                .filter(l -> l.startsWith("- "))
                .map(l -> l.substring(2))
                .distinct()
                .collect(Collectors.joining(", "));
    }


}

