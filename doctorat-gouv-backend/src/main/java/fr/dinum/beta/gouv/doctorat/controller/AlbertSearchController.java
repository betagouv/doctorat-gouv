package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.AlbertSearchHit;
import fr.dinum.beta.gouv.doctorat.dto.AlbertSearchResponse;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.service.AlbertReindexService;
import fr.dinum.beta.gouv.doctorat.service.AlbertSearchService;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;
import fr.dinum.beta.gouv.doctorat.service.SearchRerankerService;

/**
 * Contrôleur REST exposant les endpoints de recherche et d'indexation via l'API Albert.
 *
 * CONFORMITÉ RGPD :
 * - Les requêtes de recherche (query) ne sont pas loggées : elles pourraient contenir
 *   des données personnelles saisies par l'utilisateur.
 * - Les logs se limitent à des métadonnées techniques (nombre de résultats, ID techniques).
 * - Le matricule (identifiant personnel du doctorant) n'est pas loggé.
 */
@RestController
@RequestMapping("/api/albert")
public class AlbertSearchController {

    private static final Logger log = LoggerFactory.getLogger(AlbertSearchController.class);

    private final AlbertSearchService searchService;
    private final PropositionTheseService propositionService;
    private final AlbertReindexService reindexService;
    private final SearchRerankerService rerankerService;

    @Value("${albert.search.offset-pages:1}")
    private int offsetPages;

    public AlbertSearchController(AlbertSearchService searchService, PropositionTheseService propositionService,
                                   AlbertReindexService reindexService, SearchRerankerService rerankerService) {
        this.searchService = searchService;
        this.propositionService = propositionService;
        this.reindexService = reindexService;
        this.rerankerService = rerankerService;
    }

