package fr.dinum.beta.gouv.doctorat.security;

import java.net.URI;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;

import fr.dinum.beta.gouv.doctorat.config.PrcClientRegistrationRepository;

/**
 * Lecture du endpoint {@code /userinfo} de Projets Recherche Connect (PRC), <strong>tolerant
 * au Content-Type</strong>.
 *
 * <p><strong>Pourquoi ce service existe ?</strong> PRC renvoie {@code Content-Type:
 * application/jwt} alors que nous demandons {@code Accept: application/json}
 * (OIDC Core 1.0 §5.2 impose {@code application/json}). Spring lève alors
 * {@code [invalid_user_info_response] ... response contains invalid content type} et la
 * connexion echoue.</p>
 *
 * <p>Par ailleurs, l'{@code id_token} de PRC <strong>ne contient aucun claim metier</strong>
 * ({@code principalEmail}, {@code state}, {@code type}, {@code personId}... — constate en
 * recette le 29/10/2026) : {@code /userinfo} est donc notre <strong>seule source</strong>.</p>
 *
 * <p>On lit donc le corps <strong>nous-memes</strong> ({@code ResponseEntity<String>}, qui
 * accepte n'importe quel type media) puis on determine le format :</p>
 *
 * <ul>
 *   <li><strong>JSON</strong> → on l'utilise tel quel (en-tete simplement errone) ;</li>
 *   <li><strong>JWT signe (JWS)</strong> → verification de la signature via le
 *       {@code jwks_uri} de la discovery, avec repli sur la cle publique fournie par PRC
 *       ({@code PRC_USERINFO_PUBLIC_KEY}, base64 DER) ;</li>
 *   <li><strong>JWT chiffre (JWE)</strong> → impossible sans cle privee → erreur explicite.</li>
 * </ul>
 *
 * <p>🔒 <strong>La signature n'est jamais sautee</strong> : accepter un JWT sans le verifier
 * reviendrait a laisser un inconnu fournir l'identite de nos utilisateurs.</p>
 */
