package fr.dinum.beta.gouv.doctorat.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.dinum.beta.gouv.doctorat.SecurityConfig;
import fr.dinum.beta.gouv.doctorat.config.EuraxessProperties;
import fr.dinum.beta.gouv.doctorat.controller.EuraxessController;
import fr.dinum.beta.gouv.doctorat.service.EuraxessFeedService;

/**
 * Vérifie la protection par API key (header X-API-KEY, clé dédiée
 * {@code EURAXESS_API_KEY}) sur GET /api/euraxess/feed, sur le même principe
 * que l'endpoint d'export /api/export/propositions-these.
 */
@WebMvcTest(EuraxessController.class)
@TestPropertySource(properties = "EURAXESS_API_KEY=test-euraxess-key")
@Import({SecurityConfig.class, ApiKeyFilter.class, EuraxessProperties.class})
class EuraxessFeedSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EuraxessFeedService euraxessFeedService;

	@Test
	void returns401WithoutApiKey() throws Exception {
		mockMvc.perform(get("/api/euraxess/feed"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void returns401WithInvalidApiKey() throws Exception {
		mockMvc.perform(get("/api/euraxess/feed").header("X-API-KEY", "mauvaise-cle"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void returns200WithValidApiKey() throws Exception {
		when(euraxessFeedService.generateFeed()).thenReturn(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><job-opportunities isIncremental=\"false\"/>");

		mockMvc.perform(get("/api/euraxess/feed").header("X-API-KEY", "test-euraxess-key"))
				.andExpect(status().isOk());
	}
}
