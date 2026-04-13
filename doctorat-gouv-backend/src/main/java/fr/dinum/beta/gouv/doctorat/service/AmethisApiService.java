package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.model.AmethisResponse;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;

@Service
public class AmethisApiService {

    private static final Logger log = LoggerFactory.getLogger(AmethisApiService.class);

    @Value("${amethis.api.url}")
    private String apiUrl;

    @Value("${amethis.import.rattrapage:false}")
    private boolean rattrapage;

    private final PropositionTheseRepository repository;
    private final RestTemplate restTemplate = new RestTemplate();

    public AmethisApiService(PropositionTheseRepository repository) {
        this.repository = repository;
    }

    /**
     * Récupère les propositions de thèses depuis l'API AMETHIS, les traite et les sauvegarde en base. 
     * Le traitement dépend du mode d'import (normal ou rattrapage) défini dans les propriétés.
     * @return
     */
    public String importAndSavePropositionsFromAmethis() {
        log.info("Import des propositions de thèses depuis AMETHIS");

        ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);
        String json = response.getBody();

        log.info("Statut HTTP AMETHIS : {}", response.getStatusCode());
        log.info("Taille JSON AMETHIS = {}", json.length());

        if (rattrapage) {
            log.info("Mode RATTRAPAGE AMETHIS activé");
            savePropositionsFromJsonRattrapage(json);
        } else {
            log.info("Mode NORMAL AMETHIS activé");
            savePropositionsFromJson(json);
        }

