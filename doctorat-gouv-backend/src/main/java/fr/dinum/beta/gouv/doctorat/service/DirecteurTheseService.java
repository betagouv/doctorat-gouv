package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.DemandeDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtUpdateRequest;
import fr.dinum.beta.gouv.doctorat.dto.SujetDtResponse;
import fr.dinum.beta.gouv.doctorat.entity.AxeDeRecherche;
import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

@Service
public class DirecteurTheseService {

    private static final Logger log = LoggerFactory.getLogger(DirecteurTheseService.class);

    private static final String LIEN_CONVERSATION = "https://app.doctorat.gouv.fr/demande-candidat";
    private static final String LIEN_RECHERCHE = "https://app.doctorat.gouv.fr/";
    private static final int BREVO_TEMPLATE_ACCEPTATION = 67;
    private static final int BREVO_TEMPLATE_REFUS = 68;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    private String stubMailbox;

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final PropositionTheseRepository propositionTheseRepository;
    private final DemandeMiseEnRelationRepository demandeMiseEnRelationRepository;
    private final MessageEchangeRepository messageRepository;
    private final PasswordEncoder passwordEncoder;
    private final BrevoEmailService emailService;
    private final Environment environment;
    private final StoragePort storage;

    public DirecteurTheseService(UtilisateurRepository utilisateurRepository,
                                  ProfilDirecteurTheseRepository profilDtRepository,
                                  ProfilCandidatRepository profilCandidatRepository,
                                  PropositionTheseRepository propositionTheseRepository,
                                  DemandeMiseEnRelationRepository demandeMiseEnRelationRepository,
                                  MessageEchangeRepository messageRepository,
                                  PasswordEncoder passwordEncoder,
                                  BrevoEmailService emailService,
                                  Environment environment,
                                  StoragePort storage) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilDtRepository = profilDtRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.propositionTheseRepository = propositionTheseRepository;
        this.demandeMiseEnRelationRepository = demandeMiseEnRelationRepository;
        this.messageRepository = messageRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.environment = environment;
        this.storage = storage;
    }

    public Optional<ProfilDtResponse> getProfil(String userId) {
        return utilisateurRepository.findById(userId).map(this::toProfilDtResponse);
    }

    public ProfilDtResponse updateProfil(String userId, ProfilDtUpdateRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        String normalizedEmail = EmailUtils.normalize(request.getEmail());
        String ancienEmail = EmailUtils.normalize(utilisateur.getEmail());
        if (normalizedEmail != null && !normalizedEmail.equals(utilisateur.getEmail())) {
            utilisateurRepository.findByEmailIgnoreCaseAndTrim(normalizedEmail)
                .filter(u -> !u.getId().equals(userId))
                .ifPresent(u -> { throw new IllegalArgumentException("Un compte existe déjà avec cet email"); });
        }
        utilisateur.setEmail(normalizedEmail);
        if (request.getCompetences() != null) {
            utilisateur.setCompetences(new ArrayList<>(request.getCompetences()));
        }
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(userId)
            .orElse(new ProfilDirecteurThese());
        if (profil.getUtilisateur() == null) {
            profil.setUtilisateur(utilisateur);
        }
        profil.setCivilite(request.getCivilite());
        profil.setOrcid(request.getOrcid());
        profil.setEtablissement(request.getEtablissement());
        profil.setLaboratoire(request.getLaboratoire());
        profil.setEcoleDoctorale(request.getEcoleDoctorale());
        profil.setEmployeur(request.getEmployeur());
        profil.setTitre(request.getTitre());
        profil.setPrecisionTitre(request.getPrecisionTitre());
        profil.setHabilitationRecherche(request.getHabilitationRecherche());
        profil.setDomaineScientifique(request.getDomaineScientifique());
        profil.setExpertiseMots(request.getExpertiseMots());
        if (request.getMotsCles() != null) {
            profil.setMotsCles(new ArrayList<>(request.getMotsCles()));
        }
        profil.setDescriptionAxes(request.getDescriptionAxes());
        if (request.getAxes() != null) {
            List<AxeDeRecherche> axes = new ArrayList<>();
            for (AxeDeRecherche a : request.getAxes()) {
                AxeDeRecherche axe = new AxeDeRecherche();
                axe.setTitre(a.getTitre());
                axe.setPrecisions(a.getPrecisions());
                axe.setContactable(a.isContactable());
                // Identifiant stable par axe (généré côté front, backfillé ici pour les axes existants)
                axe.setExternalId(a.getExternalId() != null && !a.getExternalId().isBlank()
                    ? a.getExternalId()
                    : UUID.randomUUID().toString());
                axes.add(axe);
            }
            profil.setAxes(axes);
        }
        profilDtRepository.save(profil);

        synchroniserOffresAccompagnement(utilisateur, profil, ancienEmail);

        log.info("Profil directeur de thèse mis à jour pour l'utilisateur {}", userId);
        return toProfilDtResponse(utilisateur);
    }

    /**
     * Synchronise les offres d'accompagnement (PropositionThese de type "offre",
     * source DOCTORAT_GOUV) avec les axes de recherche contactables du directeur.
     * Règle : 1 axe contactable = 1 offre active (upsert par matricule déterministe).
     * Tout axe désactivé/supprimé (ou offre orpheline, ex. après changement d'e-mail)
     * est désactivé (active=false), jamais supprimé : l'historique et les demandes
     * de mise en relation sont préservés.
     */
    private void synchroniserOffresAccompagnement(Utilisateur utilisateur, ProfilDirecteurThese profil, String ancienEmail) {
        List<AxeDeRecherche> axes = profil.getAxes() != null ? profil.getAxes() : new ArrayList<>();
        String email = EmailUtils.normalize(utilisateur.getEmail());
        String orcid = normalizeOrcid(profil.getOrcid());
        LocalDateTime now = LocalDateTime.now();

        // 1. Offres DOCTORAT_GOUV existantes rattachées au directeur
        // (e-mail actuel + ancien e-mail + ORCID : couvre le changement d'e-mail).
        Map<String, PropositionThese> existantesParAxe = new LinkedHashMap<>();
        List<PropositionThese> candidates = new ArrayList<>();
        if (email != null) {
            candidates.addAll(propositionTheseRepository.findByDirecteurEmail(email));
        }
        if (ancienEmail != null && !ancienEmail.equals(email)) {
            candidates.addAll(propositionTheseRepository.findByDirecteurEmail(ancienEmail));
        }
        if (orcid != null) {
            candidates.addAll(propositionTheseRepository.findByDirecteurOrcid(orcid));
        }
        for (PropositionThese p : candidates) {
            if (p.getSource() == SourceThese.DOCTORAT_GOUV && p.getAxeExternalId() != null) {
                existantesParAxe.putIfAbsent(p.getAxeExternalId(), p);
            }
        }

        // 2. Upsert des axes contactables.
        List<String> axesActifs = new ArrayList<>();
        for (AxeDeRecherche axe : axes) {
            if (!axe.isContactable() || axe.getExternalId() == null || axe.getExternalId().isBlank()) {
                continue;
            }
            axesActifs.add(axe.getExternalId());
            String matricule = matriculeOffre(axe.getExternalId());
            PropositionThese offre = propositionTheseRepository.findByMatricule(matricule).orElse(null);
            boolean creation = (offre == null);
            if (creation) {
                offre = new PropositionThese();
                offre.setMatricule(matricule);
                offre.setAxeExternalId(axe.getExternalId());
                offre.setTypeProposition("offre");
                offre.setSource(SourceThese.DOCTORAT_GOUV);
                offre.setDateCreation(now);
                offre.setDateSoumission(now);
                offre.setDateMiseEnLigne(now);
                offre.setDateIntegration(now);
            }
            remplirOffreAccompagnement(offre, utilisateur, profil, axe, now);
            propositionTheseRepository.save(offre);
            if (creation) {
                log.info("Offre d'accompagnement {} créée pour le directeur {}", matricule, utilisateur.getId());
            }
        }

        // 3. Désactivation des offres dont l'axe n'est plus contactable.
        for (Map.Entry<String, PropositionThese> entry : existantesParAxe.entrySet()) {
            if (!axesActifs.contains(entry.getKey())) {
                PropositionThese offre = entry.getValue();
                if (!Boolean.FALSE.equals(offre.getActive())) {
                    offre.setActive(false);
                    offre.setDateMaj(now);
                    propositionTheseRepository.save(offre);
                    log.info("Offre d'accompagnement {} désactivée (axe non contactable) pour le directeur {}",
                        offre.getMatricule(), utilisateur.getId());
                }
            }
        }
    }

    /**
     * Matricule déterministe et stable par axe : "DG-" + 8 caractères hexadéraux
     * issus de l'UUID de l'axe (11 caractères, contrainte d'unicité en base).
     */
    private String matriculeOffre(String axeExternalId) {
        String hex = axeExternalId.replace("-", "");
        if (hex.length() < 8) {
            hex = (hex + "00000000").substring(0, 8);
        }
        return "DG-" + hex.substring(0, 8).toUpperCase();
    }

    /**
     * Recopie profil + axe vers l'offre d'accompagnement.
     * - titre axe -> titre de l'offre ; précisions axe -> résumé + thématique
     *   (champs couverts par la recherche plein texte et affichés sur la fiche) ;
     * - expertise en quelques mots + mots-clés du profil -> offre (recherche + fiche) ;
     * - direction/déposant = directeur (le contact candidat et le rattachement
     *   "Mes sujets" fonctionnent via l'e-mail/ORCID) ;
     * - pas de date limite -> exclue du flux EURAXESS (deadline obligatoire, règle R4).
     */
    private void remplirOffreAccompagnement(PropositionThese offre, Utilisateur utilisateur,
            ProfilDirecteurThese profil, AxeDeRecherche axe, LocalDateTime now) {
        String email = EmailUtils.normalize(utilisateur.getEmail());
        int annee = Year.now().getValue();

        offre.setActive(true);
        offre.setDateMaj(now);
        offre.setAnnee(annee);
        offre.setAnneeUniversitaire(String.valueOf(annee));

        offre.setTheseTitre(truncate(axe.getTitre() != null ? axe.getTitre().trim() : null, 600));
        offre.setResume(axe.getPrecisions());
        offre.setThematiqueRecherche(axe.getPrecisions());
        offre.setExpertiseMots(profil.getExpertiseMots());
        offre.setMotsCles(motsClesOffre(profil.getMotsCles()));

        offre.setDomaineScientifique(profil.getDomaineScientifique());
        offre.setEtablissementLibelle(truncate(profil.getEtablissement(), 250));
        offre.setUniteRechercheLibelle(truncate(profil.getLaboratoire(), 300));
        offre.setEcoleDoctoraleLibelle(truncate(profil.getEcoleDoctorale(), 250));

        offre.setDirectionTheseNom(truncate(utilisateur.getNom(), 50));
        offre.setDirectionThesePrenom(truncate(utilisateur.getPrenom(), 25));
        offre.setDirectionTheseEmail(email);
        offre.setDirectionTheseOrcid(truncate(normalizeOrcid(profil.getOrcid()), 20));

        offre.setDeposantNom(truncate(utilisateur.getNom(), 50));
        offre.setDeposantPrenom(truncate(utilisateur.getPrenom(), 25));
        offre.setDeposantEmail(email);
        offre.setDeposantOrcid(truncate(normalizeOrcid(profil.getOrcid()), 20));

        if (offre.getDateMiseEnLigne() == null) {
            offre.setDateMiseEnLigne(now);
        }
    }

    private Map<String, String> motsClesOffre(List<String> motsCles) {
        if (motsCles == null || motsCles.isEmpty()) {
            return null;
        }
        Map<String, String> map = new LinkedHashMap<>();
        int i = 0;
        for (String mot : motsCles) {
            if (mot != null && !mot.isBlank()) {
                map.put(String.valueOf(i++), mot.trim());
            }
        }
        return map.isEmpty() ? null : map;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() > maxLength ? value.substring(0, maxLength) : value;
    }

    public ProfilDtResponse addCompetence(String userId, String competence) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (utilisateur.getCompetences() == null) {
            utilisateur.setCompetences(new ArrayList<>());
        }
        if (!utilisateur.getCompetences().contains(competence)) {
            utilisateur.getCompetences().add(competence);
            utilisateur.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            log.info("Compétence '{}' ajoutée pour le directeur de thèse {}", competence, userId);
        }
        return toProfilDtResponse(utilisateur);
    }

    public ProfilDtResponse removeCompetence(String userId, String competence) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (utilisateur.getCompetences() != null) {
            utilisateur.getCompetences().remove(competence);
            utilisateur.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            log.info("Compétence '{}' supprimée pour le directeur de thèse {}", competence, userId);
        }
        return toProfilDtResponse(utilisateur);
    }

    /**
     * Retourne tous les sujets rattachés au directeur de thèse (actifs et inactifs).
     * Le rattachement se fait par comparaison de l'e-mail et/ou de l'ORCID du DT
     * avec les champs direction/codirection de la proposition de thèse.
     */
    public List<SujetDtResponse> getSujets(String userId) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(userId).orElse(null);
        String email = normalizeEmail(utilisateur.getEmail());
        String orcid = normalizeOrcid(profil != null ? profil.getOrcid() : null);

        List<SujetDtResponse> response = new ArrayList<>();
        for (PropositionThese p : sujets.values()) {
            response.add(toSujetDtResponse(p, email, orcid));
        }
        log.info("{} sujet(s) trouvé(s) pour le directeur de thèse {}", response.size(), userId);
        return response;
    }

    /**
     * Retourne les offres d'accompagnement créées par le directeur de thèse
     * (source DOCTORAT_GOUV, actives et inactives) depuis ses axes contactables.
     */
    public List<SujetDtResponse> getOffres(String userId) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(userId).orElse(null);
        String email = normalizeEmail(utilisateur.getEmail());
        String orcid = normalizeOrcid(profil != null ? profil.getOrcid() : null);

        List<SujetDtResponse> response = new ArrayList<>();
        for (PropositionThese p : sujets.values()) {
            if (p.getSource() == SourceThese.DOCTORAT_GOUV) {
                response.add(toSujetDtResponse(p, email, orcid));
            }
        }
        log.info("{} offre(s) d'accompagnement trouvée(s) pour le directeur de thèse {}", response.size(), userId);
        return response;
    }

    /**
     * Retourne les demandes de mise en relation envoyées ou acceptées
     * (statuts CREE et ACCEPTEE, non archivées) par des candidats
     * sur les sujets rattachés au directeur de thèse.
     */
    public List<DemandeDtResponse> getDemandes(String userId) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        if (sujets.isEmpty()) {
            return new ArrayList<>();
        }

        List<DemandeMiseEnRelation> demandes = demandeMiseEnRelationRepository
            .findByPropositionTheseIdInOrderByUpdatedAtDesc(new ArrayList<>(sujets.keySet()));

        List<String> candidatIds = demandes.stream()
            .map(DemandeMiseEnRelation::getCandidatId)
            .filter(id -> id != null)
            .distinct()
            .toList();
        Map<String, Utilisateur> candidats = new LinkedHashMap<>();
        if (!candidatIds.isEmpty()) {
            for (Utilisateur u : utilisateurRepository.findAllById(candidatIds)) {
                candidats.put(u.getId(), u);
            }
        }

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        List<DemandeDtResponse> response = new ArrayList<>();
        for (DemandeMiseEnRelation d : demandes) {
            if (d.getStatut() != StatutDemandeMiseEnRelation.CREE
                    && d.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE
                    && d.getStatut() != StatutDemandeMiseEnRelation.REFUSEE) {
                continue;
            }
            PropositionThese p = sujets.get(d.getPropositionTheseId());
            if (p == null) {
                continue;
            }
            Utilisateur candidat = candidats.get(d.getCandidatId());
            DemandeDtResponse dto = new DemandeDtResponse();
            dto.setId(d.getId());
            dto.setPropositionTheseId(d.getPropositionTheseId());
            dto.setTitreSujet(p.getTheseTitre());
            dto.setEtablissement(p.getEtablissementLibelle());
            if (candidat != null) {
                String nomComplet = ((candidat.getPrenom() != null ? candidat.getPrenom().trim() + " " : "")
                    + (candidat.getNom() != null ? candidat.getNom().trim() : "")).trim();
                dto.setCandidatNom(nomComplet.isEmpty() ? null : nomComplet);
                dto.setCandidatEmail(candidat.getEmail());
                dto.setCandidatPhotoUrl(ProfilService.urlPhoto(candidat));
            }
            dto.setDateDemande(d.getUpdatedAt() != null ? d.getUpdatedAt().format(dateTimeFormatter) : null);
            dto.setArchivee(Boolean.TRUE.equals(d.getArchivee())
                || d.getStatut() == StatutDemandeMiseEnRelation.REFUSEE);
            dto.setStatut(d.getStatut() != null ? d.getStatut().name() : null);
            dto.setNbMessagesNonLus((int) messageRepository
                .countByDemandeIdAndAuteurIdNotAndLuFalse(d.getId(), userId));
            response.add(dto);
        }
        log.info("{} demande(s) de mise en relation trouvée(s) pour le directeur de thèse {}",
            response.size(), userId);
        return response;
    }

    /**
     * Détail d'une demande de mise en relation, vérifié comme portant
     * sur un sujet rattaché au directeur de thèse.
     * Accessible aux statuts CREE et ACCEPTEE (non archivée).
     */
    public DemandeDtResponse getDemandeDetail(String userId, Long demandeId) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        DemandeMiseEnRelation d = demandeMiseEnRelationRepository.findById(demandeId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        PropositionThese p = sujets.get(d.getPropositionTheseId());
        if (p == null
            || (d.getStatut() != StatutDemandeMiseEnRelation.CREE
                && d.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE
                && d.getStatut() != StatutDemandeMiseEnRelation.REFUSEE)
            || Boolean.TRUE.equals(d.getArchivee())) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        Utilisateur candidat = utilisateurRepository.findById(d.getCandidatId()).orElse(null);
        DemandeDtResponse dto = new DemandeDtResponse();
        dto.setId(d.getId());
        dto.setPropositionTheseId(d.getPropositionTheseId());
        dto.setTitreSujet(p.getTheseTitre());
        dto.setEtablissement(p.getEtablissementLibelle());
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dto.setDateMiseEnLigne(p.getDateMiseEnLigne() != null ? p.getDateMiseEnLigne().format(dateTimeFormatter) : null);
        dto.setDateLimiteCandidature(p.getDateLimiteCandidature() != null ? p.getDateLimiteCandidature().format(dateTimeFormatter) : null);
        if (candidat != null) {
            String nomComplet = ((candidat.getPrenom() != null ? candidat.getPrenom().trim() + " " : "")
                + (candidat.getNom() != null ? candidat.getNom().trim() : "")).trim();
            dto.setCandidatNom(nomComplet.isEmpty() ? null : nomComplet);
            dto.setCandidatPrenom(candidat.getPrenom());
            dto.setCandidatEmail(candidat.getEmail());
            dto.setCandidatPhotoUrl(ProfilService.urlPhoto(candidat));
            ProfilCandidat profilCandidat = profilCandidatRepository.findByUtilisateurId(candidat.getId()).orElse(null);
            if (profilCandidat != null) {
                dto.setCandidatCivilite(profilCandidat.getCivilite());
                dto.setCandidatSituation(profilCandidat.getSituation());
                dto.setCandidatTelephone(profilCandidat.getTelephone());
                dto.setCandidatCvFilename(displayFilename(profilCandidat.getCvFilename()));
                dto.setCandidatCvSize(fileSize(profilCandidat.getCvFilename()));
                if (profilCandidat.getPiecesFilenames() != null) {
                    List<DemandeDtResponse.FichierCandidatDto> pieces = new ArrayList<>();
                    for (String piece : profilCandidat.getPiecesFilenames()) {
                        pieces.add(new DemandeDtResponse.FichierCandidatDto(displayFilename(piece), fileSize(piece)));
                    }
                    dto.setCandidatPieces(pieces);
                }
            }
        }
        dto.setDateDemande(d.getUpdatedAt() != null ? d.getUpdatedAt().format(dateTimeFormatter) : null);
        dto.setArchivee(Boolean.TRUE.equals(d.getArchivee()));
        dto.setDateAcceptation(d.getDateAcceptation() != null ? d.getDateAcceptation().format(dateTimeFormatter) : null);
        dto.setDateRefus(d.getDateRefus() != null ? d.getDateRefus().format(dateTimeFormatter) : null);
        dto.setExplicationRefus(d.getExplicationRefus());
        dto.setStatut(d.getStatut() != null ? d.getStatut().name() : null);
        dto.setMotivations(d.getMotivations());
        return dto;
    }

    /**
     * Accepte une demande de mise en relation : passage au statut ACCEPTEE,
     * enregistrement de la date d'acceptation et notification du candidat
     * (template Brevo 67, bouchonné hors production).
     */
    public DemandeDtResponse accepterDemande(String userId, Long demandeId) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        DemandeMiseEnRelation d = demandeMiseEnRelationRepository.findById(demandeId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        PropositionThese p = sujets.get(d.getPropositionTheseId());
        if (p == null || d.getStatut() != StatutDemandeMiseEnRelation.CREE
            || Boolean.TRUE.equals(d.getArchivee())) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        d.setStatut(StatutDemandeMiseEnRelation.ACCEPTEE);
        d.setDateAcceptation(LocalDateTime.now());
        d.setUpdatedAt(LocalDateTime.now());
        demandeMiseEnRelationRepository.save(d);
        log.info("Demande {} acceptée par le directeur de thèse {}", demandeId, userId);

        envoyerMailAcceptation(userId, d);
        return getDemandeDetail(userId, demandeId);
    }

    /**
     * Refuse une demande de mise en relation : passage au statut REFUSEE,
     * enregistrement de la date de refus et de l'explication transmise,
     * et notification du candidat (template Brevo 68, bouchonné hors production).
     */
    public DemandeDtResponse refuserDemande(String userId, Long demandeId, String explication) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        DemandeMiseEnRelation d = demandeMiseEnRelationRepository.findById(demandeId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        PropositionThese p = sujets.get(d.getPropositionTheseId());
        if (p == null || d.getStatut() != StatutDemandeMiseEnRelation.CREE
            || Boolean.TRUE.equals(d.getArchivee())) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        String texte = explication != null ? explication.trim() : "";
        if (texte.isEmpty()) {
            throw new IllegalArgumentException("L'explication du refus est obligatoire");
        }
        if (texte.length() > 2000) {
            texte = texte.substring(0, 2000);
        }
        d.setStatut(StatutDemandeMiseEnRelation.REFUSEE);
        d.setDateRefus(LocalDateTime.now());
        d.setExplicationRefus(texte);
        d.setArchivee(Boolean.TRUE);
        d.setUpdatedAt(LocalDateTime.now());
        demandeMiseEnRelationRepository.save(d);
        log.info("Demande {} refusée par le directeur de thèse {}", demandeId, userId);

        envoyerMailRefus(userId, d, texte);
        return getDemandeDetail(userId, demandeId);
    }

    private void envoyerMailRefus(String userId, DemandeMiseEnRelation d, String explication) {
        Utilisateur candidat = utilisateurRepository.findById(d.getCandidatId()).orElse(null);
        if (candidat == null) {
            log.warn("Candidat introuvable pour la demande {}, mail de refus non envoyé", d.getId());
            return;
        }
        Utilisateur directeur = utilisateurRepository.findById(userId).orElse(null);
        String nomDirecteur = directeur != null
            ? ((directeur.getPrenom() != null ? directeur.getPrenom().trim() + " " : "")
                + (directeur.getNom() != null ? directeur.getNom().trim() : "")).trim()
            : "";

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("prenom", candidat.getPrenom());
        params.put("nom_directeur", nomDirecteur);
        params.put("explication_refus", explication);
        params.put("lien_recherche", LIEN_RECHERCHE);

        ProfilCandidat profilCandidat = profilCandidatRepository.findByUtilisateurId(candidat.getId()).orElse(null);
        if (profilCandidat != null && profilCandidat.getCivilite() != null) {
            params.put("civilite", profilCandidat.getCivilite());
        }

        String destinataire = candidat.getEmail();
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV : mail de refus non envoyé (boîte bouchon non configurée) pour la demande {}",
                    d.getId());
                return;
            }
            destinataire = stubMailbox.trim();
            log.info("Mode DEV : mail de refus de la demande {} bouchonné vers {}", d.getId(), destinataire);
        }
        try {
            if (destinataire != null && !destinataire.isBlank()) {
                emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_REFUS, params);
                log.info("Mail de refus envoyé à {} pour la demande {}", destinataire, d.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail de refus pour la demande {}", d.getId(), e);
        }
    }

    /**
     * Outil hors production : fait revenir une demande acceptée à l'état CREE
     * (date d'acceptation effacée). Refusé en production.
     */
    public DemandeDtResponse reinitialiserDemande(String userId, Long demandeId) {
        if (environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("Réinitialisation interdite en production");
        }
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        DemandeMiseEnRelation d = demandeMiseEnRelationRepository.findById(demandeId)
            .orElseThrow(() -> new IllegalArgumentException("Demande introuvable"));
        PropositionThese p = sujets.get(d.getPropositionTheseId());
        if (p == null
            || (d.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE
                && d.getStatut() != StatutDemandeMiseEnRelation.REFUSEE)
            || Boolean.TRUE.equals(d.getArchivee())) {
            throw new IllegalArgumentException("Demande introuvable");
        }
        d.setStatut(StatutDemandeMiseEnRelation.CREE);
        d.setDateAcceptation(null);
        d.setDateRefus(null);
        d.setExplicationRefus(null);
        d.setArchivee(Boolean.FALSE);
        d.setUpdatedAt(LocalDateTime.now());
        demandeMiseEnRelationRepository.save(d);
        log.info("Demande {} réinitialisée à l'état CREE par le directeur de thèse {}", demandeId, userId);
        return getDemandeDetail(userId, demandeId);
    }

    private void envoyerMailAcceptation(String userId, DemandeMiseEnRelation d) {
        Utilisateur candidat = utilisateurRepository.findById(d.getCandidatId()).orElse(null);
        if (candidat == null) {
            log.warn("Candidat introuvable pour la demande {}, mail d'acceptation non envoyé", d.getId());
            return;
        }
        Utilisateur directeur = utilisateurRepository.findById(userId).orElse(null);
        String nomDirecteur = directeur != null
            ? ((directeur.getPrenom() != null ? directeur.getPrenom().trim() + " " : "")
                + (directeur.getNom() != null ? directeur.getNom().trim() : "")).trim()
            : "";

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("prenom", candidat.getPrenom());
        params.put("nom_directeur", nomDirecteur);
        params.put("lien_conversation", LIEN_CONVERSATION);

        ProfilCandidat profilCandidat = profilCandidatRepository.findByUtilisateurId(candidat.getId()).orElse(null);
        if (profilCandidat != null && profilCandidat.getCivilite() != null) {
            params.put("civilite", profilCandidat.getCivilite());
        }

        String destinataire = candidat.getEmail();
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV : mail d'acceptation non envoyé (boîte bouchon non configurée) pour la demande {}",
                    d.getId());
                return;
            }
            destinataire = stubMailbox.trim();
            log.info("Mode DEV : mail d'acceptation de la demande {} bouchonné vers {}", d.getId(), destinataire);
        }
        try {
            if (destinataire != null && !destinataire.isBlank()) {
                emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_ACCEPTATION, params);
                log.info("Mail d'acceptation envoyé à {} pour la demande {}", destinataire, d.getId());
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail d'acceptation pour la demande {}", d.getId(), e);
        }
    }

    private Long fileSize(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        try {
            if (storage.exists(key)) {
                return storage.size(key);
            }
        } catch (Exception e) {
            log.debug("Impossible de calculer la taille du fichier {}", key);
        }
        return null;
    }

    /**
     * Clé objet du CV ou d'une pièce du candidat d'une demande,
     * après vérification que la demande appartient bien au DT.
     */
    public String getCandidatFichier(String userId, Long demandeId, String type, Integer index) {
        Map<Long, PropositionThese> sujets = findSujetsRattaches(userId);
        DemandeMiseEnRelation d = demandeMiseEnRelationRepository.findById(demandeId)
            .orElseThrow(() -> new IllegalArgumentException("Fichier introuvable"));
        PropositionThese p = sujets.get(d.getPropositionTheseId());
        if (p == null
            || (d.getStatut() != StatutDemandeMiseEnRelation.CREE
                && d.getStatut() != StatutDemandeMiseEnRelation.ACCEPTEE)
            || Boolean.TRUE.equals(d.getArchivee())) {
            throw new IllegalArgumentException("Fichier introuvable");
        }
        ProfilCandidat profilCandidat = profilCandidatRepository.findByUtilisateurId(d.getCandidatId()).orElse(null);
        if (profilCandidat == null) {
            throw new IllegalArgumentException("Fichier introuvable");
        }
        String path;
        if ("cv".equalsIgnoreCase(type)) {
            path = profilCandidat.getCvFilename();
        } else if ("piece".equalsIgnoreCase(type) && index != null
            && profilCandidat.getPiecesFilenames() != null
            && index >= 0 && index < profilCandidat.getPiecesFilenames().size()) {
            path = profilCandidat.getPiecesFilenames().get(index);
        } else {
            throw new IllegalArgumentException("Fichier introuvable");
        }
        boolean present = false;
        try {
            present = path != null && !path.isBlank() && storage.exists(path);
        } catch (Exception e) {
            present = false;
        }
        if (!present) {
            throw new IllegalArgumentException("Fichier introuvable");
        }
        return path;
    }

    /**
     * Nom affichable d'un fichier (sans clé/chemin ni préfixe technique
     * horodatage historique ou UUID).
     */
    public String displayFilename(String key) {
        return StoragePort.displayName(key);
    }

    /**
     * Sujets rattachés au DT par comparaison de son e-mail et/ou ORCID
     * avec les champs direction/codirection des propositions.
     */
    private Map<Long, PropositionThese> findSujetsRattaches(String userId) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(userId).orElse(null);

        String email = normalizeEmail(utilisateur.getEmail());
        String orcid = normalizeOrcid(profil != null ? profil.getOrcid() : null);

        Map<Long, PropositionThese> sujets = new LinkedHashMap<>();
        if (email != null) {
            List<PropositionThese> parEmail = propositionTheseRepository.findByDirecteurEmail(email);
            if (parEmail != null) {
                for (PropositionThese p : parEmail) {
                    if (p.getId() != null) {
                        sujets.putIfAbsent(p.getId(), p);
                    }
                }
            }
        }
        if (orcid != null) {
            List<PropositionThese> parOrcid = propositionTheseRepository.findByDirecteurOrcid(orcid);
            if (parOrcid != null) {
                for (PropositionThese p : parOrcid) {
                    if (p.getId() != null) {
                        sujets.putIfAbsent(p.getId(), p);
                    }
                }
            }
        }
        return sujets;
    }

    public void changerMotDePasse(String userId, ChangementMotDePasseRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (!passwordEncoder.matches(request.getMotDePasseActuel(), utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("Le mot de passe actuel est incorrect");
        }

        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            throw new IllegalArgumentException("Les mots de passe ne correspondent pas");
        }

        utilisateur.setMotDePasse(passwordEncoder.encode(request.getNouveauMotDePasse()));
        utilisateur.setDateModification(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);
        log.info("Mot de passe modifié pour le directeur de thèse {}", userId);
    }

    private SujetDtResponse toSujetDtResponse(PropositionThese p, String email, String orcid) {
        SujetDtResponse dto = new SujetDtResponse();
        dto.setId(p.getId());
        dto.setMatricule(p.getMatricule());
        dto.setTitre(p.getTheseTitre());
        dto.setEtablissement(p.getEtablissementLibelle());
        dto.setEcoleDoctorale(p.getEcoleDoctoraleLibelle());
        dto.setLaboratoire(p.getUniteRechercheLibelle());
        dto.setActive(p.getActive());
        dto.setRole(isDirectionMatch(p, email, orcid) ? "DIRECTION" : "CODIRECTION");
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        dto.setDateMiseEnLigne(p.getDateMiseEnLigne() != null ? p.getDateMiseEnLigne().format(dateTimeFormatter) : null);
        dto.setDateLimiteCandidature(p.getDateLimiteCandidature() != null ? p.getDateLimiteCandidature().format(dateTimeFormatter) : null);
        return dto;
    }

    private boolean isDirectionMatch(PropositionThese p, String email, String orcid) {
        if (email != null
            && (email.equals(normalizeEmail(p.getDirectionTheseEmail())))) {
            return true;
        }
        return orcid != null && orcid.equals(normalizeOrcid(p.getDirectionTheseOrcid()));
    }

    private String normalizeEmail(String email) {
        return EmailUtils.normalize(email);
    }

    private String normalizeOrcid(String orcid) {
        if (orcid == null) {
            return null;
        }
        String normalized = orcid.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private ProfilDtResponse toProfilDtResponse(Utilisateur u) {
        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(u.getId()).orElse(null);

        ProfilDtResponse response = new ProfilDtResponse(
            profil != null ? profil.getCivilite() : null,
            u.getNom(),
            u.getPrenom(),
            u.getEmail()
        );
        response.setPhotoUrl(ProfilService.urlPhoto(u));
        response.setCompetences(u.getCompetences() != null ? new ArrayList<>(u.getCompetences()) : new ArrayList<>());

        // Pré-remplissage depuis les métadonnées du sujet invitant (ADUM/Amethis) : uniquement si
        // le profil n'existe pas encore. Les champs effectivement remplis sont verrouillés
        // côté front (champsVerrouilles) tant que le profil n'a pas été sauvegardé.
        boolean profilNouveau = (profil == null);
        Map<Long, PropositionThese> sujets = profilNouveau ? findSujetsRattaches(u.getId()) : new LinkedHashMap<>();
        List<String> champsVerrouilles = new ArrayList<>();

        String etablissement = profil != null ? profil.getEtablissement() : firstNonBlank(sujets, PropositionThese::getEtablissementLibelle);
        String ecoleDoctorale = profil != null ? profil.getEcoleDoctorale() : firstNonBlank(sujets, PropositionThese::getEcoleDoctoraleLibelle);
        String laboratoire = profil != null ? profil.getLaboratoire() : firstNonBlank(sujets, PropositionThese::getUniteRechercheLibelle);
        String domaineScientifique = profil != null ? profil.getDomaineScientifique() : firstNonBlank(sujets, PropositionThese::getDomaineScientifique);

        response.setOrcid(profil != null ? profil.getOrcid() : null);
        response.setEtablissement(etablissement);
        response.setLaboratoire(laboratoire);
        response.setEcoleDoctorale(ecoleDoctorale);
        response.setEmployeur(profil != null ? profil.getEmployeur() : null);
        response.setTitre(profil != null ? profil.getTitre() : null);
        response.setPrecisionTitre(profil != null ? profil.getPrecisionTitre() : null);
        response.setHabilitationRecherche(profil != null ? profil.getHabilitationRecherche() : null);
        response.setDomaineScientifique(domaineScientifique);
        response.setExpertiseMots(profil != null ? profil.getExpertiseMots() : null);
        response.setMotsCles(profil != null && profil.getMotsCles() != null ? new ArrayList<>(profil.getMotsCles()) : new ArrayList<>());
        response.setDescriptionAxes(profil != null ? profil.getDescriptionAxes() : null);
        if (profil != null && profil.getAxes() != null) {
            List<AxeDeRecherche> axesCopy = new ArrayList<>();
            for (AxeDeRecherche a : profil.getAxes()) {
                AxeDeRecherche copy = new AxeDeRecherche();
                copy.setTitre(a.getTitre());
                copy.setPrecisions(a.getPrecisions());
                copy.setContactable(a.isContactable());
                copy.setExternalId(a.getExternalId());
                axesCopy.add(copy);
            }
            response.setAxes(axesCopy);
        } else {
            response.setAxes(new ArrayList<>());
        }

        // Verrouillage transitoire : uniquement les champs effectivement pré-remplis par ADUM,
        // et uniquement tant que le profil n'a pas été sauvegardé (profil == null).
        if (profilNouveau) {
            if (etablissement != null && !etablissement.isBlank()) champsVerrouilles.add("etablissement");
            if (ecoleDoctorale != null && !ecoleDoctorale.isBlank()) champsVerrouilles.add("ecoleDoctorale");
            if (laboratoire != null && !laboratoire.isBlank()) champsVerrouilles.add("laboratoire");
            // domaineScientifique est pré-rempli mais reste éditable (pas verrouillé)
        }
        response.setChampsVerrouilles(champsVerrouilles);

        return response;
    }

    private static String firstNonBlank(Map<Long, PropositionThese> sujets,
                                       java.util.function.Function<PropositionThese, String> getter) {
        if (sujets == null) return null;
        for (PropositionThese p : sujets.values()) {
            String v = getter.apply(p);
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
