package fr.dinum.beta.gouv.doctorat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.config.PrcClientRegistrationRepository;
import fr.dinum.beta.gouv.doctorat.config.PrcProperties;

/**
 * Lecture du {@code /userinfo} de PRC : format (JSON / JWS / JWE), verification de
 * signature et coherence du {@code sub}.
 *
 * <p>Point de depart : en recette PRC renvoie {@code Content-Type: application/jwt}
 * (deviation OIDC §5.2) et son id_token ne contient aucun claim metier — ce service est
 * donc la <strong>seule source</strong> d'identite de nos utilisateurs.</p>
 */
class PrcUserInfoServiceTest {

    private static final PrcClientRegistrationRepository DEPOT =
            new PrcClientRegistrationRepository(new PrcProperties());

    /** Client sans jwkSetUri : force le repli sur la cle de secours. */
    private static ClientRegistration client() {
        return ClientRegistration.withRegistrationId("prc")
                .clientId("client-test")
                .clientSecret("secret-test")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("http://localhost:8080/login/oauth2/code/prc")
                .authorizationUri("http://localhost:9999/authorize")
                .tokenUri("http://localhost:9999/token")
                .build();
    }

    private static OidcUserRequest demande() {
        Instant maintenant = Instant.now();
        OAuth2AccessToken jeton = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER,
                "jeton-acces", maintenant, maintenant.plusSeconds(300),
                Set.of("openid", "anr", "profile"));

        Map<String, Object> claims = new HashMap<>();
        claims.put(IdTokenClaimNames.ISS, "856625");
        claims.put(IdTokenClaimNames.SUB, "sub-id-token");
        claims.put(IdTokenClaimNames.AUD, List.of("client-test"));
        claims.put(IdTokenClaimNames.IAT, maintenant);
        claims.put(IdTokenClaimNames.EXP, maintenant.plusSeconds(300));

        OidcIdToken idToken = new OidcIdToken("jeton-id", maintenant,
                maintenant.plusSeconds(300), claims);

