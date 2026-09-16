package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtUpdateRequest;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class DirecteurTheseService {

    private static final Logger log = LoggerFactory.getLogger(DirecteurTheseService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilDirecteurTheseRepository profilDtRepository;
    private final PasswordEncoder passwordEncoder;

    public DirecteurTheseService(UtilisateurRepository utilisateurRepository,
                                  ProfilDirecteurTheseRepository profilDtRepository,
                                  PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilDtRepository = profilDtRepository;
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
        return response;
    }
}
