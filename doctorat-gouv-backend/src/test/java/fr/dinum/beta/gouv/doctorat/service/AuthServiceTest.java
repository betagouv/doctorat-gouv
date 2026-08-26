package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.dinum.beta.gouv.doctorat.config.JwtConfig;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionRequest;
import fr.dinum.beta.gouv.doctorat.service.InscriptionFileService;
import fr.dinum.beta.gouv.doctorat.dto.ConnexionResponse;
import fr.dinum.beta.gouv.doctorat.dto.InscriptionRequest;
import fr.dinum.beta.gouv.doctorat.dto.UtilisateurDto;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.RoleUtilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceAuth;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private InscriptionFileService inscriptionFileService;

    @InjectMocks
    private AuthService authService;

    private Utilisateur utilisateur;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId("uuid-test");
        utilisateur.setEmail("test@exemple.fr");
        utilisateur.setMotDePasse("$2a$10$hashedPassword");
        utilisateur.setPrenom("Jean");
        utilisateur.setNom("Dupont");
        utilisateur.setRole(RoleUtilisateur.CANDIDAT);
        utilisateur.setSourceAuth(SourceAuth.MANUEL);
        utilisateur.setActif(true);
        utilisateur.setDateCreation(LocalDateTime.now());
    }

    // ==================== INSCRIPTION ====================

    @Test
    void inscrire_avecDonneesValides_retourneUtilisateurDto() {
        InscriptionRequest request = new InscriptionRequest();
        request.setEmail("nouveau@exemple.fr");
        request.setMotDePasse("MotDePasse1234!");
        request.setPrenom("Marie");
        request.setNom("Curie");
        request.setRole(RoleUtilisateur.CANDIDAT);

        when(utilisateurRepository.existsByEmail("nouveau@exemple.fr")).thenReturn(false);
        when(passwordEncoder.encode("MotDePasse1234!")).thenReturn("$2a$10$encoded");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(invocation -> {
            Utilisateur u = invocation.getArgument(0);
            u.setId("uuid-genere");
            return u;
        });
        when(jwtConfig.getExpirationMs()).thenReturn(86400000L);
        when(jwtConfig.getSecret()).thenReturn("VGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5Rm9ySlRXVGVzdGluZ1B1cnBvc2Vz");

        ConnexionResponse result = authService.inscrire(request);

        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getUtilisateur());
        assertEquals("nouveau@exemple.fr", result.getUtilisateur().getEmail());
        assertEquals("Marie", result.getUtilisateur().getPrenom());
        assertEquals("Curie", result.getUtilisateur().getNom());
        assertEquals(RoleUtilisateur.CANDIDAT, result.getUtilisateur().getRole());
        assertEquals(SourceAuth.MANUEL, result.getUtilisateur().getSourceAuth());
        assertEquals(true, result.getUtilisateur().getActif());
        assertNotNull(result.getUtilisateur().getDateCreation());
        assertEquals(86400000L, result.getExpiresIn());

        verify(utilisateurRepository).existsByEmail("nouveau@exemple.fr");
        verify(passwordEncoder).encode("MotDePasse1234!");
        verify(utilisateurRepository).save(any(Utilisateur.class));
    }

    @Test
    void inscrire_emailDejaExistant_lanceException() {
        InscriptionRequest request = new InscriptionRequest();
        request.setEmail("existant@exemple.fr");
        request.setMotDePasse("MotDePasse1234!");
        request.setPrenom("Test");
        request.setNom("User");
        request.setRole(RoleUtilisateur.CANDIDAT);

        when(utilisateurRepository.existsByEmail("existant@exemple.fr")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.inscrire(request)
        );

        assertEquals("Un compte existe déjà avec cet email", exception.getMessage());
        verify(utilisateurRepository, never()).save(any());
    }

    // ==================== CONNEXION ====================

    @Test
    void connecter_avecBonnesIdentifiants_retourneConnexionResponse() {
        ConnexionRequest request = new ConnexionRequest();
        request.setEmail("test@exemple.fr");
        request.setMotDePasse("MotDePasse1234!");

        when(utilisateurRepository.findByEmail("test@exemple.fr")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("MotDePasse1234!", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtConfig.getExpirationMs()).thenReturn(86400000L);
        when(jwtConfig.getSecret()).thenReturn("VGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5Rm9ySlRXVGVzdGluZ1B1cnBvc2Vz");

        ConnexionResponse response = authService.connecter(request);

        assertNotNull(response);
        assertNotNull(response.getToken());
        assertNotNull(response.getUtilisateur());
        assertEquals("test@exemple.fr", response.getUtilisateur().getEmail());
        assertEquals(86400000L, response.getExpiresIn());

        verify(utilisateurRepository).findByEmail("test@exemple.fr");
        verify(passwordEncoder).matches("MotDePasse1234!", "$2a$10$hashedPassword");
    }

    @Test
    void connecter_emailInconnu_lanceException() {
        ConnexionRequest request = new ConnexionRequest();
        request.setEmail("inconnu@exemple.fr");
        request.setMotDePasse("MotDePasse1234!");

        when(utilisateurRepository.findByEmail("inconnu@exemple.fr")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.connecter(request)
        );

        assertEquals("Email ou mot de passe incorrect", exception.getMessage());
    }

    @Test
    void connecter_motDePasseIncorrect_lanceException() {
        ConnexionRequest request = new ConnexionRequest();
        request.setEmail("test@exemple.fr");
        request.setMotDePasse("MauvaisMotDePasse!");

        when(utilisateurRepository.findByEmail("test@exemple.fr")).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("MauvaisMotDePasse!", "$2a$10$hashedPassword")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.connecter(request)
        );

        assertEquals("Email ou mot de passe incorrect", exception.getMessage());
    }

    // ==================== GET UTILISATEUR ====================

    @Test
    void getUtilisateur_idExistant_retourneUtilisateurDto() {
        when(utilisateurRepository.findById("uuid-test")).thenReturn(Optional.of(utilisateur));

        UtilisateurDto result = authService.getUtilisateur("uuid-test");

        assertNotNull(result);
        assertEquals("uuid-test", result.getId());
        assertEquals("test@exemple.fr", result.getEmail());
        assertEquals("Jean", result.getPrenom());
        assertEquals("Dupont", result.getNom());
    }

    @Test
    void getUtilisateur_idInconnu_lanceException() {
        when(utilisateurRepository.findById("id-inconnu")).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> authService.getUtilisateur("id-inconnu")
        );

        assertEquals("Utilisateur non trouvé", exception.getMessage());
    }

    // ==================== JWT ====================

    @Test
    void genererToken_retourneTokenValide() {
        when(jwtConfig.getExpirationMs()).thenReturn(86400000L);
        when(jwtConfig.getSecret()).thenReturn("VGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5Rm9ySlRXVGVzdGluZ1B1cnBvc2Vz");

        String token = authService.genererToken(utilisateur);

        assertNotNull(token);
        assertNotNull(authService.parserToken(token));
    }

    @Test
    void parserToken_tokenValide_retourneClaims() {
        when(jwtConfig.getExpirationMs()).thenReturn(86400000L);
        when(jwtConfig.getSecret()).thenReturn("VGhpc0lzQVZlcnlMb25nU2VjcmV0S2V5Rm9ySlRXVGVzdGluZ1B1cnBvc2Vz");

        String token = authService.genererToken(utilisateur);

        var claims = authService.parserToken(token);

        assertEquals("uuid-test", claims.getSubject());
        assertEquals("test@exemple.fr", claims.get("email"));
        assertEquals("CANDIDAT", claims.get("role"));
    }
}
