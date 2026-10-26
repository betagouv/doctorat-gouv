package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;

@Repository
public interface DemandeMiseEnRelationRepository extends JpaRepository<DemandeMiseEnRelation, Long> {

    Optional<DemandeMiseEnRelation> findByCandidatIdAndPropositionTheseId(String candidatId, Long propositionTheseId);

    Optional<DemandeMiseEnRelation> findByIdAndCandidatId(Long id, String candidatId);

    boolean existsByCandidatIdAndPropositionTheseId(String candidatId, Long propositionTheseId);

    List<DemandeMiseEnRelation> findByCandidatIdOrderByUpdatedAtDesc(String candidatId);

    List<DemandeMiseEnRelation> findByPropositionTheseIdInOrderByUpdatedAtDesc(List<Long> propositionTheseIds);

    /**
     * Demandes en attente (statut CREE, non archivées) d'un candidat.
     */
    @Query("SELECT COUNT(d) FROM DemandeMiseEnRelation d WHERE d.candidatId = :candidatId "
        + "AND d.statut = fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation.CREE "
        + "AND (d.archivee IS NULL OR d.archivee = FALSE)")
    long countDemandesEnAttenteCandidat(@Param("candidatId") String candidatId);

    /**
     * Nombre total de demandes d'un candidat, quel que soit leur statut
     * (brouillon, créée, acceptée) et leur état d'archivage.
     * Correspond au total affiché dans le tableau de bord du candidat.
     */
    long countByCandidatId(String candidatId);
}
