package fr.dinum.beta.gouv.doctorat.service;

import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.TheseSemanticDocument;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

/**
 * Ce service est responsable de construire un document sémantique à partir d'une proposition de thèse.
 */
@Service
public class TheseSemanticBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(TheseSemanticBuilder.class);

    /**
     * Construit un document sémantique à partir d'une proposition de thèse.
     * @param these
     * @return
     */
    public TheseSemanticDocument build(PropositionThese these) {
    	log.info("Construction du document sémantique pour la thèse : id={}, matricule={}", these.getId(), these.getMatricule());
        StringBuilder sb = new StringBuilder();

        // 🇫🇷 Français
        appendSection(sb, "Titre", these.getTheseTitre());
        appendMotsCles(sb, these);
        appendSection(sb, "Résumé", these.getResume());
        appendSection(sb, "Objectif", these.getObjectif());
        appendSection(sb, "Contexte", these.getContexte());
        appendSection(sb, "Profil recherché", these.getProfilRecherche());

        // 🇬🇧 Anglais
        appendSection(sb, "Title (EN)", these.getTheseTitreAnglais());
        appendSection(sb, "Keywords (EN)", joinMotsCles(these.getMotsClesAnglais()));
        appendSection(sb, "Summary (EN)", these.getResumeAnglais());
        appendSection(sb, "Profile (EN)", these.getProfilRechercheAnglais());

        String texteComplet = sb.toString().trim();

        return new TheseSemanticDocument(
                these.getId(),
                these.getMatricule(),
                texteComplet
        );
    }
    
    private void appendMotsCles(StringBuilder sb, PropositionThese sujet) {

        if (sujet.getMotsCles() == null || sujet.getMotsCles().isEmpty()) {
            return;
        }

        sb.append("### Mots-clés\n");

        // Liste des mots-clés
        sujet.getMotsCles().forEach((key, value) -> {
            sb.append("- ").append(value).append("\n");
        });

        // Phrase récapitulative
        sb.append("\nLes mots-clés principaux de ce sujet sont : ");
        sb.append(
            sujet.getMotsCles().values().stream()
                .collect(Collectors.joining(", "))
        );
        sb.append(".\n\n");
    }

    
    /**
     * Sanitize le texte en remplaçant les balises HTML par des retours à la ligne et en supprimant les espaces superflus.
     * @param input
     * @return
     */
    private String sanitize(String input) {
        if (input == null) return null;

        return input
                .replaceAll("<br>", "\n")
                .replaceAll("<br/>", "\n")
                .replaceAll("<br />", "\n")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Ajoute une section au document sémantique si la valeur n'est pas nulle ou vide. Chaque section est séparée par deux retours à la ligne.
     * @param sb
     * @param label
     * @param value
     */
    private void appendSection(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label)
              .append(" : ")
              .append(sanitize(value))
              .append("\n\n");
        }
    }


    /**
     * Joint les mots-clés en une seule chaîne de caractères séparée par des virgules. Si la map est nulle ou vide, retourne null.
     * @param motsCles
     * @return
     */
    private String joinMotsCles(Map<String, String> motsCles) {
        if (motsCles == null || motsCles.isEmpty()) {
            return null;
        }
        return String.join(", ", motsCles.values());
    }
}

