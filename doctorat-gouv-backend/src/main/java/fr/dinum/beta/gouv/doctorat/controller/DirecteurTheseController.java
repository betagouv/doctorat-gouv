package fr.dinum.beta.gouv.doctorat.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.util.List;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.DemandeDetailRequest;
import fr.dinum.beta.gouv.doctorat.dto.DemandeDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.DemandeFichierRequest;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtUpdateRequest;
import fr.dinum.beta.gouv.doctorat.dto.SujetDtResponse;
import fr.dinum.beta.gouv.doctorat.service.DirecteurTheseService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/directeur-these")
public class DirecteurTheseController {

    private static final Logger log = LoggerFactory.getLogger(DirecteurTheseController.class);

    private final DirecteurTheseService directeurTheseService;

    public DirecteurTheseController(DirecteurTheseService directeurTheseService) {
        this.directeurTheseService = directeurTheseService;
    }

    @GetMapping("/profil")
    public ResponseEntity<ProfilDtResponse> getProfil() {
        String userId = getCurrentUserId();
        log.info("Consultation du profil directeur de thèse pour l'utilisateur {}", userId);
        return directeurTheseService.getProfil(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profil")
    public ResponseEntity<ProfilDtResponse> updateProfil(@Valid @RequestBody ProfilDtUpdateRequest request) {
        String userId = getCurrentUserId();
        log.info("Mise à jour du profil directeur de thèse pour l'utilisateur {}", userId);
        ProfilDtResponse response = directeurTheseService.updateProfil(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/competences")
    public ResponseEntity<ProfilDtResponse> addCompetence(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String competence = body.get("competence");
        if (competence == null || competence.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Ajout axe de recherche '{}' pour le directeur de thèse {}", competence, userId);
        ProfilDtResponse response = directeurTheseService.addCompetence(userId, competence.trim());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/competences")
    public ResponseEntity<ProfilDtResponse> removeCompetence(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String competence = body.get("competence");
        if (competence == null || competence.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Suppression axe de recherche '{}' pour le directeur de thèse {}", competence, userId);
        ProfilDtResponse response = directeurTheseService.removeCompetence(userId, competence.trim());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/sujets")
    public ResponseEntity<List<SujetDtResponse>> getSujets() {
        String userId = getCurrentUserId();
        log.info("Consultation des sujets du directeur de thèse {}", userId);
        return ResponseEntity.ok(directeurTheseService.getSujets(userId));
    }

    @GetMapping("/mises-en-relation")
    public ResponseEntity<List<DemandeDtResponse>> getDemandes() {
        String userId = getCurrentUserId();
        log.info("Consultation des demandes de mise en relation du directeur de thèse {}", userId);
        return ResponseEntity.ok(directeurTheseService.getDemandes(userId));
    }

    @PostMapping("/mises-en-relation/detail")
    public ResponseEntity<DemandeDtResponse> getDemandeDetail(@Valid @RequestBody DemandeDetailRequest request) {
        String userId = getCurrentUserId();
        log.info("Consultation du détail de la demande {} pour le directeur de thèse {}", request.getId(), userId);
        try {
            return ResponseEntity.ok(directeurTheseService.getDemandeDetail(userId, request.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mises-en-relation/fichier")
    public ResponseEntity<Resource> getCandidatFichier(@Valid @RequestBody DemandeFichierRequest request) {
        String userId = getCurrentUserId();
        try {
            String path = directeurTheseService.getCandidatFichier(
                userId, request.getDemandeId(), request.getType(), request.getIndex());
            Path file = Path.of(path);
            String filename = file.getFileName().toString();
            Resource resource = new FileSystemResource(file);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + filename.replace("\"", "") + "\"")
                .contentLength(Files.size(file))
                .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du fichier de la demande {}", request.getDemandeId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest request) {
        String userId = getCurrentUserId();
        log.info("Changement de mot de passe pour le directeur de thèse {}", userId);
        directeurTheseService.changerMotDePasse(userId, request);
        return ResponseEntity.ok().build();
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
