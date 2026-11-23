package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.FiltreRechercheCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
// no enum import needed
import fr.dinum.beta.gouv.doctorat.repository.FiltreRechercheCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Component
public class AlerteRechercheScheduler {

    private static final Logger log = LoggerFactory.getLogger(AlerteRechercheScheduler.class);
    private static final int BREVO_TEMPLATE_ALERTE_RECHERCHE = 80;
    private static final int MAX_RESULTATS = 20;

    private final FiltreRechercheCandidatRepository filtreRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final PropositionTheseService propositionTheseService;
    private final BrevoEmailService brevoEmailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.frontend.base-url}")
    private String frontendBaseUrl;

    public AlerteRechercheScheduler(FiltreRechercheCandidatRepository filtreRepository,
                                   UtilisateurRepository utilisateurRepository,
                                   PropositionTheseService propositionTheseService,
                                   BrevoEmailService brevoEmailService) {
        this.filtreRepository = filtreRepository;
        this.utilisateurRepository = utilisateurRepository;
        this.propositionTheseService = propositionTheseService;
        this.brevoEmailService = brevoEmailService;
    }

    @Scheduled(cron = "${alerte.scheduler.cron:0 0 8 * * *}")
    public void envoyerAlertesQuotidiennes() {
        traiterFrequence("JOURNALIERE");
    }

    @Scheduled(cron = "${alerte.scheduler.hebdo.cron:0 0 8 * * MON}")
    public void envoyerAlertesHebdomadaires() {
        traiterFrequence("HEBDOMADAIRE");
    }

    private void traiterFrequence(String frequence) {
        List<FiltreRechercheCandidat> filtres =
                filtreRepository.findByAlerteActiveTrueAndFrequenceAlerte(frequence);
        for (FiltreRechercheCandidat filtre : filtres) {
            try {
                traiterFiltre(filtre);
            } catch (Exception e) {
                log.error("Erreur lors du traitement de l'alerte (filtre={})", filtre.getId(), e);
            }
        }
    }

    private void traiterFiltre(FiltreRechercheCandidat filtre) throws Exception {
        String utilisateurId = null;
        try {
            utilisateurId = filtre.getUtilisateur() != null ? filtre.getUtilisateur().getId() : null;
        } catch (Exception ignored) {
            // proxy non initialisable : on ignore
        }
        Utilisateur utilisateur = utilisateurId != null
                ? utilisateurRepository.findById(utilisateurId).orElse(null)
                : null;
        if (utilisateur == null) return;

        Map<String, Object> savedFilters = objectMapper.readValue(
                filtre.getFiltresJson(), new TypeReference<Map<String, Object>>() {});
        Map<String, String> searchFilters = convertirEnFiltresRecherche(savedFilters);

        var page = propositionTheseService.search(searchFilters, 0, MAX_RESULTATS);
        List<PropositionTheseDto> resultats = page.getContent();

        LocalDateTime seuil = filtre.getDerniereAlerteEnvoyeeLe();
        if (seuil != null) {
            resultats = resultats.stream()
                    .filter(p -> p.getDateMiseEnLigne() != null
                            && p.getDateMiseEnLigne().isAfter(seuil))
                    .collect(Collectors.toList());
        }

        if (resultats.isEmpty()) {
            filtre.setDerniereAlerteEnvoyeeLe(LocalDateTime.now());
            filtreRepository.save(filtre);
            return;
        }

        String lien = frontendBaseUrl + "/recherche?filtre=" + filtre.getId();
        Map<String, Object> params = new HashMap<>();
        params.put("prenom", utilisateur.getPrenom());
        params.put("nom_recherche", filtre.getNom());
        params.put("lien_resultats", lien);

        brevoEmailService.sendTemplateEmail(utilisateur.getEmail(), BREVO_TEMPLATE_ALERTE_RECHERCHE, params);

        filtre.setDerniereAlerteEnvoyeeLe(LocalDateTime.now());
        filtreRepository.save(filtre);
    }

    private Map<String, String> convertirEnFiltresRecherche(Map<String, Object> saved) {
        Map<String, String> out = new HashMap<>();
        if (saved == null) return out;
        for (Map.Entry<String, Object> e : saved.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            if (v == null) continue;
            if (v instanceof List) {
                String joined = ((List<?>) v).stream()
                        .map(String::valueOf)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.joining(";"));
                if (!joined.isEmpty()) out.put(key, joined);
            } else {
                String s = String.valueOf(v).trim();
                if (!s.isEmpty() && !"null".equalsIgnoreCase(s)) {
                    out.put(key, s);
                }
            }
        }
        // Conversion de l'onglet actif vers typeProposition backend
        String onglet = out.get("typeProposition");
        if ("thesis".equalsIgnoreCase(onglet)) {
            out.put("typeProposition", "proposition");
        } else if ("supervision".equalsIgnoreCase(onglet)) {
            out.put("typeProposition", "offre");
        } else if ("all".equalsIgnoreCase(onglet)) {
            out.remove("typeProposition");
        }
        return out;
    }
}
