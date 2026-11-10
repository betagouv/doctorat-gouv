package fr.dinum.beta.gouv.doctorat.security;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import fr.dinum.beta.gouv.doctorat.SecurityConfig;
import fr.dinum.beta.gouv.doctorat.controller.ScalewaySearchController;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.ScalewayQueryLogRepository;
import fr.dinum.beta.gouv.doctorat.repository.SujetEmbeddingRepository;
import fr.dinum.beta.gouv.doctorat.service.EmbeddingIndexationService;
import fr.dinum.beta.gouv.doctorat.service.PropositionTheseService;
import fr.dinum.beta.gouv.doctorat.service.SearchRerankerService;
import fr.dinum.beta.gouv.doctorat.service.VectorSearchService;

/**
 * Vérifie la protection par API key (header X-API-KEY, clé dédiée
 * {@code SCALEWAY_INDEX_API_KEY}) sur les endpoints d'administration de l'index
 * vectoriel Scaleway, et le refus par défaut pour toute route API non déclarée.
 */
@WebMvcTest(ScalewaySearchController.class)
@TestPropertySource(properties = "SCALEWAY_INDEX_API_KEY=test-scaleway-index-key")
@Import({SecurityConfig.class, ApiKeyFilter.class})
class ScalewayIndexSecurityTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VectorSearchService vectorSearchService;

	@MockitoBean
	private PropositionTheseService propositionService;

	@MockitoBean
	private SearchRerankerService rerankerService;

	@MockitoBean
	private SujetEmbeddingRepository sujetEmbeddingRepository;

	@MockitoBean
	private PropositionTheseRepository propositionTheseRepository;

	@MockitoBean
	private EmbeddingIndexationService indexationService;

	@MockitoBean
	private ScalewayQueryLogRepository scalewayQueryLogRepository;

	@Test
	void indexDeleteReturns401WithoutApiKey() throws Exception {
		mockMvc.perform(post("/api/scaleway/index/delete"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void indexDeleteReturns401WithInvalidApiKey() throws Exception {
		mockMvc.perform(post("/api/scaleway/index/delete").header("X-API-KEY", "mauvaise-cle"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void indexDeleteReturns200WithValidApiKey() throws Exception {
		org.mockito.Mockito.when(sujetEmbeddingRepository.findAll()).thenReturn(java.util.List.of());
		mockMvc.perform(post("/api/scaleway/index/delete").header("X-API-KEY", "test-scaleway-index-key"))
				.andExpect(status().isOk());
	}

	@Test
	void unknownApiRouteReturns403() throws Exception {
		mockMvc.perform(post("/api/scaleway/unknown-endpoint"))
				.andExpect(status().isForbidden());
	}
}