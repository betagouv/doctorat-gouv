package fr.dinum.beta.gouv.doctorat.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.dto.AlbertDocumentRequest;
import fr.dinum.beta.gouv.doctorat.dto.AlbertDocumentResponse;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import jakarta.annotation.PostConstruct;

@Service
public class AlbertDocumentService {
	
	private static final Logger log = LoggerFactory.getLogger(AlbertDocumentService.class);

	private final RestTemplate restTemplate;

	@Value("${albert.api.key}")
	private String apiKey;

	@Value("${albert.collection.name}")
	private String collectionName;
	
	@Value("${albert.collection.id}")
	private Integer collectionId;

	@Value("${albert.base-url:https://albert.api.etalab.gouv.fr/v1}")
	private String baseUrl;

	public AlbertDocumentService(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	
	public Long createDocument(PropositionThese sujet) {

	    String url = baseUrl + "/documents";

	    Map<String, Object> metadata = new HashMap<>();
	    metadata.put("id_interne", sujet.getId());
	    metadata.put("matricule", sujet.getMatricule());
	    metadata.put("titre", sujet.getTheseTitre());
	    metadata.put("etablissement", sujet.getEtablissementLibelle());

		AlbertDocumentRequest body = new AlbertDocumentRequest(collectionId, metadata);
		
		// Debug : afficher le JSON envoyé
		debugBodyJson(body);

	    ObjectMapper mapper = new ObjectMapper();
	    String json = "";
	    try {
	    	 json = mapper.writeValueAsString(body);
			log.info(">>> JSON envoyé = " + json);
		} catch (JsonProcessingException e) {
			log.error("Erreur lors de la conversion de l'objet en JSON", e);
		}

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.set("Authorization", "Bearer " + apiKey);
	    HttpEntity<AlbertDocumentRequest> request = new HttpEntity<>(body, headers);

	    ResponseEntity<AlbertDocumentResponse> response =
	            restTemplate.postForEntity(url, request, AlbertDocumentResponse.class);

	    return response.getBody().getId();
	}


	private void debugBodyJson(AlbertDocumentRequest body) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			log.info(">>> JSON envoyé = " + mapper.writeValueAsString(body));
		} catch (JsonProcessingException e) {
			log.error("Erreur lors de la conversion de l'objet en JSON", e);
		}
	}
	
	@PostConstruct
	public void debug() {
	    log.info(">>> DEBUG collectionId = " + collectionId);
	    log.info(">>> DEBUG collectionName = " + collectionName);
	}

}
