package fr.dinum.beta.gouv.doctorat.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.VectorSearchHit;
import fr.dinum.beta.gouv.doctorat.repository.SujetEmbeddingRepository;

@Service
public class VectorSearchService {

	private static final Logger log = LoggerFactory.getLogger(VectorSearchService.class);

	private final ScalewayEmbeddingService scalewayEmbeddingService;
	private final SujetEmbeddingRepository sujetEmbeddingRepository;

	public VectorSearchService(ScalewayEmbeddingService scalewayEmbeddingService,
							   SujetEmbeddingRepository sujetEmbeddingRepository) {
		this.scalewayEmbeddingService = scalewayEmbeddingService;
		this.sujetEmbeddingRepository = sujetEmbeddingRepository;
	}

	public List<VectorSearchHit> search(String query, int limit) {
		log.info("Recherche vectorielle : query=\"{}\", limit={}", query, limit);

		float[] queryVector = scalewayEmbeddingService.embed(query);
		if (queryVector == null) {
			log.warn("Impossible d'embedder la requête (réponse null de Scaleway)");
			return List.of();
		}

		String vectorStr = Arrays.toString(queryVector);
		List<Object[]> results = sujetEmbeddingRepository.findBestScoreBySubject(vectorStr, limit);

		List<VectorSearchHit> hits = results.stream()
			.map(row -> new VectorSearchHit(
				((Number) row[0]).longValue(),
				((Number) row[1]).doubleValue(),
				(String) row[2],
				null
			))
			.collect(Collectors.toList());

		log.info("Recherche vectorielle : {} résultat(s) trouvé(s)", hits.size());
		return hits;
	}
}
