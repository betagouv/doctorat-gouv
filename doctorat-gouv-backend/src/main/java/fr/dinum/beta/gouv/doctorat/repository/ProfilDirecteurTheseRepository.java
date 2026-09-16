package fr.dinum.beta.gouv.doctorat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;

@Repository
public interface ProfilDirecteurTheseRepository extends JpaRepository<ProfilDirecteurThese, String> {

    Optional<ProfilDirecteurThese> findByUtilisateurId(String utilisateurId);
}
