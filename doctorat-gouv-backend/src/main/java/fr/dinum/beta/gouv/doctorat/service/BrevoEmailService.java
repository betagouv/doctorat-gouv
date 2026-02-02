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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.dto.Attachment;

/**
 *  Service pour envoyer des e‑mails via l’API Brevo (anciennement Sendinblue).
 */
@Service
public class BrevoEmailService {
	
	private static final Logger log = LoggerFactory.getLogger(BrevoEmailService.class);

	@Value("${brevo.api.key}")
	private String apiKey;
	
	@Value("${brevo.api.url}")
	private String apiUrl;

	@Value("${brevo.sender.email}")
	private String senderEmail;

	@Value("${brevo.sender.name}")
	private String senderName;

	private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * Envoie un e‑mail en utilisant un template Brevo.
	 * 
	 * @param to        Adresse e‑mail du destinataire.
	 * @param templateId ID du template Brevo à utiliser.
	 * @param params    Paramètres à injecter dans le template.
	 * @throws JsonProcessingException
	 */	
	public void sendTemplateEmail(String to, int templateId, Map<String, Object> params) throws JsonProcessingException {
	 
	    Map<String, Object> body = new HashMap<>();
	    body.put("sender", Map.of("email", senderEmail, "name", senderName));
	    body.put("to", List.of(Map.of("email", to)));
	    body.put("templateId", templateId);
	    body.put("params", params);
	 
	    ObjectMapper mapper = new ObjectMapper();
	    String jsonBody = mapper.writeValueAsString(body);
	    log.debug("JSON envoyé à Brevo : {}", jsonBody);
	 
	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.set("api-key", apiKey);
	 
	    HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);
	 
	    ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, request, String.class);
	    log.info("Réponse Brevo : {}", response.getBody());
	}
	
	
	/**
	 * Envoie un e‑mail en utilisant un template Brevo avec PJ.
	 * 
	 * @param to        Adresse e‑mail du destinataire.
	 * @param templateId ID du template Brevo à utiliser.
	 * @param params    Paramètres à injecter dans le template.
	 * @throws JsonProcessingException
	 */	
	public void sendTemplateEmailWithAttachments(String to, int templateId, Map<String, Object> params, List<Attachment> attachments) throws JsonProcessingException {

	    Map<String, Object> body = new HashMap<>();
	    body.put("sender", Map.of("email", senderEmail, "name", senderName));
	    body.put("to", List.of(Map.of("email", to)));
	    body.put("templateId", templateId);
	    body.put("params", params);

	    if (attachments != null && !attachments.isEmpty()) {
	        List<Map<String, String>> attList = new ArrayList<>();
	        for (Attachment att : attachments) {
	            attList.add(Map.of(
	                    "name", att.getName(),
	                    "content", att.getBase64()
	            ));
	        }
	        body.put("attachment", attList);
	    }

	    HttpHeaders headers = new HttpHeaders();
	    headers.setContentType(MediaType.APPLICATION_JSON);
	    headers.set("api-key", apiKey);

	    HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
	    restTemplate.postForEntity(apiUrl, request, String.class);
	}
	
}
