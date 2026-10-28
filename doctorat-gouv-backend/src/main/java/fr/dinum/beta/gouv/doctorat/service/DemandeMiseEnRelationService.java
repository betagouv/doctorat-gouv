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

import fr.dinum.beta.gouv.doctorat.dto.DemandeCandidatDetailResponse;
import fr.dinum.beta.gouv.doctorat.dto.DemandeMiseEnRelationRequest;
import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.dto.TableauDeBordCandidatItemDto;
import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class DemandeMiseEnRelationService {

    private static final Logger log = LoggerFactory.getLogger(DemandeMiseEnRelationService.class);

    /** Template Brevo de notification du désistement au directeur de thèse. */
    private static final int BREVO_TEMPLATE_DESISTEMENT = 70;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    private String stubMailbox;

    private final PropositionTheseService propositionTheseService;
    private final UtilisateurRepository utilisateurRepository;
    private final DemandeMiseEnRelationRepository demandeRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final MessageEchangeRepository messageRepository;
    private final BrevoEmailService emailService;

    public DemandeMiseEnRelationService(
            PropositionTheseService propositionTheseService,
            UtilisateurRepository utilisateurRepository,
            DemandeMiseEnRelationRepository demandeRepository,
            ProfilCandidatRepository profilCandidatRepository,
            ProfilDirecteurTheseRepository profilDtRepository,
            MessageEchangeRepository messageRepository,
            BrevoEmailService emailService) {
        this.propositionTheseService = propositionTheseService;
        this.utilisateurRepository = utilisateurRepository;
        this.demandeRepository = demandeRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.profilDtRepository = profilDtRepository;
        this.messageRepository = messageRepository;
        this.emailService = emailService;
    }

    /**
     * Charge la demande existante pour un candidat et une proposition.
     */
    public Optional<DemandeMiseEnRelation> chargerDemande(String userId, long propositionTheseId) {
        return demandeRepository.findByCandidatIdAndPropositionTheseId(userId, propositionTheseId);
    }

    /**
     * Détail d'une demande vue par son candidat : demande, sujet,
     * direction de thèse et candidat. Tous statuts, archivée ou non
     * (lecture seule ; les échanges restent réservés aux acceptées).
     */
    public DemandeCandidatDetailResponse getDemandeDetailCandidat(String userId, Long demandeId) {
        DemandeMiseEnRelation demande = demandeRepository.findByIdAndCandidatId(demandeId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        PropositionTheseDto these = propositionTheseService.findById(demande.getPropositionTheseId());
        Utilisateur candidat = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        ProfilCandidat profilCandidat = profilCandidatRepository.findByUtilisateurId(userId).orElse(null);

        DemandeCandidatDetailResponse dto = new DemandeCandidatDetailResponse();
        dto.setId(demande.getId());
        dto.setStatut(demande.getStatut().name());
        dto.setMotivations(demande.getMotivations());
        dto.setArchivee(Boolean.TRUE.equals(demande.getArchivee()));
        dto.setDateDemande(demande.getUpdatedAt() != null
            ? demande.getUpdatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : null);
        dto.setDateDesistement(demande.getDateDesistement() != null
            ? demande.getDateDesistement().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : null);

        dto.setPropositionTheseId(these.getId());
        dto.setTitreSujet(these.getTheseTitre());
        dto.setDateMiseEnLigne(these.getDateMiseEnLigne() != null
            ? these.getDateMiseEnLigne().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : null);
        dto.setDateLimiteCandidature(these.getDateLimiteCandidature() != null
            ? these.getDateLimiteCandidature().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            : null);
        dto.setDateDebutThese(these.getDateDebutThese() != null
            ? these.getDateDebutThese().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            : null);
        dto.setEtablissement(these.getEtablissementLibelle());
        dto.setEcoleDoctorale(these.getEcoleDoctoraleLibelle());
        dto.setLaboratoire(these.getUniteRechercheLibelle());

        dto.setDirecteurPrenom(these.getDirectionThesePrenom());
        dto.setDirecteurNom(these.getDirectionTheseNom());
        if (these.getDirectionTheseEmail() != null) {
            utilisateurRepository.findByEmailIgnoreCaseAndTrim(these.getDirectionTheseEmail())
                .ifPresent(dt -> dto.setDirecteurPhotoUrl(ProfilService.urlPhoto(dt)));
        }

        String nomComplet = ((candidat.getPrenom() != null ? candidat.getPrenom().trim() + " " : "")
            + (candidat.getNom() != null ? candidat.getNom().trim() : "")).trim();
        dto.setCandidatNom(nomComplet.isEmpty() ? null : nomComplet);
        dto.setCandidatPrenom(candidat.getPrenom());
        dto.setCandidatPhotoUrl(ProfilService.urlPhoto(candidat));
        if (profilCandidat != null) {
            dto.setCandidatSituation(profilCandidat.getSituation());
        }
        return dto;
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
            TableauDeBordCandidatItemDto item = new TableauDeBordCandidatItemDto(
                    demande.getId(),
                    demande.getPropositionTheseId(),
                    these.getTheseTitre(),
                    these.getEtablissementLibelle(),
                    encadrantNom,
                    demande.getUpdatedAt(),
                    demande.getStatut().name(),
                    Boolean.TRUE.equals(demande.getArchivee()));
            item.setNbMessagesNonLus((int) messageRepository
                .countByDemandeIdAndAuteurIdNotAndLuFalse(demande.getId(), userId));
            result.add(item);
        }
        return result;
    }

    /**
     * Archive une demande du candidat. Les lignes existantes sans valeur
     * (archivee NULL) sont traitées comme non archivées.
     * Les demandes acceptées par un DT ne peuvent pas être archivées.
     */
    public DemandeMiseEnRelation archiverDemande(String userId, long demandeId) {
        DemandeMiseEnRelation demande = demandeRepository.findByIdAndCandidatId(demandeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (demande.getStatut() == StatutDemandeMiseEnRelation.ACCEPTEE) {
            throw new IllegalStateException("Une demande acceptée ne peut pas être archivée");
        }
        demande.setArchivee(Boolean.TRUE);
        demande.setUpdatedAt(LocalDateTime.now());
        DemandeMiseEnRelation saved = demandeRepository.save(demande);
        log.info("Demande archivée (id={}) pour candidat {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Désarchive une demande du candidat : elle réapparaît dans le fil « En cours »
     * du tableau de bord (brouillon, en attente ou mise en relation).
     *
     * <p>Une demande <strong>désistée</strong> ne peut pas être réactivée : seul
     * l'annulation de désistement (outil hors production) la remet en état.</p>
     */
    public DemandeMiseEnRelation desarchiverDemande(String userId, long demandeId) {
        DemandeMiseEnRelation demande = demandeRepository.findByIdAndCandidatId(demandeId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (!Boolean.TRUE.equals(demande.getArchivee())) {
            throw new IllegalStateException("Cette demande n'est pas archivée");
        }
        if (demande.getDateDesistement() != null) {
            throw new IllegalStateException("Une demande désistée ne peut pas être réactivée");
        }
        demande.setArchivee(Boolean.FALSE);
        demande.setUpdatedAt(LocalDateTime.now());
        DemandeMiseEnRelation saved = demandeRepository.save(demande);
        log.info("Demande désarchivée (id={}) pour candidat {}", saved.getId(), userId);
        return saved;
    }

    /**
     * Désistement (« Je ne suis plus intéressé ») : la demande acceptée est
     * archivée (affichée en MISES EN RELATION des archives) avec date et motif,
     * et le directeur de thèse est notifié (template Brevo 70, bouchonné hors production).
     */
    public DemandeMiseEnRelation seDesister(String userId, Long demandeId, String motif) {
        DemandeMiseEnRelation demande = demandeRepository.findByIdAndCandidatId(demandeId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (demande.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE
            || Boolean.TRUE.equals(demande.getArchivee())) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        String texte = motif != null ? motif.trim() : "";
        if (texte.isEmpty()) {
            throw new IllegalArgumentException("Le motif est obligatoire");
        }
        demande.setArchivee(Boolean.TRUE);
        demande.setDateDesistement(LocalDateTime.now());
        demande.setMotifDesistement(texte.length() > 2000 ? texte.substring(0, 2000) : texte);
        demande.setUpdatedAt(LocalDateTime.now());
        DemandeMiseEnRelation saved = demandeRepository.save(demande);
        log.info("Désistement du candidat {} sur la demande {}", userId, demandeId);

        envoyerMailsDesistement(userId, saved);
        return saved;
    }

    /**
     * Outil hors production : annule un désistement (désarchive, efface
     * date et motif). Refusé en production.
     */
    public DemandeCandidatDetailResponse annulerDesistement(String userId, Long demandeId,
            boolean estProduction) {
        if (estProduction) {
            throw new IllegalStateException("Annulation interdite en production");
        }
        DemandeMiseEnRelation demande = demandeRepository.findByIdAndCandidatId(demandeId, userId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        if (demande.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE
            || !Boolean.TRUE.equals(demande.getArchivee())
            || demande.getDateDesistement() == null) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        demande.setArchivee(Boolean.FALSE);
        demande.setDateDesistement(null);
        demande.setMotifDesistement(null);
        demande.setUpdatedAt(LocalDateTime.now());
        demandeRepository.save(demande);
        log.info("Désistement annulé pour la demande {} (candidat {})", demandeId, userId);
        return getDemandeDetailCandidat(userId, demandeId);
    }

    /**
     * Notification du directeur de thèse du désistement du candidat
     * (template Brevo 70 : civilité et nom du DT, nom du candidat, titre du sujet
     * et motif du désistement). Hors production, le destinataire réel est remplacé
     * par MAIL_STUB_MAILBOX (même principe que les mails 31/32).
     * L'échec d'envoi ne bloque jamais le désistement.
     */
    private void envoyerMailsDesistement(String userId, DemandeMiseEnRelation demande) {
        try {
            PropositionTheseDto these = propositionTheseService.findById(demande.getPropositionTheseId());
            Utilisateur candidat = utilisateurRepository.findById(userId).orElse(null);
            if (candidat == null) {
                log.warn("Candidat {} introuvable : mail de désistement non envoyé pour la demande {}",
                    userId, demande.getId());
                return;
            }

            String destinataire = these.getDirectionTheseEmail();
            String civilite = "";
            String nomDirecteur = "";
            if (destinataire != null && !destinataire.isBlank()) {
                Optional<Utilisateur> directeur =
                    utilisateurRepository.findByEmailIgnoreCaseAndTrim(destinataire.trim());
                if (directeur.isPresent()) {
                    Utilisateur dt = directeur.get();
                    nomDirecteur = nomComplet(dt);
                    civilite = profilDtRepository.findByUtilisateurId(dt.getId())
                        .map(p -> p.getCivilite() != null ? p.getCivilite().trim() : "")
                        .orElse("");
                }
            }
            if (nomDirecteur.isEmpty()) {
                // Repli sur les données ADUM / AMETHIS de la proposition de thèse.
                String encadrant = buildEncadrantNom(these);
                nomDirecteur = "—".equals(encadrant) ? "" : encadrant;
            }

            if (!mailEnabled) {
                if (stubMailbox == null || stubMailbox.isBlank()) {
                    log.warn("Mode DEV : mail de désistement non envoyé (boîte bouchon non configurée) pour la demande {}",
                        demande.getId());
                    return;
                }
                destinataire = stubMailbox.trim();
                log.info("Mode DEV : mail de désistement de la demande {} bouchonné vers {}",
                    demande.getId(), destinataire);
            }

            if (!isValidEmail(destinataire)) {
                log.warn("Email du directeur de thèse invalide ou absent pour la demande {}", demande.getId());
                return;
            }

            Map<String, Object> params = new HashMap<>();
            params.put("civilite", civilite);
            params.put("nom", nomDirecteur);
            params.put("nom_candidat", nomComplet(candidat));
            params.put("titre_sujet", these.getTheseTitre() != null ? these.getTheseTitre() : "");
            params.put("motif_desistement",
                demande.getMotifDesistement() != null ? demande.getMotifDesistement() : "");

            emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_DESISTEMENT, params);
            log.info("Mail de désistement envoyé au directeur de thèse {} pour la demande {}",
                destinataire, demande.getId());
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail de désistement pour la demande {}", demande.getId(), e);
        }
    }

    private String nomComplet(Utilisateur utilisateur) {
        String prenom = utilisateur.getPrenom() != null ? utilisateur.getPrenom().trim() : "";
        String nom = utilisateur.getNom() != null ? utilisateur.getNom().trim() : "";
        return (prenom + " " + nom).trim();
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

        String destinataireEncadrant = these.getDirectionTheseEmail();
        String destinataireCandidat = candidat.getEmail();

        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV : mails de demande non envoyés (boîte bouchon non configurée)");
                return;
            }
            destinataireEncadrant = stubMailbox.trim();
            destinataireCandidat = stubMailbox.trim();
            log.info("Mode DEV : mails de demande bouchonnés vers {}", stubMailbox);
        }

        envoyerMailEncadrant(candidat, these, request, destinataireEncadrant);
        envoyerMailCandidat(candidat, these, destinataireCandidat);
    }

    private void envoyerMailEncadrant(Utilisateur candidat, PropositionTheseDto these,
            DemandeMiseEnRelationRequest request, String destinataire) {
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
            if (isValidEmail(destinataire)) {
                emailService.sendTemplateEmail(destinataire, 31, params);
                log.info("Mail de demande de mise en relation envoyé à l'encadrant {}",
                        destinataire);
            } else {
                log.warn("Email encadrant invalide ou absent pour la thèse {}", these.getId());
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de l'envoi du mail à l'encadrant", e);
        }
    }

    private void envoyerMailCandidat(Utilisateur candidat, PropositionTheseDto these, String destinataire) {
        log.info("Préparation du mail de confirmation pour le candidat");

        Map<String, Object> params = new HashMap<>();
        params.put("prenom", candidat.getPrenom());
        params.put("titre_sujet", these.getTheseTitre());
        params.put("nom_plateforme", "Doctorat.gouv");
        params.put("url_sujet",
                String.format("https://app.doctorat.gouv.fr/proposition?id=%s", these.getId()));

        try {
            if (isValidEmail(destinataire)) {
                emailService.sendTemplateEmail(destinataire, 32, params);
                log.info("Mail de confirmation envoyé au candidat {}", destinataire);
            }
        } catch (JsonProcessingException e) {
            log.error("Erreur lors de l'envoi du mail de confirmation au candidat", e);
        }
    }

    private boolean isValidEmail(String email) {
        return email != null && !email.isBlank() && email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
