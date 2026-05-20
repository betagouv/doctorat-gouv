package fr.dinum.beta.gouv.doctorat.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

@RestController
@RequestMapping("/api/albert")
public class AlbertSearchController {

    private final AlbertSearchService searchService;
    private final PropositionTheseService propositionService;
    private final AlbertReindexService reindexService;

    @Value("${albert.search.limit:27}")
    private int searchLimit;

    public AlbertSearchController(AlbertSearchService searchService, PropositionTheseService propositionService,
                                   AlbertReindexService reindexService) {
        this.searchService = searchService;
        this.propositionService = propositionService;
        this.reindexService = reindexService;
    }

    /**
     * Endpoint de recherche sémantique qui retourne des résultats structurés
     * (scores, types d'intention, mots-clés suggérés) directement exploitables
     * pour l'affichage en cards.
     *
     * GET /api/albert/propositions?query=intelligence artificielle&limit=20
     */
    @GetMapping("/propositions")
    public ResponseEntity<AlbertSearchResponse> searchPropositions(
            @RequestParam("query") String query,
            @RequestParam(value = "limit", required = false) Integer limit) {
        if (limit == null) limit = searchLimit;

        // 1. Recherche sémantique dans Albert → hits structurés
        List<AlbertSearchHit> hits = searchService.searchHits(query);
        if (hits.isEmpty()) {
            return ResponseEntity.ok(new AlbertSearchResponse(query, List.of(), List.of(),
                    Map.of(), Map.of(), Map.of(), 0));
        }

        // 2. Extraire les IDs uniques des propositions de thèse
        List<Long> theseIds = hits.stream()
                .map(AlbertSearchHit::getPropositionTheseId)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        // 3. Récupérer les données complètes depuis la BDD
        Map<Long, PropositionTheseDto> theseMap = propositionService.findByIdInAsMap(theseIds);

        // 4. Construire les résultats dans l'ordre des scores
        List<PropositionTheseDto> results = new ArrayList<>();
        Map<Long, Double> scores = new HashMap<>();
        Map<Long, String> matchedTypes = new HashMap<>();
        Map<Long, String> matchedContent = new HashMap<>();

        for (AlbertSearchHit hit : hits) {
            Long propId = hit.getPropositionTheseId();
            if (!theseMap.containsKey(propId)) continue;
            if (scores.containsKey(propId)) continue; // déjà ajouté (meilleur score gardé)

            results.add(theseMap.get(propId));
            scores.put(propId, hit.getScore());
            matchedTypes.put(propId, hit.getChunkType());
            matchedContent.put(propId, hit.getContent());
        }

        // 5. Extraire les mots-clés suggérés depuis les données BDD (motsCles des thèses)
        List<String> suggestedKeywords = searchService.extractKeywordsFromResults(theseMap);

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
        int deleted = reindexService.deleteAllAlbertDocuments();
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
        int reindexed = reindexService.reindexAll();
        return ResponseEntity.ok(Map.of(
            "reindexed", reindexed,
            "message", reindexed + " sujets ré-indexés dans Albert."
        ));
    }

    /**
     * Endpoint de recherche textuelle simple (pour compatibilité).
     * GET /api/albert/search?query=...
     */
    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("query") String query) {

        Map response = searchService.search(query);

        // Utiliser la nouvelle méthode de hits pour extraire les infos
        List<AlbertSearchHit> hits = searchService.searchHits(query);

        if (hits.isEmpty()) {
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

}

