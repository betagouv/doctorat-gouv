package fr.dinum.beta.gouv.doctorat.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import fr.dinum.beta.gouv.doctorat.config.RateLimitProperties;
import fr.dinum.beta.gouv.doctorat.config.RateLimitProperties.Rule;

/**
 * Tests unitaires du {@link RateLimitFilter} : vérifie que le budget de
 * requêtes est respecté et que la réponse 429 contient Retry-After.
 */
class RateLimitFilterTest {

	private RateLimitFilter filter;

	@BeforeEach
	void setUp() {
		RateLimitProperties properties = new RateLimitProperties();
		properties.setEnabled(true);

		Rule connexion = new Rule();
		connexion.setPath("/api/connexion");
		connexion.setCapacity(3);
		connexion.setRefillTokens(3);
		connexion.setRefillPeriodSeconds(60);
		properties.setRules(List.of(connexion));

		filter = new RateLimitFilter(properties, new RateLimitKeyResolver());
	}

	private MockHttpServletResponse perform(String uri) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", uri);
		request.setRemoteAddr("10.0.0.1");
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		return response;
	}

	@Test
	void allowsRequestsWithinLimit() throws Exception {
		assertEquals(200, perform("/api/connexion").getStatus());
		assertEquals(200, perform("/api/connexion").getStatus());
		assertEquals(200, perform("/api/connexion").getStatus());
	}

	@Test
	void blocksRequestOverLimitWith429() throws Exception {
		perform("/api/connexion");
		perform("/api/connexion");
		perform("/api/connexion");

		MockHttpServletResponse blocked = perform("/api/connexion");
		assertEquals(429, blocked.getStatus());
		assertTrue(blocked.getHeader("Retry-After") != null
				&& Long.parseLong(blocked.getHeader("Retry-After")) >= 1);
		assertTrue(blocked.getContentAsString().contains("Too Many Requests"));
	}

	@Test
	void doesNotApplyToUnmatchedPath() throws Exception {
		assertEquals(200, perform("/api/propositions-these").getStatus());
	}

	@Test
	void disabledPropertiesSkipFiltering() throws Exception {
		RateLimitProperties off = new RateLimitProperties();
		off.setEnabled(false);
		RateLimitFilter disabled = new RateLimitFilter(off, new RateLimitKeyResolver());

		for (int i = 0; i < 10; i++) {
			MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/connexion");
			request.setRemoteAddr("10.0.0.1");
			MockHttpServletResponse response = new MockHttpServletResponse();
			disabled.doFilter(request, response, new MockFilterChain());
			assertEquals(200, response.getStatus());
		}
	}

	@Test
	void separateIpsGetSeparateBudgets() throws Exception {
		perform("/api/connexion");
		perform("/api/connexion");
		perform("/api/connexion");

		MockHttpServletRequest otherIp = new MockHttpServletRequest("POST", "/api/connexion");
		otherIp.setRemoteAddr("10.0.0.2");
		MockHttpServletResponse otherResponse = new MockHttpServletResponse();
		filter.doFilter(otherIp, otherResponse, new MockFilterChain());
		assertEquals(200, otherResponse.getStatus());
	}
}
