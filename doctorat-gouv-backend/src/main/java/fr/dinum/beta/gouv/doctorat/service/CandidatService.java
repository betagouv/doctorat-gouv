package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilUpdateRequest;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Service
public class CandidatService {

    private static final Logger log = LoggerFactory.getLogger(CandidatService.class);

    private final UtilisateurRepository utilisateurRepository;

    public CandidatService(UtilisateurRepository utilisateurRepository) {
        this.utilisateurRepository = utilisateurRepository;
    }

    public Optional<ProfilResponse> getProfil(String userId) {
        return utilisateurRepository.findById(userId).map(this::toProfilResponse);
    }

    public ProfilResponse updateProfil(String userId, ProfilUpdateRequest request) {
        Utilisateur utilisateur = utilisateurRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        utilisateur.setCivilite(request.getCivilite());
        utilisateur.setNom(request.getNom());
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setSituation(request.getSituation());
        utilisateur.setEmail(request.getEmail());
        utilisateur.setTelephone(request.getTelephone());
        if (request.getCompetences() != null) {
            utilisateur.setCompetences(new ArrayList<>(request.getCompetences()));
        }
        utilisateur.setDateModification(LocalDateTime.now());

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        log.info("Profil mis à jour pour l'utilisateur {}", userId);
        return toProfilResponse(saved);
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

    private ProfilResponse toProfilResponse(Utilisateur u) {
        ProfilResponse response = new ProfilResponse(
            u.getCivilite(),
            u.getNom(),
            u.getPrenom(),
            u.getSituation(),
            u.getEmail(),
            u.getTelephone()
        );
        response.setPhotoUrl(u.getPhotoUrl());
        response.setCompetences(u.getCompetences() != null ? new ArrayList<>(u.getCompetences()) : new ArrayList<>());
        response.setNbCandidatures(u.getNbCandidatures() != null ? u.getNbCandidatures() : 0);
        return response;
    }
}
