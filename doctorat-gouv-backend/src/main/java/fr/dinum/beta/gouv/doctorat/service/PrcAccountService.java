package fr.dinum.beta.gouv.doctorat.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;
import fr.dinum.beta.gouv.doctorat.exception.PrcAccountException;
import fr.dinum.beta.gouv.doctorat.exception.PrcNotInvitedException;
import fr.dinum.beta.gouv.doctorat.repository.InvitationDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.util.EmailUtils;

/**
 * Creation / liaison des comptes issus de Projets Recherche Connect (PRC).
 *
 * <p><strong>JIT provisioning</strong> : PRC est un fournisseur d'identite, pas un
 * gestionnaire de comptes. A chaque connexion, on retrouve ou l'on cree le compte
 * local, en remplissant les informations obligatoires depuis les claims PRC :</p>
 *
 * <ul>
 *   <li>{@code principalEmail} → {@code Utilisateur.email}</li>
 *   <li>{@code useFirstName} / {@code firstName} → {@code prenom} (priorite au nom d'usage)</li>
 *   <li>{@code useLastName} / {@code lastName} → {@code nom}</li>
 *   <li>{@code sub} → {@code prcSub} (cle d'unicite)</li>
 *   <li>{@code personId} → {@code prcPersonId}</li>
 * </ul>
 *
 * <p><strong>Regle anti-doublon (jamais de 2e compte pour le meme email) :</strong></p>
 * <ol>
 *   <li>recherche par {@code prcSub} → compte SSO existant ;</li>
 *   <li>sinon recherche par email → <strong>liaison</strong> du compte existant
 *       (ajout de {@code prcSub}), <strong>role conserve</strong> ;</li>
 *   <li>sinon creation JIT avec le role venu de la session.</li>
 * </ol>
 *
 * <p>⚠️ Le role de la session ne s'applique <strong>qu'a la creation</strong> : un
 * clic malheureux sur le bouton PRC depuis {@code /connexion} ne doit jamais
 * transformer un compte candidat en compte directeur de these.</p>
 */
@Service
public class PrcAccountService {

    private static final Logger log = LoggerFactory.getLogger(PrcAccountService.class);

    private static final String MESSAGE_DT_NON_INVITE =
        "Actuellement, l'inscription et la connexion à la plateforme pour les directeurs de thèse " +
        "ne sont ouvertes que sur invitation. Si vous êtes concerné, utilisez le lien reçu par e-mail ; " +
        "sinon, contactez la plateforme.";

    /** Longueur max des colonnes {@code prenom} / {@code nom} (length = 100). */
    private static final int MAX_NOM_PRENOM = 100;

    private static final String ETAT_ACTIF = "active";
    private static final String TYPE_NOMINATIF = "user-nominative";

    /**
     * Email synthetique pour un compte cree sans email (userinfo indisponible).
     *
     * <p>Le TLD {@code .invalid} est reserve par la RFC 2606 : il n'est jamais
     * routable, donc aucune collision avec une vraie adresse et aucune
     * notification email ne sera envoyee par erreur. L'email est stable par
     * {@code sub} (re-login retrouve le meme compte par {@code prcSub}).</p>
     *
     * <p>⚠️ <strong>Option A (temporaire)</strong> : a basculer en Option B
     * (refus propre) une fois PRC corrige (voir {@code spec-technique.md} §6.1).</p>
     */
    private static final String SUFFIXE_EMAIL_SQUELETTE = "@sans-email.invalid";


    private final UtilisateurRepository utilisateurRepository;
    private final InvitationDirecteurTheseRepository invitationRepository;
    private final PasswordEncoder passwordEncoder;

