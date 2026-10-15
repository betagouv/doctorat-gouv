package fr.dinum.beta.gouv.doctorat.controller;

import java.io.InputStream;
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
import fr.dinum.beta.gouv.doctorat.dto.MessageDto;
import fr.dinum.beta.gouv.doctorat.dto.MessageEnvoiRequest;
import fr.dinum.beta.gouv.doctorat.dto.MessageListeRequest;
import fr.dinum.beta.gouv.doctorat.dto.NotificationPreferenceRequest;
import fr.dinum.beta.gouv.doctorat.dto.NotificationPreferenceResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtUpdateRequest;
import fr.dinum.beta.gouv.doctorat.dto.SujetDtResponse;
import fr.dinum.beta.gouv.doctorat.service.DirecteurTheseService;
import fr.dinum.beta.gouv.doctorat.service.EchangeService;
import fr.dinum.beta.gouv.doctorat.service.NotificationPreferenceService;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/directeur-these")
public class DirecteurTheseController {

    private static final Logger log = LoggerFactory.getLogger(DirecteurTheseController.class);

    private final DirecteurTheseService directeurTheseService;
    private final EchangeService echangeService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final StoragePort storage;

    public DirecteurTheseController(DirecteurTheseService directeurTheseService,
                                    EchangeService echangeService,
                                    NotificationPreferenceService notificationPreferenceService,
                                    StoragePort storage) {
        this.directeurTheseService = directeurTheseService;
        this.echangeService = echangeService;
        this.notificationPreferenceService = notificationPreferenceService;
        this.storage = storage;
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

    @GetMapping("/offres")
    public ResponseEntity<List<SujetDtResponse>> getOffres() {
        String userId = getCurrentUserId();
        log.info("Consultation des offres d'accompagnement du directeur de thèse {}", userId);
        return ResponseEntity.ok(directeurTheseService.getOffres(userId));
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
            String key = directeurTheseService.getCandidatFichier(
                userId, request.getDemandeId(), request.getType(), request.getIndex());
            String filename = directeurTheseService.displayFilename(key);
            InputStream content = storage.open(key);
            Resource resource = new InputStreamResource(content);
            return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + filename.replace("\"", "") + "\"")
                .contentLength(storage.size(key))
                .body(resource);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Erreur lors de la lecture du fichier de la demande {}", request.getDemandeId(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/mises-en-relation/accepter")
    public ResponseEntity<DemandeDtResponse> accepterDemande(@Valid @RequestBody DemandeDetailRequest request) {
        String userId = getCurrentUserId();
        log.info("Acceptation de la demande {} par le directeur de thèse {}", request.getId(), userId);
        try {
            return ResponseEntity.ok(directeurTheseService.accepterDemande(userId, request.getId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mises-en-relation/reinitialiser")
    public ResponseEntity<DemandeDtResponse> reinitialiserDemande(@Valid @RequestBody DemandeDetailRequest request) {
        String userId = getCurrentUserId();
        log.info("Réinitialisation de la demande {} par le directeur de thèse {}", request.getId(), userId);
        try {
            return ResponseEntity.ok(directeurTheseService.reinitialiserDemande(userId, request.getId()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mises-en-relation/messages")
    public ResponseEntity<List<MessageDto>> listerMessages(@Valid @RequestBody MessageListeRequest request) {
        String userId = getCurrentUserId();
        try {
            return ResponseEntity.ok(echangeService.lister(userId, request.getDemandeId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/mises-en-relation/messages/envoyer")
    public ResponseEntity<MessageDto> envoyerMessage(@Valid @RequestBody MessageEnvoiRequest request) {
        String userId = getCurrentUserId();
        log.info("Envoi d'un message sur la demande {} par le directeur de thèse {}", request.getDemandeId(), userId);
        try {
            return ResponseEntity.ok(echangeService.envoyer(userId, request.getDemandeId(), request.getContenu()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/mot-de-passe")
    public ResponseEntity<Void> changerMotDePasse(@Valid @RequestBody ChangementMotDePasseRequest request) {
        String userId = getCurrentUserId();
        log.info("Changement de mot de passe pour le directeur de thèse {}", userId);
        directeurTheseService.changerMotDePasse(userId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/notifications")
    public ResponseEntity<NotificationPreferenceResponse> getNotifications() {
        String userId = getCurrentUserId();
        log.info("Consultation préférence notifications pour le directeur de thèse {}", userId);
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
        log.info("Mise à jour préférence notifications pour le directeur de thèse {}", userId);
        try {
            return ResponseEntity.ok(notificationPreferenceService.updatePreference(userId, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getName();
    }
}
