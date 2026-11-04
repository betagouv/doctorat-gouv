package fr.dinum.beta.gouv.doctorat.exception;

/**
 * Levee lorsqu'un directeur de these tente de se connecter via PRC alors que
 * son e-mail n'a pas ete invite (pas de trace dans invitation_directeur_these).
 */
public class PrcNotInvitedException extends PrcAccountException {

    private static final long serialVersionUID = 1L;

    public PrcNotInvitedException(String message) {
        super(message);
    }
}
