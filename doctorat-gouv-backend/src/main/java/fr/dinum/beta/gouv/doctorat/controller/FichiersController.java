package fr.dinum.beta.gouv.doctorat.controller;

import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.service.DirecteurTheseAuthorizationService;
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
    private final DemandeMiseEnRelationRepository demandeRepository;
    private final DirecteurTheseAuthorizationService directeurTheseAuthorizationService;

    public FichiersController(UtilisateurRepository utilisateurRepository,
                              PhotoService photoService,
                              DemandeMiseEnRelationRepository demandeRepository,
                              DirecteurTheseAuthorizationService directeurTheseAuthorizationService) {
        this.utilisateurRepository = utilisateurRepository;
        this.photoService = photoService;
        this.demandeRepository = demandeRepository;
        this.directeurTheseAuthorizationService = directeurTheseAuthorizationService;
    }

    @GetMapping("/utilisateurs/{userId}/photo")
    public ResponseEntity<Resource> getPhoto(@PathVariable("userId") String userId, Authentication auth) {
        String currentUserId = (auth != null) ? auth.getName() : null;

        // Un utilisateur peut toujours voir sa propre photo
        if (currentUserId == null || (!currentUserId.equals(userId) && !canViewPhoto(currentUserId, userId))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

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

    /**
     * Vérifie si l'utilisateur courant peut voir la photo de l'utilisateur cible.
     * Règle : les deux sont participants d'une demande de mise en relation non archivée
     * (statut CREE ou ACCEPTEE).
     */
    private boolean canViewPhoto(String currentUserId, String targetUserId) {
        // Récupérer les sujets rattachés au DT courant (si c'est un DT)
        java.util.Map<Long, fr.dinum.beta.gouv.doctorat.entity.PropositionThese> sujetsDt =
            directeurTheseAuthorizationService.findSujetsRattaches(currentUserId);
        if (!sujetsDt.isEmpty()) {
            // L'utilisateur courant est un DT : vérifier si le candidat targetUserId
            // a une demande non archivée sur l'un de ses sujets
            java.util.List<Long> sujetIds = new java.util.ArrayList<>(sujetsDt.keySet());
            return demandeRepository.existsDemandeNonArchiveeByCandidatAndSujets(targetUserId, sujetIds);
        }

        // L'utilisateur courant est un candidat : vérifier si le targetUserId est un DT
        // rattaché à un sujet sur lequel le candidat courant a une demande non archivée
        java.util.Map<Long, fr.dinum.beta.gouv.doctorat.entity.PropositionThese> sujetsTarget =
            directeurTheseAuthorizationService.findSujetsRattaches(targetUserId);
        if (!sujetsTarget.isEmpty()) {
            java.util.List<Long> sujetIds = new java.util.ArrayList<>(sujetsTarget.keySet());
            return demandeRepository.existsDemandeNonArchiveeByCandidatAndSujets(currentUserId, sujetIds);
        }

        return false;
    }
}
