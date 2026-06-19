package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.dto.VectorSearchHit;
import fr.dinum.beta.gouv.doctorat.enums.DomaineScientifique;
import fr.dinum.beta.gouv.doctorat.enums.RegionsFrance;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.SujetEmbeddingRepository;
import fr.dinum.beta.gouv.doctorat.service.EmbeddingIndexationService;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;
import fr.dinum.beta.gouv.doctorat.service.ScalewayEmbeddingService;
import fr.dinum.beta.gouv.doctorat.service.SearchRerankerService;
import fr.dinum.beta.gouv.doctorat.service.VectorSearchService;

@RestController
@RequestMapping("/api/scaleway")
public class ScalewaySearchController {

	private static final Logger log = LoggerFactory.getLogger(ScalewaySearchController.class);

	// Seuils de pertinence basés sur le score composite (vectoriel + lexical)
	// Calibrés avec la distribution observée des scores (juin 2026)
	// Version 1 (juin 2026) : TRES_PERTINENT >= 0.78
	// Version 2 (juin 2026) : TRES_PERTINENT >= 0.91 (seuil relevé après test terrain)
	private static final double SEUIL_TRES_PERTINENT = 0.91;
	private static final double SEUIL_PERTINENT = 0.70;
	private static final double SEUIL_FAIBLEMENT_PERTINENT = 0.60;

	// Mots-clés géographiques FR/EN pour détecter une intention de localisation
	private static final String[] GEO_KEYWORDS = {
		// Français
		"proche", "près", "pas loin", "autour", "alentours", "voisinage",
		"proximité", "proximite", "à coté", "à côté", "aux environs",
		"secteur de", "zone de", "du côté de",
		// English
		"near", "close to", "around", "nearby", "vicinity", "close by",
		"not far", "located in", "located near", "situated in"
	};

	// Détecte les prépositions suivies d'un nom propre (ville, région…)
	// Ex: "à Paris", "dans le Var", "en Île-de-France", "sur Lyon", "aux alentours"
	private static final Pattern GEO_PREPOSITION_PATTERN = Pattern.compile(
		"(?:^|\\s)(?:à|aux|dans|vers|sur|en)\\s+(?:le\\s+|la\\s+|l'|les\\s+)?[A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s|$)"
	);

	// Extrait le nom de ville après un mot-clé géographique
	// Ex: "proche de Paris" → "Paris", "à Lyon" → "Lyon"
	private static final Pattern GEO_CITY_EXTRACT_PATTERN = Pattern.compile(
		"(?:(?:proche|près|pas loin|autour|voisinage|proximité|proximite|à coté|à côté|aux environs)\\s+(?:de|d')\\s*|(?:à|aux|vers|sur|en)\\s+(?:le\\s+|la\\s+|l'|les\\s+)?)([A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s*-\\s*[A-ZÀ-Ÿ][A-Za-zÀ-ÿ]+)?)"
	);

	private final VectorSearchService vectorSearchService;
	private final PropositionTheseService propositionService;
	private final SearchRerankerService rerankerService;
	private final ScalewayEmbeddingService scalewayEmbeddingService;
	private final SujetEmbeddingRepository sujetEmbeddingRepository;
	private final PropositionTheseRepository propositionTheseRepository;
	private final EmbeddingIndexationService indexationService;

	public ScalewaySearchController(VectorSearchService vectorSearchService,
									PropositionTheseService propositionService,
									SearchRerankerService rerankerService,
									ScalewayEmbeddingService scalewayEmbeddingService,
									SujetEmbeddingRepository sujetEmbeddingRepository,
									PropositionTheseRepository propositionTheseRepository,
									EmbeddingIndexationService indexationService) {
		this.vectorSearchService = vectorSearchService;
		this.propositionService = propositionService;
		this.rerankerService = rerankerService;
		this.scalewayEmbeddingService = scalewayEmbeddingService;
		this.sujetEmbeddingRepository = sujetEmbeddingRepository;
		this.propositionTheseRepository = propositionTheseRepository;
		this.indexationService = indexationService;
	}

	private static String niveauPertinence(double compositeScore) {
		if (compositeScore >= SEUIL_TRES_PERTINENT) return "TRES_PERTINENT";
		if (compositeScore >= SEUIL_PERTINENT) return "PERTINENT";
		if (compositeScore >= SEUIL_FAIBLEMENT_PERTINENT) return "FAIBLEMENT_PERTINENT";
		return "MASQUE";
	}

