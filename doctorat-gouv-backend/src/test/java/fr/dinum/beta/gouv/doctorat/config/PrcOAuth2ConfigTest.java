package fr.dinum.beta.gouv.doctorat.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.ResolvableType;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;

/**
 * Decouverte du bean {@code JwtDecoderFactory<ClientRegistration>} par Spring Security.
 *
 * <p><strong>Pourquoi ce test ?</strong> en recette le 29/10/2026, chaque connexion PRC
 * echouait :</p>
 *
 * <pre>
 * [invalid_id_token] Signed JWT rejected: Another algorithm expected,
 * or no matching key(s) found
 * </pre>
 *
 * <p>Car {@code OidcIdTokenDecoderFactory} (Spring Security 7) verifie en <strong>RS256</strong>
 * alors que PRC annonce <strong>PS256</strong>. Le correctif est le bean
 * {@code prcIdTokenDecoderFactory} de {@link PrcOAuth2Config} — mais Spring ne le recupere
 * que s'il est <strong>unique</strong> :
 * {@code OAuth2LoginConfigurer.getJwtDecoderFactoryBean()} fait un {@code getIfUnique()},
 * et retombe <strong>silencieusement</strong> sur le RS256 par defaut si le bean est absent.
 * Une erreur de ce type ne se voit donc QU'a la connexion reelle.</p>
 *
 * <p>Ce test reproduit exactement la resolution de Spring, sans contexte web ni BDD
 * et <strong>sans aucun appel reseau</strong> (la decouverte OIDC est paresseuse).</p>
 */
class PrcOAuth2ConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PrcProperties.class, PrcOAuth2Config.class);

    /** Type tel que Spring le cherche ({@code OAuth2LoginConfigurer.getJwtDecoderFactoryBean}). */
    private static final ResolvableType TYPE_DECODEUR =
            ResolvableType.forClassWithGenerics(JwtDecoderFactory.class, ClientRegistration.class);

    /**
     * Compte les beans {@code JwtDecoderFactory<ClientRegistration>} — exactement ce que
     * Spring fait via {@code getBeanNamesForType(type)} puis {@code getIfUnique()}.
     *
     * <p>{@code run()} ne renvoie rien : on capture dans un {@link AtomicReference}.</p>
     */
    private int compterDecodeurs(ApplicationContextRunner cible) {
        AtomicReference<Integer> compte = new AtomicReference<>();
        cible.run(context -> compte.set(context.getBeanNamesForType(TYPE_DECODEUR).length));
        return compte.get();
    }

    @Test
    void prc_actif_le_bean_decodeur_est_unique_et_trouve() {
        int nb = compterDecodeurs(runner.withPropertyValues("prc.issuer-uri=http://localhost:9999"));

        assertEquals(1, nb,
                "Spring doit trouver EXACTEMENT UN JwtDecoderFactory<ClientRegistration> : "
                        + "0 (ou plusieurs) et il valide en RS256 par defaut, donc rejette "
                        + "l'id_token PRC signe en PS256");
    }

    /**
     * Le service OIDC qui n'appelle pas {@code /userinfo} doit lui aussi etre <strong>unique</strong> :
     * {@code OAuth2LoginConfigurer} cherche un bean {@code OAuth2UserService<OidcUserRequest, OidcUser>}
     * via {@code getIfUnique()} et retombe silencieusement sur le service par defaut (celui qui
     * plante sur le {@code Content-Type: application/jwt} de PRC) s'il n'en trouve pas.
     */
    @Test
    void prc_actif_le_service_oidc_sans_userinfo_est_unique() {
        AtomicReference<Integer> compte = new AtomicReference<>();
        ResolvableType type = ResolvableType.forClassWithGenerics(
                OAuth2UserService.class, OidcUserRequest.class, OidcUser.class);

        runner.withPropertyValues("prc.issuer-uri=http://localhost:9999")
                .run(context -> compte.set(context.getBeanNamesForType(type).length));

        assertEquals(1, compte.get(),
                "Spring doit trouver UN SEUL OAuth2UserService<OidcUserRequest, OidcUser>");
    }

    @Test
    void prc_inactif_aucun_bean_inutile() {
        // On force les deux proprietes a vide : un PRC_ISSUER_URI present dans
        // l'environnement de la machine ne doit pas activer le module par accident.
        runner.withPropertyValues("prc.issuer-uri=", "PRC_ISSUER_URI=").run(context -> {
            assertEquals(0, context.getBeanNamesForType(PrcClientRegistrationRepository.class).length,
                    "Sans config PRC, aucun depot OAuth2 ne doit exister (design optionnel)");
            assertEquals(0, context
                            .getBeanNamesForType(ResolvableType
                                    .forClassWithGenerics(JwtDecoderFactory.class, ClientRegistration.class))
                            .length,
                    "Sans config PRC, aucun decodeur id_token ne doit exister");
        });
    }

    @Test
    void algorithme_signature_defaut_avant_decouverte() {
        runner.withPropertyValues("prc.issuer-uri=http://localhost:9999").run(context -> {
            PrcClientRegistrationRepository depot =
                    context.getBean(PrcClientRegistrationRepository.class);
            // PS256 = algorithme annonce par PRC, utilise tant que la discovery n'a pas
            // donne le chiffre reel (le faux IdP local annonce RS256).
            assertEquals("PS256", depot.getIdTokenSigningAlg());
        });
    }

    @Test
    void demarrage_sans_appel_reseau() {
        runner.withPropertyValues("prc.issuer-uri=http://localhost:9999").run(context -> {
            PrcClientRegistrationRepository depot =
                    context.getBean(PrcClientRegistrationRepository.class);
            // Exigence fondamentale : l'application doit demarrer et rester debout
            // meme si PRC est injoignable. La decouverte n'a lieu qu'au premier clic.
            assertFalse(depot.isReady(),
                    "Aucune decouverte OIDC ne doit avoir eu lieu au demarrage");
        });
    }
}
