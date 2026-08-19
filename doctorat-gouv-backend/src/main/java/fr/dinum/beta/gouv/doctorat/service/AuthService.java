package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.ConnexionRequest;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionRequest;
import fr.dinum.beta.gouv.doctorat.dto.UtilisateurDto;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    /**
     * Inscrit un nouvel utilisateur.
     * TODO: remplacer par la vraie implémentation (hash BCrypt, vérification unicité email)
     */
    public UtilisateurDto inscrire(InscriptionRequest request) {
        log.info("AuthService.bouchonné - inscription de {}", request.getEmail());

        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmail(request.getEmail());
        dto.setPrenom(request.getPrenom());
        dto.setNom(request.getNom());
        dto.setRole(request.getRole());
        dto.setSourceAuth(SourceAuth.MANUEL);
        dto.setActif(true);
        dto.setDateCreation(LocalDateTime.now());
        return dto;
    }

    /**
     * Authentifie un utilisateur.
     * TODO: remplacer par la vraie implémentation (vérification credentials, génération JWT)
     */
    public ConnexionResponse connecter(ConnexionRequest request) {
        log.info("AuthService.bouchonné - connexion de {}", request.getEmail());

        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(UUID.randomUUID().toString());
        dto.setEmail(request.getEmail());
        dto.setPrenom("Prénom");
        dto.setNom("Nom");
        dto.setRole(RoleUtilisateur.CANDIDAT);
        dto.setSourceAuth(SourceAuth.MANUEL);
        dto.setActif(true);
        dto.setDateCreation(LocalDateTime.now());

        return new ConnexionResponse(dto);
    }

    /**
     * Récupère un utilisateur par son ID.
     * TODO: remplacer par la vraie implémentation (requête BDD)
     */
    public UtilisateurDto getUtilisateur(String id) {
        log.info("AuthService.bouchonné - getUtilisateur {}", id);

        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(id);
        dto.setEmail("utilisateur@exemple.fr");
        dto.setPrenom("Prénom");
        dto.setNom("Nom");
        dto.setRole(RoleUtilisateur.CANDIDAT);
        dto.setSourceAuth(SourceAuth.MANUEL);
        dto.setActif(true);
        dto.setDateCreation(LocalDateTime.now());
        return dto;
    }
}
