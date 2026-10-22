package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.config.JwtConfig;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionRequest;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionCompletRequest;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionRequest;
import fr.dinum.beta.gouv.doctorat.dto.UtilisateurDto;
import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final int BREVO_TEMPLATE_CONFIRMATION_CANDIDAT = 61;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    private String stubMailbox;

    private final UtilisateurRepository utilisateurRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final InscriptionFileService inscriptionFileService;
    private final BrevoEmailService emailService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       ProfilCandidatRepository profilCandidatRepository,
                       PasswordEncoder passwordEncoder,
                       JwtConfig jwtConfig,
                       InscriptionFileService inscriptionFileService,
                       BrevoEmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.inscriptionFileService = inscriptionFileService;
        this.emailService = emailService;
    }

    public ConnexionResponse inscrire(InscriptionRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        log.info("Inscription de {}", email);

        if (utilisateurRepository.existsByEmailIgnoreCaseAndTrim(email)) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet email");
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setNom(request.getNom());
        utilisateur.setRole(request.getRole());
        utilisateur.setSourceAuth(SourceAuth.MANUEL);
        utilisateur.setActif(true);
        utilisateur.setDateCreation(LocalDateTime.now());

        utilisateur = utilisateurRepository.save(utilisateur);

        envoyerMailConfirmationCandidat(utilisateur);

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
        base.setRole(request.getRole() != null ? request.getRole() : RoleUtilisateur.CANDIDAT);

        ConnexionResponse response = inscrire(base);
        String userId = response.getUtilisateur().getId();

        Utilisateur utilisateur = utilisateurRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Utilisateur introuvable après inscription"));

        utilisateur.setDemarche(request.getDemarche());

        ProfilCandidat profilCandidat = new ProfilCandidat();
        profilCandidat.setUtilisateur(utilisateur);
        profilCandidat.setCivilite(request.getCivilite());
        profilCandidat.setSituation(request.getSituation());
        profilCandidat.setTelephone(request.getTelephone());
        profilCandidat.setMasterConfirme(Boolean.TRUE.equals(request.getMasterConfirme()));

        if (cv != null && !cv.isEmpty()) {
            String cvPath = inscriptionFileService.storeCv(userId, cv);
            profilCandidat.setCvFilename(cvPath);
        }
        List<String> piecePaths = inscriptionFileService.storePieces(userId, pieces);
        profilCandidat.setPiecesFilenames(piecePaths);

        profilCandidatRepository.save(profilCandidat);

        return response;
    }

    private String genererMotDePasseTemporaire() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Envoie le mail de confirmation de profil au candidat (template Brevo 61 :
     * {@code {{ params.prenom }} {{ params.nom }}}).
     * L'échec d'envoi ne bloque jamais l'inscription.
     */
    private void envoyerMailConfirmationCandidat(Utilisateur utilisateur) {
        if (utilisateur.getRole() != RoleUtilisateur.CANDIDAT) {
            return;
        }
        String destinataire = utilisateur.getEmail();
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV : mail de confirmation d'inscription non envoyé pour {}", destinataire);
                return;
            }
            destinataire = stubMailbox.trim();
            log.info("Mode DEV : mail de confirmation d'inscription bouchonné vers {}", destinataire);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("prenom", utilisateur.getPrenom());
        params.put("nom", utilisateur.getNom());
        try {
            if (destinataire != null && !destinataire.isBlank()) {
                emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_CONFIRMATION_CANDIDAT, params);
                log.info("Mail de confirmation d'inscription envoyé à {}", destinataire);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail de confirmation d'inscription à {}", destinataire, e);
        }
    }

    public ConnexionResponse connecter(ConnexionRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        log.info("Connexion de {}", email);

        Utilisateur utilisateur = utilisateurRepository.findByEmailIgnoreCaseAndTrim(email)
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
