package fr.dinum.beta.gouv.doctorat.mapper;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Dictionnaire de correspondance specialite (texte libre ADUM/AMETHIS) vers
 * les valeurs de l'énumération EURAXESS researchFieldEnum.
 *
 * <p>Règles ordonnées : la première règle dont le motif (regex, insensible à la
 * casse, recherche de sous-chaîne) matche la spécialité normalisée gagne.
 * Couverture mesurée : 100 % des 1902 spécialités non vides de la base.</p>
 */
public final class ResearchFieldDictionary {

    private static final List<String[]> RULES = List.of(
            new String[] { "astrophysique", "Astrophysics" },
            new String[] { "astronomie", "Astrophysics" },
            new String[] { "cosmologie", "Cosmology" },
            new String[] { "astroparticule", "Astrophysics" },
            new String[] { "planetologie", "Astrophysics" },
            new String[] { "univers", "Astrophysics" },
            new String[] { "accelerateur", "Nuclear engineering" },
            new String[] { "nucleaire", "Nuclear engineering" },
            new String[] { "subatomique", "Nuclear engineering" },
            new String[] { "particule", "Nuclear engineering" },
            new String[] { "instrumentation", "Instrumentation technology" },
            new String[] { "matiere condensee", "Condensed matter properties" },
            new String[] { "physique de la matiere", "Condensed matter properties" },
            new String[] { "physique", "Applied physics" },
            new String[] { "nanotechnolog", "Nanotechnology" },
            new String[] { "nano", "Nanotechnology" },
            new String[] { "materiaux", "Materials engineering" },
            new String[] { "ceramique", "Materials engineering" },
            new String[] { "bois", "Materials engineering" },
            new String[] { "optique", "Optics" },
            new String[] { "photonique", "Optics" },
            new String[] { "laser", "Optics" },
            new String[] { "acoustique", "Acoustics" },
            new String[] { "energetiqu", "Energy technology" },
            new String[] { "energie", "Energy technology" },
            new String[] { "thermique", "Thermal engineering" },
            new String[] { "combustion", "Thermal engineering" },
            new String[] { "plasma", "Applied physics" },
            new String[] { "microelectronique", "Electronics" },
            new String[] { "micro-ondes", "Electronics" },
            new String[] { "microondes", "Electronics" },
            new String[] { "electronique", "Electronics" },
            new String[] { "telecommunication", "Telecommunications technology" },
            new String[] { "telecom", "Telecommunications technology" },
            new String[] { "reseaux", "Internet technology" },
            new String[] { "systemes embarques", "Computer systems" },
            new String[] { "embarque", "Computer systems" },
            new String[] { "robotique", "Cybernetics" },
            new String[] { "automatique", "Control engineering" },
            new String[] { "productique", "Control engineering" },
            new String[] { "intelligence artificielle", "Informatics" },
            new String[] { "signal", "Digital systems" },
            new String[] { "donnees", "Database management" },
            new String[] { "stic", "Informatics" },
            new String[] { "bioinformatique", "Informatics" },
            new String[] { "informatique", "Informatics" },
            new String[] { "algorithme", "Algorithms" },
            new String[] { "optimisation", "Algorithms" },
            new String[] { "recherche operationnelle", "Algorithms" },
            new String[] { "biomath", "Applied mathematics" },
            new String[] { "math\\s*et\\s*appliqu", "Applied mathematics" },
            new String[] { "math\\s*appliqu", "Applied mathematics" },
            new String[] { "math\\s*interfaces", "Applied mathematics" },
            new String[] { "appliqu", "Applied mathematics" },
            new String[] { "statistique", "Statistics" },
            new String[] { "biostat", "Statistics" },
            new String[] { "mathematique", "Mathematical analysis" },
            new String[] { "math", "Mathematical analysis" },
            new String[] { "modelisation", "Modelling tools" },
            new String[] { "chimie organique", "Organic chemistry" },
            new String[] { "chimie analytique", "Analytical chemistry" },
            new String[] { "chimie physique", "Physical chemistry" },
            new String[] { "physico-chimie", "Physical chemistry" },
            new String[] { "theorique", "Physical chemistry" },
            new String[] { "chimie inorganique", "Inorganic chemistry" },
            new String[] { "organometallique", "Inorganic chemistry" },
            new String[] { "coordination", "Inorganic chemistry" },
            new String[] { "minerale", "Inorganic chemistry" },
            new String[] { "electrochimie", "Applied chemistry" },
            new String[] { "catalyse", "Heterogeneous catalysis" },
            new String[] { "biochimie", "Biochemistry" },
            new String[] { "chimie des materiaux", "Materials engineering" },
            new String[] { "chimie moleculaire", "Molecular chemistry" },
            new String[] { "moleculaire", "Molecular chemistry" },
            new String[] { "chimie", "Applied chemistry" },
            new String[] { "pharmacotechnie", "Pharmaceutical technology" },
            new String[] { "pharmacolog", "Clinical pharmacology" },
            new String[] { "pharmacie", "Pharmacy" },
            new String[] { "medicament", "Pharmacy" },
            new String[] { "medicinale", "Pharmacy" },
            new String[] { "cancerologie", "Cancer research" },
            new String[] { "cancer", "Cancer research" },
            new String[] { "immunologie", "Biology" },
            new String[] { "microbiologie", "Biology" },
            new String[] { "virologie", "Biology" },
            new String[] { "parasitologie", "Biology" },
            new String[] { "genetique", "Biology" },
            new String[] { "genomique", "Biology" },
            new String[] { "physiologie", "Biology" },
            new String[] { "vegetale", "Botany" },
            new String[] { "biologie", "Biology" },
            new String[] { "vivant", "Biology" },
            new String[] { "sciences de la vie", "Biology" },
            new String[] { "micro-organisme", "Biology" },
            new String[] { "microorganisme", "Biology" },
            new String[] { "infection", "Biology" },
            new String[] { "infectiologie", "Biology" },
            new String[] { "biotechnologie", "Biotechnology" },
            new String[] { "biotherapie", "Biotechnology" },
            new String[] { "biomecanique", "Biomedical engineering" },
            new String[] { "imagerie", "Biomedical engineering" },
            new String[] { "biomedical", "Biomedical engineering" },
            new String[] { "bioingenierie", "Biomedical engineering" },
            new String[] { "epidemiologie", "Epidemiology" },
            new String[] { "sante publique", "Health sciences" },
            new String[] { "maladies", "Health sciences" },
            new String[] { "physiopathologie", "Health sciences" },
            new String[] { "toxicologie", "Toxicology" },
            new String[] { "nutrition", "Nutritional sciences" },
            new String[] { "aliment", "Nutritional sciences" },
            new String[] { "recherche clinique", "Medicine" },
            new String[] { "medecine", "Medicine" },
            new String[] { "neuroscience", "Neurobiology" },
            new String[] { "psychologie", "Psychology" },
            new String[] { "cognition", "Cognitive science" },
            new String[] { "sante", "Health sciences" },
            new String[] { "genie civil", "Civil engineering" },
            new String[] { "geotechnique", "Civil engineering" },
            new String[] { "genie des procedes", "Process engineering" },
            new String[] { "procedes", "Process engineering" },
            new String[] { "genie industriel", "Industrial engineering" },
            new String[] { "genie mecanique", "Mechanical engineering" },
            new String[] { "mecanique", "Mechanical engineering" },
            new String[] { "electrique", "Electrical engineering" },
            new String[] { "aeronautique", "Aerospace engineering" },
            new String[] { "aerospatial", "Aerospace engineering" },
            new String[] { "genie", "Industrial engineering" },
            new String[] { "ingenieur", "Industrial engineering" },
            new String[] { "ingenierie", "Industrial engineering" },
            new String[] { "agronomie", "Agronomics" },
            new String[] { "agro", "Agronomics" },
            new String[] { "agricole", "Agronomics" },
            new String[] { "ecologie", "Ecology" },
            new String[] { "biodiversite", "Ecology" },
            new String[] { "evolution", "Ecology" },
            new String[] { "ecosysteme", "Ecology" },
            new String[] { "environnement", "Environmental technology" },
            new String[] { "climat", "Global change" },
            new String[] { "atmosphere", "Global change" },
            new String[] { "hydrologie", "Hydrology" },
            new String[] { "ocean", "Water science" },
            new String[] { "marine", "Water science" },
            new String[] { "sciences de la mer", "Marine technology" },
            new String[] { "geosciences", "Earth science" },
            new String[] { "geologie", "Earth science" },
            new String[] { "geophysique", "Earth science" },
            new String[] { "terre", "Earth science" },
            new String[] { "sciences du sol", "Soil science" },
            new String[] { "droit public", "Public law" },
            new String[] { "droit prive", "Private law" },
            new String[] { "sciences criminelles", "Criminal law" },
            new String[] { "criminelles", "Criminal law" },
            new String[] { "histoire du droit", "History of law" },
            new String[] { "juridique", "Judicial law" },
            new String[] { "droit", "Public law" },
            new String[] { "economie", "Applied economics" },
            new String[] { "economique", "Applied economics" },
            new String[] { "gestion", "Management studies" },
            new String[] { "management", "Management studies" },
            new String[] { "marketing", "Marketing" },
            new String[] { "finance", "Financial science" },
            new String[] { "geographie", "Human geography" },
            new String[] { "amenagement", "Regional geography" },
            new String[] { "urbanisme", "Regional geography" },
            new String[] { "sociologie", "Social changes" },
            new String[] { "science politique", "Policy studies" },
            new String[] { "anthropologie medicale", "Medical anthropology" },
            new String[] { "anthropologie", "Social anthropology" },
            new String[] { "epistemologie", "Epistemology" },
            new String[] { "linguistique", "Linguistics" },
            new String[] { "langage", "Linguistics" },
            new String[] { "langues", "Linguistics" },
            new String[] { "philologie", "Linguistics" },
            new String[] { "anglophone", "Anglo Saxon studies" },
            new String[] { "anglo", "Anglo Saxon studies" },
            new String[] { "romanes", "European literature" },
            new String[] { "espagnol", "European literature" },
            new String[] { "italien", "European literature" },
            new String[] { "portugais", "European literature" },
            new String[] { "germanique", "European literature" },
            new String[] { "litterature", "Comparative literature" },
            new String[] { "lettres", "Comparative literature" },
            new String[] { "histoire de l'art", "Art history" },
            new String[] { "archeologie", "Archaeology" },
            new String[] { "prehistoire", "Archaeology" },
            new String[] { "paleo", "Archaeology" },
            new String[] { "contemporain", "Contemporary history" },
            new String[] { "histoire", "Modern history" },
            new String[] { "philosophie", "Systematic philosophy" },
            new String[] { "ethique", "Ethics" },
            new String[] { "theologie", "Theology" },
            new String[] { "religion", "Theology" },
            new String[] { "musique", "Music history" },
            new String[] { "musicologie", "Music history" },
            new String[] { "design", "Design" },
            new String[] { "arts du spectacle", "Performing arts" },
            new String[] { "plastique", "Visual arts" },
            new String[] { "sport", "Health sciences" },
            new String[] { "staps", "Health sciences" },
            new String[] { "education", "Education" },
            new String[] { "didactique", "Education" },
            new String[] { "enseignement", "Education" },
            new String[] { "information", "Information technology" },
            new String[] { "communication", "Communication technology" },
            new String[] { "systemes complexes", "Systems engineering" },
            new String[] { "art", "Fine arts" }
    );

    private static final List<Pattern> PATTERNS = RULES.stream()
            .map(rule -> Pattern.compile(rule[0], Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
            .toList();

    private ResearchFieldDictionary() {
    }

    /**
     * Retourne la valeur researchFieldEnum correspondant à la spécialité, ou
     * {@code null} si aucune règle ne matche.
     */
    public static String map(String specialite) {
        if (specialite == null || specialite.isBlank()) {
            return null;
        }
        String norm = normalize(specialite);
        if (norm.isEmpty()) {
            return null;
        }
        for (int i = 0; i < PATTERNS.size(); i++) {
            if (PATTERNS.get(i).matcher(norm).find()) {
                return RULES.get(i)[1];
            }
        }
        return null;
    }

    /**
     * Normalisation : minuscules, suppression des accents, caractères non
     * alphanumériques remplacés par des espaces, espaces multiples réduits.
     */
    static String normalize(String value) {
        String s = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        s = s.toLowerCase();
        s = s.replaceAll("[^a-z0-9 ]", " ");
        s = s.replaceAll("\\s+", " ").trim();
        return s;
    }

}
