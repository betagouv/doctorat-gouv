package fr.dinum.beta.gouv.doctorat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;

import fr.dinum.beta.gouv.doctorat.security.PrcIdTokenValidator;

/**
 * Declaration du client OIDC Projets Recherche Connect (PRC).
 *
 * <p><strong>Optionnel</strong> : cette configuration n'est prise en compte que si PRC est actif
 * (variable {@code PRC_ISSUER_URI} renseignee). Sans elle, aucun {@link ClientRegistrationRepository}
 * n'existe et {@code .oauth2Login()} n'est pas branche dans {@code SecurityConfig} — l'application
 * demarre et fonctionne exactement comme avant.</p>
 *
 * <p>Le client est reconstruit par decouverte OIDC ({@code /.well-known/openid-configuration})
 * plutot que par saisie manuelle des endpoints : la signature des id_token est verifiee via le
 * {@code jwks_uri} de la discovery.</p>
 *
 * <p>⚠️ <strong>L'algorithme de signature ne peut PAS etre laisse au defaut de Spring</strong> :
 * {@code OidcIdTokenDecoderFactory} impose {@code RS256} alors que PRC annonce
 * {@code PS256} — d'ou le bean {@code prcIdTokenDecoderFactory} qui lit l'algorithme
 * dans la discovery (voir {@code invalid_id_token} recette 29/10/2026).</p>
 *
 * <p>⚠️ La decouverte est un appel reseau : il est <strong>deferre au premier clic</strong> sur
 * le bouton PRC via {@link PrcClientRegistrationRepository}, pour que l'application demarre
 * meme si PRC est injoignable ou mal configure.</p>
 */
@Configuration(proxyBeanMethods = false)
@Conditional(PrcOAuth2Config.PrcActifCondition.class)
public class PrcOAuth2Config {

	/** Identifiant du client dans Spring Security (utilise dans l'URL de retour). */
	public static final String REGISTRATION_ID = "prc";

	/**
	 * URL de retour enregistree chez PRC : {@code {baseUrl}/login/oauth2/code/prc}.
	 * En local : {@code http://localhost:8080/login/oauth2/code/prc}.
	 */
	public static final String REDIRECT_URI = "{baseUrl}/login/oauth2/code/prc";

	/**
	 * Depot paresseux unique : AUCUN appel reseau au demarrage. La decouverte OIDC n'a lieu
	 * qu'au premier clic sur le bouton PRC, et une erreur chez PRC n'affecte alors
	 * que la tentative de connexion, jamais le boot de l'application.
	 *
	 * <p>Le type retourne est la classe concrete : une seule definition de bean est alors
	 * enregistree, injectable comme {@link PrcClientRegistrationRepository} (PrcLoginController)
	 * <strong>et</strong> comme {@link ClientRegistrationRepository} (SecurityFilterChain).
	 * Deux {@code @Bean} distincts creeraient deux definitions de meme type et une erreur
	 * d'ambiguite a l'injection.</p>
	 */
	@Bean
	public PrcClientRegistrationRepository clientRegistrationRepository(PrcProperties prc) {
		return new PrcClientRegistrationRepository(prc);
	}

