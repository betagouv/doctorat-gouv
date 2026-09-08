package fr.dinum.beta.gouv.doctorat.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationRequest;
import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationResponse;
import fr.dinum.beta.gouv.doctorat.service.DemandeMiseEnRelationService;

@RestController
@RequestMapping("/api/candidat/demande-mise-en-relation")
public class DemandeMiseEnRelationController {

    private static final Logger log = LoggerFactory.getLogger(DemandeMiseEnRelationController.class);

    private final DemandeMiseEnRelationService service;

    public DemandeMiseEnRelationController(DemandeMiseEnRelationService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<DemandeMiseEnRelationResponse> envoyerDemande(
            @RequestBody DemandeMiseEnRelationRequest request) {

        String userId = getCurrentUserId();
        log.info("Demande de mise en relation reçue pour l'utilisateur {}", userId);

        // Validation basique
        validateRequest(request);

        // Traitement
        service.traiterDemande(userId, request);

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
