package fr.dinum.beta.gouv.doctorat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;

@Repository
public interface ProfilCandidatRepository extends JpaRepository<ProfilCandidat, String> {

    Optional<ProfilCandidat> findByUtilisateurId(String utilisateurId);
}
