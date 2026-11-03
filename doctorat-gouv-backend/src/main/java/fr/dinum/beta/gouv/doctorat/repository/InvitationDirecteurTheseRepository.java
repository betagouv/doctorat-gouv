package fr.dinum.beta.gouv.doctorat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.InvitationDirecteurThese;

@Repository
public interface InvitationDirecteurTheseRepository extends JpaRepository<InvitationDirecteurThese, Long> {

    boolean existsByEmail(String email);

    Optional<InvitationDirecteurThese> findByEmail(String email);
}
