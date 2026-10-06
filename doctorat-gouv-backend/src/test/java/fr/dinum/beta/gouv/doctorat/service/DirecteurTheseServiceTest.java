package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import fr.dinum.beta.gouv.doctorat.dto.ProfilDtResponse;
import fr.dinum.beta.gouv.doctorat.dto.ProfilDtUpdateRequest;
import fr.dinum.beta.gouv.doctorat.dto.SujetDtResponse;
import fr.dinum.beta.gouv.doctorat.entity.AxeDeRecherche;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.SourceThese;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

/**
 * Synchronisation axes contactables -> offres d'accompagnement (source DOCTORAT_GOUV).
 */
@ExtendWith(MockitoExtension.class)
class DirecteurTheseServiceTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ProfilDirecteurTheseRepository profilDtRepository;
    @Mock
    private ProfilCandidatRepository profilCandidatRepository;
    @Mock
    private PropositionTheseRepository propositionTheseRepository;
    @Mock
    private DemandeMiseEnRelationRepository demandeMiseEnRelationRepository;
    @Mock
    private MessageEchangeRepository messageRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private BrevoEmailService emailService;
    @Mock
    private Environment environment;

    @InjectMocks
    private DirecteurTheseService directeurTheseService;

    private Utilisateur utilisateur;
    private ProfilDirecteurThese profil;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId("dt-1");
        utilisateur.setNom("Curie");
        utilisateur.setPrenom("Marie");
        utilisateur.setEmail("marie.curie@exemple.fr");

        profil = new ProfilDirecteurThese();
        profil.setUtilisateur(utilisateur);
        profil.setEtablissement("Université X");
        profil.setLaboratoire("Labo Y");
        profil.setEcoleDoctorale("ED Z");
        profil.setDomaineScientifique("9");
        profil.setExpertiseMots("IA pour la santé");
        profil.setMotsCles(new ArrayList<>(List.of("IA", "santé")));
        profil.setDescriptionAxes("Mes axes");
        profil.setAxes(new ArrayList<>());

        when(utilisateurRepository.findById("dt-1")).thenReturn(Optional.of(utilisateur));
        when(profilDtRepository.findByUtilisateurId("dt-1")).thenReturn(Optional.of(profil));
    }

    private ProfilDtUpdateRequest baseRequest() {
        ProfilDtUpdateRequest request = new ProfilDtUpdateRequest();
        request.setNom("Curie");
        request.setPrenom("Marie");
        request.setEmail("marie.curie@exemple.fr");
        request.setCompetences(new ArrayList<>());
        request.setCivilite("M.");
        request.setTitre("Directeur de recherche");
        request.setEtablissement("Université X");
        request.setLaboratoire("Labo Y");
        request.setEcoleDoctorale("ED Z");
        request.setDomaineScientifique("9");
        request.setExpertiseMots("IA pour la santé");
        request.setMotsCles(new ArrayList<>(List.of("IA", "santé")));
        request.setDescriptionAxes("Mes axes");
        return request;
    }

    private static AxeDeRecherche axe(String titre, boolean contactable, String externalId) {
        AxeDeRecherche a = new AxeDeRecherche();
        a.setTitre(titre);
        a.setPrecisions("Modalités d'encadrement et attendus");
        a.setContactable(contactable);
        a.setExternalId(externalId);
        return a;
    }

    @Test
    void updateProfil_axeContactable_creeOffreAccompagnement() {
        ProfilDtUpdateRequest request = baseRequest();
        request.setAxes(new ArrayList<>(List.of(
            axe("Encadrement IA", true, "11111111-2222-4333-8444-555555555555"))));

        when(propositionTheseRepository.findByDirecteurEmail("marie.curie@exemple.fr"))
            .thenReturn(List.of());
        when(propositionTheseRepository.findByMatricule("DG-11111111"))
            .thenReturn(Optional.empty());

        ProfilDtResponse response = directeurTheseService.updateProfil("dt-1", request);

        ArgumentCaptor<PropositionThese> captor = ArgumentCaptor.forClass(PropositionThese.class);
        verify(propositionTheseRepository).save(captor.capture());
        PropositionThese offre = captor.getValue();

        assertEquals("offre", offre.getTypeProposition());
        assertEquals(SourceThese.DOCTORAT_GOUV, offre.getSource());
        assertEquals(Boolean.TRUE, offre.getActive());
        assertEquals("DG-11111111", offre.getMatricule());
        assertEquals("11111111-2222-4333-8444-555555555555", offre.getAxeExternalId());
        assertEquals("Encadrement IA", offre.getTheseTitre());
        assertEquals("Modalités d'encadrement et attendus", offre.getResume());
        assertEquals("Modalités d'encadrement et attendus", offre.getThematiqueRecherche());
        assertEquals("IA pour la santé", offre.getExpertiseMots());
        assertEquals(Map.of("0", "IA", "1", "santé"), offre.getMotsCles());
        assertEquals("marie.curie@exemple.fr", offre.getDirectionTheseEmail());
        assertEquals("marie.curie@exemple.fr", offre.getDeposantEmail());
        assertEquals("Curie", offre.getDirectionTheseNom());
        assertEquals("Université X", offre.getEtablissementLibelle());
        assertEquals("Labo Y", offre.getUniteRechercheLibelle());
        assertNotNull(offre.getDateMiseEnLigne());
        assertNotNull(offre.getAnnee());

        // L'identifiant stable est renvoyé au front (idempotence des saves suivants).
        assertEquals(1, response.getAxes().size());
        assertEquals("11111111-2222-4333-8444-555555555555", response.getAxes().get(0).getExternalId());
    }

    @Test
    void updateProfil_axeDevenuNonContactable_desactiveOffreSansSupprimer() {
        ProfilDtUpdateRequest request = baseRequest();
        request.setAxes(new ArrayList<>(List.of(
            axe("Encadrement IA", false, "ext-1"))));

        PropositionThese existante = new PropositionThese();
        existante.setId(42L);
        existante.setMatricule("DG-EXT10000");
        existante.setAxeExternalId("ext-1");
        existante.setSource(SourceThese.DOCTORAT_GOUV);
        existante.setActive(true);

        when(propositionTheseRepository.findByDirecteurEmail("marie.curie@exemple.fr"))
            .thenReturn(List.of(existante));

        directeurTheseService.updateProfil("dt-1", request);

        verify(propositionTheseRepository, never()).findByMatricule(anyString());
        ArgumentCaptor<PropositionThese> captor = ArgumentCaptor.forClass(PropositionThese.class);
        verify(propositionTheseRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getId());
        assertFalse(captor.getValue().getActive());
    }

    @Test
    void updateProfil_axeSansExternalId_backfillEtCreeOffre() {
        ProfilDtUpdateRequest request = baseRequest();
        request.setAxes(new ArrayList<>(List.of(axe("Axe legacy", true, null))));

        when(propositionTheseRepository.findByDirecteurEmail("marie.curie@exemple.fr"))
            .thenReturn(List.of());
        when(propositionTheseRepository.findByMatricule(anyString()))
            .thenReturn(Optional.empty());

        ProfilDtResponse response = directeurTheseService.updateProfil("dt-1", request);

        ArgumentCaptor<PropositionThese> captor = ArgumentCaptor.forClass(PropositionThese.class);
        verify(propositionTheseRepository).save(captor.capture());
        assertNotNull(captor.getValue().getAxeExternalId());
        assertTrue(captor.getValue().getMatricule().startsWith("DG-"));
        assertNotNull(response.getAxes().get(0).getExternalId());
    }

    @Test
    void getOffres_neRetourneQueLaSourceDoctoratGouv() {
        PropositionThese sujetAdum = new PropositionThese();
        sujetAdum.setId(1L);
        sujetAdum.setMatricule("AB-2026_0001");
        sujetAdum.setSource(SourceThese.ADUM);
        sujetAdum.setTheseTitre("Sujet ADUM");
        sujetAdum.setActive(true);

        PropositionThese offre = new PropositionThese();
        offre.setId(2L);
        offre.setMatricule("DG-11111111");
        offre.setSource(SourceThese.DOCTORAT_GOUV);
        offre.setTheseTitre("Encadrement IA");
        offre.setActive(true);
        offre.setDirectionTheseEmail("marie.curie@exemple.fr");

        when(propositionTheseRepository.findByDirecteurEmail("marie.curie@exemple.fr"))
            .thenReturn(List.of(sujetAdum, offre));

        List<SujetDtResponse> offres = directeurTheseService.getOffres("dt-1");

        assertEquals(1, offres.size());
        assertEquals("DG-11111111", offres.get(0).getMatricule());
        assertEquals("Encadrement IA", offres.get(0).getTitre());
    }
}
