package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;

/**
 * Validation des uploads (PDF, tailles, quota) et format des clés objet.
 * Le backend est mocké : aucun accès disque ni réseau.
 */
@ExtendWith(MockitoExtension.class)
class InscriptionFileServiceTest {

    private static final String USER_ID = "user-test";

    @Mock
    private StoragePort storage;

    @InjectMocks
    private InscriptionFileService service;

    private MockMultipartFile pdf(String name, long size) {
        byte[] content = new byte[(int) size];
        content[0] = '%';
        return new MockMultipartFile("file", name, "application/pdf", content);
    }

    @Test
    void storeCv_retourneCleObjet() {
        when(storage.store(anyString(), any(), eq("application/pdf")))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String key = service.storeCv(USER_ID, pdf("mon-cv.pdf", 100));

        assertTrue(key.startsWith("inscription/" + USER_ID + "/cv/"),
            "clé inattendue : " + key);
        assertTrue(key.endsWith("_mon-cv.pdf"), "clé inattendue : " + key);
        verify(storage).store(eq(key), any(), eq("application/pdf"));
    }

    @Test
    void storeCv_sanitizeNomDeFichier() {
        when(storage.store(anyString(), any(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));

        String key = service.storeCv(USER_ID, pdf("mon cv accentué.pdf", 100));

        assertTrue(key.matches("inscription/" + USER_ID + "/cv/[0-9a-f-]+_[A-Za-z0-9._-]+"),
            "clé inattendue : " + key);
    }

    @Test
    void storeCv_nonPdf_rejete() {
        MockMultipartFile faux = new MockMultipartFile("file", "cv.txt", "text/plain", "x".getBytes());

        assertThrows(IllegalArgumentException.class, () -> service.storeCv(USER_ID, faux));
        verify(storage, never()).store(anyString(), any(), anyString());
    }

    @Test
    void storeCv_tropLourd_rejete() {
        assertThrows(IllegalArgumentException.class,
            () -> service.storeCv(USER_ID, pdf("cv.pdf", 1_048_577L)));
        verify(storage, never()).store(anyString(), any(), anyString());
    }

    @Test
    void storePieces_sixFichiers_rejete() {
        List<org.springframework.web.multipart.MultipartFile> fichiers = List.of(
            pdf("p1.pdf", 10), pdf("p2.pdf", 10), pdf("p3.pdf", 10),
            pdf("p4.pdf", 10), pdf("p5.pdf", 10), pdf("p6.pdf", 10));

        assertThrows(IllegalArgumentException.class, () -> service.storePieces(USER_ID, fichiers));
        verify(storage, never()).store(anyString(), any(), anyString());
    }

    @Test
    void storePieces_pieceTropLourde_rejete() {
        assertThrows(IllegalArgumentException.class,
            () -> service.storePieces(USER_ID, List.of(pdf("p.pdf", 5_242_881L))));
    }

    @Test
    void replaceCv_supprimeAncienApresStockage() {
        String oldKey = "inscription/" + USER_ID + "/cv/ancien.pdf";
        String newKey = "inscription/" + USER_ID + "/cv/nouveau.pdf";
        when(storage.store(anyString(), any(), anyString())).thenReturn(newKey);
        when(storage.exists(oldKey)).thenReturn(true);

        assertEquals(newKey, service.replaceCv(USER_ID, oldKey, pdf("nouveau.pdf", 100)));

        var ordre = org.mockito.Mockito.inOrder(storage);
        ordre.verify(storage).store(anyString(), any(), anyString());
        ordre.verify(storage).delete(oldKey);
    }

    @Test
    void replaceCv_echecSuppressionAncien_nEchouePas() {
        when(storage.store(anyString(), any(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.exists(anyString())).thenReturn(true);
        org.mockito.Mockito.doThrow(new RuntimeException("OOS indisponible"))
            .when(storage).delete(anyString());

        String key = service.replaceCv(USER_ID, "inscription/" + USER_ID + "/cv/ancien.pdf",
            pdf("nouveau.pdf", 100));

        assertTrue(key.endsWith("_nouveau.pdf"));
    }

    @Test
    void addPiece_quotaAtteint_rejete() {
        assertThrows(IllegalArgumentException.class,
            () -> service.addPiece(USER_ID, 5, pdf("p.pdf", 100)));
        verify(storage, never()).store(anyString(), any(), anyString());
    }

    @Test
    void deletePiece_cleVide_rejete() {
        assertThrows(IllegalArgumentException.class, () -> service.deletePiece(" "));
        verify(storage, never()).delete(anyString());
    }

    @Test
    void storePieces_transmetContenuExact() {
        when(storage.store(anyString(), any(), anyString()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile fichier = pdf("p.pdf", 50);

        service.storePieces(USER_ID, List.of(fichier));

        ArgumentCaptor<byte[]> contenu = ArgumentCaptor.forClass(byte[].class);
        verify(storage).store(anyString(), contenu.capture(), eq("application/pdf"));
        assertEquals(50, contenu.getValue().length);
    }
}
