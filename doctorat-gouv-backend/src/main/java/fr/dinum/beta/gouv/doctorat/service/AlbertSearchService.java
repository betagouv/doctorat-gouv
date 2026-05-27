package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import fr.dinum.beta.gouv.doctorat.dto.AlbertSearchHit;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;

/**
 * Service dédié à la recherche sémantique dans Albert.
 *
 * CONFORMITÉ RGPD :
 * - Les requêtes de recherche (question) ne sont pas loggées : elles pourraient contenir
 *   des données personnelles saisies par l'utilisateur (ex. nom, email).
 * - Le contenu des chunks renvoyés par Albert n'est pas loggé : il peut contenir des
 *   données à caractère personnel (ex. matricule du doctorant, informations nominatives).
 * - Seules les métadonnées techniques sont tracées : nombre de résultats, identifiants
 *   internes de proposition (ID technique, non nominatif).
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

    @Value("${albert.search.api-limit:100}")
    private int apiSearchLimit;

    @Value("${albert.search.score-threshold:0.1}")
    private double scoreThreshold;

    @Value("${albert.search.method:hybrid}")
    private String searchMethod;

    public AlbertSearchService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Effectue une recherche sémantique dans Albert à partir d'une question.
     * Retourne la réponse brute (utilisé pour la compatibilité).
     *
     * RGPD : la question et la réponse ne sont pas loggées (données potentiellement personnelles).
     */
    public Map search(String question) {
    	
    	log.info("Appel à l'API Albert search");

        String url = baseUrl + "/search";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new HashMap<>();
        body.put("query", question);
        body.put("collection_ids", List.of(collectionId));
        body.put("limit", apiSearchLimit);
        body.put("method", searchMethod);
        if ("semantic".equals(searchMethod)) {
            body.put("score_threshold", scoreThreshold);
        }

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response =
                restTemplate.exchange(url, HttpMethod.POST, entity, Map.class);

        log.debug("Statut HTTP de la réponse Albert : {}", response.getStatusCode());

        return response.getBody();
    }

    /**
     * Effectue une recherche sémantique et retourne des hits structurés.
     * Chaque hit contient l'ID de la proposition, le score, le type de chunk et le contenu.
     *
     * RGPD : le matricule (identifiant personnel) n'est pas loggé. Le contenu des chunks
     * (pouvant contenir des données personnelles) n'est pas loggé non plus.
     */
    public List<AlbertSearchHit> searchHits(String question) {
    	log.debug("Recherche sémantique via Albert");

        Map rawResponse = search(question);
        List<Map<String, Object>> data = (List<Map<String, Object>>) rawResponse.get("data");

        if (data == null || data.isEmpty()) {
            log.info("Aucun résultat trouvé via Albert");
            return List.of();
        }

        List<AlbertSearchHit> hits = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Map<String, Object> item : data) {
            double score = ((Number) item.get("score")).doubleValue();

            Map<String, Object> chunk = (Map<String, Object>) item.get("chunk");
            if (chunk == null) continue;

            String content = (String) chunk.get("content");
            if (content == null || content.isBlank()) continue;

            Map<String, Object> chunkMetadata = (Map<String, Object>) chunk.get("metadata");
            String chunkType = chunkMetadata != null && chunkMetadata.get("type") instanceof String
                    ? (String) chunkMetadata.get("type") : "general";

            // Les métadonnées (id_interne, matricule) sont dans chunk.metadata
            Long propositionTheseId = null;
            String matricule = null;

            if (chunkMetadata != null) {
                Object idObj = chunkMetadata.get("id_interne");
                if (idObj instanceof Number) {
                    propositionTheseId = ((Number) idObj).longValue();
                }
                matricule = (String) chunkMetadata.get("matricule");
            }

            Object docIdObj = chunk.get("document_id");
            Long albertDocumentId = (docIdObj instanceof Number) ? ((Number) docIdObj).longValue() : null;

            // Déduplication par (propositionTheseId, chunkType) pour éviter les doublons
            String dedupKey = propositionTheseId + "_" + chunkType + "_" + content.hashCode();
            if (propositionTheseId == null || seen.contains(dedupKey)) continue;
            seen.add(dedupKey);

            AlbertSearchHit hit = new AlbertSearchHit(
                propositionTheseId, matricule, score, chunkType, content, albertDocumentId
            );
            hits.add(hit);
        }

        // Trier par score décroissant
        hits.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

        log.info("{} résultat(s) trouvé(s) via Albert pour l'ID collection {}", hits.size(), collectionId);
        return hits;
    }

    /**
     * Extrait les mots-clés suggérés depuis les données complètes des thèses (champ motsCles en BDD).
     * Utilise les mots-clés des résultats de recherche plutôt que le contenu des chunks
     * (car les chunks retournés par Albert sont extraits du PDF et ne contiennent pas les mots-clés).
     */
    public List<String> extractKeywordsFromResults(Map<Long, PropositionTheseDto> theseMap) {
        return theseMap.values().stream()
            .filter(dto -> dto.getMotsCles() != null && !dto.getMotsCles().isEmpty())
            .flatMap(dto -> dto.getMotsCles().values().stream())
            .filter(k -> k != null && !k.isBlank())
            .distinct()
            .limit(20)
            .collect(Collectors.toList());
    }

    public String buildAnswerFromSearchResult(Map response, int maxChunks) {

        List<Map<String, Object>> data = (List<Map<String, Object>>) response.get("data");
        if (data == null || data.isEmpty()) {
            return null;
        }

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

        return java.util.Arrays.stream(mergedContent.split("\\R"))
                .map(String::trim)
                .filter(l -> l.startsWith("- "))
                .map(l -> l.substring(2))
                .distinct()
                .collect(Collectors.joining(", "));
    }


}

