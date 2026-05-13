package fr.dinum.beta.gouv.doctorat.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.service.AlbertSearchService;

@RestController
@RequestMapping("/api/albert")
public class AlbertSearchController {

    private final AlbertSearchService searchService;

    public AlbertSearchController(AlbertSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam("query") String query) {

        Map response = searchService.search(query);
        
        if (query.toLowerCase().contains("mot") && query.toLowerCase().contains("clé")) {
            String merged = searchService.buildAnswerFromSearchResult(response, 5);
            String keywords = searchService.extractKeywords(merged);

            if (keywords == null || keywords.isBlank()) {
                return Map.of(
                    "answer", "Je n’ai pas trouvé de mots-clés pertinents pour cette question.",
                    "empty", true
                );
            }

            return Map.of(
                "answer", "Mots-clés trouvés : " + keywords,
                "empty", false
            );
        } else {
            String answer = searchService.buildAnswerFromSearchResult(response, 3);

            if (answer == null) {
                return Map.of(
                    "answer", "Je n’ai trouvé aucun passage pertinent dans les sujets de thèse pour cette question.",
                    "empty", true
                );
            }

            return Map.of(
                "answer", answer,
                "empty", false
            );
        }



    }

}

