package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.dinum.beta.gouv.doctorat.config.AdumApiProperties;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.model.AdumResponse;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
/**
 *  Service pour interagir avec l'API ADUM.
 */
@Service
public class AdumApiService {
	
	private static final Logger log = LoggerFactory.getLogger(AdumApiService.class);

	private final AdumApiProperties properties;
	private final RestTemplate restTemplate;
	private final PropositionTheseRepository propositionTheseRepository;

	public AdumApiService(AdumApiProperties properties, PropositionTheseRepository propositionTheseRepository) {
		this.properties = properties;
		this.propositionTheseRepository = propositionTheseRepository;
		this.restTemplate = new RestTemplate();
		this.restTemplate.getInterceptors()
				.add(new BasicAuthenticationInterceptor(properties.getUsername(), properties.getPassword()));
	}

	/**
	 * Méthode pour importer les propositions de thèses depuis l'API ADUM.
	 * @return
	 */
	public String importAndSavePropositionsFromAdum() {
		log.info("Import des propositions de thèses depuis ADUM pour l'année {}", properties.getYear());
		String url = String.format("%s?annee=%d", properties.getBaseUrl(), properties.getYear());
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		log.info("Import terminé avec le statut HTTP : {}", response.getStatusCode());
		String responseBody = response.getBody();
		log.debug("Taille JSON ADUM (brut) = {}", responseBody.length());
		savePropositionsFromJson(responseBody);
		return responseBody;
	}
	
	/**
	 * Méthode pour sauvegarder les propositions de thèses depuis une chaîne JSON.
	 * @param jsonString
	 */
	public void savePropositionsFromJson(String jsonString) {
		log.info("Sauvegarde des propositions de thèses depuis le JSON");

		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

		AdumResponse response;
		try {
			log.debug("JSON reçu : {}", jsonString);
			response = mapper.readValue(jsonString, AdumResponse.class);
		} catch (JsonProcessingException e) {
			log.error("Erreur de traitement JSON : {}", e.getMessage(), e);
			return;
		}

		List<PropositionThese> propositions = response.getPropositions();
		List<PropositionThese> toSave = new ArrayList<>();

		for (PropositionThese p : propositions) {
			// Vérifie si la proposition doit être insérée ou mise à jour
			Optional<PropositionThese> existingOpt = propositionTheseRepository.findForUpdate(p.getMatricule(), p.getDateMaj());

			if (existingOpt.isPresent()) {
				// Mise à jour nécessaire
				p.setId(existingOpt.get().getId());
				toSave.add(p);
				log.debug("Proposition {} mise à jour (dateMaj plus récente)", p.getMatricule());
			} else {
				// Vérifie si elle existe déjà sans besoin de mise à jour
				if (propositionTheseRepository.findByMatricule(p.getMatricule()).isEmpty()) {
					// Nouvelle proposition
					toSave.add(p);
					log.debug("Nouvelle proposition {} insérée", p.getMatricule());
				} else {
					log.debug("Proposition {} ignorée (déjà à jour)", p.getMatricule());
				}
			}
		}

		log.info("Nombre de propositions à insérer/mettre à jour : {}", toSave.size());
		propositionTheseRepository.saveAll(toSave);
	}
	
}
