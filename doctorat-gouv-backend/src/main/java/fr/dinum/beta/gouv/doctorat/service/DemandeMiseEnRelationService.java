package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;

import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationRequest;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.dto.TableauDeBordCandidatItemDto;
import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class DemandeMiseEnRelationService {

    private static final Logger log = LoggerFactory.getLogger(DemandeMiseEnRelationService.class);

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    private final PropositionTheseService propositionTheseService;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeMiseEnRelationRepository demandeRepository;
    private final BrevoEmailService emailService;

    public DemandeMiseEnRelationService(
            PropositionTheseService propositionTheseService,
            UtilisateurRepository utilisateurRepository,
            DemandeMiseEnRelationRepository demandeRepository,
            BrevoEmailService emailService) {
        this.propositionTheseService = propositionTheseService;
        this.utilisateurRepository = utilisateurRepository;
        this.demandeRepository = demandeRepository;
        this.emailService = emailService;
    }

    /**
     * Charge la demande existante pour un candidat et une proposition.
     */
    public Optional<DemandeMiseEnRelation> chargerDemande(String userId, long propositionTheseId) {
        return demandeRepository.findByCandidatIdAndPropositionTheseId(userId, propositionTheseId);
    }

    /**
     * Liste les demandes du candidat pour le tableau de bord, enrichies
     * avec les données de la thèse.
     * Mapping colonnes : BROUILLON -> Brouillon, CREE -> En attente.
     */
    public List<TableauDeBordCandidatItemDto> listerDemandesParCandidat(String userId) {
        List<DemandeMiseEnRelation> demandes = demandeRepository.findByCandidatIdOrderByUpdatedAtDesc(userId);
        if (demandes.isEmpty()) {
            return List.of();
        }

        List<Long> propositionIds = demandes.stream()
                .map(DemandeMiseEnRelation::getPropositionTheseId)
                .distinct()
                .toList();
        Map<Long, PropositionTheseDto> theses = propositionTheseService.findByIdInAsMap(propositionIds);

        List<TableauDeBordCandidatItemDto> result = new ArrayList<>();
        for (DemandeMiseEnRelation demande : demandes) {
            PropositionTheseDto these = theses.get(demande.getPropositionTheseId());
            if (these == null) {
                log.warn("Thèse {} introuvable pour la demande {}", demande.getPropositionTheseId(), demande.getId());
                continue;
            }
            String encadrantNom = buildEncadrantNom(these);
            result.add(new TableauDeBordCandidatItemDto(
                    demande.getId(),
                    demande.getPropositionTheseId(),
                    these.getTheseTitre(),
                    these.getEtablissementLibelle(),
                    encadrantNom,
                    demande.getUpdatedAt(),
                    demande.getStatut().name(),
                    Boolean.TRUE.equals(demande.getArchivee())));
        }
        return result;
    }

    /**
     * Archive une demande du candidat. Les lignes existantes sans valeur
     * (archivee NULL) sont traitées comme non archivées.
     */
    public DemandeMiseEnRelation archiverDemande(String userId, long demandeId) {
        DemandeMiseEnRelation demande = demandeRepository.findByIdAndCandidatId(demandeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        demande.setArchivee(Boolean.TRUE);
        demande.setUpdatedAt(LocalDateTime.now());
        DemandeMiseEnRelation saved = demandeRepository.save(demande);
        log.info("Demande archivée (id={}) pour candidat {}", saved.getId(), userId);
        return saved;
    }

    private String buildEncadrantNom(PropositionTheseDto these) {
        String prenom = these.getDirectionThesePrenom() != null ? these.getDirectionThesePrenom().trim() : "";
        String nom = these.getDirectionTheseNom() != null ? these.getDirectionTheseNom().trim() : "";
        String fullName = (prenom + " " + nom).trim();
        return fullName.isEmpty() ? "—" : fullName;
    }

    /**
     * Sauvegarde ou met à jour la demande en statut BROUILLON.
     */
    public DemandeMiseEnRelation sauvegarderBrouillon(String userId, DemandeMiseEnRelationRequest request) {
        Optional<DemandeMiseEnRelation> existing = demandeRepository
                .findByCandidatIdAndPropositionTheseId(userId, request.getIdPropositionThese());

        DemandeMiseEnRelation demande;
        if (existing.isPresent()) {
            demande = existing.get();
            if (demande.getStatut() == StatutDemandeMiseEnRelation.CREE) {
                throw new IllegalArgumentException(
                        "Une demande a déjà été envoyée pour cette thèse. Impossible de modifier le brouillon.");
            }
            demande.setMotivations(request.getMotivations());
            demande.setRgpdConsent(request.getRgpdConsent());
            demande.setUpdatedAt(LocalDateTime.now());
        } else {
            demande = new DemandeMiseEnRelation(
                    userId,
                    request.getIdPropositionThese(),
                    request.getMotivations(),
                    request.getRgpdConsent(),
                    StatutDemandeMiseEnRelation.BROUILLON);
        }

        DemandeMiseEnRelation saved = demandeRepository.save(demande);
        log.info("Brouillon sauvegardé (id={}) pour candidat {} et thèse {}",
                saved.getId(), userId, request.getIdPropositionThese());
        return saved;
    }

    /**
     * Met à jour la demande en statut CREE et envoie les emails.
     */
    public void envoyerDemande(String userId, DemandeMiseEnRelationRequest request) {
        Optional<DemandeMiseEnRelation> existing = demandeRepository
                .findByCandidatIdAndPropositionTheseId(userId, request.getIdPropositionThese());

        DemandeMiseEnRelation demande;
        if (existing.isPresent()) {
            demande = existing.get();
            if (demande.getStatut() == StatutDemandeMiseEnRelation.CREE) {
                throw new IllegalArgumentException("Une demande a déjà été envoyée pour cette thèse.");
            }
            demande.setMotivations(request.getMotivations());
            demande.setRgpdConsent(request.getRgpdConsent());
            demande.setStatut(StatutDemandeMiseEnRelation.CREE);
            demande.setUpdatedAt(LocalDateTime.now());
        } else {
            demande = new DemandeMiseEnRelation(
                    userId,
                    request.getIdPropositionThese(),
                    request.getMotivations(),
                    request.getRgpdConsent(),
                    StatutDemandeMiseEnRelation.CREE);
        }

        demandeRepository.save(demande);
        log.info("Demande envoyée (id={}) pour candidat {} et thèse {}",
                demande.getId(), userId, request.getIdPropositionThese());

        // Récupérer les données pour les emails
        Utilisateur candidat = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        PropositionTheseDto these = propositionTheseService.findById(request.getIdPropositionThese());

        if (mailEnabled) {
            envoyerMailEncadrant(candidat, these, request);
            envoyerMailCandidat(candidat, these);
        } else {
            log.warn("Mode DEV : mails désactivés");
        }
    }

    private void envoyerMailEncadrant(Utilisateur candidat, PropositionTheseDto these,
            DemandeMiseEnRelationRequest request) {
        log.info("Préparation du mail de demande de mise en relation pour l'encadrant");

        Map<String, Object> params = new HashMap<>();
        params.put("prenom_candidat", candidat.getPrenom());
        params.put("nom_candidat", candidat.getNom());
        params.put("email_candidat", candidat.getEmail());
        params.put("titre_sujet", these.getTheseTitre());
        params.put("motivation", request.getMotivations());
        params.put("nom_plateforme", "Doctorat.gouv");

        String urlSujet = String.format("https://app.doctorat.gouv.fr/proposition?id=%s", these.getId());
        params.put("url_sujet", urlSujet);

        try {
            if (isValidEmail(these.getDirectionTheseEmail())) {
                emailService.sendTemplateEmail(these.getDirectionTheseEmail(), 31, params);
                log.info("Mail de demande de mise en relation envoyé à l'encadrant {}",
                        these.getDirectionTheseEmail());
            } else {
                log.warn("Email encadrant invalide ou absent pour la thèse {}", these.getId());
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de l'envoi du mail à l'encadrant", e);
        }
    }

    private void envoyerMailCandidat(Utilisateur candidat, PropositionTheseDto these) {
        log.info("Préparation du mail de confirmation pour le candidat");

        Map<String, Object> params = new HashMap<>();
        params.put("prenom", candidat.getPrenom());
        params.put("titre_sujet", these.getTheseTitre());
        params.put("nom_plateforme", "Doctorat.gouv");
        params.put("url_sujet",
                String.format("https://app.doctorat.gouv.fr/proposition?id=%s", these.getId()));

        try {
            if (isValidEmail(candidat.getEmail())) {
                emailService.sendTemplateEmail(candidat.getEmail(), 32, params);
                log.info("Mail de confirmation envoyé au candidat {}", candidat.getEmail());
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de l'envoi du mail de confirmation au candidat", e);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isBlank() && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
