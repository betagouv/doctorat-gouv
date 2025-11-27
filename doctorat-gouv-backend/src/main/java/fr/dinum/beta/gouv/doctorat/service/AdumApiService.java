package fr.dinum.beta.gouv.doctorat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import fr.dinum.beta.gouv.doctorat.config.AdumApiProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;

/**
 *  Service pour interagir avec l'API ADUM.
 */
@Service
public class AdumApiService {
	
	private static final Logger log = LoggerFactory.getLogger(AdumApiService.class);

	private final AdumApiProperties properties;
	private final RestTemplate restTemplate;

	public AdumApiService(AdumApiProperties properties) {
		this.properties = properties;
		this.restTemplate = new RestTemplate();
		this.restTemplate.getInterceptors()
				.add(new BasicAuthenticationInterceptor(properties.getUsername(), properties.getPassword()));
	}

	/**
	 * Méthode pour importer les propositions de thèses depuis l'API ADUM.
	 * @return
	 */
	public String importPropositions() {
		log.info("Import des propositions de thèses depuis ADUM pour l'année {}", properties.getYear());
		String url = String.format("%s?annee=%d", properties.getBaseUrl(), properties.getYear());
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		log.info("Import terminé avec le statut HTTP : {}", response.getStatusCode());
		return response.getBody();
	}
}
