package fr.dinum.beta.gouv.doctorat.controller;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.dto.ConnexionRequest;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionRequest;
import fr.dinum.beta.gouv.doctorat.dto.UtilisateurDto;
import fr.dinum.beta.gouv.doctorat.service.AuthService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/inscription")
    public ResponseEntity<?> inscrire(@Valid @RequestBody InscriptionRequest request) {
        log.info("POST /api/inscription - email: {}", request.getEmail());
        try {
            ConnexionResponse response = authService.inscrire(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/connexion")
    public ResponseEntity<?> connecter(@Valid @RequestBody ConnexionRequest request) {
        log.info("POST /api/connexion - email: {}", request.getEmail());
        try {
            ConnexionResponse response = authService.connecter(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getUtilisateurCourant() {
        log.info("GET /api/me");
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        UtilisateurDto utilisateur = authService.getUtilisateur(userId);
        return ResponseEntity.ok(utilisateur);
    }

    @PostMapping("/deconnexion")
    public ResponseEntity<?> deconnexion() {
        log.info("POST /api/deconnexion");
        return ResponseEntity.noContent().build();
    }
}
