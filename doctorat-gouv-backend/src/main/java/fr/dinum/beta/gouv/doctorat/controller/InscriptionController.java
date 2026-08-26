package fr.dinum.beta.gouv.doctorat.controller;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionCompletRequest;
import fr.dinum.beta.gouv.doctorat.service.AuthService;
import jakarta.validation.Valid;

/**
 * Contrôleur dédié à l'inscription complète (multipart) : coordonnées JSON + fichiers.
 */
@RestController
@RequestMapping("/api")
public class InscriptionController {

    private static final Logger log = LoggerFactory.getLogger(InscriptionController.class);

    private final AuthService authService;

    public InscriptionController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping(value = "/inscription/complet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> inscrireComplet(
            @RequestPart("coordonnees") @Valid InscriptionCompletRequest coordonnees,
            @RequestPart("cv") MultipartFile cv,
            @RequestPart(value = "piecesComplementaires", required = false) List<MultipartFile> pieces) {
        log.info("POST /api/inscription/complet - email: {}", coordonnees.getEmail());
        try {
            ConnexionResponse response = authService.inscrireComplet(coordonnees, cv, pieces);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
