package fr.dinum.beta.gouv.doctorat.controller;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationRequest;
import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationResponse;
import fr.dinum.beta.gouv.doctorat.dto.TableauDeBordCandidatItemDto;
import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.service.DemandeMiseEnRelationService;

@RestController
@RequestMapping("/api/candidat/demande-mise-en-relation")
public class DemandeMiseEnRelationController {

    private static final Logger log = LoggerFactory.getLogger(DemandeMiseEnRelationController.class);

    private final DemandeMiseEnRelationService service;

    public DemandeMiseEnRelationController(DemandeMiseEnRelationService service) {
        this.service = service;
    }

    @GetMapping("/mes-demandes")
    public ResponseEntity<List<TableauDeBordCandidatItemDto>> mesDemandes() {
        String userId = getCurrentUserId();
        log.info("Chargement tableau de bord pour candidat {}", userId);
        return ResponseEntity.ok(service.listerDemandesParCandidat(userId));
    }

    @GetMapping
    public ResponseEntity<DemandeMiseEnRelationResponse> chargerDemande(
            @RequestParam long propositionTheseId) {

        String userId = getCurrentUserId();
        log.info("Chargement demande pour candidat {} et thèse {}", userId, propositionTheseId);

        Optional<DemandeMiseEnRelation> demande = service.chargerDemande(userId, propositionTheseId);

        if (demande.isEmpty()) {
            return ResponseEntity.ok(new DemandeMiseEnRelationResponse(
                    "Aucune demande existante", false));
        }

        DemandeMiseEnRelation d = demande.get();
        DemandeMiseEnRelationResponse response = new DemandeMiseEnRelationResponse(
                "Demande chargée", true);
        response.setId(d.getId());
        response.setMotivations(d.getMotivations());
        response.setRgpdConsent(d.getRgpdConsent());
        response.setStatut(d.getStatut().name());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/brouillon")
    public ResponseEntity<DemandeMiseEnRelationResponse> sauvegarderBrouillon(
            @RequestBody DemandeMiseEnRelationRequest request) {

        String userId = getCurrentUserId();
        log.info("Sauvegarde brouillon pour candidat {} et thèse {}", userId, request.getIdPropositionThese());

        validateRequest(request);

        DemandeMiseEnRelation demande = service.sauvegarderBrouillon(userId, request);

        DemandeMiseEnRelationResponse response = new DemandeMiseEnRelationResponse(
                "Brouillon enregistré", true);
        response.setId(demande.getId());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/archiver")
    public ResponseEntity<DemandeMiseEnRelationResponse> archiverDemande(
            @PathVariable("id") long id) {

        String userId = getCurrentUserId();
        log.info("Archivage demande {} pour candidat {}", id, userId);

        try {
            DemandeMiseEnRelation demande = service.archiverDemande(userId, id);
            DemandeMiseEnRelationResponse response = new DemandeMiseEnRelationResponse(
                    "Demande archivée", true);
            response.setId(demande.getId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    public ResponseEntity<DemandeMiseEnRelationResponse> envoyerDemande(
            @RequestBody DemandeMiseEnRelationRequest request) {

        String userId = getCurrentUserId();
        log.info("Envoi demande pour candidat {} et thèse {}", userId, request.getIdPropositionThese());

        validateRequest(request);

        service.envoyerDemande(userId, request);

        return ResponseEntity.ok(new DemandeMiseEnRelationResponse(
                "Demande de mise en relation envoyée avec succès", true));
    }

    private void validateRequest(DemandeMiseEnRelationRequest request) {
        if (request.getIdPropositionThese() <= 0) {
            throw new IllegalArgumentException("ID de proposition invalide");
        }
        if (request.getMotivations() == null || request.getMotivations().isBlank()) {
            throw new IllegalArgumentException("Les motivations sont obligatoires");
        }
        if (request.getMotivations().length() > 3500) {
            throw new IllegalArgumentException("Les motivations ne doivent pas dépasser 3500 caractères");
        }
        if (request.getRgpdConsent() == null || !request.getRgpdConsent()) {
            throw new IllegalArgumentException("Le consentement RGPD est obligatoire");
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
