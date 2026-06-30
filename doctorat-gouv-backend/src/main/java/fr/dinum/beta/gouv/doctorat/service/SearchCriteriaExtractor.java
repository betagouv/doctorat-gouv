package fr.dinum.beta.gouv.doctorat.service;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchCriteriaExtractor {

    public record Criteria(String cleanedQuery, String financementType, String localisation) {}

    private static final Pattern FINANCEMENT_PHRASE = Pattern.compile(
        "avec\\s+(?:un\\s+)?financement(?:\\s+de\\s+type)?\\s+(\\S+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern FINANCEMENT_WORD = Pattern.compile(
        "(?<!\\w)(cifre)(?!\\w)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOCATION_PROXIMITE = Pattern.compile(
        "(?:(?:proche|proches|près)\\s+de|autour\\s+de)\\s+(\\w+(?:-\\w+)?(?:(?:\\s+|\\s+d')?\\w+)?)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LOCATION_ENDPHRASE = Pattern.compile(
        "(?:à|dans|en)\\s+(\\w+(?:-\\w+)?(?:\\s+\\w+)?)\\s*$",
        Pattern.CASE_INSENSITIVE
    );

    private static final Set<String> NOISE_WORDS = Set.of(
        "la", "le", "les", "des", "un", "une", "mon", "ton", "son",
        "cette", "ces", "leur", "leurs",
        "recherche", "sujet", "these", "thèse", "domaine", "cadre",
        "milieu", "sein", "partie", "fonction",
        // Mots couramment confondus avec des localisations
        "ia", "ai", "ml", "data", "big", "climat", "climatique",
        "climatiques", "environnement", "numerique", "numérique", "digital",
        "sante", "santé", "biologie", "medecine", "médecine", "chimie",
        "physique", "mathematiques", "mathématiques", "economie", "économie",
        "droit", "histoire", "sociologie", "psychologie", "philosophie",
        "education", "éducation", "formation", "apprentissage",
        "informatique", "science", "sciences"
    );

    public static Criteria extract(String query) {
        if (query == null || query.isBlank()) {
            return new Criteria(query, null, null);
        }

        String cleaned = query;
        String financementType = null;
        String localisation = null;

        Matcher m = FINANCEMENT_PHRASE.matcher(cleaned);
        if (m.find()) {
            financementType = m.group(1).toLowerCase();
            cleaned = m.replaceAll("").trim();
        } else {
            m = FINANCEMENT_WORD.matcher(cleaned);
            if (m.find()) {
                financementType = m.group(1).toLowerCase();
                cleaned = m.replaceAll("").trim();
            }
        }

        m = LOCATION_PROXIMITE.matcher(cleaned);
        if (m.find()) {
            localisation = m.group(1).toLowerCase();
            cleaned = m.replaceAll("").trim();
        } else {
            m = LOCATION_ENDPHRASE.matcher(cleaned);
            if (m.find()) {
                String candidate = m.group(1).toLowerCase();
                if (!NOISE_WORDS.contains(candidate)) {
                    localisation = candidate;
                    cleaned = cleaned.substring(0, m.start()).trim();
                }
            }
        }

        cleaned = cleaned.replaceAll("\\s+", " ").trim();
        if (cleaned.isBlank()) {
            cleaned = query;
        }

        return new Criteria(cleaned, financementType, localisation);
    }

}