    /**
     * Endpoint de recherche sémantique qui retourne des résultats structurés
     * (scores, types d'intention, mots-clés suggérés) directement exploitables
     * pour l'affichage en cards.
     *
     * GET /api/albert/propositions?query=intelligence artificielle&limit=20
     *
     * RGPD : la requête (query) n'est pas loggée.
     */
    @GetMapping("/propositions")
    public ResponseEntity<AlbertSearchResponse> searchPropositions(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", required = false) Integer limit) {
        if (limit == null) limit = 100;
        long startTime = System.currentTimeMillis();

        log.info("Recherche sémantique via /api/albert/propositions (limit={})", limit);

        // 1. Extraire les tokens significatifs
        List<String> tokens = rerankerService.extractTokens(query);

        // 2. Générer des variantes de requête pour le multi-appel Albert
        List<String> queryVariants = rerankerService.generateQueryVariants(tokens);
        if (queryVariants.isEmpty()) {
            queryVariants = List.of(query);
        }

        // 3. Recherche multi-requêtes dans Albert (en parallèle avec offset)
        List<AlbertSearchHit> mergedHits = searchInParallel(queryVariants);
        int totalAlbertCalls = queryVariants.size() * offsetPages;
        log.info("Requêtes Albert : {} variante(s) × {} page(s) offset = {} appel(s)",
            queryVariants.size(), offsetPages, totalAlbertCalls);

        // 4. Recherche SQL (LIKE) pour compléter le rappel
        String sqlQuery = String.join(" ", tokens);
        Map<Long, PropositionTheseDto> sqlResults;
        if (!tokens.isEmpty()) {
            sqlResults = propositionService.searchByQueryAsMap(sqlQuery);
        } else {
            sqlResults = Map.of();
        }

        // 5. Fusion : les résultats Albert (multi-requêtes) sont priorisés,
        //    complétés par les résultats SQL-only
        Map<Long, PropositionTheseDto> sourceResults = new HashMap<>();
        if (!mergedHits.isEmpty()) {
            List<Long> albertIds = mergedHits.stream()
                .map(AlbertSearchHit::getPropositionTheseId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
            sourceResults.putAll(propositionService.findByIdInAsMap(albertIds));
        }
        // Ajouter les résultats SQL qui ne seraient pas déjà dans Albert
        sqlResults.forEach(sourceResults::putIfAbsent);

        log.info("Albert ({} variantes × {} pages): {} hit(s) uniques, SQL: {} résultat(s), fusion: {}",
            queryVariants.size(), offsetPages, mergedHits.size(), sqlResults.size(), sourceResults.size());

        if (sourceResults.isEmpty()) {
            return ResponseEntity.ok(new AlbertSearchResponse(query, List.of(), List.of(),
                    Map.of(), Map.of(), Map.of(), 0));
        }

        // 5. IDs uniques depuis Albert (présents dans SQL)
        List<Long> albertIds = mergedHits.stream()
                .map(AlbertSearchHit::getPropositionTheseId)
                .filter(id -> id != null && sourceResults.containsKey(id))
                .distinct()
                .collect(Collectors.toList());

        // 6. Reranker les résultats Albert (score composite)
        List<Long> rerankedAlbertIds = rerankerService.rerank(mergedHits, query, sourceResults);

        // 7. Appliquer le mode AND (présence de "et" dans la requête)
        List<Long> rerankedFiltered;
        if (!tokens.isEmpty() && query.toLowerCase().matches(".*\\bet\\b.*")) {
            List<Long> allTokensMatch = rerankedAlbertIds.stream()
                .filter(id -> {
                    PropositionTheseDto dto = sourceResults.get(id);
                    return dto != null && rerankerService.computeKeywordScore(tokens, dto) >= 1.0;
                })
                .collect(Collectors.toList());
            if (allTokensMatch.size() >= 5) {
                rerankedFiltered = allTokensMatch;
                log.info("Mode AND : {} résultat(s) contenant tous les tokens", allTokensMatch.size());
            } else {
                rerankedFiltered = rerankedAlbertIds;
                log.info("Mode AND : seulement {} résultat(s) avec tous les tokens, fallback vers non filtré", allTokensMatch.size());
            }
        } else {
            rerankedFiltered = rerankedAlbertIds;
        }

        // 8. IDs SQL-only (pas trouvés par Albert)
        List<Long> sqlOnlyIds = sourceResults.keySet().stream()
                .filter(id -> !albertIds.contains(id))
                .limit(limit)
                .collect(Collectors.toList());

        // 9. Fusion : Albert rankés d'abord, puis SQL-only
        List<Long> mergedIds = new ArrayList<>();
        mergedIds.addAll(rerankedFiltered);
        mergedIds.addAll(sqlOnlyIds);
        if (mergedIds.size() > limit) mergedIds = mergedIds.subList(0, limit);

        // 7. Construire les résultats dans l'ordre fusionné
        List<PropositionTheseDto> results = new ArrayList<>();
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, String> matchedTypes = new HashMap<>();
        Map<Long, String> matchedContent = new HashMap<>();

        for (Long propId : mergedIds) {
            if (!sourceResults.containsKey(propId)) continue;
            results.add(sourceResults.get(propId));

            // Score Albert si disponible, sinon 0
            double maxScore = mergedHits.stream()
                .filter(h -> propId.equals(h.getPropositionTheseId()))
                .mapToDouble(AlbertSearchHit::getScore)
                .max()
                .orElse(0.0);
            scores.put(propId, maxScore);

            // Type et contenu du meilleur chunk Albert (si disponible)
            mergedHits.stream()
                .filter(h -> propId.equals(h.getPropositionTheseId()))
                .findFirst()
                .ifPresent(bestHit -> {
                    matchedTypes.put(propId, bestHit.getChunkType());
                    matchedContent.put(propId, bestHit.getContent());
                });
        }

        // 8. Extraire les mots-clés suggérés depuis les données BDD (motsCles des thèses)
        List<String> suggestedKeywords = searchService.extractKeywordsFromResults(sourceResults);

        long duration = System.currentTimeMillis() - startTime;
        log.info("{} résultat(s) retourné(s) pour la recherche sémantique (durée={}ms)", results.size(), duration);

        AlbertSearchResponse response = new AlbertSearchResponse(
                query, results, suggestedKeywords,
                scores, matchedTypes, matchedContent, results.size());

        return ResponseEntity.ok(response);
    }

    /**
     * Supprime tous les anciens documents Albert (réinitialise les index).
     * POST /api/albert/index/delete
     */
    @PostMapping("/index/delete")
    public ResponseEntity<Map<String, Object>> deleteAllIndexes() {
        log.info("Suppression de tous les documents Albert demandée");
        int deleted = reindexService.deleteAllAlbertDocuments();
        log.info("{} document(s) Albert supprimé(s)", deleted);
        return ResponseEntity.ok(Map.of(
            "deleted", deleted,
            "message", deleted + " documents Albert supprimés. Les index seront recréés au prochain passage du scheduler."
        ));
    }

    /**
     * Supprime tous les anciens index et ré-indexe immédiatement tous les sujets actifs.
     * POST /api/albert/index/reindex
     */
    @PostMapping("/index/reindex")
    public ResponseEntity<Map<String, Object>> reindexAll() {
        log.info("Réindexation complète Albert demandée");
        int reindexed = reindexService.reindexAll();
        log.info("{} sujet(s) ré-indexé(s) dans Albert", reindexed);
        return ResponseEntity.ok(Map.of(
            "reindexed", reindexed,
            "message", reindexed + " sujets ré-indexés dans Albert."
        ));
    }

    /**
     * Endpoint de recherche textuelle simple (pour compatibilité).
     * GET /api/albert/search?query=...
     *
     * RGPD : la requête (query) n'est pas loggée.
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("query") String query) {

        log.info("Recherche textuelle via /api/albert/search");

        Map response = searchService.search(query);

        // Utiliser la nouvelle méthode de hits pour extraire les infos
        List<AlbertSearchHit> hits = searchService.searchHits(query);

        if (hits.isEmpty()) {
            log.info("Aucun résultat trouvé pour la recherche textuelle simple");
            return Map.of(
                "answer", "Je n'ai trouvé aucun passage pertinent dans les sujets de thèse pour cette question.",
                "empty", true,
                "hits", List.of()
            );
        }

        // Construire une réponse textuelle
        StringBuilder answer = new StringBuilder();
        answer.append("**Résultats trouvés (").append(hits.size()).append(")**\n\n");

        int count = 0;
        for (AlbertSearchHit hit : hits) {
            if (count >= 5) break;
            String typeLabel = switch (hit.getChunkType()) {
                case "mots_cles" -> "Mots-clés";
                case "resume" -> "Résumé";
                case "contexte" -> "Contexte";
                case "objectif" -> "Objectif";
                case "titre" -> "Titre";
                case "profil" -> "Profil recherché";
                default -> "Général";
            };
            answer.append("📌 Sujet #").append(hit.getPropositionTheseId())
                  .append(" | ").append(typeLabel)
                  .append(" | Score: ").append(String.format("%.2f", hit.getScore()))
                  .append("\n");
            answer.append(hit.getContent()).append("\n\n");
            count++;
        }

        log.info("{} résultat(s) trouvé(s) pour la recherche textuelle", hits.size());

        return Map.of(
            "answer", answer.toString(),
            "empty", false,
            "hits", hits.stream().limit(5).map(h -> Map.of(
                    "propositionId", h.getPropositionTheseId(),
                "score", h.getScore(),
                "chunkType", h.getChunkType(),
                "matricule", h.getMatricule()
            )).toList()
        );
    }

    /**
     * Exécute plusieurs requêtes Albert en parallèle (variantes × offsets)
     * et fusionne les résultats en gardant pour chaque sujet le chunk avec le meilleur score.
     */
    private List<AlbertSearchHit> searchInParallel(List<String> queries) {
        int step = 100; // correspond à apiSearchLimit
        List<CompletableFuture<List<AlbertSearchHit>>> futures = new ArrayList<>();

        for (String query : queries) {
            for (int page = 0; page < offsetPages; page++) {
                int offset = page * step;
                futures.add(CompletableFuture.supplyAsync(() -> searchService.searchHits(query, offset)));
            }
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Fusion : garder le meilleur hit par sujet (score le plus haut)
        Map<Long, AlbertSearchHit> bestHitBySubject = new LinkedHashMap<>();
        for (CompletableFuture<List<AlbertSearchHit>> future : futures) {
            for (AlbertSearchHit hit : future.join()) {
                Long id = hit.getPropositionTheseId();
                if (id == null) continue;
                AlbertSearchHit existing = bestHitBySubject.get(id);
                if (existing == null || hit.getScore() > existing.getScore()) {
                    bestHitBySubject.put(id, hit);
                }
            }
        }

        log.debug("Fusion offset : {} appel(s) → {} sujets uniques", futures.size(), bestHitBySubject.size());

        List<AlbertSearchHit> merged = new ArrayList<>(bestHitBySubject.values());
        merged.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return merged;
    }

}

