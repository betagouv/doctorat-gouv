package fr.dinum.beta.gouv.doctorat.controller;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.dinum.beta.gouv.doctorat.config.PrcProperties;
import fr.dinum.beta.gouv.doctorat.config.PrcClientRegistrationRepository;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Point d'entrée du parcours d'authentification externe Projets Recherche Connect (PRC).
 *
 * <p>Le bouton « Continuer avec Projets Recherche Connect » de la page {@code /connexion}
 * est un simple lien de navigation : ce contrôleur mémorise le rôle demandé en session
 * puis relaie vers le point d'autorisation OIDC de Spring Security
 * ({@code /oauth2/authorization/prc}), qui redirige vers la mire PRC.</p>
 *
 * <p>Le rôle est stocké <strong>avant</strong> la redirection car, au moment du retour
 * (callback {@code /login/oauth2/code/prc}), Spring ne peut pas savoir si l'utilisateur
 * venait de la page de connexion ou de l'inscription directeur. Il n'est repris qu'au
 * moment de la création du compte (voir la phase 2 du plan PRC).</p>
 */
@RestController
@RequestMapping("/api/prc")
public class PrcLoginController {

    private static final Logger log = LoggerFactory.getLogger(PrcLoginController.class);

    /** Clé de session portant le rôle demandé avant la redirection vers PRC. */
    public static final String SESSION_ROLE = "prc.role";

    /** Point d'autorisation OIDC géré par {@code .oauth2Login()} dans {@code SecurityConfig}. */
    private static final String AUTHORIZE_URL = "/oauth2/authorization/prc";

    /**
     * Rôles acceptés pour une connexion via PRC.
     * Restreint volontairement : ce paramètre vient d'un lien public, il ne doit jamais
     * permettre d'obtenir un rôle à privilèges (ADMIN, ECOLE_DOCTORALE, LABORATOIRE).
     */
    private static final Set<RoleUtilisateur> ROLES_AUTORISES =
            EnumSet.of(RoleUtilisateur.DIRECTEUR_THESE);

    private final PrcProperties prcProperties;
    private final PrcClientRegistrationRepository prcRegistrationRepo; // NOUVEAU

    // NOUVEAU constructeur injectant à la fois PrcProperties et le dépôt paresseux
    public PrcLoginController(PrcProperties prcProperties,
                              PrcClientRegistrationRepository prcRegistrationRepo) {
        this.prcProperties = prcProperties;
        this.prcRegistrationRepo = prcRegistrationRepo;
    }

    /**
     * Statut de PRC : {@code GET /api/prc/statut} → {@code {"actif":bool,"ready":bool}}.
     *
     * <p>"actif" indique si l'issuer-uri est renseigné (le backend peut être configuré).</p>
     * <p>"ready" indique si le client OIDC a été découvert (c'est-à-dire qu'il peut traiter les connexions).</p>
     */
    @GetMapping("/statut")
    public ResponseEntity<Map<String, Object>> statut() {
        Map<String, Object> map = new HashMap<>();
        map.put("actif", prcProperties.isActive());
        map.put("ready", prcRegistrationRepo.isReady());
        return ResponseEntity.ok(map);
    }

    /**
     * Démarre le flux OIDC : {@code GET /api/prc/login?role=DIRECTEUR_THESE}.
     *
     * <p>503 si PRC n'est pas configuré, 400 si le rôle est absent ou inconnu,
     * 403 s'il n'est pas autorisé pour PRC.</p>
     * <p>503 si le client OIDC n'est pas encore prêt (découverte OIDC échouée ou toujours en attente).</p>
     */
    @GetMapping("/login")
    public void login(@RequestParam("role") RoleUtilisateur role,
                      HttpServletRequest request,
                      HttpServletResponse response) throws IOException {

        // Vérification de l'activitié (présence de l'issuer)
        if (!prcProperties.isActive()) {
            log.warn("Tentative de connexion PRC alors que le service n'est pas configuré");
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Projets Recherche Connect n'est pas activé sur cet environnement");
            return;
        }

        // Découverte OIDC paresseuse : la première connexion la débloque. Un simple contrôle
        // sur isReady() empêcherait toute tentative, la découverte n'étant enclenchée que par
        // Spring Security après cette redirection.
        if (!prcRegistrationRepo.preparer()) {
            log.error("Connexion PRC refusée : découverte OIDC en échec sur {}. Détail : {}",
                    prcProperties.getIssuerUri(), prcRegistrationRepo.getDerniereErreur());
            response.sendError(HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "Projets Recherche Connect est momentanément indisponible");
            return;
        }

        if (!ROLES_AUTORISES.contains(role)) {
            log.warn("Tentative de connexion PRC avec un rôle non autorisé : {}", role);
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Rôle non autorisé pour Projets Recherche Connect");
            return;
        }

        request.getSession(true).setAttribute(SESSION_ROLE, role.name());
        log.info("Redirection vers Projets Recherche Connect (rôle {})", role);
        response.sendRedirect(AUTHORIZE_URL);
    }
}
