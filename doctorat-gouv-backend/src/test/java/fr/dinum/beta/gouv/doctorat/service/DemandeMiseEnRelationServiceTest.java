package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.DemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.entity.ProfilDirecteurThese;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.enums.StatutDemandeMiseEnRelation;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.MessageEchangeRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilDirecteurTheseRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;

/**
 * Désistement d'une demande acceptée : notification du directeur de thèse
 * (template Brevo 70), bouchonnée vers MAIL_STUB_MAILBOX hors production,
 * sur le même principe que les mails 31/32 de demande de mise en relation.
 */
@ExtendWith(MockitoExtension.class)
class DemandeMiseEnRelationServiceTest {

    private static final String USER_ID = "candidat-1";
    private static final String DT_ID = "dt-1";
    private static final Long DEMANDE_ID = 42L;
    private static final Long THESE_ID = 7L;
    private static final String DT_EMAIL = "directeur@univ.fr";
    private static final String BOITE_BOUCHON = "recette@exemple.fr";

    @Mock
    private PropositionTheseService propositionTheseService;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private DemandeMiseEnRelationRepository demandeRepository;
    @Mock
    private ProfilCandidatRepository profilCandidatRepository;
    @Mock
    private ProfilDirecteurTheseRepository profilDtRepository;
    @Mock
    private MessageEchangeRepository messageRepository;
    @Mock
    private BrevoEmailService emailService;

    @InjectMocks
    private DemandeMiseEnRelationService service;

    private DemandeMiseEnRelation demande;
    private Utilisateur candidat;

    @BeforeEach
    void setUp() {
        demande = new DemandeMiseEnRelation(USER_ID, THESE_ID, "Motivations", Boolean.TRUE,
            StatutDemandeMiseEnRelation.ACCEPTEE);
        demande.setId(DEMANDE_ID);

        candidat = new Utilisateur();
        candidat.setId(USER_ID);
        candidat.setPrenom("Alice");
        candidat.setNom("Martin");
        candidat.setEmail("alice.martin@exemple.fr");
    }

    private void stubDesistementOk() {
        when(demandeRepository.findByIdAndCandidatId(DEMANDE_ID, USER_ID))
            .thenReturn(Optional.of(demande));
        when(demandeRepository.save(any(DemandeMiseEnRelation.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        PropositionTheseDto these = new PropositionTheseDto();
        these.setTheseTitre("Sujet de these");
        these.setDirectionThesePrenom("Jean");
        these.setDirectionTheseNom("Dupont");
        these.setDirectionTheseEmail(DT_EMAIL);
        when(propositionTheseService.findById(THESE_ID)).thenReturn(these);

        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(candidat));

        Utilisateur directeur = new Utilisateur();
        directeur.setId(DT_ID);
        directeur.setPrenom("Jean");
        directeur.setNom("Dupont");
        directeur.setEmail(DT_EMAIL);
        when(utilisateurRepository.findByEmailIgnoreCaseAndTrim(DT_EMAIL))
            .thenReturn(Optional.of(directeur));

        ProfilDirecteurThese profilDt = new ProfilDirecteurThese();
        profilDt.setCivilite("M.");
        when(profilDtRepository.findByUtilisateurId(DT_ID)).thenReturn(Optional.of(profilDt));
    }

    @Test
    void seDesister_mailActive_envoieLaNotificationAuDirecteur() throws Exception {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        stubDesistementOk();

        service.seDesister(USER_ID, DEMANDE_ID, "Je poursuis ailleurs");

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(eq(DT_EMAIL), eq(70), paramsCaptor.capture());
        assertEquals("M.", paramsCaptor.getValue().get("civilite"));
        assertEquals("Jean Dupont", paramsCaptor.getValue().get("nom"));
        assertEquals("Alice Martin", paramsCaptor.getValue().get("nom_candidat"));
        assertEquals("Sujet de these", paramsCaptor.getValue().get("titre_sujet"));
        assertEquals("Je poursuis ailleurs", paramsCaptor.getValue().get("motif_desistement"));
    }

    @Test
    void seDesister_mailDesactive_avecBoiteBouchon_envoieVersLeBouchon() throws Exception {
        ReflectionTestUtils.setField(service, "mailEnabled", false);
        ReflectionTestUtils.setField(service, "stubMailbox", BOITE_BOUCHON);
        stubDesistementOk();

        service.seDesister(USER_ID, DEMANDE_ID, "Je poursuis ailleurs");

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(eq(BOITE_BOUCHON), eq(70), paramsCaptor.capture());
        assertEquals("Je poursuis ailleurs", paramsCaptor.getValue().get("motif_desistement"));
        assertEquals("Jean Dupont", paramsCaptor.getValue().get("nom"));
    }

    @Test
    void seDesister_mailDesactive_sansBoiteBouchon_nEnvoiePas() throws Exception {
        ReflectionTestUtils.setField(service, "mailEnabled", false);
        ReflectionTestUtils.setField(service, "stubMailbox", "");
        stubDesistementOk();

        service.seDesister(USER_ID, DEMANDE_ID, "Je poursuis ailleurs");

        verify(emailService, never()).sendTemplateEmail(anyString(), anyInt(), anyMap());
    }

    @Test
    void seDesister_emailDirecteurAbsent_nEnvoiePas() throws Exception {
        ReflectionTestUtils.setField(service, "mailEnabled", true);
        when(demandeRepository.findByIdAndCandidatId(DEMANDE_ID, USER_ID))
            .thenReturn(Optional.of(demande));
        when(demandeRepository.save(any(DemandeMiseEnRelation.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(propositionTheseService.findById(THESE_ID)).thenReturn(theseSansEmailDuDt());
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(candidat));

        service.seDesister(USER_ID, DEMANDE_ID, "Je poursuis ailleurs");

        verify(emailService, never()).sendTemplateEmail(anyString(), anyInt(), anyMap());
    }

    private PropositionTheseDto theseSansEmailDuDt() {
        PropositionTheseDto these = new PropositionTheseDto();
        these.setTheseTitre("Sujet de these");
        these.setDirectionThesePrenom("Jean");
        these.setDirectionTheseNom("Dupont");
        these.setDirectionTheseEmail(" ");
        return these;
    }
}