    public PrcAccountService(UtilisateurRepository utilisateurRepository,
                             InvitationDirecteurTheseRepository invitationRepository,
                             PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.invitationRepository = invitationRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Resultat d'une connexion PRC : le compte local + l'information de creation.
     *
     * <p>{@code compteCree} distingue une <strong>creation JIT</strong> (compte neuf,
     * profil a completer) d'une <strong>connexion/reconnexion</strong> a un compte deja
     * existant (aller directement au tableau de bord).</p>
     *
     * @param utilisateur le compte local, pret a etre connecte (JWT a generer par l'appelant)
     * @param compteCree  {@code true} si le compte vient d'etre cree au cours de cet appel
     */
    public record ResultatConnexionPrc(Utilisateur utilisateur, boolean compteCree) {
    }

    /**
     * Retrouve ou cree le compte local correspondant a l'identite PRC.
     *
     * @param oidc utilisateur OIDC charge par Spring (id_token + userinfo fusionnes)
     * @param role role demande a la creation (depuis la session) ; ignore si le compte existe deja
     * @return le compte local + l'indicateur de creation (pour choisir la page de destination)
     * @throws PrcAccountException si l'identite PRC est non conforme (state/type/email/sub/nom absents)
     */
    @Transactional
    public ResultatConnexionPrc trouverOuCreer(OidcUser oidc, RoleUtilisateur role) {

        // Decouverte du format reel du userinfo PRC (etape de recette phase 1).
        log.info("Claims PRC recus : {}", oidc.getClaims());

        // --- Controles metier (spec-technique.md §8) ---
        // Degradation gracieuse (Option A, temporaire) : si userinfo est indisponible,
        // `state`/`type` sont absents — on ne peut pas les verifier. PRC reste la source
        // de verite (il n'aurait pas emis de jeton pour un compte desactive) ; on trace
        // un WARN pour garder la traçabilite. A basculer en refus strict (Option B)
        // une fois PRC corrige.
        String state = oidc.getClaimAsString("state");
        if (state != null && !state.isBlank()) {
            if (!ETAT_ACTIF.equalsIgnoreCase(state.trim())) {
                throw new PrcAccountException("Compte PRC non actif (state=" + state + ")");
            }
        } else {
            log.warn("Claim 'state' absent du jeton PRC (userinfo indisponible ?) — "
                    + "controle saute (Option A, temporaire)");
        }

        String type = oidc.getClaimAsString("type");
        if (type != null && !type.isBlank()) {
            if (!TYPE_NOMINATIF.equalsIgnoreCase(type.trim())) {
                throw new PrcAccountException("Identite PRC non nominative (type=" + type + ")");
            }
        } else {
            log.warn("Claim 'type' absent du jeton PRC — controle saute (Option A, temporaire)");
        }

        String sub = oidc.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new PrcAccountException("Claim 'sub' absente de l'id_token PRC");
        }

        String email = EmailUtils.normalize(oidc.getClaimAsString("principalEmail"));
        if (email == null) {
            // Degradation gracieuse (Option A, temporaire) : pas d'email transmis par PRC
            // (userinfo indisponible). On cree un compte squelette avec un email synthetique
            // stable (derive du sub) ; l'utilisateur complete son profil depuis
            // /espace-directeur-these.
            // TODO Option B (a basculer) : refuser proprement (PrcAccountException) si
            // l'email manque, pour eviter tout risque de doublon d'identite.
            email = "prc-" + sub + SUFFIXE_EMAIL_SQUELETTE;
            log.warn("Aucun email fourni par PRC : creation d'un compte SQUELETTE "
                    + "(email synthetique {}) — l'utilisateur complete son profil", email);
        }

        // Phase 1 : pas d'ecran de choix, on continue avec principalEmail (phase 2).
        List<String> autresEmails = oidc.getClaimAsStringList("otherEmails");
        if (autresEmails != null && autresEmails.size() > 1) {
            log.warn("Compte PRC avec {} emails secondaires {} — ecran de choix reporte (phase 2), "
                    + "poursuite avec l'email principal {}", autresEmails.size(), autresEmails, email);
        }

        String personId = oidc.getClaimAsString("personId");
        String prenom = prenomDe(oidc);
        String nom = nomDe(oidc);

        // --- 1. Compte SSO deja lie a ce sub ---
        Optional<Utilisateur> parSub = utilisateurRepository.findByPrcSub(sub);
        if (parSub.isPresent()) {
            Utilisateur utilisateur = parSub.get();
            majIdentite(utilisateur, email, prenom, nom, personId);
            log.info("Connexion PRC : compte {} retrouve par prcSub (role conserve : {})",
                    utilisateur.getId(), utilisateur.getRole());
            // Compte deja existant : le front ira directement au tableau de bord.
            return new ResultatConnexionPrc(utilisateurRepository.save(utilisateur), false);
        }

        // --- 2. Compte existant trouve par email : LIAISON (jamais de doublon) ---
        Optional<Utilisateur> parEmail = utilisateurRepository.findByEmailIgnoreCaseAndTrim(email);
        if (parEmail.isPresent()) {
            Utilisateur utilisateur = parEmail.get();
            if (utilisateur.getPrcSub() != null && !utilisateur.getPrcSub().equals(sub)) {
                log.warn("Compte {} rattache au sub PRC {} puis au nouveau sub {} "
                        + "(identite PRC mise a jour)", utilisateur.getId(), utilisateur.getPrcSub(), sub);
            }
            utilisateur.setPrcSub(sub);
            utilisateur.setPrcPersonId(personId);
            majIdentite(utilisateur, email, prenom, nom, personId);
            // ⚠️ ROLE CONSERVE : on n'applique jamais le role de la session ici.
            log.info("Connexion PRC : compte {} existant LIE par email {} (role conserve : {})",
                    utilisateur.getId(), email, utilisateur.getRole());
            // Compte deja existant (simple liaison) : le front ira au tableau de bord.
            return new ResultatConnexionPrc(utilisateurRepository.save(utilisateur), false);
        }

        // --- 3. Creation JIT ---
        if (role == null) {
            throw new PrcAccountException("Aucun role fourni pour la creation du compte PRC");
        }
        if (role == RoleUtilisateur.DIRECTEUR_THESE
                && !invitationRepository.existsByEmail(email)) {
            throw new PrcNotInvitedException(MESSAGE_DT_NON_INVITE);
        }
        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setEmail(email);
        // Colonnes NOT NULL : mot de passe aleatoire (jamais connu, l'utilisateur
        // se connecte toujours via PRC) et date de creation.
        utilisateur.setMotDePasse(passwordEncoder.encode(UUID.randomUUID().toString()));
        utilisateur.setPrenom(prenom != null ? prenom : "A renseigner");
        utilisateur.setNom(nom != null ? nom : "A renseigner");
        utilisateur.setRole(role);
        utilisateur.setSourceAuth(SourceAuth.PROJETS_RECHERCHE_CONNECT);
        utilisateur.setActif(true);
        utilisateur.setDateCreation(LocalDateTime.now());
        utilisateur.setPrcSub(sub);
        utilisateur.setPrcPersonId(personId);

        utilisateur = utilisateurRepository.save(utilisateur);
        log.info("Creation JIT d'un compte PRC : id={} email={} role={} personId={}",
                utilisateur.getId(), email, role, personId);
        // Compte tout juste cree : le front ira completer le profil (auto-rempli par PRC).
        return new ResultatConnexionPrc(utilisateur, true);
    }

    /**
     * Met a jour l'identite (prenom/nom depuis PRC, priorite aux noms d'usage).
     * L'email n'est <strong>jamais</strong> modifie : il sert de cle de liaison et
     * l'uniqueite en base pourrait etre violee par un changement d'email chez PRC.
     */
    private void majIdentite(Utilisateur utilisateur, String emailPrc, String prenom, String nom, String personId) {
        // Degradation gracieuse (Option A) : si userinfo indisponible, on ne ecrase
        // jamais une vraie identite par null.
        if (prenom != null) {
            utilisateur.setPrenom(prenom);
        }
        if (nom != null) {
            utilisateur.setNom(nom);
        }
        if (personId != null && !personId.isBlank()) {
            utilisateur.setPrcPersonId(personId);
        }
        if (emailPrc != null && !emailPrc.equalsIgnoreCase(utilisateur.getEmail())) {
            log.info("Email PRC ({}) different de l'email du compte ({}) : on conserve l'email du compte",
                    emailPrc, utilisateur.getEmail());
        }
    }

    private String prenomDe(OidcUser oidc) {
        String prenom = premierNonVide(oidc.getClaimAsString("useFirstName"),
                oidc.getClaimAsString("firstName"));
        if (prenom == null) {
            return null; // absence toleree (Option A) : placeholder en creation
        }
        return tronquer(prenom);
    }

    private String nomDe(OidcUser oidc) {
        String nom = premierNonVide(oidc.getClaimAsString("useLastName"),
                oidc.getClaimAsString("lastName"));
        if (nom == null) {
            return null; // absence toleree (Option A) : placeholder en creation
        }
        return tronquer(nom);
    }

    private static String premierNonVide(String premier, String second) {
        if (premier != null && !premier.isBlank()) {
            return premier.trim();
        }
        if (second != null && !second.isBlank()) {
            return second.trim();
        }
        return null;
    }

    /** Les colonnes prenom/nom sont limitées a 100 caracteres : on tronque plutot que de risquer une erreur SQL. */
    private static String tronquer(String valeur) {
        String v = valeur.trim();
        if (v.length() > MAX_NOM_PRENOM) {
            log.warn("Valeur PRC trop longue ({} car.) tronquee a {} caracteres : {}",
                    v.length(), MAX_NOM_PRENOM, v.substring(0, 30) + "...");
            return v.substring(0, MAX_NOM_PRENOM);
        }
        return v;
    }
}
