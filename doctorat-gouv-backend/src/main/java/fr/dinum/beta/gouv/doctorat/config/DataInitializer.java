package fr.dinum.beta.gouv.doctorat.config;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@Configuration
@Profile("dev")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UtilisateurRepository utilisateurRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UtilisateurRepository utilisateurRepository, PasswordEncoder passwordEncoder) {
        this.utilisateurRepository = utilisateurRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (utilisateurRepository.count() == 0) {
            Utilisateur candidat = new Utilisateur();
            candidat.setEmail("candidat@doctorat.gouv.fr");
            candidat.setMotDePasse(passwordEncoder.encode("MotDePasse123!"));
            candidat.setPrenom("Marie");
            candidat.setNom("Curie");
            candidat.setRole(RoleUtilisateur.CANDIDAT);
            candidat.setSourceAuth(SourceAuth.MANUEL);
            candidat.setActif(true);
            candidat.setDateCreation(LocalDateTime.now());
            utilisateurRepository.save(candidat);

            Utilisateur directeur = new Utilisateur();
            directeur.setEmail("directeur@doctorat.gouv.fr");
            directeur.setMotDePasse(passwordEncoder.encode("MotDePasse123!"));
            directeur.setPrenom("Jean");
            directeur.setNom("Pierre");
            directeur.setRole(RoleUtilisateur.DIRECTEUR_THESE);
            directeur.setSourceAuth(SourceAuth.MANUEL);
            directeur.setActif(true);
            directeur.setDateCreation(LocalDateTime.now());
            utilisateurRepository.save(directeur);

            log.info("Utilisateurs de test créés : candidat@doctorat.gouv.fr / directeur@doctorat.gouv.fr (MDP: MotDePasse123!)");
        }
    }
}
