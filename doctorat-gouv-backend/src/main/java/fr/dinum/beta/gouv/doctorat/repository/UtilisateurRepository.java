package fr.dinum.beta.gouv.doctorat.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;

/**
 * Repository Spring Data pour l'entité Utilisateur.
 * Fournit les requêtes de recherche par email, par franceConnectId et l'existence par email.
 * Les recherches par email sont insensibles à la casse et aux espaces (LOWER + TRIM)
 * car les emails ADUM/AMETHIS et les saisies utilisateurs ont des casses variables.
 */
@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    Optional<Utilisateur> findByFranceConnectId(String franceConnectId);

    boolean existsByEmail(String email);

    @Query("SELECT u FROM Utilisateur u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
    Optional<Utilisateur> findByEmailIgnoreCaseAndTrim(@Param("email") String email);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Utilisateur u WHERE LOWER(TRIM(u.email)) = LOWER(TRIM(:email))")
    boolean existsByEmailIgnoreCaseAndTrim(@Param("email") String email);
}