public class PrcUserInfoService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private static final Logger log = LoggerFactory.getLogger(PrcUserInfoService.class);

    /** Lecture directe : on ignore le declare, on determine le format au corps. */
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final TypeReference<Map<String, Object>> TYPE_CORPS =
            new TypeReference<>() { };

    /** Delais : le clic utilisateur ne doit pas pendre indfiniment. */
    private static final int TIMEOUT_MS = 15_000;

    private final PrcClientRegistrationRepository depot;

    /** Cle de secours PRC (base64 DER, sans entetes PEM) ; null si non configuree. */
    private final PublicKey cleSecours;

    public PrcUserInfoService(PrcClientRegistrationRepository depot,
                              @Value("${prc.userinfo-public-key:}") String clePubliqueBase64) {
        this.depot = depot;
        this.cleSecours = chargerCle(clePubliqueBase64);
        if (this.cleSecours != null) {
            log.info("Cle publique de secours PRC chargee (PRC_USERINFO_PUBLIC_KEY)");
        }
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        Map<String, Object> claims = null;
        try {
            String corps = lireCorps(userRequest);
            claims = interpreter(corps, userRequest);
        } catch (OAuth2AuthenticationException e) {
            // PRC renvoie un Content-Type non conforme (application/jwt) ou une erreur reseau :
            // on ne fait PAS echouer la connexion pour autant. On continue avec l'id_token seul,
            // et PrcAccountService gere l'absence des claims metier (compte squelette si besoin).
            // TODO Option B -> A : basculer en refus propre une fois PRC corrige (iss + userinfo).
            log.warn("Lecture /userinfo impossible ({}). On continue avec l'id_token seul — "
                    + "les claims PRC (email, nom, state, type...) seront manquants.", e.getMessage());
            claims = new java.util.HashMap<>();
        }

        return construireUtilisateur(claims, userRequest);
    }

    /**
     * Assemble l'{@code OidcUser} : coherence {@code sub} (OIDC Core §16.11) puis fusion
     * id_token + userinfo.
     *
     * <p>Visible <strong>package</strong> uniquement pour les tests : cette logique est
     * pure (aucun reseau) et c'est elle qui decide si un utilisateur est accepte.</p>
     *
     * <p><strong>Degradation gracieuse</strong> (Option A, temporaire) : si le userinfo a
     * echoue, {@code claims} est vide — on ne verifie pas le {@code sub} (rien a comparer)
     * et on fusionne uniquement les claims de l'id_token. Le compte squelette sera cree
     * cote {@code PrcAccountService}.</p>
     */
    OidcUser construireUtilisateur(Map<String, Object> claims, OidcUserRequest userRequest) {

        OidcIdToken idToken = userRequest.getIdToken();

        // Degradation gracieuse (Option A, temporaire) : si le userinfo a echoue, `claims`
        // est vide - on ne verifie pas le sub (rien a comparer) et on fusionne uniquement
        // les claims de l'id_token. Le compte squelette sera cree cote PrcAccountService.
        boolean userinfoLu = claims != null && !claims.isEmpty();

        if (userinfoLu) {
            // OIDC Core section 16.11 : le sub du userinfo DOIT egaler celui de l'id_token.
            String subIdToken = idToken.getSubject();
            Object subUserInfo = claims.get("sub");
            if (subUserInfo == null || !subIdToken.equals(String.valueOf(subUserInfo))) {
                throw echec("Claim 'sub' du userinfo (" + subUserInfo + ") different de celui de "
                        + "l'id_token (" + subIdToken + ")");
            }
        }

        Map<String, Object> attributs = new HashMap<>(idToken.getClaims());
        if (userinfoLu) {
            attributs.putAll(claims);
        }

        Collection<GrantedAuthority> autorites = new ArrayList<>();
        for (String portee : userRequest.getAccessToken().getScopes()) {
            autorites.add(new SimpleGrantedAuthority("SCOPE_" + portee));
        }

        if (userinfoLu) {
            log.info("UserInfo PRC lu : {} claims ({})", claims.size(), claims.keySet());
            return new DefaultOidcUser(autorites, idToken, new OidcUserInfo(claims));
        }

        // Degradation : pas de userinfo, on construit l'OidcUser depuis l'id_token seul.
        log.warn("UserInfo PRC indisponible - OidcUser construit depuis l'id_token seul "
                + "(claims metier manquants : email, nom, state, type...)");
        return new DefaultOidcUser(autorites, idToken);
    }

    // ------------------------------------------------------------------
    // Lecture HTTP
    // ------------------------------------------------------------------

    /**
     * Appelle {@code /userinfo} et retourne le corps <strong>brut</strong>.
     *
     * <p>On demande {@code String.class} : {@code StringHttpMessageConverter} accepte
     * n'importe quel type media, donc aucune {@code UnknownContentTypeException} —
     * contrairement au service standard de Spring qui exige {@code application/json}.</p>
     */
    private String lireCorps(OidcUserRequest userRequest) {
        String uri = userRequest.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUri();
        if (uri == null || uri.isBlank()) {
            throw echec("UserInfo Endpoint absent de la configuration du client PRC");
        }

        HttpHeaders enTetes = new HttpHeaders();
        enTetes.setBearerAuth(userRequest.getAccessToken().getTokenValue());
        enTetes.setAccept(List.of(MediaType.APPLICATION_JSON));

        SimpleClientHttpRequestFactory fabrique = new SimpleClientHttpRequestFactory();
        fabrique.setConnectTimeout(TIMEOUT_MS);
        fabrique.setReadTimeout(TIMEOUT_MS);
        RestTemplate rest = new RestTemplate(fabrique);

        try {
            ResponseEntity<String> reponse =
                    rest.exchange(RequestEntity.get(URI.create(uri)).headers(enTetes).build(), String.class);
            String corps = reponse.getBody();
            if (corps == null || corps.isBlank()) {
                throw echec("Reponse /userinfo vide (HTTP " + reponse.getStatusCode() + ")");
            }
            log.info("UserInfo PRC : HTTP {} content-type={}, corps de {} caractere(s)",
                    reponse.getStatusCode(),
                    reponse.getHeaders().getContentType(),
                    corps.length());
            return corps;
        } catch (org.springframework.web.client.RestClientException e) {
            // Le corps d'erreur contient souvent le message OAuth2 utile au diagnostic.
            throw echec("Echec de l'appel /userinfo sur " + uri + " : " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Interpretation du corps (JSON / JWS / JWE)
    // ------------------------------------------------------------------

    /**
     * Determine et interprete le format du corps (JSON / JWS / JWE).
     *
     * <p>Visible <strong>package</strong> pour les tests (aucun reseau dans les chemins
     * JSON / JWE / invalide).</p>
     */
    Map<String, Object> interpreter(String corps, OidcUserRequest userRequest) {

        String brut = corps.trim();

        // 1) JSON : l'en-tete etait simplement errone.
        if (brut.startsWith("{")) {
            try {
                Map<String, Object> claims = MAPPER.readValue(brut, TYPE_CORPS);
                log.info("UserInfo PRC : format JSON (content-type errone uniquement) — "
                        + "claims {}", claims.keySet());
                return claims;
            } catch (Exception invalide) {
                throw echec("Corps /userinfo de type JSON mais illisible : " + invalide.getMessage());
            }
        }

        int segments = brut.split("\\.").length;
        if (segments == 5) {
            throw echec("Le /userinfo renvoie un JWT CHIFFRE (JWE, 5 segments). Nous ne "
                    + "pouvons pas le dechiffrer sans cle privee — PRC doit renvoyer du JSON "
                    + "(OIDC §5.2) ou un JWT signe. Header : " + enteteJson(brut));
        }
        if (segments != 3) {
            throw echec("Corps /userinfo non interpretable (ni JSON ni JWT) : "
                    + abruger(brut, 120));
        }

        // 2) JWT signe : on VERIFIE la signature (jamais de lecture sans verification).
        log.info("UserInfo PRC : format JWT-SIGNE (JWS) — header {}", enteteJson(brut));
        return verifierSignature(brut, userRequest);
    }

    private Map<String, Object> verifierSignature(String corps, OidcUserRequest userRequest) {

        String jwksUri = userRequest.getClientRegistration().getProviderDetails().getJwkSetUri();
        RuntimeException dernierEchec = null;

        // Source primaire : le jwks_uri de la discovery (la standard, recommandee par PRC).
        if (jwksUri != null && !jwksUri.isBlank()) {
            try {
                NimbusJwtDecoder decodeur = NimbusJwtDecoder.withJwkSetUri(jwksUri)
                        // Le defaut du builder n'accepte QUE RS256 (piege connu) : on ouvre
                        // l'algorithme annonce par la discovery + les classiques RSA.
                        .jwsAlgorithms(algos -> {
                            algos.add(algorithmeAnnonce());
                            algos.add(SignatureAlgorithm.RS256);
                            algos.add(SignatureAlgorithm.PS256);
                        })
                        .build();
                // On ne garde que la verification de SIGNATURE : une reponse userinfo n'est
                // pas un jeton d'acces, ses horodatages ne sont pas exploitables de la meme
                // facon (et peuvent etre absents).
                decodeur.setJwtValidator(jwt -> OAuth2TokenValidatorResult.success());

                Jwt jwt = decodeur.decode(corps);
                log.info("UserInfo PRC : signature VERIFIEE via jwks_uri (alg {})",
                        jwt.getHeaders().get("alg"));
                return jwt.getClaims();
            } catch (RuntimeException e) {
                dernierEchec = e;
                log.warn("Verification via jwks_uri ({}) impossible : {}", jwksUri, e.getMessage());
            }
        }

        // Source de secours : cle publique fournie par PRC (« en cas de besoin »).
        if (cleSecours != null) {
            try {
                Map<String, Object> claims = verifierAvecCleSecours(corps);
                log.info("UserInfo PRC : signature VERIFIEE via la cle publique de secours");
                return claims;
            } catch (Exception e) {
                dernierEchec = new IllegalStateException(e.getMessage(), e);
                log.warn("Verification via la cle de secours impossible : {}", e.getMessage());
            }
        }

        throw echec("Signature du /userinfo non verifiable ("
                + (dernierEchec != null ? dernierEchec.getMessage() : "jwks_uri absent")
                + "). Header : " + enteteJson(corps));
    }

    private Map<String, Object> verifierAvecCleSecours(String corps) throws Exception {
        SignedJWT jwt = SignedJWT.parse(corps);
        String alg = jwt.getHeader().getAlgorithm() != null
                ? jwt.getHeader().getAlgorithm().getName() : "";

        // RSASSAVerifier couvre TOUTE la famille RSA (RS256/384/512 comme PS256/384/512) :
        // il delegue a RSASSA.getSignerAndVerifier(JWSAlgorithm) qui choisit le bon
        // padding d'apres l'alg de l'en-tete. (Il n'existe PAS de RSASSAPSSVerifier
        // dans Nimbus 10.x — d'ou la classe RSASSAVerifier seule.)
        String famille = alg.toUpperCase(java.util.Locale.ROOT);
        if (!famille.startsWith("RS") && !famille.startsWith("PS")) {
            throw new IllegalStateException("algorithme " + alg
                    + " non pris en charge par la cle de secours RSA");
        }

        JWSVerifier verificateur = new RSASSAVerifier((RSAPublicKey) cleSecours);
        if (!jwt.verify(verificateur)) {
            throw new IllegalStateException("signature invalide (algorithme " + alg + ")");
        }
        log.info("UserInfo PRC : signature de secours verifiee (alg {})", alg);
        return jwt.getJWTClaimsSet().toJSONObject();
    }

    /** Algorithme annonce par PRC dans la discovery (defaut : PS256, sa valeur connue). */
    private SignatureAlgorithm algorithmeAnnonce() {
        try {
            return SignatureAlgorithm.valueOf(depot.getIdTokenSigningAlg());
        } catch (RuntimeException inconnu) {
            return SignatureAlgorithm.PS256;
        }
    }

    // ------------------------------------------------------------------
    // Divers
    // ------------------------------------------------------------------

    private static PublicKey chargerCle(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }
        try {
            String propre = base64.trim().replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(propre);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "PRC_USERINFO_PUBLIC_KEY invalide (attendu : base64 DER sur une seule ligne) : "
                            + e.getMessage(), e);
        }
    }

    /** En-tete JOSE decode (alg, kid, enc...) — ne contient aucune donnee personnelle. */
    private static String enteteJson(String corps) {
        try {
            String entete = corps.split("\\.")[0];
            byte[] decode = Base64.getUrlDecoder().decode(entete);
            return new String(decode, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "(en-tete illisible)";
        }
    }

    private static String abruger(String valeur, int taille) {
        return valeur.length() <= taille ? valeur : valeur.substring(0, taille) + "...";
    }

    private static OAuth2AuthenticationException echec(String message) {
        log.error("UserInfo PRC refuse : {}", message);
        // Meme code que le service standard : notre failureHandler renvoie l'utilisateur
        // sur /connexion?erreur=prc plutot qu'une 500.
        return new OAuth2AuthenticationException(
                new OAuth2Error("invalid_user_info_response", message, null), message);
    }
}
