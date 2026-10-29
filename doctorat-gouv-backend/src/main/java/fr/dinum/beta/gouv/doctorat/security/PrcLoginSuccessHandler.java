package fr.dinum.beta.gouv.doctorat.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.controller.PrcLoginController;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.exception.PrcAccountException;
import fr.dinum.beta.gouv.doctorat.service.AuthService;
import fr.dinum.beta.gouv.doctorat.service.PrcAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * Handler de succes du retour OIDC Projets Recherche Connect (PRC).
 *
 * <p>Appele par Spring Security apres un echange de code reussi
 * ({@code /login/oauth2/code/prc}). Il transforme l'identite PRC en
 * <strong>notre</strong> session applicative :</p>
 *
 * <ol>
 *   <li>lit le role demande en session (pose par {@code GET /api/prc/login}) ;</li>
 *   <li>trouve ou cree le compte local via {@link PrcAccountService} ;</li>
 *   <li>genere <strong>notre JWT</strong> via {@link AuthService#genererToken} ;</li>
 *   <li>redirige vers Angular avec {@code ?token=} (le front purge l'URL ensuite).</li>
 * </ol>
 *
 * <p>Un refus metier (compte PRC non actif, identite non nominative...) atterrit
 * sur {@code /connexion?erreur=prc} avec un message clair — jamais d'erreur 500.</p>
 */
@Component
public class PrcLoginSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(PrcLoginSuccessHandler.class);

    private final PrcAccountService prcAccountService;
    private final AuthService authService;
    private final String frontendBaseUrl;

    public PrcLoginSuccessHandler(PrcAccountService prcAccountService,
                                  AuthService authService,
                                  @Value("${app.frontend.base-url:https://app.doctorat.gouv.fr}") String frontendBaseUrl) {
        this.prcAccountService = prcAccountService;
        this.authService = authService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        if (!(authentication.getPrincipal() instanceof OidcUser oidc)) {
            log.error("Retour PRC inattendu : principal de type {} (OidcUser attendu)",
                    authentication.getPrincipal() != null ? authentication.getPrincipal().getClass() : "null");
            redirigerVersErreur(response);
            return;
        }

        // Le role n'est valable qu'une fois : on le lit puis on le purge de la session.
        RoleUtilisateur role = lireEtPurgerRole(request);

        try {
            PrcAccountService.ResultatConnexionPrc resultat = prcAccountService.trouverOuCreer(oidc, role);
            Utilisateur utilisateur = resultat.utilisateur();
            String token = authService.genererToken(utilisateur);

            log.info("Connexion PRC reussie : utilisateur id={} role={} sourceAuth={} compteCree={}",
                    utilisateur.getId(), utilisateur.getRole(), utilisateur.getSourceAuth(), resultat.compteCree());

            // Le role retenu est TOUJOURS celui du compte (jamais celui de la session) :
            // un compte candidat lie via PRC ne doit pas finir sur le tableau directeur.
            String destination = destinationPour(resultat, utilisateur);
            log.info("Redirection PRC -> {} (compteCree={})", destination, resultat.compteCree());
            response.sendRedirect(frontendBaseUrl + destination + "?token=" + token);

        } catch (PrcAccountException e) {
            // Refus metier attendu (state/type/claims) : pas un incident technique.
            log.warn("Connexion PRC refusee : {}", e.getMessage());
            redirigerVersErreur(response);

        } catch (Exception e) {
            // Erreur inattendue (BDD, contrainte, ...) : on ne laisse PAS fuiter une
            // stacktrace au navigateur. On logue pour le diagnostic et on renvoie
            // l'utilisateur sur /connexion?erreur=prc (le reste de l'app fonctionne).
            log.error("Erreur inattendue lors du retour PRC", e);
            redirigerVersErreur(response);
        }
    }

    /**
     * Choix de la page de destination apres un retour PRC reussi.
     *
     * <ul>
     *   <li><strong>Compte cree</strong> (creation JIT) → le profil
     *       ({@code /espace-directeur-these}) : l'onglet « Votre profil » est celui par
     *       defaut, et ses champs proviennent deja d'{@link Utilisateur} (nom, prenom,
     *       email renseignes depuis les claims PRC). L'utilisateur complete ensuite
     *       etablissement, laboratoire, etc.</li>
     *   <li><strong>Compte existant</strong> → directement le tableau de bord correspondant
     *       a <strong>son</strong> role : jamais le tableau directeur pour un compte candidat.</li>
     * </ul>
     *
     * <p>Le paramètre {@code token} est presente sur les deux cibles : il est traite au
     * demarrage de l'app Angular (APP_INITIALIZER), avant le routage et les gardes.</p>
     */
    private String destinationPour(PrcAccountService.ResultatConnexionPrc resultat, Utilisateur utilisateur) {

        boolean estDirecteur = utilisateur.getRole() == RoleUtilisateur.DIRECTEUR_THESE;

        if (resultat.compteCree() && estDirecteur) {
            // Compte neuf : completions du profil d'abord.
            return "/espace-directeur-these";
        }

        // Compte existant (ou role non directeur) : tableau de bord du role reel.
        if (estDirecteur) {
            return "/tableau-de-bord-directeur-these";
        }
        if (utilisateur.getRole() == RoleUtilisateur.CANDIDAT) {
            return "/tableau-de-bord-candidat";
        }

        // Autres roles (ecole doctorale, laboratoire, admin) : pas de tableau dedie
        // en phase 1 -> accueil, l'utilisateur garde ses droits habituels.
        log.info("Retour PRC pour un role sans tableau dedie : {}", utilisateur.getRole());
        return "/";
    }

    /**
     * Lit le role stocke par {@link PrcLoginController} puis le supprime : il ne
     * doit servir qu'a cette connexion. Retourne {@code null} si absent/invalide —
     * {@link PrcAccountService} n'en a besoin que pour la creation d'un compte.
     */
    private RoleUtilisateur lireEtPurgerRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return null;
        }
        Object brut = session.getAttribute(PrcLoginController.SESSION_ROLE);
        session.removeAttribute(PrcLoginController.SESSION_ROLE);
        if (brut == null) {
            return null;
        }
        try {
            return RoleUtilisateur.valueOf(brut.toString());
        } catch (IllegalArgumentException e) {
            log.warn("Role PRC inconnu en session : {}", brut);
            return null;
        }
    }

    private void redirigerVersErreur(HttpServletResponse response) throws IOException {
        response.sendRedirect(frontendBaseUrl + "/connexion?erreur=prc");
    }
}
