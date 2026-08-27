package fr.dinum.beta.gouv.doctorat.controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionCompletRequest;
import fr.dinum.beta.gouv.doctorat.service.AuthService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * Contrôleur dédié à l'inscription complète (multipart) : coordonnées JSON + fichiers.
 */
@RestController
@RequestMapping("/api")
public class InscriptionController {

    private static final Logger log = LoggerFactory.getLogger(InscriptionController.class);

    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator;

    public InscriptionController(AuthService authService, Validator validator) {
        this.authService = authService;
        this.validator = validator;
    }

    @PostMapping(value = "/inscription/complet", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> inscrireComplet(MultipartHttpServletRequest request) {
        String coordonneesJson = extraireCoordonnees(request);
        if (coordonneesJson == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Les coordonnées sont obligatoires"));
        }

        InscriptionCompletRequest coordonnees;
        try {
            coordonnees = objectMapper.readValue(coordonneesJson, InscriptionCompletRequest.class);
        } catch (IOException e) {
            log.warn("Coordonnees JSON invalide", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Le format des coordonnées est invalide"));
        }

        Set<ConstraintViolation<InscriptionCompletRequest>> violations = validator.validate(coordonnees);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .collect(Collectors.joining(" ; "));
            return ResponseEntity.badRequest().body(Map.of("error", message));
        }

        MultipartFile cv = request.getFile("cv");
        if (cv == null || cv.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Le CV est obligatoire"));
        }

        List<MultipartFile> pieces = request.getMultiFileMap().get("piecesComplementaires");

        log.info("POST /api/inscription/complet - email: {}", coordonnees.getEmail());
        try {
            ConnexionResponse response = authService.inscrireComplet(coordonnees, cv, pieces);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String extraireCoordonnees(MultipartHttpServletRequest request) {
        MultipartFile file = request.getFile("coordonnees");
        if (file != null && !file.isEmpty()) {
            try {
                return new String(file.getBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.warn("Lecture de la partie coordonnees impossible", e);
                return null;
            }
        }
        return request.getParameter("coordonnees");
    }
}
