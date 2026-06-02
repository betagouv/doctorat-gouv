package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.AlbertSearchHit;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;

@Service
public class SearchRerankerService {

    private static final Logger log = LoggerFactory.getLogger(SearchRerankerService.class);

    private static final Set<String> STOP_WORDS = Set.of(
        "le", "la", "les", "des", "un", "une", "du", "de", "à", "au", "aux",
        "et", "ou", "est", "sont", "que", "qui", "dans", "pour", "sur", "par",
        "pas", "ne", "non", "plus", "tres", "bien", "fait",
        "avec", "sans", "chez", "entre", "vers", "depuis",
        "the", "a", "an", "in", "on", "of", "for", "to", "with", "by",
        "je", "tu", "il", "elle", "nous", "vous", "ils", "elles",
        "ce", "cet", "cette", "ces", "mon", "ton", "son", "ma", "ta", "sa",
        "mes", "tes", "ses", "notre", "votre", "leur",
        "moi", "toi", "lui", "eux",
        "qu", "n", "s", "d", "l", "m",
        "sujet", "sujets", "these", "theses", "thèse", "thèses",
        "lié", "liés", "liée", "liées", "travail", "travaux",
        "recherche", "recherches", "étude", "études", "etude", "etudes",
        "projet", "projets", "domaine", "domaines", "cadre",
        "question", "questions", "besoin", "besoins",
        "faut", "doit", "peut", "peuvent", "avoir", "être",
        "veux", "vais", "aller"
    );

    private static final double ALBERT_WEIGHT = 0.25;
    private static final double KEYWORD_WEIGHT = 0.75;

    public List<Long> rerank(List<AlbertSearchHit> hits, String query,
                             Map<Long, PropositionTheseDto> theseMap) {

        List<String> tokens = extractTokens(query);
        if (tokens.isEmpty()) {
            return hits.stream()
                .map(AlbertSearchHit::getPropositionTheseId)
                .distinct()
                .collect(Collectors.toList());
        }

        Map<Long, Double> compositeScores = new LinkedHashMap<>();

        for (AlbertSearchHit hit : hits) {
            Long propId = hit.getPropositionTheseId();
            if (propId == null || compositeScores.containsKey(propId)) continue;

            PropositionTheseDto dto = theseMap.get(propId);
            if (dto == null) continue;

            double composite = computeComposite(tokens, hit, dto);
            compositeScores.put(propId, composite);
        }

        log.debug("Reranking terminé : {} résultats reclassés", compositeScores.size());

        return compositeScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    public double computeScoreForHit(AlbertSearchHit hit, String query, PropositionTheseDto dto) {
        List<String> tokens = extractTokens(query);
        if (tokens.isEmpty()) return hit.getScore();
        return computeComposite(tokens, hit, dto);
    }

    private double computeComposite(List<String> tokens, AlbertSearchHit hit, PropositionTheseDto dto) {
        double keywordScore = computeKeywordScore(tokens, dto);
        double chunkWeight = getChunkWeight(hit.getChunkType());
        return (hit.getScore() * ALBERT_WEIGHT + keywordScore * KEYWORD_WEIGHT) * chunkWeight;
    }

    public List<String> extractTokens(String query) {
        if (query == null || query.isBlank()) return List.of();

        return Arrays.stream(query.toLowerCase().split("\\s+"))
            .map(t -> t.replaceAll("[^a-z0-9àâçéèêëîïôûùüÿñæœ]", ""))
            .filter(t -> t.length() > 1)
            .filter(t -> !STOP_WORDS.contains(t))
            .collect(Collectors.toList());
    }

    /**
     * Génère des variantes de requête à partir des tokens extraits,
     * pour une recherche multi-requêtes dans Albert.
     * <p>
     * Stratégie :
     * - 1 token  → [token] (pas de variante)
     * - 2 tokens → [tous, dernier] (le dernier est souvent le terme spécifique)
     * - 3+ tokens → [tous, sans le premier, premier+dernier]
     * </p>
     */
    public List<String> generateQueryVariants(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) return List.of();
        if (tokens.size() == 1) return List.of(tokens.get(0));

        List<String> variants = new ArrayList<>();

        // V1 : tous les tokens joints (requête la plus précise)
        variants.add(String.join(" ", tokens));

        if (tokens.size() == 2) {
            // V2 : dernier token seul (plus spécifique, complète le rappel)
            variants.add(tokens.get(1));
        } else {
            // V2 : tous les tokens sauf le premier
            variants.add(String.join(" ", tokens.subList(1, tokens.size())));
            // V3 : premier + dernier token (général + spécifique)
            variants.add(tokens.get(0) + " " + tokens.get(tokens.size() - 1));
        }

        return variants.stream().distinct().collect(Collectors.toList());
    }

