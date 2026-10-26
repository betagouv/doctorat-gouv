package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import fr.dinum.beta.gouv.doctorat.dto.ChangementMotDePasseRequest;
import fr.dinum.beta.gouv.doctorat.dto.ProfilResponse;
import fr.dinum.beta.gouv.doctorat.entity.ProfilCandidat;
import fr.dinum.beta.gouv.doctorat.entity.Utilisateur;
import fr.dinum.beta.gouv.doctorat.repository.DemandeMiseEnRelationRepository;
import fr.dinum.beta.gouv.doctorat.repository.ProfilCandidatRepository;
import fr.dinum.beta.gouv.doctorat.repository.UtilisateurRepository;
import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;

/**
 * Fichiers du candidat sur son profil : affichage, remplacement du CV,
 * ajout/suppression de pièces (avec suppression backend).
 */
@ExtendWith(MockitoExtension.class)
class ProfilServiceTest {

    private static final String USER_ID = "user-test";

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private ProfilCandidatRepository profilCandidatRepository;
    @Mock
    private DemandeMiseEnRelationRepository demandeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private StoragePort storage;
    @Mock
    private InscriptionFileService inscriptionFileService;
    @Mock
    private BrevoEmailService emailService;
    @Mock
    private PhotoService photoService;

    @InjectMocks
    private ProfilService profilService;

    private Utilisateur utilisateur;
    private ProfilCandidat profil;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId(USER_ID);
        utilisateur.setEmail("candidat@exemple.fr");
        utilisateur.setNom("Test");
        utilisateur.setPrenom("Candidat");

        profil = new ProfilCandidat();
        profil.setUtilisateur(utilisateur);
        profil.setCvFilename("inscription/" + USER_ID + "/cv/de56f538-ab22-4d20-a436-de45af755e7f_cv.pdf");
        profil.setPiecesFilenames(new ArrayList<>(
            List.of("inscription/" + USER_ID + "/pieces/1d55c554-7c27-4789-baac-220aa0b1ab3c_piece.pdf")));

