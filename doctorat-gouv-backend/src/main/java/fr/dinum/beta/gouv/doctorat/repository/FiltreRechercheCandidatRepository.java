package fr.dinum.beta.gouv.doctorat.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.FiltreRechercheCandidat;

@Repository
public interface FiltreRechercheCandidatRepository extends JpaRepository<FiltreRechercheCandidat, String> {

    List<FiltreRechercheCandidat> findByUtilisateurIdOrderByDateCreationDesc(String utilisateurId);

    Optional<FiltreRechercheCandidat> findByIdAndUtilisateurId(String id, String utilisateurId);

    List<FiltreRechercheCandidat> findByAlerteActiveTrueAndFrequenceAlerte(String frequence);
}
