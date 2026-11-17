package fr.dinum.beta.gouv.doctorat.security;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Résolution de la clé de limitation : <strong>adresse IP</strong> uniquement
 * (décision projet : pas de limitation par utilisateur JWT en v1).
 *
 * <p>Prend en compte {@code X-Forwarded-For} (proxy / Scalingo) avec repli sur
 * {@code remoteAddr}. La première IP de la chaîne {@code X-Forwarded-For} est
 * retenue (client réel).</p>
 */
@Component
public class RateLimitKeyResolver {

	private static final String X_FORWARDED_FOR = "X-Forwarded-For";

	/**
	 * Retourne l'adresse IP utilisée comme clé de limitation.
	 */
	public String resolve(HttpServletRequest request) {
		String forwarded = request.getHeader(X_FORWARDED_FOR);
		if (forwarded != null && !forwarded.isBlank()) {
			int comma = forwarded.indexOf(',');
			String first = comma > 0 ? forwarded.substring(0, comma) : forwarded;
			first = first.trim();
			if (!first.isEmpty()) {
				return first;
			}
		}
		String remote = request.getRemoteAddr();
		return remote != null ? remote : "unknown";
	}
}
