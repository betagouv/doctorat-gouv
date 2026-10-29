package fr.dinum.beta.gouv.doctorat.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Handler d'echec du retour OIDC Projets Recherche Connect (PRC).
 *
 * <p>Tout echec (code refuse, id_token invalide, PKCE non verifie, timeout...)
 * atterrit sur {@code /connexion?erreur=prc} : le reste de l'application reste
 * utilise et l'utilisateur voit un message clair plutot qu'une page blanche.
 * Le detail est logue cote backend pour le diagnostic.</p>
 */
@Component
public class PrcLoginFailureHandler implements AuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(PrcLoginFailureHandler.class);

    private final String frontendBaseUrl;

    public PrcLoginFailureHandler(@Value("${app.frontend.base-url}") String frontendBaseUrl) {
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("Echec de connexion PRC sur {} : {} ({})",
                request.getRequestURI(),
                exception.getMessage(),
                exception.getClass().getSimpleName(),
                exception);
        response.sendRedirect(frontendBaseUrl + "/connexion?erreur=prc");
    }
}
