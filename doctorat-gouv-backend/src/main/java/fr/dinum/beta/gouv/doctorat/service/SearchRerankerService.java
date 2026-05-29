package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

            double keywordScore = computeKeywordScore(tokens, dto);
            double titleBonus = computeTitleBonus(tokens, dto);
            double chunkWeight = getChunkWeight(hit.getChunkType());

            double composite = (hit.getScore() * 0.5 + keywordScore * 0.3 + titleBonus * 0.1) * chunkWeight;
            compositeScores.put(propId, composite);
        }

        log.debug("Reranking terminé : {} résultats reclassés", compositeScores.size());

        return compositeScores.entrySet().stream()
            .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
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

        List<String> allFields = new ArrayList<>();

        if (dto.getTheseTitre() != null) {
            for (int i = 0; i < 2; i++) allFields.add(dto.getTheseTitre().toLowerCase());
        }
        if (dto.getTheseTitreAnglais() != null) {
            for (int i = 0; i < 2; i++) allFields.add(dto.getTheseTitreAnglais().toLowerCase());
        }
        if (dto.getMotsCles() != null) {
            String kw = String.join(" ", dto.getMotsCles().values()).toLowerCase();
            for (int i = 0; i < 3; i++) allFields.add(kw);
        }
        if (dto.getMotsClesAnglais() != null) {
            String kw = String.join(" ", dto.getMotsClesAnglais().values()).toLowerCase();
            for (int i = 0; i < 3; i++) allFields.add(kw);
        }
        if (dto.getResume() != null) allFields.add(dto.getResume().toLowerCase());
        if (dto.getResumeAnglais() != null) allFields.add(dto.getResumeAnglais().toLowerCase());
        if (dto.getObjectif() != null) allFields.add(dto.getObjectif().toLowerCase());
        if (dto.getContexte() != null) allFields.add(dto.getContexte().toLowerCase());

        String combinedText = String.join(" ", allFields);

        if (combinedText.isBlank()) return 0.0;

        int found = 0;
        for (String token : tokens) {
            if (combinedText.contains(token)) found++;
        }

        return (double) found / tokens.size();
    }

    double computeTitleBonus(List<String> tokens, PropositionTheseDto dto) {
        String titre = dto.getTheseTitre() != null ? dto.getTheseTitre().toLowerCase() : "";
        String titreEn = dto.getTheseTitreAnglais() != null ? dto.getTheseTitreAnglais().toLowerCase() : "";
        String combined = titre + " " + titreEn;

        if (combined.isBlank()) return 0.0;

        long found = tokens.stream().filter(combined::contains).count();
        if (found == tokens.size()) return 1.0;
        if (found > 0) return 0.5;
        return 0.0;
    }

    double getChunkWeight(String chunkType) {
        if (chunkType == null) return 1.0;
        return switch (chunkType) {
            case "titre" -> 1.2;
            case "resume", "mots_cles" -> 1.1;
            case "contexte", "objectif", "profil" -> 1.0;
            default -> 0.8;
        };
    }
}
