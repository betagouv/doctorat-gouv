package fr.dinum.beta.gouv.doctorat.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

@Repository
public interface PropositionTheseRepository extends JpaRepository<PropositionThese, Long>, JpaSpecificationExecutor<PropositionThese> {

	Optional<PropositionThese> findByMatricule(String matricule);
	
	// Vérifie si une proposition existe avec ce matricule et une dateMaj plus ancienne
    @Query("SELECT p FROM PropositionThese p WHERE p.matricule = :matricule AND (p.dateMaj IS NULL OR p.dateMaj < :dateMaj)")
    Optional<PropositionThese> findForUpdate(@Param("matricule") String matricule, @Param("dateMaj") LocalDateTime dateMaj);

}