    public double computeKeywordScore(List<String> tokens, PropositionTheseDto dto) {
        if (tokens.isEmpty()) return 0.0;

        double score = 0.0;

        for (String token : tokens) {
            boolean foundInTitle = false;
            boolean foundInResume = false;
            boolean foundInMotsCles = false;

            // Titre français : poids fort
            if (contains(dto.getTheseTitre(), token)) {
                score += 0.40;
                foundInTitle = true;
            }
            // Titre anglais : bonus
            if (contains(dto.getTheseTitreAnglais(), token)) {
                score += 0.20;
                foundInTitle = true;
            }

            // Résumé français : poids important
            if (contains(dto.getResume(), token)) {
                score += 0.30;
                foundInResume = true;
            }
            if (contains(dto.getResumeAnglais(), token)) {
                score += 0.15;
                foundInResume = true;
            }

            // Mots-clés : poids important
            if (containsInMap(dto.getMotsCles(), token)) {
                score += 0.30;
                foundInMotsCles = true;
            }
            if (containsInMap(dto.getMotsClesAnglais(), token)) {
                score += 0.15;
                foundInMotsCles = true;
            }

            // Objectif et contexte : poids moyen
            if (contains(dto.getObjectif(), token)) score += 0.20;
            if (contains(dto.getContexte(), token)) score += 0.15;

            // Bonus de répétition : si le token est dans 2+ champs différents
            int fieldsFound = (foundInTitle ? 1 : 0) + (foundInResume ? 1 : 0) + (foundInMotsCles ? 1 : 0);
            if (fieldsFound >= 2) {
                score += 0.10 * (fieldsFound - 1);
            }
        }

        return Math.min(score, 1.0);
    }

    private static boolean contains(String field, String token) {
        return field != null && field.toLowerCase().contains(token);
    }

    private static boolean containsInMap(Map<String, String> map, String token) {
        if (map == null) return false;
        return map.values().stream().anyMatch(v -> v != null && v.toLowerCase().contains(token));
    }

    public double computeTitleBonus(List<String> tokens, PropositionTheseDto dto) {
        String titre = dto.getTheseTitre() != null ? dto.getTheseTitre().toLowerCase() : "";
        String titreEn = dto.getTheseTitreAnglais() != null ? dto.getTheseTitreAnglais().toLowerCase() : "";
        String combined = titre + " " + titreEn;

        if (combined.isBlank()) return 0.0;

        long found = tokens.stream().filter(combined::contains).count();
        if (found == 0) return 0.0;
        return (double) found / tokens.size();
    }

    public boolean allTokensFound(List<String> tokens, PropositionTheseDto dto) {
        for (String token : tokens) {
            boolean found = contains(dto.getTheseTitre(), token)
                || contains(dto.getTheseTitreAnglais(), token)
                || contains(dto.getResume(), token)
                || contains(dto.getResumeAnglais(), token)
                || containsInMap(dto.getMotsCles(), token)
                || containsInMap(dto.getMotsClesAnglais(), token)
                || contains(dto.getObjectif(), token)
                || contains(dto.getContexte(), token);
            if (!found) return false;
        }
        return true;
    }

    public double getChunkWeight(String chunkType) {
        if (chunkType == null) return 1.0;
        return switch (chunkType) {
            case "titre" -> 1.2;
            case "resume", "mots_cles" -> 1.1;
            case "contexte", "objectif", "profil" -> 1.0;
            default -> 0.8;
        };
    }
}