        Mockito.lenient().when(profilCandidatRepository.findByUtilisateurId(USER_ID)).thenReturn(Optional.of(profil));
        Mockito.lenient().when(storage.exists(anyString())).thenReturn(true);
        Mockito.lenient().when(storage.size(anyString())).thenReturn(1024L);
    }

    private MockMultipartFile pdf(String name) {
        return new MockMultipartFile("cv", name, "application/pdf", "%PDF-1.4 test".getBytes());
    }

    @Test
    void getProfil_exposeCvEtPieces() {
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateur));

        ProfilResponse response = profilService.getProfil(USER_ID).orElseThrow();

        assertNotNull(response.getCv());
        assertEquals("cv.pdf", response.getCv().getFilename());
        assertEquals(1024L, response.getCv().getSize());
        assertEquals(1, response.getPieces().size());
        assertEquals("piece.pdf", response.getPieces().get(0).getFilename());
    }

    @Test
    void getProfil_compteLesDemandesPlutotQueLeChampUtilisateur() {
        utilisateur.setNbCandidatures(99); // champ hérité, jamais incrémenté côté back
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateur));
        when(demandeRepository.countByCandidatId(USER_ID)).thenReturn(3L);

        ProfilResponse response = profilService.getProfil(USER_ID).orElseThrow();

        assertEquals(3, response.getNbCandidatures());
    }

    @Test
    void remplacerCv_stockeNouveauEtMetAJourBase() {
        String newKey = "inscription/" + USER_ID + "/cv/a8d36a19-6c1a-4d52-b927-0c826cfe5482_nouveau.pdf";
        when(inscriptionFileService.replaceCv(eq(USER_ID), anyString(), any())).thenReturn(newKey);

        ProfilResponse response = profilService.remplacerCv(USER_ID, pdf("nouveau.pdf"));

        verify(inscriptionFileService).replaceCv(eq(USER_ID), eq("inscription/" + USER_ID + "/cv/de56f538-ab22-4d20-a436-de45af755e7f_cv.pdf"), any());
        verify(profilCandidatRepository).save(profil);
        assertEquals(newKey, profil.getCvFilename());
        assertNotNull(response.getCv());
    }

    @Test
    void ajouterPieces_respecteLimiteCinq() {
        profil.setPiecesFilenames(new ArrayList<>(List.of("p1", "p2", "p3", "p4", "p5")));
        when(inscriptionFileService.addPiece(eq(USER_ID), anyInt(), any()))
            .thenThrow(new IllegalArgumentException("Maximum 5 pièces complémentaires autorisées"));

        assertThrows(IllegalArgumentException.class,
            () -> profilService.ajouterPieces(USER_ID, List.of(pdf("trop.pdf"))));
        verify(profilCandidatRepository, never()).save(any());
    }

    @Test
    void ajouterPieces_ajouteEtSauvegarde() {
        String newKey = "inscription/" + USER_ID + "/pieces/f39cecd2-17b6-44cb-a5c1-9664203fd018_nouvelle.pdf";
        when(inscriptionFileService.addPiece(eq(USER_ID), eq(1), any())).thenReturn(newKey);

        ProfilResponse response = profilService.ajouterPieces(USER_ID, List.of(pdf("nouvelle.pdf")));

        verify(profilCandidatRepository).save(profil);
        assertTrue(profil.getPiecesFilenames().contains(newKey));
        assertEquals(2, response.getPieces().size());
    }

    @Test
    void supprimerPiece_supprimeBackendEtBase() {
        ProfilResponse response = profilService.supprimerPiece(USER_ID, 0);

        verify(inscriptionFileService).deletePiece("inscription/" + USER_ID + "/pieces/1d55c554-7c27-4789-baac-220aa0b1ab3c_piece.pdf");
        verify(profilCandidatRepository).save(profil);
        assertTrue(profil.getPiecesFilenames().isEmpty());
        assertTrue(response.getPieces().isEmpty());
    }

    @Test
    void supprimerPiece_indexInvalide_lanceException() {
        assertThrows(IllegalArgumentException.class, () -> profilService.supprimerPiece(USER_ID, 5));
        verify(inscriptionFileService, never()).deletePiece(anyString());
        verify(profilCandidatRepository, never()).save(any());
    }

    @Test
    void getMonFichier_cvExistant_retourneCle() {
        assertEquals("inscription/" + USER_ID + "/cv/de56f538-ab22-4d20-a436-de45af755e7f_cv.pdf",
            profilService.getMonFichier(USER_ID, "cv", null));
    }

    @Test
    void getMonFichier_pieceAbsenteDuBackend_lanceException() {
        when(storage.exists(anyString())).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> profilService.getMonFichier(USER_ID, "piece", 0));
    }

    private ChangementMotDePasseRequest nouvelleDemandeChangementMdp() {
        ChangementMotDePasseRequest request = new ChangementMotDePasseRequest();
        request.setMotDePasseActuel("Ancien1234!");
        request.setNouveauMotDePasse("Nouveau1234!");
        request.setConfirmationMotDePasse("Nouveau1234!");
        return request;
    }

    private void stubChangementMdpOk() {
        utilisateur.setMotDePasse("$2a$10$ancien");
        utilisateur.setDateModification(java.time.LocalDateTime.of(2026, 10, 21, 10, 30));
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateur));
        when(passwordEncoder.matches("Ancien1234!", "$2a$10$ancien")).thenReturn(true);
        when(passwordEncoder.encode("Nouveau1234!")).thenReturn("$2a$10$nouveau");
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void changerMotDePasse_mailActive_envoieNotification() throws Exception {
        ReflectionTestUtils.setField(profilService, "mailEnabled", true);
        stubChangementMdpOk();

        profilService.changerMotDePasse(USER_ID, nouvelleDemandeChangementMdp());

        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(emailService).sendTemplateEmail(eq("candidat@exemple.fr"), eq(73), paramsCaptor.capture());
        assertEquals("Candidat", paramsCaptor.getValue().get("prenom"));
        assertEquals("Test", paramsCaptor.getValue().get("nom"));
    }

    @Test
    void changerMotDePasse_mailDesactive_nEnvoiePasDeMail() throws Exception {
        ReflectionTestUtils.setField(profilService, "mailEnabled", false);
        stubChangementMdpOk();

        profilService.changerMotDePasse(USER_ID, nouvelleDemandeChangementMdp());

        verify(emailService, never()).sendTemplateEmail(anyString(), anyInt(), anyMap());
    }

    private MockMultipartFile jpegPhoto() {
        byte[] jpeg = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0};
        return new MockMultipartFile("photo", "avatar.jpg", "image/jpeg", jpeg);
    }

    @Test
    void remplacerPhoto_stockeNouvelleEtSupprimeAncienne() {
        utilisateur.setPhotoUrl("photos/" + USER_ID + "/ancienne.jpg");
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoService.storePhoto(eq(USER_ID), any())).thenReturn("photos/" + USER_ID + "/nouvelle.jpg");

        ProfilResponse response = profilService.remplacerPhoto(USER_ID, jpegPhoto());

        verify(photoService).deletePhotoQuietly("photos/" + USER_ID + "/ancienne.jpg");
        assertEquals("photos/" + USER_ID + "/nouvelle.jpg", utilisateur.getPhotoUrl());
        // L'URL exposée au front est l'endpoint, jamais la clé
        assertEquals("/api/fichiers/utilisateurs/" + USER_ID + "/photo", response.getPhotoUrl());
    }

    @Test
    void remplacerPhoto_sansAnciennePhoto_neSupprimeRien() {
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));
        when(photoService.storePhoto(eq(USER_ID), any())).thenReturn("photos/" + USER_ID + "/nouvelle.jpg");

        profilService.remplacerPhoto(USER_ID, jpegPhoto());

        verify(photoService, never()).deletePhotoQuietly(anyString());
    }

    @Test
    void supprimerPhoto_supprimeDuStockageEtDeLaBase() {
        utilisateur.setPhotoUrl("photos/" + USER_ID + "/a.jpg");
        when(utilisateurRepository.findById(USER_ID)).thenReturn(Optional.of(utilisateur));
        when(utilisateurRepository.save(any(Utilisateur.class))).thenAnswer(inv -> inv.getArgument(0));

        ProfilResponse response = profilService.supprimerPhoto(USER_ID);

        verify(photoService).deletePhotoQuietly("photos/" + USER_ID + "/a.jpg");
        assertNull(utilisateur.getPhotoUrl());
        assertNull(response.getPhotoUrl());
    }

    @Test
    void urlPhoto_retourneEndpointSiPhotoPresente() {
        utilisateur.setPhotoUrl("photos/" + USER_ID + "/a.jpg");
        assertEquals("/api/fichiers/utilisateurs/" + USER_ID + "/photo", ProfilService.urlPhoto(utilisateur));
    }

    @Test
    void urlPhoto_retourneNullSiPasDePhoto() {
        assertNull(ProfilService.urlPhoto(utilisateur));
    }
}

