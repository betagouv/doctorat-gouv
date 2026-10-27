package fr.dinum.beta.gouv.doctorat.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception metier levee lors de la creation / liaison d'un compte via
 * Projets Recherche Connect (PRC) : identite non conforme (state/type),
 * claims manquants, role absent, etc.
 *
 * <p>Elle est interceptee par le handler de succes OAuth2 qui renvoie
 * l'utilisateur sur {@code /connexion} avec un message clair — jamais
 * d'erreur 500 ni de stacktrace exposee.</p>
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class PrcAccountException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PrcAccountException(String message) {
        super(message);
    }
}
