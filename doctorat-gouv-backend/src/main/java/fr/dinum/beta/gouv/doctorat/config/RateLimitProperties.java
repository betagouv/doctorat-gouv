package fr.dinum.beta.gouv.doctorat.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du rate limiting (Bucket4j) par endpoint.
 *
 * <p>Chaque règle définit un chemin (avec joker {@code **}) et les paramètres du
 * « token bucket » : capacité maximale et rechargement automatique. La clé de
 * limitation est <strong>toujours l'adresse IP</strong> (voir
 * {@link fr.dinum.beta.gouv.doctorat.security.RateLimitKeyResolver}).</p>
 *
 * <p>Exemple :</p>
 * <pre>
 * rate-limit.enabled=true
 * rate-limit.rules[0].path=/api/connexion
 * rate-limit.rules[0].capacity=5
 * rate-limit.rules[0].refill-tokens=5
 * rate-limit.rules[0].refill-period-seconds=60
 * </pre>
 *
 * <p>La désactivation globale ({@code rate-limit.enabled=false}) est utilisée en test.</p>
 */
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

	/** Activation globale du rate limiting. */
	private boolean enabled = true;

	/** Liste des règles par chemin. */
	private List<Rule> rules = new ArrayList<>();

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	/**
	 * Une règle de rate limiting : chemin ciblé + paramètres du bucket.
	 */
	public static class Rule {

		/** Chemin (joker {@code **} autorisé, ex. {@code /api/albert/**}). */
		private String path;

		/** Nombre maximal de requêtes dans la fenêtre (capacité du bucket). */
		private long capacity = 10;

		/** Nombre de jetons ajoutés à chaque rechargement. */
		private long refillTokens = 10;

		/** Période de rechargement en secondes. */
		private long refillPeriodSeconds = 60;

		public String getPath() {
			return path;
		}

		public void setPath(String path) {
			this.path = path;
		}

		public long getCapacity() {
			return capacity;
		}

		public void setCapacity(long capacity) {
			this.capacity = capacity;
		}

		public long getRefillTokens() {
			return refillTokens;
		}

		public void setRefillTokens(long refillTokens) {
			this.refillTokens = refillTokens;
		}

		public long getRefillPeriodSeconds() {
			return refillPeriodSeconds;
		}

		public void setRefillPeriodSeconds(long refillPeriodSeconds) {
			this.refillPeriodSeconds = refillPeriodSeconds;
		}
	}
}
