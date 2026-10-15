package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.MessageEchange;

@Repository
public interface MessageEchangeRepository extends JpaRepository<MessageEchange, Long> {

    List<MessageEchange> findByDemandeIdOrderByCreatedAtAsc(Long demandeId);

    long countByDemandeIdAndAuteurIdNotAndLuFalse(Long demandeId, String auteurId);

    long countByDemandeIdInAndAuteurIdNotAndLuFalse(List<Long> demandeIds, String auteurId);

    /**
     * Messages non lus destinés au candidat (toutes ses demandes, tout statuts non archivés).
     */
    @Query(value = "SELECT COUNT(*) FROM message_echange m JOIN demande_mise_en_relation d ON d.id = m.demande_id "
        + "WHERE m.auteur_id <> :userId AND m.lu = FALSE "
        + "AND d.candidat_id = :userId AND (d.archivee IS NULL OR d.archivee = FALSE)",
        nativeQuery = true)
    long countNonLusCandidat(@Param("userId") String userId);

    /**
     * Lignes historiques restées en clair (colonne brute sans préfixe ENC1:).
     * Le converter JPA les lit en clair puis les re-chiffre au {@code save}.
     */
    @Query(value = "SELECT * FROM message_echange WHERE contenu NOT LIKE 'ENC1:%'",
        nativeQuery = true)
    Page<MessageEchange> findLegacyCleartext(Pageable pageable);
}