        return new OidcUserRequest(client(), jeton, idToken);
    }

    private static PrcUserInfoService service(String clePubliqueBase64) {
        return new PrcUserInfoService(DEPOT, clePubliqueBase64);
    }

    private static String messageErreur(OAuth2AuthenticationException e) {
        return e.getError().getDescription() != null
                ? e.getError().getDescription() : e.getMessage();
    }

    // ------------------------------------------------------------------
    // Formats de corps
    // ------------------------------------------------------------------

    @Test
    void corps_json_est_lu_meme_si_le_content_type_ment() {
        // Le cas le plus probable : du JSON pur avec un Content-Type errone.
        String corps = "{\"sub\":\"sub-id-token\",\"principalEmail\":\"a@b.fr\",\"state\":\"active\"}";

        Map<String, Object> claims = service("").interpreter(corps, demande());

        assertEquals("a@b.fr", claims.get("principalEmail"));
        assertEquals("active", claims.get("state"));
    }

    @Test
    void corps_jwt_chiffre_produit_une_erreur_explicite() {
        // 5 segments = JWE (chiffre) : impossible sans cle privee.
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> service("").interpreter("aaaa.bbbb.cccc.dddd.eeee", demande()));

        assertTrue(messageErreur(e).contains("CHIFFRE"),
                "L'erreur doit signaler un JWT chiffre, sinon on diagnostique dans le vide");
    }

    @Test
    void corps_ininterpretable_produit_une_erreur_explicite() {
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> service("").interpreter("bonjour tout le monde", demande()));

        assertTrue(messageErreur(e).contains("non interpretable"),
                "Ni JSON ni JWT : l'erreur doit le dire et montrer le debut du corps");
    }

    @Test
    void jwt_sans_aucune_source_de_verification_est_refuse() {
        // Un JWT ne peut JAMAIS etre lu sans verification : c'est le principe meme.
        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> service("").interpreter("aaaa.bbbb.cccc", demande()));

        assertTrue(messageErreur(e).contains("Signature"),
                "Sans jwks_uri ni cle de secours, on refuse plutot que de faire confiance");
    }

    // ------------------------------------------------------------------
    // Verification de signature (le vrai cas PRC : PS256)
    // ------------------------------------------------------------------

    @Test
    void jwt_ps256_est_verifie_avec_la_cle_publique_de_secours() throws Exception {
        KeyPair paire = genererPaire();
        assertNotNull(paire);

        String cleBase64 = Base64.getEncoder().encodeToString(paire.getPublic().getEncoded());
        PrcUserInfoService svc = service(cleBase64);

        Map<String, Object> claimsAttendus = new HashMap<>();
        claimsAttendus.put("sub", "sub-id-token");
        claimsAttendus.put("principalEmail", "a@b.fr");
        String jwt = signerPs256(paire.getPrivate(), claimsAttendus);

        Map<String, Object> claims = svc.interpreter(jwt, demande());

        assertEquals("a@b.fr", claims.get("principalEmail"),
                "La signature PS256 doit etre acceptee via la cle de secours");
    }

    @Test
    void jwt_signe_par_une_autre_cle_est_rejete() throws Exception {
        KeyPair legitime = genererPaire();
        KeyPair attaquant = genererPaire();

        String cleLegitime = Base64.getEncoder().encodeToString(legitime.getPublic().getEncoded());
        PrcUserInfoService svc = service(cleLegitime);

        // Signe par une AUTRE cle que celle fournie.
        String jwt = signerPs256(attaquant.getPrivate(),
                Map.of("sub", "sub-id-token", "principalEmail", "a@b.fr"));

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> svc.interpreter(jwt, demande()));
        assertTrue(messageErreur(e).contains("Signature"),
                "Une signature etrangere doit etre refusee");
    }

    // ------------------------------------------------------------------
    // Construction de l'OidcUser (coherence sub + fusion des claims)
    // ------------------------------------------------------------------

    @Test
    void sub_identique_construit_l_utilisateur_avec_les_deux_sources_de_claims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "sub-id-token");       // identique a l'id_token
        claims.put("principalEmail", "a@b.fr");
        claims.put("state", "active");

        OidcUser utilisateur = service("").construireUtilisateur(claims, demande());

        assertEquals("sub-id-token", utilisateur.getSubject());
        // Les claims PRC viennent du userinfo, ceux de l'id_token sont conserves.
        assertEquals("a@b.fr", utilisateur.getClaimAsString("principalEmail"));
        assertNotNull(utilisateur.getClaimAsString("iss"));
    }

    @Test
    void sub_different_de_l_id_token_est_refuse() {
        // OIDC Core §16.11 : sans cela, un userinfo volerait l'identite d'un autre.
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "autre-sub");
        claims.put("principalEmail", "a@b.fr");

        OAuth2AuthenticationException e = assertThrows(OAuth2AuthenticationException.class,
                () -> service("").construireUtilisateur(claims, demande()));
        assertTrue(messageErreur(e).contains("different"),
                "Un sub divergent entre id_token et userinfo doit etre refuse");
    }

    @Test
    void sub_absent_du_userinfo_est_refuse() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("principalEmail", "a@b.fr");

        assertThrows(OAuth2AuthenticationException.class,
                () -> service("").construireUtilisateur(claims, demande()));
    }

    // ------------------------------------------------------------------
    // Cle de secours (base64 DER)
    // ------------------------------------------------------------------

    @Test
    void cle_publique_absente_est_acceptee() {
        // Sans cle : le service se construit, il utilisera le jwks_uri.
        assertNotNull(service(""));
        assertNotNull(service(null));
        assertNotNull(service("   "));
    }

    @Test
    void cle_publique_invalide_leve_une_erreur_claire() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> service("ce-n'est-pas-du-base64!"));
        assertTrue(e.getMessage().contains("PRC_USERINFO_PUBLIC_KEY"),
                "L'erreur doit pointer vers la variable a corriger");
    }

    // ------------------------------------------------------------------
    // Utilitaires
    // ------------------------------------------------------------------

    private static KeyPair genererPaire() throws Exception {
        KeyPairGenerator generateur = KeyPairGenerator.getInstance("RSA");
        generateur.initialize(2048);
        return generateur.generateKeyPair();
    }

    /** Signe un JWT en PS256 (RFC 7518 : RSASSA-PSS, SHA-256, sel de 32 octets). */
    private static String signerPs256(PrivateKey cle, Map<String, Object> claims) throws Exception {
        String entete = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"PS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.US_ASCII));
        String corps = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(new ObjectMapper().writeValueAsBytes(claims));
        String message = entete + "." + corps;

        Signature signature = Signature.getInstance("RSASSA-PSS");
        signature.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1));
        signature.initSign(cle);
        signature.update(message.getBytes(StandardCharsets.US_ASCII));

        String signe = Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        return message + "." + signe;
    }
}