	/**
	 * Verificateur d'id_token : indique a Spring <strong>l'algorithme de signature reel</strong>
	 * annonce par la discovery, au lieu du RS256 codé en dur.
	 *
	 * <p><strong>Pourquoi c'est indispensable :</strong> par defaut,
	 * {@code OidcIdTokenDecoderFactory} resout toujours
	 * {@code SignatureAlgorithm.RS256}. Or PRC annonce
	 * {@code id_token_signing_alg_values_supported: ["PS256"]}. Le decodage echoue alors
	 * a chaque connexion :</p>
	 *
	 * <pre>
	 * [invalid_id_token] An error occurred while attempting to decode the Jwt:
	 * Signed JWT rejected: Another algorithm expected, or no matching key(s) found
	 * </pre>
	 *
	 * <p><strong>Point de branchement officiel</strong> :
	 * {@code OAuth2LoginConfigurer.getJwtDecoderFactoryBean()} recherche un bean de type
	 * {@code JwtDecoderFactory<ClientRegistration>} et appelle
	 * {@code OidcAuthorizationCodeAuthenticationProvider.setJwtDecoderFactory(...)}.
	 * Il doit être <strong>unique</strong> ({@code getIfUnique()}) — c'est le cas ici.</p>
	 *
	 * <p>L'algorithme vient de la discovery (et non d'une constante) : le faux IdP local,
	 * qui annonce {@code RS256}, fonctionne alors avec <strong>le même code</strong>.</p>
	 *
	 * <p>Aucun impact hors PRC : ce bean n'existe que si PRC est actif, et
	 * {@code .oauth2Login()} n'est branche que dans ce meme cas.</p>
	 */
	@Bean
	public JwtDecoderFactory<ClientRegistration> prcIdTokenDecoderFactory(
			PrcClientRegistrationRepository depot) {

		OidcIdTokenDecoderFactory factory = new OidcIdTokenDecoderFactory();

		// 1) Algorithme de signature : PRC annonce PS256, Spring impose RS256.
		factory.setJwsAlgorithmResolver(clientRegistration -> {
			String annonce = depot.getIdTokenSigningAlg();
			try {
				return SignatureAlgorithm.valueOf(annonce);
			} catch (IllegalArgumentException inconnu) {
				// Algorithme annonce mais absent de l'enum Spring : on ne bloque pas la
				// connexion pour autant, on retombe sur PRC et on trace pour le diagnostic.
				org.slf4j.LoggerFactory.getLogger(PrcOAuth2Config.class)
						.warn("Algorithme de signature id_token inconnu de Spring Security : {} "
								+ "(discovery) — repli sur PS256", annonce, inconnu);
				return SignatureAlgorithm.PS256;
			}
		});

		// 2) Validation des claims : OidcIdTokenValidator parse le claim "iss" en URL,
		//    alors que PRC y met un identifiant numerique ("856625") -> IllegalArgumentException
		//    et une 500 au lieu d'un echec d'authentification propre. On remplace donc le
		//    validateur (classe final), en conservant TOUS ses controles.
		factory.setJwtValidatorFactory(clientRegistration -> new DelegatingOAuth2TokenValidator<>(
				// Validite temporelle (exp/iat, skew 60 s) : on garde l'implementation Spring.
				new JwtTimestampValidator(),
				new PrcIdTokenValidator(
						clientRegistration.getClientId(),
						depot.getIssuerOrigine(),
						clientRegistration.getProviderDetails().getIssuerUri())));

		return factory;
	}

	/**
	 * Service OIDC qui <strong>n'appelle pas le endpoint {@code /userinfo}</strong>.
	 *
	 * <p><strong>Pourquoi ?</strong> PRC renvoie {@code Content-Type: application/jwt}
	 * alors que nous demandons explicitement {@code Accept: application/json}
	 * ({@code OAuth2UserRequestEntityConverter}). Spring leve alors :</p>
	 *
	 * <pre>
	 * [invalid_user_info_response] response contains invalid content type
	 * 'application/jwt;charset=UTF-8'
	 * </pre>
	 *
	 * <p>C'est une deviation a <strong>OIDC Core 1.0 §5.2</strong> (« MUST return a
	 * Content-Type of application/json ») — et a la propre documentation de PRC, qui
	 * annonce un objet JSON. Question posee a leur equipe.</p>
	 *
	 * <p><strong>On ne lit donc que l'{@code id_token}</strong>, dont les claims sont
	 * eux aussi signes par PRC et valides (signature + iss + aud). Si un claim manque
	 * ({@code principalEmail}, {@code state}, {@code type}...), {@link
	 * fr.dinum.beta.gouv.doctorat.service.PrcAccountService} logue
	 * « Claims PRC recus » puis refuse proprement : on verra alors quoi que manque.</p>
	 *
	 * <p>🔒 <strong>Échapatoire</strong> : si PRC corrige son {@code /userinfo} en JSON,
	 * activer sans redeployer le code :
	 * {@code PRC_USERINFO_ENABLED=true} (voir {@code prc.userinfo.enabled}).</p>
	 */
	@Bean
	public OAuth2UserService<OidcUserRequest, OidcUser> prcOidcUserService(
			@Value("${prc.userinfo.enabled:false}") boolean userInfoActive) {

		OidcUserService service = new OidcUserService();
		// false = ignorer /userinfo (reponse non conforme). true = comportement standard.
		service.setRetrieveUserInfo(request -> userInfoActive);
		return service;
	}

	/**
	 * Active la configuration seulement si PRC est renseigne.
	 *
	 * <p>Un {@code @ConditionOnProperty} classique ne garantit pas la resolution detendue d'une
	 * variable d'environnement ({@code PRC_ISSUER_URI} → {@code prc.issuer-uri}) ; on lit donc
	 * l'environnement explicitement, avec repli sur le nom de la variable brute.</p>
	 */
	static class PrcActifCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			Environment env = context.getEnvironment();
			return !value(env, "prc.issuer-uri").isBlank() || !value(env, "PRC_ISSUER_URI").isBlank();
		}

		private static String value(Environment env, String key) {
			String v = env.getProperty(key);
			return v == null ? "" : v.trim();
		}
	}
}
