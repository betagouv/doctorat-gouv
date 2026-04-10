package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.model.AdumResponse;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
/**
 *  Service pour interagir avec l'API ADUM.
 */
@Service
public class AdumApiService {
	
	private static final Logger log = LoggerFactory.getLogger(AdumApiService.class);
	
	@Value("${adum.import.rattrapage:false}")
	private boolean rattrapage;

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
		log.info("URL de l'API ADUM : {}", url);
		ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
		log.info("Import terminé avec le statut HTTP : {}", response.getStatusCode());
		String responseBody = response.getBody();
		log.info("Taille JSON ADUM (brut) = {}", responseBody.length());
		if (rattrapage) {
			// En mode rattrapage, on met à jour toutes les propositions sans vérifier la date de mise à jour
			log.info("Mode RATTRAPAGE activé : toutes les propositions seront mises à jour sans vérification de la date de mise à jour");
		    savePropositionsFromJsonRattrapage(responseBody);
		} else {
			// En mode normal, on ne met à jour que les propositions plus récentes que celles déjà en base
			log.info("Mode NORMAL activé : seules les propositions avec une date de mise à jour plus récente seront mises à jour");
		    savePropositionsFromJson(responseBody);
		}

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
			p.setAnnee(properties.getYear());
			p.setSource(SourceThese.ADUM);
			// Vérifie si la proposition doit être insérée ou mise à jour
			Optional<PropositionThese> existingOpt = propositionTheseRepository.findForUpdate(p.getMatricule(), p.getDateMaj());

			if (existingOpt.isPresent()) {
				// Mise à jour nécessaire
				p.setId(existingOpt.get().getId());
				p.setDateIntegration(existingOpt.get().getDateIntegration()); // On conserve l’ancienne date d’intégration
				p.setActive(true); // réactivation si besoin
				toSave.add(p);
				log.info("Proposition {} mise à jour (dateMaj plus récente)", p.getMatricule());
			} else {
				// Vérifie si elle existe déjà sans besoin de mise à jour
				Optional<PropositionThese> existing = propositionTheseRepository.findByMatricule(p.getMatricule());

			    if (existing.isEmpty()) {
			        // Nouvelle proposition, il faut l'insérer
			    	p.setDateIntegration(LocalDateTime.now());
			        p.setActive(true);
			        toSave.add(p);
			        log.info("Nouvelle proposition {} insérée", p.getMatricule());

			    } else if (Boolean.FALSE.equals(existing.get().getActive())) {
			        // Réactivation
			        p.setId(existing.get().getId());
			        p.setDateIntegration(existing.get().getDateIntegration()); //  on conserve l’ancienne date
			        p.setActive(true);
			        toSave.add(p);
			        log.info("Proposition {} réactivée (était désactivée)", p.getMatricule());

			    } else {
			        log.debug("Proposition {} ignorée (déjà à jour)", p.getMatricule());
			    }
			}
		}

		log.info("Nombre de propositions à insérer/mettre à jour : {}", toSave.size());
		propositionTheseRepository.saveAll(toSave);
		
		// Désactivation des sujets absents d'ADUM
		log.info("Vérification des propositions locales absentes d'ADUM pour désactivation");
		desactivateMissingPropositions(propositions);

	}
	
	/**
	 * Méthode pour sauvegarder les propositions de thèses depuis une chaîne JSON en mode rattrapage (mise à jour systématique).
	 * @param jsonString
	 */
	public void savePropositionsFromJsonRattrapage(String jsonString) {
	    log.info("Sauvegarde des propositions en mode RATTRAPAGE");

	    ObjectMapper mapper = new ObjectMapper();
	    mapper.registerModule(new JavaTimeModule());
	    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

	    AdumResponse response;
	    try {
	        response = mapper.readValue(jsonString, AdumResponse.class);
	    } catch (JsonProcessingException e) {
	        log.error("Erreur de traitement JSON : {}", e.getMessage(), e);
	        return;
	    }

	    List<PropositionThese> propositions = response.getPropositions();
	    List<PropositionThese> toSave = new ArrayList<>();

	    for (PropositionThese p : propositions) {
	    	
	    	log.info("Traitement de la proposition (matricule {}) en mode RATTRAPAGE", p.getMatricule());
	    	
	    	p.setAnnee(properties.getYear());
	    	p.setSource(SourceThese.ADUM);

	        Optional<PropositionThese> existingOpt =
	                propositionTheseRepository.findByMatricule(p.getMatricule());

	        if (existingOpt.isPresent()) {
	            // Mise à jour systématique
	            p.setId(existingOpt.get().getId());
	            p.setDateIntegration(existingOpt.get().getDateIntegration()); // On conserve l’ancienne date d’intégration même en rattrapage
	            p.setActive(true);
	            toSave.add(p);
	            log.info("Proposition {} mise à jour (rattrapage)", p.getMatricule());
	        } else {
	            // Nouvelle proposition
	        	p.setDateIntegration(LocalDateTime.now()); // Nouvelle intégration pour les nouvelles propositions même en rattrapage
	            p.setActive(true);
	            toSave.add(p);
	            log.info("Nouvelle proposition {} insérée (rattrapage)", p.getMatricule());
	        }
	    }

	    log.info("Nombre de propositions à insérer/mettre à jour (rattrapage) : {}", toSave.size());
	    propositionTheseRepository.saveAll(toSave);

	    // Désactivation des sujets absents d'ADUM
	    desactivateMissingPropositions(propositions);
	}

	
	/**
	 * Désactive les propositions locales qui ne sont plus présentes dans la liste ADUM.
	 *
	 * @param propositionsAdum liste des propositions renvoyées par ADUM
	 */
	private void desactivateMissingPropositions(List<PropositionThese> propositionsAdum) {

	    log.info("Début de la désactivation des propositions ADUM manquantes");

	    int currentYear = properties.getYear();

	    // 1. Matricules renvoyés par ADUM aujourd’hui
	    Set<String> matriculesAdum = propositionsAdum.stream()
	            .map(PropositionThese::getMatricule)
	            .collect(Collectors.toSet());

	    // 2. On récupère uniquement les sujets ADUM actifs de l’année courante
	    List<PropositionThese> localAdum =
	            propositionTheseRepository.findActiveBySourceAndAnnee(SourceThese.ADUM, currentYear);

	    // 3. Désactivation des sujets ADUM absents du flux
	    List<PropositionThese> toDesactivate = localAdum.stream()
	            .filter(p -> !matriculesAdum.contains(p.getMatricule()))
	            .peek(p -> p.setActive(false))
	            .collect(Collectors.toList());

	    if (toDesactivate.isEmpty()) {
	        log.info("Aucune désactivation ADUM nécessaire");
	    } else {
	        log.info("Désactivation de {} propositions ADUM", toDesactivate.size());
	        propositionTheseRepository.saveAll(toDesactivate);
	    }

	    log.info("Fin de la désactivation des propositions ADUM manquantes");
	}

	
	public AdumApiProperties getProperties() {
	    return properties;
	}

	
}
