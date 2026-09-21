package fr.dinum.beta.gouv.doctorat.util;

import java.util.Locale;

/**
 * Normalisation centralisée des e-mails.
 * Les flux ADUM / AMETHIS fournissent des e-mails avec des casses et espaces variables
 * (ex : MonMail@Domaine.fr vs MONMAIL@domaine.fr). Sans normalisation, un directeur
 * qui s'inscrit avec une casse différente ne peut plus se connecter ni retrouver
 * ses sujets sur /mes-sujets-directeur-these.
 */
public final class EmailUtils {

    private EmailUtils() {
    }

    /**
     * Normalise un e-mail : trim + minuscules. Retourne null si vide/null.
     */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }
}
