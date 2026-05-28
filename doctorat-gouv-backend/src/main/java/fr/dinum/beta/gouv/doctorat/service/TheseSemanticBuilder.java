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
        appendSectionWithHeader(sb, "Titre", these.getTheseTitre());
        appendMotsCles(sb, these);
        appendSectionWithHeader(sb, "Résumé", these.getResume());
        appendSectionWithHeader(sb, "Objectif", these.getObjectif());
        appendSectionWithHeader(sb, "Contexte", these.getContexte());
        appendSectionWithHeader(sb, "Profil recherché", these.getProfilRecherche());
        appendSectionWithHeader(sb, "Discipline", these.getDomaineScientifique());
        appendSectionWithHeader(sb, "Spécialité", these.getSpecialite());
        appendSectionWithHeader(sb, "Établissement", buildEtablissement(these));
        appendSectionWithHeader(sb, "Laboratoire", these.getUniteRechercheLibelle());
        appendSectionWithHeader(sb, "École doctorale", these.getEcoleDoctoraleLibelle());
        appendSectionWithHeader(sb, "Domaine", these.getDomaine());

        // 🇬🇧 Anglais
        appendSectionWithHeader(sb, "Title (EN)", these.getTheseTitreAnglais());
        appendSectionWithHeader(sb, "Keywords (EN)", joinMotsCles(these.getMotsClesAnglais()));
        appendSectionWithHeader(sb, "Summary (EN)", these.getResumeAnglais());
        appendSectionWithHeader(sb, "Profile (EN)", these.getProfilRechercheAnglais());

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
        sb.append("Les mots-clés principaux de ce sujet sont : ");
        sb.append(
            sujet.getMotsCles().values().stream()
                .collect(Collectors.joining(", "))
        );
        sb.append(".\n\n");
    }

    /**
     * Ajoute une section avec un header ### pour que TextChunker puisse détecter le type.
     */
    private void appendSectionWithHeader(StringBuilder sb, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        sb.append("### ").append(label).append("\n");
        sb.append(sanitize(value)).append("\n\n");
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

    private String buildEtablissement(PropositionThese sujet) {
        StringBuilder sb = new StringBuilder();
        if (sujet.getEtablissementLibelle() != null) {
            sb.append(sujet.getEtablissementLibelle());
        }
        if (sujet.getEtablissementVille() != null) {
            sb.append(" — ").append(sujet.getEtablissementVille());
        }
        if (sujet.getEtablissementCodePostal() != null) {
            sb.append(" (").append(sujet.getEtablissementCodePostal()).append(")");
        }
        return sb.length() > 0 ? sb.toString() : null;
    }
}

