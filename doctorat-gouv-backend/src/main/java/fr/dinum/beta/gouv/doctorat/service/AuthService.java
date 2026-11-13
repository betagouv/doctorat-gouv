package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import fr.dinum.beta.gouv.doctorat.repository.InvitationDirecteurTheseRepository;
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

    private static final String MESSAGE_DT_NON_INVITE =
        "Actuellement, l'inscription et la connexion à la plateforme pour les directeurs de thèse " +
        "ne sont ouvertes que sur invitation. Si vous êtes concerné, utilisez le lien reçu par e-mail ; " +
        "sinon, contactez la plateforme.";

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    @Value("${app.mail.stub-mailbox:}")
    private String stubMailbox;

    @Value("${app.frontend.base-url:https://app.doctorat.gouv.fr}")
    private String frontendBaseUrl;

    @Value("${app.email.verification.dev-mode:false}")
    private boolean emailVerificationDevMode;

    private final UtilisateurRepository utilisateurRepository;
    private final InvitationDirecteurTheseRepository invitationRepository;
    private final ProfilCandidatRepository profilCandidatRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;
    private final InscriptionFileService inscriptionFileService;
    private final BrevoEmailService emailService;

    public AuthService(UtilisateurRepository utilisateurRepository,
                       InvitationDirecteurTheseRepository invitationRepository,
                       ProfilCandidatRepository profilCandidatRepository,
                       PasswordEncoder passwordEncoder,
                       JwtConfig jwtConfig,
                       InscriptionFileService inscriptionFileService,
                       BrevoEmailService emailService) {
        this.utilisateurRepository = utilisateurRepository;
        this.invitationRepository = invitationRepository;
        this.profilCandidatRepository = profilCandidatRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
        this.inscriptionFileService = inscriptionFileService;
        this.emailService = emailService;
    }

    private static final int BREVO_TEMPLATE_ACTIVATION = 76;

    public ConnexionResponse inscrire(InscriptionRequest request) {
        String email = EmailUtils.normalize(request.getEmail());
        log.info("Inscription de {}", email);

        if (request.getRole() == RoleUtilisateur.DIRECTEUR_THESE
                && !invitationRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(MESSAGE_DT_NON_INVITE);
        }

        if (utilisateurRepository.existsByEmailIgnoreCaseAndTrim(email)) {
            // Réponse neutre (V5) : on ne révèle pas si l'email existe déjà
            log.info("Inscription tentée avec un email déjà existant : {}", email);
            UtilisateurDto dto = new UtilisateurDto();
            dto.setEmail(email);
            return new ConnexionResponse(dto, null, 0);
        }

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        utilisateur.setMotDePasse(passwordEncoder.encode(request.getMotDePasse()));
        utilisateur.setPrenom(request.getPrenom());
        utilisateur.setNom(request.getNom());
        utilisateur.setRole(request.getRole());
        utilisateur.setSourceAuth(SourceAuth.MANUEL);
        utilisateur.setActif(false);
        utilisateur.setEmailVerified(false);
        utilisateur.setDateCreation(LocalDateTime.now());

        // Token d'activation (usage unique, durée 24 h)
        String activationToken = UUID.randomUUID().toString();
        utilisateur.setEmailVerificationToken(activationToken);
        utilisateur.setEmailVerificationExpiresAt(LocalDateTime.now().plusHours(24));

        utilisateur = utilisateurRepository.save(utilisateur);

        envoyerMailActivation(utilisateur, activationToken);

        // Pas de JWT : le compte doit être activé avant connexion
        UtilisateurDto dto = toDto(utilisateur);
        return new ConnexionResponse(dto, null, 0);
    }

    /**
     * Active un compte à partir du token d'activation reçu par e-mail.
     * Retourne un JWT si l'activation réussit.
     */
    public ConnexionResponse activerCompte(String token) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token d'activation invalide"));

        if (utilisateur.getEmailVerificationExpiresAt() == null
                || utilisateur.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Le lien d'activation a expiré");
        }

        utilisateur.setActif(true);
        utilisateur.setEmailVerified(true);
        utilisateur.setEmailVerifiedAt(LocalDateTime.now());
        utilisateur.setEmailVerificationToken(null);
        utilisateur.setEmailVerificationExpiresAt(null);
        utilisateurRepository.save(utilisateur);

        String jwt = genererToken(utilisateur);
        UtilisateurDto dto = toDto(utilisateur);
        return new ConnexionResponse(dto, jwt, jwtConfig.getExpirationMs());
    }

    /**
     * Confirme un changement d'e-mail à partir du token reçu par e-mail.
     * Applique le nouvel e-mail (pendingEmail) et marque l'e-mail comme vérifié.
     */
    public void confirmerChangementEmail(String token) {
        Utilisateur utilisateur = utilisateurRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de confirmation invalide"));

        if (utilisateur.getEmailVerificationExpiresAt() == null
                || utilisateur.getEmailVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Le lien de confirmation a expiré");
        }

        if (utilisateur.getPendingEmail() == null || utilisateur.getPendingEmail().isBlank()) {
            throw new IllegalArgumentException("Aucun changement d'e-mail en attente");
        }

        String nouvelEmail = utilisateur.getPendingEmail();
        // Vérifier que le nouvel email n'est pas déjà utilisé
        utilisateurRepository.findByEmailIgnoreCaseAndTrim(nouvelEmail)
                .filter(u -> !u.getId().equals(utilisateur.getId()))
                .ifPresent(u -> { throw new IllegalArgumentException("Un compte existe déjà avec cet email"); });

        utilisateur.setEmail(nouvelEmail);
        utilisateur.setPendingEmail(null);
        utilisateur.setEmailVerified(true);
        utilisateur.setEmailVerifiedAt(LocalDateTime.now());
        utilisateur.setEmailVerificationToken(null);
        utilisateur.setEmailVerificationExpiresAt(null);
        utilisateurRepository.save(utilisateur);
    }

    /**
     * Envoie le mail d'activation (template Brevo) avec le lien d'activation.
     * L'échec d'envoi ne bloque jamais l'inscription.
     */
    private void envoyerMailActivation(Utilisateur utilisateur, String token) {
        String destinataire = utilisateur.getEmail();
        if (!mailEnabled) {
            if (stubMailbox == null || stubMailbox.isBlank()) {
                log.warn("Mode DEV : mail d'activation non envoyé pour {}", destinataire);
                if (emailVerificationDevMode) {
                    log.info("Mode DEV (email.verification.dev-mode=true) : token d'activation pour {} = {}", utilisateur.getEmail(), token);
                }
                return;
            }
            destinataire = stubMailbox.trim();
            log.info("Mode DEV : mail d'activation bouchonné vers {}", destinataire);
        }
        Map<String, Object> params = new HashMap<>();
        params.put("prenom", utilisateur.getPrenom());
        params.put("nom", utilisateur.getNom());
        params.put("lien_activation", frontendBaseUrl + "/activation?token=" + token);
        try {
            if (destinataire != null && !destinataire.isBlank()) {
                emailService.sendTemplateEmail(destinataire, BREVO_TEMPLATE_ACTIVATION, params);
                log.info("Mail d'activation envoyé à {}", destinataire);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi du mail d'activation à {}", destinataire, e);
        }
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

        if (base.getRole() == RoleUtilisateur.DIRECTEUR_THESE
                && !invitationRepository.existsByEmail(EmailUtils.normalize(request.getEmail()))) {
            throw new IllegalArgumentException(MESSAGE_DT_NON_INVITE);
        }

        // Réponse neutre (V5) : si l'email existe déjà, on ne révèle pas
        if (utilisateurRepository.existsByEmailIgnoreCaseAndTrim(EmailUtils.normalize(request.getEmail()))) {
            log.info("Inscription complète tentée avec un email déjà existant : {}", request.getEmail());
            UtilisateurDto dto = new UtilisateurDto();
            dto.setEmail(EmailUtils.normalize(request.getEmail()));
            return new ConnexionResponse(dto, null, 0);
        }

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

        Optional<Utilisateur> utilisateurOpt = utilisateurRepository.findByEmailIgnoreCaseAndTrim(email);

        // Hash BCrypt factice pour homogénéiser le temps de réponse (V5)
        String hashFactice = "$2a$10$abcdefghijklmnopqrstuv";

        boolean motDePasseValide;
        if (utilisateurOpt.isPresent()) {
            Utilisateur utilisateur = utilisateurOpt.get();
            if (!Boolean.TRUE.equals(utilisateur.getActif()) || !Boolean.TRUE.equals(utilisateur.getEmailVerified())) {
                // Compte non activé : matches factice pour homogénéiser
                passwordEncoder.matches(request.getMotDePasse(), hashFactice);
                throw new IllegalArgumentException("Email ou mot de passe incorrect");
            }
            motDePasseValide = passwordEncoder.matches(request.getMotDePasse(), utilisateur.getMotDePasse());
        } else {
            // Compte inexistant : matches factice pour homogénéiser le temps de réponse
            passwordEncoder.matches(request.getMotDePasse(), hashFactice);
            motDePasseValide = false;
        }

        if (!motDePasseValide) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        Utilisateur utilisateur = utilisateurOpt.get();
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
