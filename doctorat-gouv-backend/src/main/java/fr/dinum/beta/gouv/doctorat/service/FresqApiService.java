package fr.dinum.beta.gouv.doctorat.service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.config.FresqApiProperties;
import fr.dinum.beta.gouv.doctorat.model.FresqTokenResponse;
import fr.dinum.beta.gouv.doctorat.model.FresqSearchResult;
import fr.dinum.beta.gouv.doctorat.model.FresqSearchContent;
import fr.dinum.beta.gouv.doctorat.model.FresqEtablissementResult;
import fr.dinum.beta.gouv.doctorat.model.FresqDiplomeDetail;

@Service
public class FresqApiService {
	
	private static final Logger log = LoggerFactory.getLogger(FresqApiService.class);
	
	private final FresqApiProperties properties;
	private final RestTemplate restTemplate;
	private final ObjectMapper objectMapper;
	
	private String cachedToken;
	private Instant tokenExpiry;
	
	public FresqApiService(FresqApiProperties properties) {
		this.properties = properties;
		this.objectMapper = new ObjectMapper();
		this.restTemplate = new RestTemplate();
		
		this.restTemplate.getInterceptors()
				.add(new BasicAuthenticationInterceptor(properties.getClientUsername(), ""));
	}
	
	/**
	 * Obtient un token d'accès Fresq avec cache.
	 */
	public String getAccessToken() {
		if (cachedToken != null && tokenExpiry != null && Instant.now().isBefore(tokenExpiry)) {
			log.info("Réutilisation du token Fresq en cache (expire à {})", tokenExpiry);
			return cachedToken;
		}
		
		log.info("Obtention d'un nouveau token Fresq via URL: {}", properties.getTokenUrl());
		log.info("Credentials Fresq - username: '{}', password présent: {}", 
				properties.getUsername(), properties.getPassword() != null && !properties.getPassword().isEmpty());
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		
		MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
		body.add("grant_type", "password");
		body.add("scope", "openid");
		body.add("username", properties.getUsername());
		body.add("password", properties.getPassword());
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
		
		try {
			ResponseEntity<FresqTokenResponse> response = restTemplate.exchange(
					properties.getTokenUrl(),
					HttpMethod.POST,
					request,
					FresqTokenResponse.class
			);
			
			log.info("Réponse token Fresq - status: {}", response.getStatusCode());
			
			FresqTokenResponse tokenResponse = response.getBody();
			if (tokenResponse != null && tokenResponse.getAccessToken() != null) {
				cachedToken = tokenResponse.getAccessToken();
				// Token valid for 4 hours, refresh 5 minutes before expiry
				tokenExpiry = Instant.now().plusSeconds(tokenResponse.getExpiresIn() - 300);
				log.info("Token Fresq obtenu avec succès, expire dans {} secondes", tokenResponse.getExpiresIn());
				return cachedToken;
			} else {
				log.error("Réponse token Fresq vide ou sans access_token. Status: {}, Body: {}", 
						response.getStatusCode(), response.getBody());
			}
		} catch (HttpStatusCodeException e) {
			log.error("Erreur HTTP lors de l'obtention du token Fresq - Status: {}, Body: {}", 
					e.getStatusCode(), e.getResponseBodyAsString());
			if (e.getStatusCode() == HttpStatus.FOUND) {
				log.error("Redirection 302 reçue. Location: {}", 
						e.getResponseHeaders() != null ? e.getResponseHeaders().getFirst("Location") : "N/A");
			}
		} catch (Exception e) {
			log.error("Erreur lors de l'obtention du token Fresq: {}", e.getMessage(), e);
		}
		
		return null;
	}
	
	/**
	 * Vérifie si un établissement existe dans Fresq par son UAI.
	 */
	public boolean existeEtablissement(String uai) {
		FresqEtablissementResult result = rechercherEtablissement(uai);
		boolean existe = result != null && !result.isEmpty();
		log.info("Vérification établissement UAI {}: {}", uai, existe ? "trouvé" : "non trouvé");
		return existe;
	}
	
