package fr.dinum.beta.gouv.doctorat.controller;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.service.PhotoService;

/**
 * Lecture des fichiers des utilisateurs (photos de profil) via un endpoint
 * authentifié. La clé de stockage OOS n'est jamais exposée ; seul le
 * déterministe {@code /api/fichiers/utilisateurs/{id}/photo} est servi.
 */
@RestController
@RequestMapping("/api/fichiers")
public class FichiersController {

    private static final Logger log = LoggerFactory.getLogger(FichiersController.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PhotoService photoService;

    public FichiersController(UtilisateurRepository utilisateurRepository,
                              PhotoService photoService) {
        this.utilisateurRepository = utilisateurRepository;
        this.photoService = photoService;
    }

    @GetMapping("/utilisateurs/{userId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable("userId") String userId) {
        return utilisateurRepository.findById(userId)
            .filter(u -> u.getPhotoUrl() != null && !u.getPhotoUrl().isBlank())
            .map(u -> {
                try {
                    String key = u.getPhotoUrl();
                    if (!photoService.exists(key)) {
                        return ResponseEntity.notFound().<Resource>build();
                    }
                    InputStream content = photoService.open(key);
                    Resource resource = new InputStreamResource(content);
                    return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .contentLength(photoService.size(key))
                        .body(resource);
                } catch (Exception e) {
                    log.error("Erreur lors de la lecture de la photo de l'utilisateur {}", userId, e);
                    return ResponseEntity.internalServerError().<Resource>build();
                }
            })
            .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