        return json;
    }

    /**
     * Traite les propositions de thèses AMETHIS en mode "normal" : on compare les dates de mise à jour et on n'active que les propositions 
     * présentes dans le JSON qui sont nouvelles ou qui ont été mises à jour depuis la dernière import. 
     * Les propositions déjà actives localement mais absentes du JSON seront désactivées.
     * @param json
     */
    public void savePropositionsFromJson(String json) {
    	
    	log.info("Début du traitement des propositions de thèses AMETHIS");
    	
        List<PropositionThese> propositions = parse(json);
        
        // 🔥 BOUCHON DIAGNOSTIC : détecter les doublons AMETHIS
        detectDuplicateMatricules(propositions);
        int currentYear = Year.now().getValue();
        int compteur = 1; // Compteur pour garantir l'unicité du matricule en cas de réimport rapide

        List<PropositionThese> toSave = new ArrayList<>();

        for (PropositionThese p : propositions) {
        	
            p.setSource(SourceThese.AMETHIS);
            p.setAnnee(currentYear);
            
            // Bouchon pour éviter les doublons en cas de réimport rapide : on ajoute un suffixe temporel au matricule pour garantir son unicité
            p.setMatricule(buildUniqueMatricule(p, compteur++));


            Optional<PropositionThese> existingOpt =
                    repository.findForUpdate(p.getMatricule(), p.getDateMaj());

            if (existingOpt.isPresent()) {
                p.setId(existingOpt.get().getId());
                p.setDateIntegration(existingOpt.get().getDateIntegration());
                p.setActive(true);
                toSave.add(p);
            } else {
                Optional<PropositionThese> existing = repository.findByMatricule(p.getMatricule());

                if (existing.isEmpty()) {
                    p.setDateIntegration(LocalDateTime.now());
                    p.setActive(true);
                    toSave.add(p);
                } else if (Boolean.FALSE.equals(existing.get().getActive())) {
                    p.setId(existing.get().getId());
                    p.setDateIntegration(existing.get().getDateIntegration());
                    p.setActive(true);
                    toSave.add(p);
                }
            }
        }

        repository.saveAll(toSave);

        desactivateMissingPropositions(propositions);
    }

    /**
     * Version "rattrapage" du traitement des propositions de thèses AMETHIS : on ne compare pas les dates de mise à jour et on réactive systématiquement 
     * les propositions présentes dans le JSON, même si elles sont déjà actives localement. Cela permet de "rattraper" des propositions qui auraient été désactivées 
     * par erreur ou qui auraient été manquées lors d'un précédent import.
     * @param json
     */
    public void savePropositionsFromJsonRattrapage(String json) {
    	
    	log.info("Début du traitement des propositions de thèses AMETHIS en mode RATTRAPAGE");
    	
        List<PropositionThese> propositions = parse(json);

        int currentYear = Year.now().getValue();
        List<PropositionThese> toSave = new ArrayList<>();
        
        int compteur = 1; // Compteur pour garantir l'unicité du matricule en cas de réimport rapide

        for (PropositionThese p : propositions) {
            p.setSource(SourceThese.AMETHIS);
            p.setAnnee(currentYear);
            
            // Bouchon pour éviter les doublons en cas de réimport rapide : on ajoute un suffixe temporel au matricule pour garantir son unicité
            p.setMatricule(buildUniqueMatricule(p, compteur++));

            Optional<PropositionThese> existingOpt = repository.findByMatricule(p.getMatricule());

            if (existingOpt.isPresent()) {
                p.setId(existingOpt.get().getId());
                p.setDateIntegration(existingOpt.get().getDateIntegration());
                p.setActive(true);
                toSave.add(p);
            } else {
                p.setDateIntegration(LocalDateTime.now());
                p.setActive(true);
                toSave.add(p);
            }
        }

        repository.saveAll(toSave);

        desactivateMissingPropositions(propositions);
    }

    /**
     * Désactive les propositions de thèses AMETHIS présentes localement mais absentes de la liste importée pour l'année donnée.
     * @param amethisList
     * @param year
     */
    private void desactivateMissingPropositions(List<PropositionThese> amethisList) {
    	
    	log.info("Début de la désactivation des propositions de thèses AMETHIS manquantes");
    	
        Set<String> matricules = amethisList.stream()
                .map(PropositionThese::getMatricule)
                .collect(Collectors.toSet());

        List<PropositionThese> localAmethis = 
                repository.findActiveBySource(SourceThese.AMETHIS);

        List<PropositionThese> toDesactivate = localAmethis.stream()
                .filter(p -> !matricules.contains(p.getMatricule()))
                .peek(p -> p.setActive(false))
                .collect(Collectors.toList());

        if (!toDesactivate.isEmpty()) {
        	log.info("Nombre de propositions à désactiver : {}", toDesactivate.size());
            repository.saveAll(toDesactivate);
        } else {
			log.info("Aucune proposition à désactiver");
        }

    }

    /**
     * Parse le JSON de réponse de l'API AMETHIS en une liste de propositions de thèse. En cas d'erreur, retourne une liste vide.
     * @param json
     * @return
     */
    private List<PropositionThese> parse(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            return mapper.readValue(json, AmethisResponse.class).getPropositions();
        } catch (Exception e) {
            log.error("Erreur JSON AMETHIS", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Détecte et log les matricules en doublon dans la liste AMETHIS reçue.
     * Cette méthode ne modifie rien en base : elle sert uniquement au diagnostic.
     */
    private void detectDuplicateMatricules(List<PropositionThese> propositions) {

        log.warn("🔍 Vérification des doublons de matricule AMETHIS...");

        // Regroupe les propositions par matricule
        Map<String, List<PropositionThese>> grouped =
                propositions.stream()
                        .collect(Collectors.groupingBy(PropositionThese::getMatricule));

        // Filtre uniquement les matricules apparaissant plus d'une fois
        List<String> duplicates = grouped.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(Map.Entry::getKey)
                .toList();

        if (duplicates.isEmpty()) {
            log.warn("✅ Aucun doublon de matricule AMETHIS détecté.");
            return;
        }

        log.error("❌ Doublons détectés dans les matricules AMETHIS :");

        for (String m : duplicates) {
            List<PropositionThese> list = grouped.get(m);

            log.error(" - Matricule '{}' apparaît {} fois", m, list.size());

            for (PropositionThese p : list) {
                log.error("     → Titre: {}", p.getTheseTitre());
            }
        }

        log.error("⚠️ Import AMETHIS potentiellement invalide : doublons détectés.");
    }

    
    private String buildUniqueMatricule(PropositionThese p, int compteur) {
        String base = p.getMatricule(); // matricule envoyé par Amethis

        // Heure-minute du jour (HHmm)
        String time = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HHmm"));

        String matricule = base + "-" + compteur + "-"+ time;

        // Sécurité : tronquer si > 25 caractères
        if (matricule.length() > 25) {
            matricule = matricule.substring(0, 25);
        }

        return matricule;
    }

}

