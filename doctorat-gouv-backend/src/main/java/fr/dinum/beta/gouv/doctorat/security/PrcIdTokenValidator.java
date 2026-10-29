package fr.dinum.beta.gouv.doctorat.security;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validation des id_token Projets Recherche Connect (PRC).
 *
 * <p><strong>Pourquoi remplacer {@code OidcIdTokenValidator} ?</strong> PRC viole la spec
 * OIDC Core 1.0 §2 : l'Issuer Identifier <em>doit</em> etre une URL, mais son claim
 * {@code iss} porte l'identifiant numerique de la discovery ({@code "856625"}).</p>
 *
 * <p>Spring appelle alors {@code idToken.getIssuer()} qui passe par
 * {@code ClaimAccessor.getClaimAsURL} et leve :</p>
 *
 * <pre>
 * java.lang.IllegalArgumentException: Unable to convert claim 'iss' of type
 * 'class java.lang.String' to URL.
 * </pre>
 *
 * <p>Cette exception <strong>non maitrisee</strong> provoque une 500 et contourne notre
 * handler d'echec (pas de retour propre sur {@code /connexion?erreur=prc}). Le validateur
 * Spring est {@code public final} : impossible de l'etendre, on le <strong>remplace</strong>.</p>
 *
 * <p><strong>Aucun controle n'est supprime.</strong> On reproduit fidelement
 * {@code OidcIdTokenValidator.validate()} :</p>
 *
 * <ul>
 *   <li>claims obligatoires : {@code iss}, {@code sub}, {@code aud}, {@code exp}, {@code iat} ;</li>
 *   <li>{@code iss} == issuer de la discovery (comparaison <strong>chaine</strong>, pas URL) ;</li>
 *   <li>{@code aud} contient le {@code client_id} ;</li>
 *   <li>{@code azp} obligatoire si plusieurs audiences, et egal au {@code client_id} ;</li>
 *   <li>validite temporelle {@code exp} / {@code iat} : deleguee a
 *       {@link org.springframework.security.oauth2.jwt.JwtTimestampValidator} (skew 60 s).</li>
 * </ul>
 *
 * <p>⚠️ La <strong>signature</strong> n'est pas geree ici : {@code NimbusJwtDecoder} la
 * verifie <strong>avant</strong> d'appeler ce validateur (voir le passage de
 * {@code invalid_id_token} — signature acceptee — a cette etape).</p>
 */
public class PrcIdTokenValidator implements OAuth2TokenValidator<Jwt> {

    private static final Logger log = LoggerFactory.getLogger(PrcIdTokenValidator.class);

    private final String clientId;

    /** Valeurs acceptees pour {@code iss} : issuer brut de la discovery + issuer configure. */
    private final Set<String> issuersAttendus = new LinkedHashSet<>();

    public PrcIdTokenValidator(String clientId, String issuerDiscovery, String issuerConfigure) {
        this.clientId = clientId;
        ajouter(issuersAttendus, issuerDiscovery);
        ajouter(issuersAttendus, issuerConfigure);
    }

    private static void ajouter(Set<String> cible, String valeur) {
        if (valeur != null && !valeur.isBlank()) {
            cible.add(valeur.trim());
        }
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt idToken) {

        Map<String, Object> invalidClaims = new HashMap<>();

        // --- Claims obligatoires -----------------------------------------------
        String issuer = idToken.getClaimAsString(IdTokenClaimNames.ISS);
        if (issuer == null || issuer.isBlank()) {
            invalidClaims.put(IdTokenClaimNames.ISS, issuer);
        }

        String subject = idToken.getSubject();
        if (subject == null) {
            invalidClaims.put(IdTokenClaimNames.SUB, subject);
        }

        List<String> audience = idToken.getAudience();
        if (audience == null || audience.isEmpty()) {
            invalidClaims.put(IdTokenClaimNames.AUD, audience);
        }

        if (idToken.getExpiresAt() == null) {
            invalidClaims.put(IdTokenClaimNames.EXP, null);
        }
        if (idToken.getIssuedAt() == null) {
            invalidClaims.put(IdTokenClaimNames.IAT, null);
        }

        // --- iss : egalite en CHAINE (et non en URL) ---------------------------
        if (issuer != null && !issuer.isBlank() && !issuersAttendus.contains(issuer)) {
            // Trace indispensable : sans elle, un ecart d'issuer ne se lit nulle part.
            log.warn("Issuer du jeton PRC « {} » inattendu (attendu parmi {}) — jeton rejette.",
                    issuer, issuersAttendus);
            invalidClaims.put(IdTokenClaimNames.ISS, issuer);
        }

        // --- aud : doit contenir notre client_id --------------------------------
        if (audience != null && !audience.isEmpty() && !audience.contains(clientId)) {
            invalidClaims.put(IdTokenClaimNames.AUD, audience);
        }

        // --- azp : regles 4 et 5 de la spec OIDC --------------------------------
        String authorizedParty = idToken.getClaimAsString(IdTokenClaimNames.AZP);
        if (audience != null && audience.size() > 1 && authorizedParty == null) {
            invalidClaims.put(IdTokenClaimNames.AZP, null);
        }
        if (authorizedParty != null && !authorizedParty.equals(clientId)) {
            invalidClaims.put(IdTokenClaimNames.AZP, authorizedParty);
        }

        if (!invalidClaims.isEmpty()) {
            return OAuth2TokenValidatorResult.failure(invalidIdToken(invalidClaims));
        }
        return OAuth2TokenValidatorResult.success();
    }

    private static OAuth2Error invalidIdToken(Map<String, Object> invalidClaims) {
        // Meme code d'erreur que Spring : notre failureHandler le traite comme un echec
        // d'authentification et renvoie l'utilisateur sur /connexion?erreur=prc.
        return new OAuth2Error("invalid_id_token",
                "The ID Token contains invalid claims: " + invalidClaims,
                "https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation");
    }
}
