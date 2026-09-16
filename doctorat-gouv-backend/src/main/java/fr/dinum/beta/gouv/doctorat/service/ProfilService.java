package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilUpdateRequest;
import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class ProfilService {

    private static final Logger log = LoggerFactory.getLogger(ProfilService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfilService(UtilisateurRepository utilisateurRepository,
                         ProfilCandidatRepository profilCandidatRepository,
                         PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Optional<ProfilResponse> getProfil(String userId) {
        return utilisateurRepository.findById(userId).map(this::toProfilResponse);
    }

    public ProfilResponse updateProfil(String userId, ProfilUpdateRequest request) {
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

        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(userId)
            .orElse(new ProfilCandidat());
        if (profil.getUtilisateur() == null) {
            profil.setUtilisateur(utilisateur);
        }
        profil.setCivilite(request.getCivilite());
        profil.setSituation(request.getSituation());
        profil.setTelephone(request.getTelephone());
        profil.setOrcid(request.getOrcid());
        profil.setEtablissement(request.getEtablissement());
        profil.setLaboratoire(request.getLaboratoire());
        profilCandidatRepository.save(profil);

        log.info("Profil mis à jour pour l'utilisateur {}", userId);
        return toProfilResponse(utilisateur);
    }

    public ProfilResponse addCompetence(String userId, String competence) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (utilisateur.getCompetences() == null) {
            utilisateur.setCompetences(new ArrayList<>());
        }
        if (!utilisateur.getCompetences().contains(competence)) {
            utilisateur.getCompetences().add(competence);
            utilisateur.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            log.info("Compétence '{}' ajoutée pour l'utilisateur {}", competence, userId);
        }
        return toProfilResponse(utilisateur);
    }

    public ProfilResponse removeCompetence(String userId, String competence) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        if (utilisateur.getCompetences() != null) {
            utilisateur.getCompetences().remove(competence);
            utilisateur.setDateModification(LocalDateTime.now());
            utilisateurRepository.save(utilisateur);
            log.info("Compétence '{}' supprimée pour l'utilisateur {}", competence, userId);
        }
        return toProfilResponse(utilisateur);
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
        log.info("Mot de passe modifié pour l'utilisateur {}", userId);
    }

    private ProfilResponse toProfilResponse(Utilisateur u) {
        ProfilCandidat profil = profilCandidatRepository.findByUtilisateurId(u.getId()).orElse(null);

        ProfilResponse response = new ProfilResponse(
            profil != null ? profil.getCivilite() : null,
            u.getNom(),
            u.getPrenom(),
            profil != null ? profil.getSituation() : null,
            u.getEmail(),
            profil != null ? profil.getTelephone() : null
        );
        response.setPhotoUrl(u.getPhotoUrl());
        response.setCompetences(u.getCompetences() != null ? new ArrayList<>(u.getCompetences()) : new ArrayList<>());
        response.setNbCandidatures(u.getNbCandidatures() != null ? u.getNbCandidatures() : 0);
        response.setOrcid(profil != null ? profil.getOrcid() : null);
        response.setEtablissement(profil != null ? profil.getEtablissement() : null);
        response.setLaboratoire(profil != null ? profil.getLaboratoire() : null);
        return response;
    }
}
