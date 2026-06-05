package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ScalewayEmbeddingService {

	private static final Logger log = LoggerFactory.getLogger(ScalewayEmbeddingService.class);

	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${scaleway.api.key}")
	private String apiKey;

	@Value("${scaleway.api.url}")
	private String apiUrl;

	@Value("${scaleway.model:bge-multilingual-gemma2}")
	private String model;

	public ScalewayEmbeddingService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	public float[] embed(String text) {
		List<String> inputs = new ArrayList<>();
		inputs.add(text);
		List<float[]> results = embedBatch(inputs);
		return results.isEmpty() ? null : results.get(0);
	}

	public List<float[]> embedBatch(List<String> texts) {
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setBearerAuth(apiKey);

			String body = objectMapper.writeValueAsString(
				java.util.Map.of("model", model, "input", texts));

			HttpEntity<String> request = new HttpEntity<>(body, headers);
			ResponseEntity<JsonNode> response = restTemplate.postForEntity(
				apiUrl, request, JsonNode.class);

			JsonNode dataArray = response.getBody().get("data");
			List<float[]> embeddings = new ArrayList<>();
			for (JsonNode item : dataArray) {
				JsonNode embeddingNode = item.get("embedding");
				float[] vec = new float[embeddingNode.size()];
				for (int i = 0; i < embeddingNode.size(); i++) {
					vec[i] = (float) embeddingNode.get(i).asDouble();
				}
				embeddings.add(vec);
			}
			return embeddings;

		} catch (Exception e) {
			log.error("Erreur appel API Scaleway ({} textes)", texts.size(), e);
			throw new RuntimeException("Échec de l'appel d'embedding Scaleway", e);
		}
	}
}
