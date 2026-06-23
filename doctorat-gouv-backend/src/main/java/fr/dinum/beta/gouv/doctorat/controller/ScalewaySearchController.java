package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
	// Ex: "à Paris", "dans le Var", "en Île-de-France"
	// NOTE: "sur" est volontairement exclu car trop ambigu (ex: "sujet sur l'IA")
	private static final Pattern GEO_PREPOSITION_PATTERN = Pattern.compile(
		"(?:^|\\s)(?:à|aux|dans|vers|en)\\s+(?:le\\s+|la\\s+|l'|les\\s+)?[A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s|$)"
	);

	// Extrait le nom de ville après un mot-clé de proximité (fiable)
	// Ex: "proche de Paris" → "Paris"
	// Non ancré : on cherche la dernière occurrence dans toute la requête
	private static final Pattern GEO_CITY_PROXIMITY_PATTERN = Pattern.compile(
		"(?:proche|proches|près|pas loin|autour|voisinage|proximité|proximite|à coté|à côté|aux environs)\\s+(?:de|d')\\s*([A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s*-\\s*[A-ZÀ-Ÿ][A-Za-zÀ-ÿ]+)?)"
	);

	// Extrait le nom de ville après une préposition simple (moins fiable)
	// Ex: "à Paris", "dans le Var", "en Bretagne"
	// Non ancré : on cherche la dernière occurrence dans toute la requête
	private static final Pattern GEO_CITY_PREPOSITION_PATTERN = Pattern.compile(
		"(?:à|aux|dans|vers|en)\\s+(?:le\\s+|la\\s+|l'|les\\s+)?([A-ZÀ-Ÿ][A-Za-zÀ-ÿ-]+(?:\\s*-\\s*[A-ZÀ-Ÿ][A-Za-zÀ-ÿ]+)?)"
	);

	// Mots qui ne doivent PAS être considérés comme des localisations
	private static final java.util.Set<String> NON_LOCATION_WORDS = java.util.Set.of(
		"ia", "ai", "ml", "deep", "data", "big", "climat", "climatique",
		"climatiques", "environnement", "numerique", "numérique", "digital",
		"sante", "santé", "biologie", "medecine", "médecine", "chimie",
		"physique", "mathematiques", "mathématiques", "economie", "économie",
		"droit", "histoire", "sociologie", "psychologie", "philosophie",
		"education", "éducation", "formation", "apprentissage",
		"informatique", "science", "sciences", "recherche", "these", "thèse",
		"la", "le", "les", "des", "un", "une"
	);

	// Record pour stocker le résultat d'un match d'intention
	// value = valeur extraite (ville, organisme), matchedText = texte brut matché, position = index de début
	private record IntentMatch(String value, String matchedText, int position) {}

	// Mots-clés financement FR/EN
	private static final String[] FUNDING_KEYWORDS = {
		"financé", "financement", "finance", "bourse", "subvention",
		"fundé", "funding",
		"funded", "grant", "scholarship", "sponsored"
	};

	// Pattern de split pour le financement : nécessite "financé/funded/sponsored" + "par/by",
	// ou un mot-clé autonome suffisamment spécifique (bourse, subvention, grant, scholarship).
	private static final Pattern SPLIT_FUNDING_PATTERN = Pattern.compile(
		"(?:financé|funded|sponsored)\\s+(?:par|by)\\s+|bourse\\s+|subvention\\s+|grant\\s+|scholarship\\s+",
		Pattern.CASE_INSENSITIVE
	);

	// Nettoie les prépositions/articles devant un nom d'organisme
	private static final Pattern FUNDING_LEADING_CLEAN = Pattern.compile(
		"^(?:par |pour |by |de |du |des |d'|le |la |l'|les |un |une |aux |au |avec |chez |sans |sous |the |a |an )+",
		Pattern.CASE_INSENSITIVE
	);

	private final VectorSearchService vectorSearchService;
	private final PropositionTheseService propositionService;
	private final SearchRerankerService rerankerService;
	private final SujetEmbeddingRepository sujetEmbeddingRepository;
	private final PropositionTheseRepository propositionTheseRepository;
	private final EmbeddingIndexationService indexationService;

	public ScalewaySearchController(VectorSearchService vectorSearchService,
									PropositionTheseService propositionService,
									SearchRerankerService rerankerService,
									SujetEmbeddingRepository sujetEmbeddingRepository,
									PropositionTheseRepository propositionTheseRepository,
									EmbeddingIndexationService indexationService) {
		this.vectorSearchService = vectorSearchService;
		this.propositionService = propositionService;
		this.rerankerService = rerankerService;
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
	 * Extrait la DERNIÈRE intention de localisation dans toute la requête.
	 * Scan indépendamment les patterns de proximité et les prépositions simples,
	 * et retourne celle qui apparaît le plus tard (position la plus élevée).
	 * Retourne un IntentMatch avec la valeur, le texte brut matché, et sa position.
	 */
	private IntentMatch extractCityFromGeoQuery(String query) {
		if (query == null || query.isBlank()) return null;

		// 1. Patterns de proximité : on prend la dernière occurrence valide
		IntentMatch lastProximity = null;
		Matcher proximityM = GEO_CITY_PROXIMITY_PATTERN.matcher(query);
		while (proximityM.find()) {
			String candidate = proximityM.group(1).trim().toLowerCase();
			if (!NON_LOCATION_WORDS.contains(candidate)) {
				lastProximity = new IntentMatch(candidate, proximityM.group(), proximityM.start());
			}
		}

		// 2. Prépositions simples : on prend la dernière occurrence valide
		IntentMatch lastPrep = null;
		Matcher prepM = GEO_CITY_PREPOSITION_PATTERN.matcher(query);
		while (prepM.find()) {
			String candidate = prepM.group(1).trim().toLowerCase();
			if (!NON_LOCATION_WORDS.contains(candidate)) {
				lastPrep = new IntentMatch(candidate, prepM.group(), prepM.start());
			}
		}

		// 3. On prend celle qui apparaît le plus tard dans la requête
		if (lastProximity != null && lastPrep != null) {
			return lastProximity.position() >= lastPrep.position() ? lastProximity : lastPrep;
		}
		if (lastProximity != null) return lastProximity;
		if (lastPrep != null) return lastPrep;
		return null;
	}

	/**
	 * Vérifie si un DTO correspond à la ville extraite
	 * (dans uniteRechercheVille ou etablissementVille)
	 */
	private boolean dtoMatchesCity(PropositionTheseDto dto, String cityLower) {
		if (dto == null || cityLower == null) return false;
		String urVille = dto.getUniteRechercheVille();
		String etabVille = dto.getEtablissementVille();
		return (urVille != null && urVille.toLowerCase().contains(cityLower))
			|| (etabVille != null && etabVille.toLowerCase().contains(cityLower));
	}

	/**
	 * Détecte si la requête contient une intention de financement
	 */
	private boolean containsFundingIntent(String query) {
		if (query == null || query.isBlank()) return false;
		String lower = query.toLowerCase().trim();
		for (String kw : FUNDING_KEYWORDS) {
			if (lower.contains(kw)) return true;
		}
		return false;
	}

	/**
	 * Extrait la DERNIÈRE intention de financement dans toute la requête.
	 * Trouve le dernier mot-clé funding, puis délimite l'organisme jusqu'au
	 * prochain mot-clé géographique (ou fin de chaîne), puis nettoie
	 * les prépositions/articles en tête.
	 * Ex: "thèses en France financé par établissement public proche de Paris"
	 *     → IntentMatch("établissement public", "financé par établissement public", pos)
	 */
	private IntentMatch extractFundingOrgFromQuery(String query) {
		if (query == null || query.isBlank()) return null;
		String lower = query.toLowerCase();

		// Trouver le DERNIER mot-clé funding
		int bestIdx = -1;
		String bestKw = null;
		for (String kw : FUNDING_KEYWORDS) {
			int idx = lower.lastIndexOf(kw);
			if (idx >= 0 && idx > bestIdx) {
				bestIdx = idx;
				bestKw = kw;
			}
		}
		if (bestKw == null) return null;

		// Texte après le mot-clé funding (dans la requête originale, préservant la casse)
		int afterStart = bestIdx + bestKw.length();
		String after = query.substring(afterStart);

		// Trouver le prochain mot-clé géo dans la suite du texte
		int boundary = after.length();
		Matcher geoM = GEO_CITY_PROXIMITY_PATTERN.matcher(after);
		if (geoM.find()) {
			boundary = geoM.start();
		} else {
			Matcher prepM = GEO_CITY_PREPOSITION_PATTERN.matcher(after);
			if (prepM.find()) {
				boundary = prepM.start();
			}
		}

		// Extraire la partie organisme (sans le mot-clé funding)
		String orgPart = after.substring(0, boundary).trim();

		// Nettoyer les prépositions/articles en tête
		Matcher clean = FUNDING_LEADING_CLEAN.matcher(orgPart);
		String cleanedOrg = clean.replaceAll("").trim();

		if (cleanedOrg.isEmpty()) return null;

		// matchedText = texte brut depuis le mot-clé funding jusqu'à la limite
		String matchedText = query.substring(bestIdx, bestIdx + bestKw.length() + boundary).trim();

		return new IntentMatch(cleanedOrg.toLowerCase(), matchedText, bestIdx);
	}

	/**
	 * Vérifie si un DTO correspond à l'organisme financeur extrait.
	 * Pour gérer les variantes de conjonction (ex: "collectivité locale ou territoriale"
	 * vs "collectivité locale et territorial"), l'org est splitté sur ou/et et
	 * chaque partie est testée individuellement.
	 */
	private boolean dtoMatchesFunding(PropositionTheseDto dto, String orgLower) {
		if (dto == null || orgLower == null) return false;
		String origine = dto.getFinancementOrigine();
		String employeur = dto.getFinancementEmployeur();
		String details = dto.getFinancementDetails();
		List<String> types = dto.getFinancementTypes();
		if (origine == null && employeur == null && details == null && (types == null || types.isEmpty())) return false;

		// 1. Split sur les conjonctions pour matcher des sous-parties
		String[] parts = orgLower.split("\\s+(?:ou|et|or|and)\\s+|\\s*,\\s*");
		for (String part : parts) {
			String trimmed = part.trim();
			if (trimmed.isEmpty()) continue;
			if (fieldContains(origine, trimmed) || fieldContains(employeur, trimmed) || fieldContains(details, trimmed) || typeListContains(types, trimmed)) {
				return true;
			}
		}

		// 2. Appariement progressif : "établissement public français" > 2 mots
		//    On retire le dernier mot à chaque itération pour matcher
		//    "Établissement public" stocké en base.
		String[] words = orgLower.split("\\s+");
		for (int end = words.length - 1; end >= 2; end--) {
			String sub = String.join(" ", java.util.Arrays.copyOfRange(words, 0, end)).trim();
			if (sub.length() < 4) continue;
			if (fieldContains(origine, sub) || fieldContains(employeur, sub) || fieldContains(details, sub) || typeListContains(types, sub)) {
				return true;
			}
		}

		return false;
	}

	/** Vérifie si la liste de types de financement contient la valeur (lowercase). */
	private boolean typeListContains(List<String> types, String value) {
		if (types == null) return false;
		for (String t : types) {
			if (t != null && t.toLowerCase().contains(value)) return true;
		}
		return false;
	}

	/** Vérifie si un champ texte (nullable) contient la valeur (lowercase). */
	private boolean fieldContains(String field, String value) {
		return field != null && field.toLowerCase().contains(value);
	}

	/**
	 * Nettoie la requête en retirant les textes matchés des intentions
	 * (localisation, financement), pour obtenir le cœur de la recherche.
	 * Ex: "sujets santé proche de Paris financé par le CNRPS"
	 *     → "sujets santé"
	 * Si le cœur obtenu est vide ou trop court (< 3 car), retourne la requête originale.
	 */
	private String cleanCoreQuery(String query, IntentMatch... matches) {
		if (query == null || query.isBlank()) return query;
		if (matches == null || matches.length == 0) return query;

		// Trier par position décroissante pour ne pas décaler les indices
		List<IntentMatch> list = java.util.Arrays.stream(matches)
			.filter(m -> m != null)
			.sorted((a, b) -> Integer.compare(b.position(), a.position()))
			.collect(Collectors.toList());

		if (list.isEmpty()) return query;

		String result = query;
		for (IntentMatch m : list) {
			int start = m.position();
			int end = start + m.matchedText().length();
			if (start >= 0 && end <= result.length()) {
				result = result.substring(0, start) + result.substring(end);
			}
		}
		result = result.trim();

		// Si le cœur est trop court, on garde la requête originale
		if (result.length() < 3) return query;
		return result;
	}

	@GetMapping("/propositions")
	public ResponseEntity<Map<String, Object>> search(
			@RequestParam Map<String, String> allParams) {

		String query = allParams.get("query");
		if (query == null || query.isBlank()) {
			return ResponseEntity.badRequest().body(Map.of("error", "Le paramètre query est requis"));
		}
		query = query.trim();
		int limit = Integer.parseInt(allParams.getOrDefault("limit", "100"));

		long startTime = System.currentTimeMillis();
		log.info("Recherche vectorielle via /api/scaleway/propositions (limit={})", limit);

		// Retirer le "?" final qui parasiterait les intentions
		boolean hasTrailingQuestionMark = query.endsWith("?");
		String cleanQuery = hasTrailingQuestionMark ? query.substring(0, query.length() - 1).trim() : query;

		// Extraire les intentions indépendamment dans toute la requête
		IntentMatch cityMatch = extractCityFromGeoQuery(cleanQuery);
		IntentMatch fundingMatch = extractFundingOrgFromQuery(cleanQuery);

		// Valider chaque intention indépendamment (0, 1 ou 2 possibles)
		String extractedCity = null;
		String extractedFundingOrg = null;
		if (cityMatch != null) {
			extractedCity = cityMatch.value();
		}
		if (fundingMatch != null) {
			extractedFundingOrg = fundingMatch.value();
			int fundingWordCount = extractedFundingOrg.split("\\s+").length;
			if (fundingWordCount > 8) {
				log.warn("Intention de financement invalide: \"{}\" ({} mots) — trop long, ignorée",
					extractedFundingOrg, fundingWordCount);
				fundingMatch = null;
				extractedFundingOrg = null;
			}
		}

		// Nettoyer la requête en retirant les textes matchés pour l'embedding
		String core = cleanCoreQuery(cleanQuery, cityMatch, fundingMatch);
		String vectorQuery = hasTrailingQuestionMark ? core + " ?" : core;
		if (!vectorQuery.equals(query)) {
			log.info("Requête nettoyée pour l'embedding: \"{}\" → \"{}\"", query, vectorQuery);
		}

		// Construire la map des intentions pour la réponse
		Map<String, String> intents = new LinkedHashMap<>();
		intents.put("core", vectorQuery);
		if (extractedCity != null) intents.put("location", extractedCity);
		if (extractedFundingOrg != null) intents.put("funding", extractedFundingOrg);

		// 1. Recherche vectorielle (sur la requête nettoyée)
		List<VectorSearchHit> hits = vectorSearchService.search(vectorQuery, limit);

		if (hits.isEmpty()) {
			return ResponseEntity.ok(Map.of(
				"query", query,
				"intents", intents,
				"results", List.of(),
				"totalResults", 0,
				"durationMs", System.currentTimeMillis() - startTime
			));
		}

		// 2. Récupérer les DTO des sujets trouvés
		List<Long> ids = hits.stream().map(VectorSearchHit::getSujetId).collect(Collectors.toList());
		Map<Long, PropositionTheseDto> theseMap = propositionService.findByIdInAsMap(ids);

		// 3. Calculer les scores composites (vectoriel + keywords)
		// Les tokens sont extraits du core nettoyé, pas de la requête brute
		// avec intentions, pour que le scoring ne dépende pas du financement/localisation
		List<String> tokens = rerankerService.extractTokens(vectorQuery);
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

		// 6. Détection localisation : construction de la map par résultat
		Map<String, Boolean> locationMatchedMap = new HashMap<>();
		boolean anyLocationMatched = false;
		if (extractedCity != null) {
			for (PropositionTheseDto dto : results) {
				boolean matches = dtoMatchesCity(dto, extractedCity);
				if (dto.getId() != null) {
					locationMatchedMap.put(String.valueOf(dto.getId()), matches);
				}
				if (matches) anyLocationMatched = true;
			}
		}
		boolean locationNotMatched = containsGeoIntent(query)
			&& extractedCity != null
			&& !anyLocationMatched;

		// 7. Détection financement : construction de la map par résultat
		Map<String, Boolean> fundingMatchedMap = new HashMap<>();
		int fundingMatchCount = 0;
		if (extractedFundingOrg != null) {
			for (PropositionTheseDto dto : results) {
				boolean matches = dtoMatchesFunding(dto, extractedFundingOrg);
				if (dto.getId() != null) {
					fundingMatchedMap.put(String.valueOf(dto.getId()), matches);
					if (matches) fundingMatchCount++;
				}
			}
			log.info("Funding: org=\"{}\", {}/{} résultats matchés", extractedFundingOrg, fundingMatchCount, results.size());
		}

		long duration = System.currentTimeMillis() - startTime;
		log.info("{} résultat(s) retourné(s) pour la recherche vectorielle (durée={}ms, locationNotMatched={})",
			results.size(), duration, locationNotMatched);
		if (extractedCity != null) {
			log.info("Location: city=\"{}\", {}/{} résultats matchés",
				extractedCity,
				locationMatchedMap.values().stream().filter(v -> v).count(),
				results.size());
		}

		return ResponseEntity.ok(Map.ofEntries(
			Map.entry("query", query),
			Map.entry("intents", intents),
			Map.entry("results", results),
			Map.entry("scores", compositeScores),
			Map.entry("vectorScores", vectorScores),
			Map.entry("relevanceLevels", relevanceLevels),
			Map.entry("matchedTypes", matchedTypes),
			Map.entry("matchedContent", matchedContent),
			Map.entry("totalResults", results.size()),
			Map.entry("durationMs", duration),
			Map.entry("locationNotMatched", locationNotMatched),
			Map.entry("locationMatchedMap", locationMatchedMap),
			Map.entry("fundingMatchedMap", fundingMatchedMap)
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
