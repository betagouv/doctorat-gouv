package fr.dinum.beta.gouv.doctorat.repository;

import java.time.LocalDateTime;
import java.util.List;
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
    
    @Query("SELECT DISTINCT p.specialite FROM PropositionThese p WHERE p.specialite IS NOT NULL")
    List<String> findDistinctDisciplines();

    @Query("SELECT DISTINCT p.uniteRechercheVille FROM PropositionThese p WHERE p.uniteRechercheVille IS NOT NULL")
    List<String> findDistinctLocalisations();

    @Query("SELECT DISTINCT p.uniteRechercheLibelle FROM PropositionThese p WHERE p.uniteRechercheLibelle IS NOT NULL")
    List<String> findDistinctLaboratoires();

    @Query("SELECT DISTINCT p.etablissementLibelle FROM PropositionThese p WHERE p.etablissementLibelle IS NOT NULL")
    List<String> findDistinctEcoles();
    
    /** Valeurs distinctes de domainesImpactListe */
    @Query("SELECT DISTINCT d FROM PropositionThese p JOIN p.domainesImpactListe d")
    List<String> findDistinctDomainesImpact();

    /** Valeurs distinctes de objectifsDeveloppementDurableListe */
    @Query("SELECT DISTINCT o FROM PropositionThese p JOIN p.objectifsDeveloppementDurableListe o")
    List<String> findDistinctObjectifsDurables();

}
