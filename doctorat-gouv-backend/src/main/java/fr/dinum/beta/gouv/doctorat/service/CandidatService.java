package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
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
        utilisateur.setDateModification(LocalDateTime.now());

        Utilisateur saved = utilisateurRepository.save(utilisateur);
        log.info("Profil mis à jour pour l'utilisateur {}", userId);
        return toProfilResponse(saved);
    }

    private ProfilResponse toProfilResponse(Utilisateur u) {
        return new ProfilResponse(
            u.getCivilite(),
            u.getNom(),
            u.getPrenom(),
            u.getSituation(),
            u.getEmail(),
            u.getTelephone()
        );
    }
}
