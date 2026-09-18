package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtUpdateRequest;
import fr.dinum.beta.gouv.doctorat.dto.SujetDtResponse;
import fr.dinum.beta.gouv.doctorat.entity.AxeDeRecherche;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class DirecteurTheseService {

    private static final Logger log = LoggerFactory.getLogger(DirecteurTheseService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final PropositionTheseRepository propositionTheseRepository;
    private final PasswordEncoder passwordEncoder;

    public DirecteurTheseService(UtilisateurRepository utilisateurRepository,
                                  ProfilDirecteurTheseRepository profilDtRepository,
                                  PropositionTheseRepository propositionTheseRepository,
                                  PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilDtRepository = profilDtRepository;
        this.propositionTheseRepository = propositionTheseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<ProfilDtResponse> getProfil(String userId) {
        return utilisateurRepository.findById(userId).map(this::toProfilDtResponse);
    }

    public ProfilDtResponse updateProfil(String userId, ProfilDtUpdateRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setEmail(request.getEmail());
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
                axes.add(axe);
            }
            profil.setAxes(axes);
        }
        profilDtRepository.save(profil);

        log.info("Profil directeur de thèse mis à jour pour l'utilisateur {}", userId);
        return toProfilDtResponse(utilisateur);
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
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));
        ProfilDirecteurThese profil = profilDtRepository.findByUtilisateurId(userId).orElse(null);

        String email = normalizeEmail(utilisateur.getEmail());
        String orcid = normalizeOrcid(profil != null ? profil.getOrcid() : null);

        Map<Long, PropositionThese> sujets = new LinkedHashMap<>();
        if (email != null) {
            for (PropositionThese p : propositionTheseRepository.findByDirecteurEmail(email)) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }
        if (orcid != null) {
            for (PropositionThese p : propositionTheseRepository.findByDirecteurOrcid(orcid)) {
                if (p.getId() != null) {
                    sujets.putIfAbsent(p.getId(), p);
                }
            }
        }

        List<SujetDtResponse> response = new ArrayList<>();
        for (PropositionThese p : sujets.values()) {
            response.add(toSujetDtResponse(p, email, orcid));
        }
        log.info("{} sujet(s) trouvé(s) pour le directeur de thèse {}", response.size(), userId);
        return response;
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
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
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
        response.setPhotoUrl(u.getPhotoUrl());
        response.setCompetences(u.getCompetences() != null ? new ArrayList<>(u.getCompetences()) : new ArrayList<>());
        response.setOrcid(profil != null ? profil.getOrcid() : null);
        response.setEtablissement(profil != null ? profil.getEtablissement() : null);
        response.setLaboratoire(profil != null ? profil.getLaboratoire() : null);
        response.setEcoleDoctorale(profil != null ? profil.getEcoleDoctorale() : null);
        response.setEmployeur(profil != null ? profil.getEmployeur() : null);
        response.setTitre(profil != null ? profil.getTitre() : null);
        response.setPrecisionTitre(profil != null ? profil.getPrecisionTitre() : null);
        response.setHabilitationRecherche(profil != null ? profil.getHabilitationRecherche() : null);
        response.setDomaineScientifique(profil != null ? profil.getDomaineScientifique() : null);
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
                axesCopy.add(copy);
            }
            response.setAxes(axesCopy);
        } else {
            response.setAxes(new ArrayList<>());
        }
        return response;
    }
}
