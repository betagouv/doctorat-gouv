package fr.dinum.beta.gouv.doctorat.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Declaration du client OIDC Projets Recherche Connect (PRC).
 *
 * <p><strong>Optionnel</strong> : cette configuration n'est prise en compte que si PRC est actif
 * (variable {@code PRC_ISSUER_URI} renseignee). Sans elle, aucun {@link ClientRegistrationRepository}
 * n'existe et {@code .oauth2Login()} n'est pas branche dans {@code SecurityConfig} — l'application
 * demarre et fonctionne exactement comme avant.</p>
 *
 * <p>Le client est reconstruit par decouverte OIDC ({@code /.well-known/openid-configuration})
 * plutot que par saisie manuelle des endpoints : la signature des tokens est alors laissee au
 * defaut de Spring Security via {@code jwks_uri}, conformement a la spec technique PRC.</p>
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
