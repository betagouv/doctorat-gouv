package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.MessageEchange;

@Repository
public interface MessageEchangeRepository extends JpaRepository<MessageEchange, Long> {

    List<MessageEchange> findByDemandeIdOrderByCreatedAtAsc(Long demandeId);

    long countByDemandeIdAndAuteurIdNotAndLuFalse(Long demandeId, String auteurId);

    /**
     * Lignes historiques restées en clair (colonne brute sans préfixe ENC1:).
     * Le converter JPA les lit en clair puis les re-chiffre au {@code save}.
     */
    @Query(value = "SELECT * FROM message_echange WHERE contenu NOT LIKE 'ENC1:%'",
        nativeQuery = true)
    Page<MessageEchange> findLegacyCleartext(Pageable pageable);
}
