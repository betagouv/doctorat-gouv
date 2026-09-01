package fr.dinum.beta.gouv.doctorat.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilUpdateRequest;
import fr.dinum.beta.gouv.doctorat.service.CandidatService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidat")
public class CandidatController {

    private static final Logger log = LoggerFactory.getLogger(CandidatController.class);

    private final CandidatService candidatService;

    public CandidatController(CandidatService candidatService) {
        this.candidatService = candidatService;
    }

    @GetMapping("/profil")
    public ResponseEntity<ProfilResponse> getProfil() {
        String userId = getCurrentUserId();
        log.info("Consultation du profil pour l'utilisateur {}", userId);
        return candidatService.getProfil(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profil")
    public ResponseEntity<ProfilResponse> updateProfil(@Valid @RequestBody ProfilUpdateRequest request) {
        String userId = getCurrentUserId();
        log.info("Mise à jour du profil pour l'utilisateur {}", userId);
        ProfilResponse response = candidatService.updateProfil(userId, request);
        return ResponseEntity.ok(response);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
