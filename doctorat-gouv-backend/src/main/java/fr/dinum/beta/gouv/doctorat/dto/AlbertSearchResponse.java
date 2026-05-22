package fr.dinum.beta.gouv.doctorat.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO de réponse pour la recherche sémantique via Albert.
 *
 * RGPD : ne pas logguer le contenu des champs results, matchedContent ou query
 * car ils peuvent contenir des données à caractère personnel.
 */
public class AlbertSearchResponse {

    private String query;
    private List<PropositionTheseDto> results;
    private List<String> suggestedKeywords;
    private Map<Long, Double> scores;
    private Map<Long, String> matchedTypes;
    private Map<Long, String> matchedContent;
    private int totalResults;

    public AlbertSearchResponse() {}

    public AlbertSearchResponse(String query, List<PropositionTheseDto> results, List<String> suggestedKeywords,
                                 Map<Long, Double> scores, Map<Long, String> matchedTypes,
                                 Map<Long, String> matchedContent, int totalResults) {
        this.query = query;
        this.results = results;
        this.suggestedKeywords = suggestedKeywords;
        this.scores = scores;
        this.matchedTypes = matchedTypes;
        this.matchedContent = matchedContent;
        this.totalResults = totalResults;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public List<PropositionTheseDto> getResults() { return results; }
    public void setResults(List<PropositionTheseDto> results) { this.results = results; }

    public List<String> getSuggestedKeywords() { return suggestedKeywords; }
    public void setSuggestedKeywords(List<String> suggestedKeywords) { this.suggestedKeywords = suggestedKeywords; }

    public Map<Long, Double> getScores() { return scores; }
    public void setScores(Map<Long, Double> scores) { this.scores = scores; }

    public Map<Long, String> getMatchedTypes() { return matchedTypes; }
    public void setMatchedTypes(Map<Long, String> matchedTypes) { this.matchedTypes = matchedTypes; }

    public Map<Long, String> getMatchedContent() { return matchedContent; }
    public void setMatchedContent(Map<Long, String> matchedContent) { this.matchedContent = matchedContent; }

    public int getTotalResults() { return totalResults; }
    public void setTotalResults(int totalResults) { this.totalResults = totalResults; }
}
