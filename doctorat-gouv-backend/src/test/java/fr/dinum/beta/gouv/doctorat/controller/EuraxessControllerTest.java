package fr.dinum.beta.gouv.doctorat.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.dinum.beta.gouv.doctorat.config.EuraxessProperties;
import fr.dinum.beta.gouv.doctorat.service.EuraxessEmptyFeedException;
import fr.dinum.beta.gouv.doctorat.service.EuraxessFeedService;

/**
 * Test d'intégration du contrôleur EURAXESS : 200 text/xml quand le flux a
 * des offres, 204 No Content quand aucune offre (ou erreur interne) — aucun
 * XML vide émis (non conforme au XSD).
 */
@WebMvcTest(EuraxessController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(EuraxessProperties.class)
class EuraxessControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EuraxessFeedService euraxessFeedService;

	@Test
	void returnsXmlFeed() throws Exception {
		when(euraxessFeedService.generateFeed()).thenReturn(
				"<?xml version=\"1.0\" encoding=\"UTF-8\"?><job-opportunities isIncremental=\"false\"/>");

		mockMvc.perform(get("/api/euraxess/feed"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_XML))
				.andExpect(content().string(
						org.hamcrest.Matchers.containsString("<job-opportunities")));
	}

	@Test
	void returnsNoContentWhenNoOffer() throws Exception {
		when(euraxessFeedService.generateFeed())
				.thenThrow(new EuraxessEmptyFeedException("Aucune offre active à publier"));

		mockMvc.perform(get("/api/euraxess/feed"))
				.andExpect(status().isNoContent());
	}

	@Test
	void returnsNoContentOnInternalError() throws Exception {
		when(euraxessFeedService.generateFeed()).thenThrow(new IllegalStateException("boom"));

		mockMvc.perform(get("/api/euraxess/feed"))
				.andExpect(status().isNoContent());
	}
}
