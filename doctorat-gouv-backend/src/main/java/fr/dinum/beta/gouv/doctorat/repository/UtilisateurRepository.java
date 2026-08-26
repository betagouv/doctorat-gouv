package fr.dinum.beta.gouv.doctorat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;

/**
 * Repository Spring Data pour l'entité Utilisateur.
 * Fournit les requêtes de recherche par email, par franceConnectId et l'existence par email.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByFranceConnectId(String franceConnectId);

    boolean existsByEmail(String email);
}