	/**
	 * Recherche un établissement dans Fresq par son UAI.
	 */
	public FresqEtablissementResult rechercherEtablissement(String uai) {
		String token = getAccessToken();
		if (token == null) {
			log.error("Impossible d'obtenir le token Fresq pour la recherche d'établissement");
			return null;
		}
		
		String url = String.format("%s/operateurs/etablissement_ecole?filter_uai=%s", 
				properties.getBaseUrl(), uai);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		
		HttpEntity<Void> request = new HttpEntity<>(headers);
		
		try {
			ResponseEntity<FresqEtablissementResult> response = restTemplate.exchange(
					url,
					HttpMethod.GET,
					request,
					FresqEtablissementResult.class
			);
			
			FresqEtablissementResult result = response.getBody();
			if (result != null) {
				log.info("Recherche établissement UAI {}: {} résultat(s)", uai, 
						result.getDatas() != null ? result.getDatas().size() : 0);
			}
			return result;
		} catch (Exception e) {
			log.error("Erreur lors de la recherche d'établissement Fresq pour UAI {}: {}", uai, e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Recherche des diplômes de doctorat dans Fresq.
	 */
	public FresqSearchResult rechercherDiplomesDoctorat(int pageSize, int pageNumber) {
		String token = getAccessToken();
		if (token == null) {
			log.error("Impossible d'obtenir le token Fresq pour la recherche de diplômes");
			return null;
		}
		
		String url = String.format("%s/recherche/", properties.getBaseUrl());
		
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(token);
		
		Map<String, Object> body = new HashMap<>();
		body.put("Term", "*");
		body.put("codesTypeDiplome", List.of("diplome_doctorat"));
		body.put("pageSize", pageSize);
		body.put("pageNumber", pageNumber);
		
		HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
		
		try {
			ResponseEntity<FresqSearchResult> response = restTemplate.exchange(
					url,
					HttpMethod.POST,
					request,
					FresqSearchResult.class
			);
			
			FresqSearchResult result = response.getBody();
			if (result != null) {
				log.info("Recherche diplômes doctorat page {}: {} résultats sur {}", 
						pageNumber, result.getNumberOfElements(), result.getTotalElements());
			}
			return result;
		} catch (Exception e) {
			log.error("Erreur lors de la recherche de diplômes doctorat: {}", e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Récupère le détail d'un diplôme.
	 */
	public FresqDiplomeDetail getDiplomeDetail(String diplome, String id) {
		String token = getAccessToken();
		if (token == null) {
			log.error("Impossible d'obtenir le token Fresq pour le détail du diplôme");
			return null;
		}
		
		String url = String.format("%s/diplomes/%s/%s", properties.getBaseUrl(), diplome, id);
		
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(token);
		
		HttpEntity<Void> request = new HttpEntity<>(headers);
		
		try {
			ResponseEntity<FresqDiplomeDetail> response = restTemplate.exchange(
					url,
					HttpMethod.GET,
					request,
					FresqDiplomeDetail.class
			);
			
			return response.getBody();
		} catch (Exception e) {
			log.error("Erreur lors de la récupération du détail du diplôme {}: {}", id, e.getMessage(), e);
			return null;
		}
	}
	
	/**
	 * Récupère toutes les écoles doctorales depuis Fresq (pagination automatique).
	 */
	public List<FresqSearchContent> recupererToutesLesEcolesDoctorales() {
		List<FresqSearchContent> toutesLesEcoles = new ArrayList<>();
		int pageNumber = 0;
		int pageSize = 100;
		boolean hasNextPage = true;
		
		while (hasNextPage) {
			FresqSearchResult result = rechercherDiplomesDoctorat(pageSize, pageNumber);
			if (result == null || result.getContent() == null || result.getContent().isEmpty()) {
				log.info("Plus de pages à récupérer (page {})", pageNumber);
				hasNextPage = false;
			} else {
				toutesLesEcoles.addAll(result.getContent());
				pageNumber++;
				hasNextPage = pageNumber < result.getTotalPages();
				log.info("Page {} récupérée, {} écoles doctorales au total sur {} pages", 
						pageNumber, toutesLesEcoles.size(), result.getTotalPages());
			}
		}
		
		log.info("Total des écoles doctorales récupérées depuis Fresq: {}", toutesLesEcoles.size());
		return toutesLesEcoles;
	}
}
