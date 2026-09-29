package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.MessageEchange;

@Repository
public interface MessageEchangeRepository extends JpaRepository<MessageEchange, Long> {

    List<MessageEchange> findByDemandeIdOrderByCreatedAtAsc(Long demandeId);

    long countByDemandeIdAndAuteurIdNotAndLuFalse(Long demandeId, String auteurId);
}
