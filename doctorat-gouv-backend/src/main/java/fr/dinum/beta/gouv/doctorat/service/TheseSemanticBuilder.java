package fr.dinum.beta.gouv.doctorat.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.TheseSemanticDocument;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

@Service
public class TheseSemanticBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(TheseSemanticBuilder.class);

    public TheseSemanticDocument build(PropositionThese these) {
    	log.info("Construction du document sémantique pour la thèse : id={}, matricule={}", these.getId(), these.getMatricule());
        StringBuilder sb = new StringBuilder();

        // 🇫🇷 Français
        appendSection(sb, "Titre", these.getTheseTitre());
        appendSection(sb, "Mots-clés", joinMotsCles(these.getMotsCles()));
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

    private void appendSection(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(" : ").append(value.trim()).append("\n\n");
        }
    }

    private String joinMotsCles(Map<String, String> motsCles) {
        if (motsCles == null || motsCles.isEmpty()) {
            return null;
        }
        return String.join(", ", motsCles.values());
    }
}