	/**
	 * Filtre un DTO selon les paramètres de filtre passés dans la requête.
	 * Seuls les paramètres de filtre reconnus sont appliqués (discipline, localisation,
	 * defisSociete, laboratoire, ecole, annee, typeProposition).
	 * Les paramètres inconnus (query, limit, sortField…) sont ignorés.
	 */
	private boolean matchesFilters(PropositionTheseDto dto, Map<String, String> params) {
		if (dto == null) return false;

		// 1. Localisation
		String localisation = params.get("localisation");
		if (localisation != null && !localisation.isBlank()) {
			String postalCode = dto.getUniteRechercheCodePostal();
			if (postalCode == null || postalCode.isBlank()) return false;
			boolean matchesRegion = false;
			for (String region : localisation.split(";")) {
				region = region.trim();
				if (region.isEmpty()) continue;
				for (String dept : RegionsFrance.departementsFromRegion(region)) {
					if (postalCode.startsWith(dept)) { matchesRegion = true; break; }
				}
				if (matchesRegion) break;
			}
			if (!matchesRegion) return false;
		}

		// 2. Discipline
		String discipline = params.get("discipline");
		if (discipline != null && !discipline.isBlank()) {
			boolean matches = false;
			for (String val : discipline.split(";")) {
				String code = DomaineScientifique.codeFromLabel(val.trim());
				if (code != null && code.equals(dto.getDomaineScientifique())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 3. Défis de société (combo domainesImpactListe + objectifsDeveloppementDurableListe)
		String defisSociete = params.get("defisSociete");
		if (defisSociete != null && !defisSociete.isBlank()) {
			for (String val : defisSociete.split(";")) {
				String lower = val.trim().toLowerCase();
				if (lower.isEmpty()) continue;
				boolean matchDomaines = dto.getDomainesImpactListe() != null
					&& dto.getDomainesImpactListe().stream().anyMatch(d -> d.toLowerCase().contains(lower));
				boolean matchOdd = dto.getObjectifsDeveloppementDurableListe() != null
					&& dto.getObjectifsDeveloppementDurableListe().stream().anyMatch(o -> o.toLowerCase().contains(lower));
				if (!matchDomaines && !matchOdd) return false;
			}
		}

		// 4. Laboratoire (LIKE)
		String laboratoire = params.get("laboratoire");
		if (laboratoire != null && !laboratoire.isBlank()) {
			String labo = dto.getUniteRechercheLibelle();
			if (labo == null) return false;
			boolean matches = false;
			for (String val : laboratoire.split(";")) {
				if (labo.toLowerCase().contains(val.trim().toLowerCase())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 5. École (IN)
		String ecole = params.get("ecole");
		if (ecole != null && !ecole.isBlank()) {
			String ecoleVal = dto.getEtablissementLibelle();
			if (ecoleVal == null) return false;
			boolean matches = false;
			for (String val : ecole.split(";")) {
				if (ecoleVal.equals(val.trim())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 6. Année
		String annee = params.get("annee");
		if (annee != null && !annee.isBlank()) {
			String anneeUniv = dto.getAnneeUniversitaire();
			if (anneeUniv == null) return false;
			boolean matches = false;
			for (String val : annee.split(";")) {
				if (anneeUniv.startsWith(val.trim())) {
					matches = true;
					break;
				}
			}
			if (!matches) return false;
		}

		// 7. Type proposition
		String typeProposition = params.get("typeProposition");
		if (typeProposition != null && !typeProposition.isBlank()) {
			String type = dto.getTypeProposition();
			if (type == null || !type.equals(typeProposition.trim())) return false;
		}

		return true;
	}

	/**
	 * Détecte si la requête contient une intention géographique :
	 * - mots-clés explicites (proche, près, near…)
	 * - préposition + nom de ville/région (à Paris, dans le Var, en Bretagne…)
	 */
	private boolean containsGeoIntent(String query) {
		if (query == null || query.isBlank()) return false;

		// Mots-clés insensibles à la casse
		String lower = query.toLowerCase().trim();
		for (String kw : GEO_KEYWORDS) {
			if (lower.contains(kw)) return true;
		}

		// Préposition + nom propre (ex: "à Paris", "dans le Var")
		Matcher m = GEO_PREPOSITION_PATTERN.matcher(query);
		return m.find();
	}

	/**
	 * Extrait le nom de ville mentionné après un mot-clé géographique
	 * (proche de X, près de X, à X, etc.)
	 */
	private String extractCityFromGeoQuery(String query) {
		if (query == null || query.isBlank()) return null;
		Matcher m = GEO_CITY_EXTRACT_PATTERN.matcher(query);
		if (m.find()) {
			return m.group(1).trim().toLowerCase();
		}
		return null;
	}

	/**
	 * Vérifie si au moins un résultat contient la ville extraite
	 * (dans uniteRechercheVille ou etablissementVille)
	 */
	private boolean anyResultMatchesCity(List<PropositionTheseDto> results, String cityLower) {
		if (results == null || results.isEmpty() || cityLower == null) return false;
		int topN = Math.min(20, results.size());
		for (int i = 0; i < topN; i++) {
			PropositionTheseDto dto = results.get(i);
			String urVille = dto.getUniteRechercheVille();
			String etabVille = dto.getEtablissementVille();
			if ((urVille != null && urVille.toLowerCase().contains(cityLower))
				|| (etabVille != null && etabVille.toLowerCase().contains(cityLower))) {
				return true;
			}
		}
		return false;
	}

	@GetMapping("/propositions")
	public ResponseEntity<Map<String, Object>> search(
			@RequestParam Map<String, String> allParams) {

		String query = allParams.get("query");
		if (query == null || query.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Le paramètre query est requis"));
		}
		int limit = Integer.parseInt(allParams.getOrDefault("limit", "100"));

		long startTime = System.currentTimeMillis();
		log.info("Recherche vectorielle via /api/scaleway/propositions (limit={})", limit);

		// 1. Recherche vectorielle
		List<VectorSearchHit> hits = vectorSearchService.search(query, limit);

		if (hits.isEmpty()) {
			return ResponseEntity.ok(Map.of(
				"query", query,
				"results", List.of(),
				"totalResults", 0,
				"durationMs", System.currentTimeMillis() - startTime
			));
		}

		// 2. Récupérer les DTO des sujets trouvés
		List<Long> ids = hits.stream().map(VectorSearchHit::getSujetId).collect(Collectors.toList());
		Map<Long, PropositionTheseDto> theseMap = propositionService.findByIdInAsMap(ids);

		// 3. Calculer les scores composites (vectoriel + keywords)
		List<String> tokens = rerankerService.extractTokens(query);
		Map<Long, Double> compositeScores = new HashMap<>();
		Map<Long, Double> vectorScores = new HashMap<>();
		for (VectorSearchHit hit : hits) {
			PropositionTheseDto dto = theseMap.get(hit.getSujetId());
			vectorScores.put(hit.getSujetId(), hit.getScore());
			double score = hit.getScore();
			if (dto != null && !tokens.isEmpty()) {
				double kwScore = rerankerService.computeKeywordScore(tokens, dto);
				score = Math.min(score + kwScore * 1.2, 1.0);
			}
			compositeScores.put(hit.getSujetId(), score);
		}

		// 4. Trier par score vectoriel brut (similarité sémantique réelle)
		// et calculer le niveau de pertinence basé sur le composite (vectoriel + keywords)
		Map<Long, String> relevanceLevels = new HashMap<>();
		List<Long> sortedIds = vectorScores.entrySet().stream()
			.sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
			.limit(limit)
			.peek(e -> relevanceLevels.put(e.getKey(),
				niveauPertinence(compositeScores.getOrDefault(e.getKey(), 0.0))))
			.map(Map.Entry::getKey)
			.filter(id -> matchesFilters(theseMap.get(id), allParams))
			.collect(Collectors.toList());

		// 5. Construire la réponse
		List<PropositionTheseDto> results = new ArrayList<>();
		Map<Long, String> matchedTypes = new HashMap<>();
		Map<Long, String> matchedContent = new HashMap<>();
		for (Long id : sortedIds) {
			PropositionTheseDto dto = theseMap.get(id);
			if (dto != null) {
				results.add(dto);
				for (VectorSearchHit hit : hits) {
					if (id.equals(hit.getSujetId())) {
						matchedTypes.put(id, hit.getBlocType());
						matchedContent.put(id, hit.getContenuMatche());
						break;
					}
				}
			}
		}

		// 6. Détection localisation : extraction du nom de ville après mot-clé géo
		String extractedCity = extractCityFromGeoQuery(query);
		boolean locationNotMatched = containsGeoIntent(query)
			&& extractedCity != null
			&& !anyResultMatchesCity(results, extractedCity);

		long duration = System.currentTimeMillis() - startTime;
		log.info("{} résultat(s) retourné(s) pour la recherche vectorielle (durée={}ms, locationNotMatched={})",
			results.size(), duration, locationNotMatched);

		return ResponseEntity.ok(Map.of(
			"query", query,
			"results", results,
			"scores", compositeScores,
			"vectorScores", vectorScores,
			"relevanceLevels", relevanceLevels,
			"matchedTypes", matchedTypes,
			"matchedContent", matchedContent,
			"totalResults", results.size(),
			"durationMs", duration,
			"locationNotMatched", locationNotMatched
		));
	}

	@Transactional
	@PostMapping("/index/delete")
	public ResponseEntity<Map<String, Object>> deleteAllIndexes() {
		log.info("Suppression de tous les embeddings Scaleway demandée");
		int deleted = sujetEmbeddingRepository.findAll().size();
		sujetEmbeddingRepository.deleteAllInBatch();
		propositionTheseRepository.clearDateIndexationScaleway();
		log.info("{} embedding(s) Scaleway supprimé(s)", deleted);
		return ResponseEntity.ok(Map.of(
			"deleted", deleted,
			"message", deleted + " embeddings supprimés. Les index seront recréés au prochain passage du scheduler."
		));
	}

	@Transactional
	@PostMapping("/index/reindex")
	public ResponseEntity<Map<String, Object>> reindexAll() {
		log.info("Réindexation complète Scaleway demandée");
		sujetEmbeddingRepository.deleteAllInBatch();
		propositionTheseRepository.clearDateIndexationScaleway();
		indexationService.indexerTout();
		log.info("Réindexation Scaleway terminée");
		return ResponseEntity.ok(Map.of(
			"message", "Réindexation Scaleway terminée."
		));
	}
}
