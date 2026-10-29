package fr.dinum.beta.gouv.doctorat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validation des id_token PRC — dont l'anomalie majeure : le claim {@code iss} n'est PAS
 * une URL (identifiant numerique {@code "856625"}), ce qui faisait planter
 * {@code OidcIdTokenValidator} en {@link IllegalArgumentException} (500) en recette
 * le 29/10/2026.
 *
 * <p>Valide que notre remplacement accepte ce cas <strong>sans affaiblir</strong> les
 * autres controles (aud, azp, claims obligatoires).</p>
 */
class PrcIdTokenValidatorTest {

    private static final String CLIENT_ID = "doctorat-gouv-local";
    private static final String ISSUER_PRC = "856625";
    private static final String ISSUER_CONFIGURE =
            "https://preprod.projetsrechercheconnect.fr/user/IdPOAuth2/abc";

    private final PrcIdTokenValidator validateur =
            new PrcIdTokenValidator(CLIENT_ID, ISSUER_PRC, ISSUER_CONFIGURE);

    /**
     * Construit un jeton. Les claims a valeur {@code null} sont simplement omis, ce qui
     * permet de tester un claim obligatoire absent.
     *
     * <p>⚠️ {@code Jwt.Builder} exige des {@code Instant} pour {@code iat} / {@code exp}
     * (et non des secondes epoch).</p>
     */
    private Jwt jeton(String issuer, String sub, String aud, String azp) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", issuer);
        if (sub != null) {
            claims.put("sub", sub);
        }
        claims.put("aud", aud);
        if (azp != null) {
            claims.put("azp", azp);
        }
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(300));

        return Jwt.withTokenValue("jeton-bidon")
                .header("alg", "none")
                .claims(m -> m.putAll(claims))
                .build();
    }

    /** Jeton conforme, tel que PRC le signe (issuer numerique). */
    private Jwt jetonValide() {
        return jeton(ISSUER_PRC, "prc-sub-factice", CLIENT_ID, null);
    }

    private static String premierCodeErreur(OAuth2TokenValidatorResult resultat) {
        return resultat.getErrors().iterator().next().getErrorCode();
    }

    @Test
    void issuer_numerique_prc_est_accepte() {
        OAuth2TokenValidatorResult resultat = validateur.validate(jetonValide());

        assertFalse(resultat.hasErrors(),
                "L'issuer non-URL de PRC doit etre accepte : c'est l'objet meme de ce validateur");
    }

    @Test
    void issuer_configure_est_aussi_accepte() {
        assertFalse(validateur.validate(jeton(ISSUER_CONFIGURE, "prc-sub", CLIENT_ID, null))
                .hasErrors());
    }

    @Test
    void issuer_inconnu_est_rejete() {
        OAuth2TokenValidatorResult resultat =
                validateur.validate(jeton("issuer-ennemi", "prc-sub", CLIENT_ID, null));

        assertTrue(resultat.hasErrors(), "Un issuer inconnu doit etre rejete");
        assertEquals("invalid_id_token", premierCodeErreur(resultat),
                "Le code d'erreur doit rester invalid_id_token pour declencher notre failureHandler");
    }

    @Test
    void aud_sans_notre_client_id_est_rejete() {
        assertTrue(validateur.validate(jeton(ISSUER_PRC, "prc-sub", "autre-client", null))
                        .hasErrors(),
                "Le jeton doit contenir notre client_id dans aud");
    }

    @Test
    void claim_sub_absent_est_rejete() {
        assertTrue(validateur.validate(jeton(ISSUER_PRC, null, CLIENT_ID, null)).hasErrors(),
                "sub est obligatoire (OIDC)");
    }

    @Test
    void claim_issuer_absent_est_rejete() {
        assertTrue(validateur.validate(jeton(null, "prc-sub", CLIENT_ID, null)).hasErrors(),
                "iss est obligatoire (OIDC)");
    }

    @Test
    void azp_incompatible_avec_client_id_est_rejete() {
        assertTrue(validateur.validate(jeton(ISSUER_PRC, "prc-sub", CLIENT_ID, "autre-client"))
                        .hasErrors(),
                "Si azp est present, il doit valoir notre client_id");
    }

    @Test
    void aud_multiple_sans_azp_est_rejete() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("iss", ISSUER_PRC);
        claims.put("sub", "prc-sub");
        // Audiences multiples : azp devient obligatoire (regle 4 OIDC).
        claims.put("aud", java.util.List.of(CLIENT_ID, "autre-client"));
        claims.put("iat", Instant.now());
        claims.put("exp", Instant.now().plusSeconds(300));

        Jwt jeton = Jwt.withTokenValue("jeton-bidon")
                .header("alg", "none")
                .claims(m -> m.putAll(claims))
                .build();

        assertTrue(validateur.validate(jeton).hasErrors(),
                "Avec plusieurs audiences, azp doit etre present");
    }

    @Test
    void sans_issuer_attendu_aucun_jeton_n_est_accepte() {
        // Fail closed : sans issuer utilisable, aucun jeton ne passe (plutot que tous).
        PrcIdTokenValidator sansAttendus = new PrcIdTokenValidator(CLIENT_ID, null, "   ");

        assertTrue(sansAttendus.validate(jetonValide()).hasErrors(),
                "Sans issuer attendu, aucun jeton ne doit passer (fail closed)");
    }
}
