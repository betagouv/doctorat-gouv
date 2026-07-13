package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.EcoleDoctorale;

@Repository
public interface EcoleDoctoraleRepository extends JpaRepository<EcoleDoctorale, Long> {
	
	Optional<EcoleDoctorale> findByNumero(String numero);
	
	Optional<EcoleDoctorale> findByFresqRecordId(String fresqRecordId);
	
	List<EcoleDoctorale> findByActiveTrue();
	
	@Query("SELECT e FROM EcoleDoctorale e WHERE e.active = true AND (e.numero LIKE %:search OR e.libelle LIKE %:search)")
	List<EcoleDoctorale> searchByNumeroOrLibelle(@Param("search") String search);
	
	@Query("SELECT e FROM EcoleDoctorale e WHERE e.etablissementRor = :ror")
	List<EcoleDoctorale> findByEtablissementRor(@Param("ror") String ror);
	
	@Query("SELECT e FROM EcoleDoctorale e WHERE e.uai = :uai")
	List<EcoleDoctorale> findByUai(@Param("uai") String uai);
	
	@Query("SELECT e FROM EcoleDoctorale e WHERE e.academie = :academie")
	List<EcoleDoctorale> findByAcademie(@Param("academie") String academie);
}
