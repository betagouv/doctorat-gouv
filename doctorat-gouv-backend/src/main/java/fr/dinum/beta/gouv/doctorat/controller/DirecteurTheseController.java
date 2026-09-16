package fr.dinum.beta.gouv.doctorat.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilUpdateRequest;
import fr.dinum.beta.gouv.doctorat.service.ProfilService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/directeur-these")
public class DirecteurTheseController {

    private static final Logger log = LoggerFactory.getLogger(DirecteurTheseController.class);

    private final ProfilService profilService;

    public DirecteurTheseController(ProfilService profilService) {
        this.profilService = profilService;
    }

    @GetMapping("/profil")
    public ResponseEntity<ProfilResponse> getProfil() {
        String userId = getCurrentUserId();
        log.info("Consultation du profil directeur de thèse pour l'utilisateur {}", userId);
        return profilService.getProfil(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profil")
    public ResponseEntity<ProfilResponse> updateProfil(@Valid @RequestBody ProfilUpdateRequest request) {
        String userId = getCurrentUserId();
        log.info("Mise à jour du profil directeur de thèse pour l'utilisateur {}", userId);
        ProfilResponse response = profilService.updateProfil(userId, request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest request) {
        String userId = getCurrentUserId();
        log.info("Changement de mot de passe pour le directeur de thèse {}", userId);
        profilService.changerMotDePasse(userId, request);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
