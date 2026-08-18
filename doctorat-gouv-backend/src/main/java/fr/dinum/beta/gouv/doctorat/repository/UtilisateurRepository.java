package fr.dinum.beta.gouv.doctorat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByFranceConnectId(String franceConnectId);

    boolean existsByEmail(String email);
}
