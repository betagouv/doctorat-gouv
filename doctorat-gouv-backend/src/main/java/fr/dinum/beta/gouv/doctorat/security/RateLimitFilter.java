package fr.dinum.beta.gouv.doctorat.security;

import java.io.IOException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import fr.dinum.beta.gouv.doctorat.config.RateLimitProperties;
import fr.dinum.beta.gouv.doctorat.config.RateLimitProperties.Rule;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtre de rate limiting (Bucket4j, « token bucket ») ciblé sur certains endpoints.
 *
 * <p>Comportement :</p>
 * <ul>
 *   <li>inactif si {@code rate-limit.enabled=false} (tests) ;</li>
 *   <li>ne s'applique qu'aux chemins couverts par une règle {@code rate-limit.rules} ;</li>
 *   <li>clé = adresse IP (via {@link RateLimitKeyResolver}) ;</li>
 *   <li>buckets en mémoire (Caffeine, expiration automatique) ;</li>
 *   <li>réponse {@code 429} + {@code Retry-After} si le budget est épuisé.</li>
 * </ul>
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

	private final RateLimitProperties properties;
	private final RateLimitKeyResolver keyResolver;
	private final AntPathMatcher pathMatcher = new AntPathMatcher();

	/** Buckets par « chemin|IP ». Expiration après 10 min d'inactivité, max 100k entrées. */
	private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
			.maximumSize(100_000)
			.expireAfterAccess(Duration.ofMinutes(10))
			.build();

	public RateLimitFilter(RateLimitProperties properties, RateLimitKeyResolver keyResolver) {
		this.properties = properties;
		this.keyResolver = keyResolver;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		if (!properties.isEnabled()) {
			return true;
		}
		String uri = request.getRequestURI();
		if (isStaticResource(uri)) {
			return true;
		}
		return findRule(uri) == null;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request,
									HttpServletResponse response,
									FilterChain filterChain)
			throws ServletException, IOException {

		String uri = request.getRequestURI();
		Rule rule = findRule(uri);
		if (rule == null) {
			filterChain.doFilter(request, response);
			return;
		}

		String ip = keyResolver.resolve(request);
		String bucketKey = rule.getPath() + "|" + ip;
		Bucket bucket = buckets.get(bucketKey, k -> createBucket(rule));

		ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
		if (probe.isConsumed()) {
			// Headers informatifs (optionnels) puis passage de la requête.
			response.setHeader("X-RateLimit-Limit", String.valueOf(rule.getCapacity()));
			response.setHeader("X-RateLimit-Remaining", String.valueOf(probe.getRemainingTokens()));
			filterChain.doFilter(request, response);
		} else {
			long nanosToWait = probe.getNanosToWaitForRefill();
			long secondsToWait = Math.max(1, (nanosToWait + 999_999_999L) / 1_000_000_000L);
			log.warn("Rate limit dépassé sur {} pour IP {} (Retry-After {}s)", uri, ip, secondsToWait);

			response.setStatus(429);
			response.setHeader("Retry-After", String.valueOf(secondsToWait));
			response.setContentType("application/json;charset=UTF-8");
			response.getWriter().write(String.format(
					"{\"error\":\"Too Many Requests\",\"message\":\"Trop de requêtes, réessayez dans %ds\",\"retryAfterSeconds\":%d}",
					secondsToWait, secondsToWait));
		}
	}

	/**
	 * Trouve la première règle correspondant au chemin (joker {@code **} supporté).
	 */
	private Rule findRule(String uri) {
		if (uri == null) {
			return null;
		}
		for (Rule rule : properties.getRules()) {
			if (rule.getPath() != null && pathMatcher.match(rule.getPath(), uri)) {
				return rule;
			}
		}
		return null;
	}

	private static boolean isStaticResource(String uri) {
		return uri.startsWith("/assets/")
				|| uri.startsWith("/browser/")
				|| uri.equals("/favicon.ico")
				|| uri.equals("/sitemap.xml")
				|| uri.equals("/robots.txt");
	}

	private static Bucket createBucket(Rule rule) {
		Bandwidth limit = Bandwidth.builder()
				.capacity(rule.getCapacity())
				.refillGreedy(rule.getRefillTokens(), Duration.ofSeconds(rule.getRefillPeriodSeconds()))
				.build();
		return Bucket.builder().addLimit(limit).build();
	}
}
