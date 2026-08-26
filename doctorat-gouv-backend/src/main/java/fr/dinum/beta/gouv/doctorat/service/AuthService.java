package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.config.JwtConfig;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionRequest;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionCompletRequest;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionRequest;
import fr.dinum.beta.gouv.doctorat.dto.UtilisateurDto;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final InscriptionFileService inscriptionFileService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       PasswordEncoder passwordEncoder,
                       JwtConfig jwtConfig,
                       InscriptionFileService inscriptionFileService) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.inscriptionFileService = inscriptionFileService;
    }

    public ConnexionResponse inscrire(InscriptionRequest request) {
        log.info("Inscription de {}", request.getEmail());

        if (utilisateurRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(request.getEmail());
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setNom(request.getNom());
        utilisateur.setRole(request.getRole());
        utilisateur.setSourceAuth(SourceAuth.MANUEL);
        utilisateur.setActif(true);
        utilisateur.setDateCreation(LocalDateTime.now());

        utilisateur = utilisateurRepository.save(utilisateur);

        String token = genererToken(utilisateur);
        UtilisateurDto dto = toDto(utilisateur);
        return new ConnexionResponse(dto, token, jwtConfig.getExpirationMs());
    }

    /**
     * Inscription complète (multipart) : crée le compte via {@link #inscrire(InscriptionRequest)}
     * puis stocke les coordonnées supplémentaires et les fichiers uploadés (CV + pièces).
     * Retourne un ConnexionResponse (token) pour auto-connecter l'utilisateur.
     */
    public ConnexionResponse inscrireComplet(InscriptionCompletRequest request,
                                             MultipartFile cv,
                                             List<MultipartFile> pieces) {
        log.info("Inscription complète de {}", request.getEmail());

        InscriptionRequest base = new InscriptionRequest();
        base.setEmail(request.getEmail());
        base.setMotDePasse(request.getMotDePasse() != null && !request.getMotDePasse().isBlank()
                ? request.getMotDePasse()
                : genererMotDePasseTemporaire());
        base.setPrenom(request.getPrenom());
        base.setNom(request.getNom());
        base.setRole(RoleUtilisateur.CANDIDAT);

        ConnexionResponse response = inscrire(base);
        String userId = response.getUtilisateur().getId();

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après inscription"));

        utilisateur.setDemarche(request.getDemarche());
        utilisateur.setCivilite(request.getCivilite());
        utilisateur.setSituation(request.getSituation());
        utilisateur.setTelephone(request.getTelephone());
        utilisateur.setMasterConfirme(request.getMasterConfirme());

        String cvPath = inscriptionFileService.storeCv(userId, cv);
        utilisateur.setCvFilename(cvPath);
        List<String> piecePaths = inscriptionFileService.storePieces(userId, pieces);
        utilisateur.setPiecesFilenames(piecePaths);

        utilisateurRepository.save(utilisateur);

        return response;
    }

    private String genererMotDePasseTemporaire() {
        return UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
    }

    public ConnexionResponse connecter(ConnexionRequest request) {
        log.info("Connexion de {}", request.getEmail());

        Utilisateur utilisateur = utilisateurRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasse())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        String token = genererToken(utilisateur);
        UtilisateurDto dto = toDto(utilisateur);

        return new ConnexionResponse(dto, token, jwtConfig.getExpirationMs());
    }

    public UtilisateurDto getUtilisateur(String id) {
        log.info("Récupération utilisateur {}", id);

        Utilisateur utilisateur = utilisateurRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        return toDto(utilisateur);
    }

    public String genererToken(Utilisateur utilisateur) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtConfig.getExpirationMs());

        return Jwts.builder()
                .subject(utilisateur.getId())
                .claim("email", utilisateur.getEmail())
                .claim("role", utilisateur.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                .compact();
    }

    public Claims parserToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private UtilisateurDto toDto(Utilisateur u) {
        UtilisateurDto dto = new UtilisateurDto();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setPrenom(u.getPrenom());
        dto.setNom(u.getNom());
        dto.setRole(u.getRole());
        dto.setSourceAuth(u.getSourceAuth());
        dto.setActif(u.getActif());
        dto.setDateCreation(u.getDateCreation());
        return dto;
    }
}
