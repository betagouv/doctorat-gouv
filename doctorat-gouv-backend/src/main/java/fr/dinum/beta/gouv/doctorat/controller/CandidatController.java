package fr.dinum.beta.gouv.doctorat.controller;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.NotificationPreferenceRequest;
import fr.dinum.beta.gouv.doctorat.dto.NotificationPreferenceResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilUpdateRequest;
import fr.dinum.beta.gouv.doctorat.service.NotificationPreferenceService;
import fr.dinum.beta.gouv.doctorat.service.ProfilService;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidat")
public class CandidatController {

    private static final Logger log = LoggerFactory.getLogger(CandidatController.class);

    private final ProfilService profilService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final StoragePort storage;

    public CandidatController(ProfilService profilService,
                              NotificationPreferenceService notificationPreferenceService,
                              StoragePort storage) {
        this.profilService = profilService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.storage = storage;
    }

    @GetMapping("/profil")
    public ResponseEntity<ProfilResponse> getProfil() {
        String userId = getCurrentUserId();
        log.info("Consultation du profil pour l'utilisateur {}", userId);
        return profilService.getProfil(userId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profil")
    public ResponseEntity<ProfilResponse> updateProfil(@Valid @RequestBody ProfilUpdateRequest request) {
        String userId = getCurrentUserId();
        log.info("Mise à jour du profil pour l'utilisateur {}", userId);
        ProfilResponse response = profilService.updateProfil(userId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/competences")
    public ResponseEntity<ProfilResponse> addCompetence(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String competence = body.get("competence");
        if (competence == null || competence.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Ajout compétence '{}' pour l'utilisateur {}", competence, userId);
        ProfilResponse response = profilService.addCompetence(userId, competence.trim());
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/competences")
    public ResponseEntity<ProfilResponse> removeCompetence(@RequestBody Map<String, String> body) {
        String userId = getCurrentUserId();
        String competence = body.get("competence");
        if (competence == null || competence.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        log.info("Suppression compétence '{}' pour l'utilisateur {}", competence, userId);
        ProfilResponse response = profilService.removeCompetence(userId, competence.trim());
        return ResponseEntity.ok(response);
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest request) {
        String userId = getCurrentUserId();
        log.info("Changement de mot de passe pour l'utilisateur {}", userId);
        profilService.changerMotDePasse(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<NotificationPreferenceResponse> getNotifications() {
        String userId = getCurrentUserId();
        log.info("Consultation préférence notifications pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(notificationPreferenceService.getPreference(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/notifications")
    public ResponseEntity<NotificationPreferenceResponse> updateNotifications(
            @Valid @RequestBody NotificationPreferenceRequest request) {
        String userId = getCurrentUserId();
        log.info("Mise à jour préférence notifications pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(notificationPreferenceService.updatePreference(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/fichiers/cv")
    public ResponseEntity<Resource> getMonCv() {
        String userId = getCurrentUserId();
        try {
            return streamerFichier(profilService.getMonFichier(userId, "cv", null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du CV de l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/fichiers/pieces/{index}")
    public ResponseEntity<Resource> getMaPiece(@PathVariable("index") int index) {
        String userId = getCurrentUserId();
        try {
            return streamerFichier(profilService.getMonFichier(userId, "piece", index));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la lecture d'une pièce de l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/fichiers/cv")
    public ResponseEntity<?> remplacerCv(@RequestParam("cv") MultipartFile cv) {
        String userId = getCurrentUserId();
        log.info("Remplacement du CV pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(profilService.remplacerCv(userId, cv));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors du remplacement du CV de l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/fichiers/pieces")
    public ResponseEntity<?> ajouterPieces(@RequestParam("pieces") List<MultipartFile> pieces) {
        String userId = getCurrentUserId();
        log.info("Ajout de pièce(s) pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(profilService.ajouterPieces(userId, pieces));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de l'ajout de pièce(s) pour l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/fichiers/pieces/{index}")
    public ResponseEntity<?> supprimerPiece(@PathVariable("index") int index) {
        String userId = getCurrentUserId();
        log.info("Suppression d'une pièce pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(profilService.supprimerPiece(userId, index));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression d'une pièce pour l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/fichiers/photo")
    public ResponseEntity<?> remplacerPhoto(@RequestParam("photo") MultipartFile photo) {
        String userId = getCurrentUserId();
        log.info("Remplacement de la photo de profil pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(profilService.remplacerPhoto(userId, photo));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors du remplacement de la photo de l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/fichiers/photo")
    public ResponseEntity<?> supprimerPhoto() {
        String userId = getCurrentUserId();
        log.info("Suppression de la photo de profil pour l'utilisateur {}", userId);
        try {
            return ResponseEntity.ok(profilService.supprimerPhoto(userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Erreur lors de la suppression de la photo de l'utilisateur {}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    private ResponseEntity<Resource> streamerFichier(String key) {
        String filename = StoragePort.displayName(key);
        InputStream content = storage.open(key);
        Resource resource = new InputStreamResource(content);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "inline; filename=\"" + filename.replace("\"", "") + "\"")
            .contentLength(storage.size(key))
            .body(resource);
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
