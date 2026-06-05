package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.SujetEmbedding;

@Repository
public interface SujetEmbeddingRepository extends JpaRepository<SujetEmbedding, Long> {

	@Query(value = """
		SELECT id, proposition_these_id, bloc_type, contenu,
		       1 - (embedding <=> CAST(:queryVector AS vector)) AS score
		FROM sujet_embedding
		ORDER BY embedding <=> CAST(:queryVector AS vector)
		LIMIT :limit
		""", nativeQuery = true)
	List<Object[]> findNearestByVector(@Param("queryVector") String queryVector,
									   @Param("limit") int limit);

	@Query(value = """
		SELECT proposition_these_id,
		       MAX(1 - (embedding <=> CAST(:queryVector AS vector))) AS best_score,
		       MAX(bloc_type) AS best_bloc_type
		FROM sujet_embedding
		GROUP BY proposition_these_id
		ORDER BY best_score DESC
		LIMIT :limit
		""", nativeQuery = true)
	List<Object[]> findBestScoreBySubject(@Param("queryVector") String queryVector,
										  @Param("limit") int limit);

	List<SujetEmbedding> findByPropositionTheseIdIn(List<Long> ids);

	void deleteByPropositionTheseId(Long propositionTheseId);
}
