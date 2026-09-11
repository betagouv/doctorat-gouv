package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;

@Repository
public interface DemandeMiseEnRelationRepository extends JpaRepository<DemandeMiseEnRelation, Long> {

    Optional<DemandeMiseEnRelation> findByCandidatIdAndPropositionTheseId(String candidatId, Long propositionTheseId);

    Optional<DemandeMiseEnRelation> findByIdAndCandidatId(Long id, String candidatId);

    boolean existsByCandidatIdAndPropositionTheseId(String candidatId, Long propositionTheseId);

    List<DemandeMiseEnRelation> findByCandidatIdOrderByUpdatedAtDesc(String candidatId);
}
