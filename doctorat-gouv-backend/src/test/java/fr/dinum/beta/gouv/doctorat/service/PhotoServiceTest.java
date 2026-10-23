package fr.dinum.beta.gouv.doctorat.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import fr.dinum.beta.gouv.doctorat.service.storage.StoragePort;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    private static final String USER_ID = "user-test";

    @Mock
    private StoragePort storage;

    private PhotoService photoService;

    @BeforeEach
    void setUp() {
        photoService = new PhotoService(storage);
    }

    private byte[] jpegBytes() {
        // Magic bytes JPEG : FF D8 FF E0 (minimum 12 octets pour la détection)
        byte[] bytes = new byte[12];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        bytes[2] = (byte) 0xFF;
        bytes[3] = (byte) 0xE0;
        return bytes;
    }

    private byte[] pngBytes() {
        return new byte[] {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0
        };
    }

    private byte[] webpBytes() {
        byte[] bytes = new byte[12];
        bytes[0] = 'R'; bytes[1] = 'I'; bytes[2] = 'F'; bytes[3] = 'F';
        bytes[8] = 'W'; bytes[9] = 'E'; bytes[10] = 'B'; bytes[11] = 'P';
        return bytes;
    }

    @Test
    void storePhoto_stockeJpegAvecCléServeur() {
        MockMultipartFile file = new MockMultipartFile("photo", "avatar.jpg", "image/jpeg", jpegBytes());
        when(storage.store(anyString(), any(byte[].class), eq("image/jpeg")))
            .thenAnswer(inv -> inv.getArgument(0));

        String key = photoService.storePhoto(USER_ID, file);

        assertTrue(key.startsWith("photos/" + USER_ID + "/"), "clé serveur attendue, reçue : " + key);
        assertTrue(key.endsWith(".jpg"), "extension .jpg attendue");
        ArgumentCaptor<byte[]> contentCaptor = ArgumentCaptor.forClass(byte[].class);
        verify(storage).store(eq(key), contentCaptor.capture(), eq("image/jpeg"));
        assertEquals(12, contentCaptor.getValue().length);
    }

    @Test
    void storePhoto_stockePng() {
        MockMultipartFile file = new MockMultipartFile("photo", "a.png", "image/png", pngBytes());
        when(storage.store(anyString(), any(byte[].class), eq("image/png")))
            .thenAnswer(inv -> inv.getArgument(0));

        String key = photoService.storePhoto(USER_ID, file);
        assertTrue(key.endsWith(".png"));
    }

    @Test
    void storePhoto_rejetteFichierNonImage() {
        MockMultipartFile file = new MockMultipartFile("photo", "x.pdf", "application/pdf", "%PDF-1.4".getBytes());
        assertThrows(IllegalArgumentException.class, () -> photoService.storePhoto(USER_ID, file));
    }

    @Test
    void storePhoto_rejetteTailleTropImportante() {
        byte[] big = new byte[2_097_153]; // > 2 Mo
        big[0] = (byte) 0xFF; big[1] = (byte) 0xD8; big[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("photo", "big.jpg", "image/jpeg", big);
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> photoService.storePhoto(USER_ID, file));
        assertTrue(e.getMessage().contains("2 Mo"));
    }

    @Test
    void storePhoto_detecteWebpMalgreContentTypeClient() {
        // Content-Type mensonger : la détection se fait sur les magic bytes
        MockMultipartFile file = new MockMultipartFile("photo", "x.bin", "application/octet-stream", webpBytes());
        when(storage.store(anyString(), any(byte[].class), eq("image/webp")))
            .thenAnswer(inv -> inv.getArgument(0));

        String key = photoService.storePhoto(USER_ID, file);
        assertTrue(key.endsWith(".webp"));
    }

    @Test
    void deletePhotoQuietly_supprimeSilencieusement() {
        photoService.deletePhotoQuietly("photos/" + USER_ID + "/abc.jpg");
        verify(storage).delete("photos/" + USER_ID + "/abc.jpg");
    }

    @Test
    void deletePhotoQuietly_ignoreNullEtVide() {
        photoService.deletePhotoQuietly(null);
        photoService.deletePhotoQuietly("");
        // Aucune interaction storage attendue
    }

    @Test
    void deletePhotoQuietly_nEchecPasSurErreur() {
        org.mockito.Mockito.doThrow(new RuntimeException("OOS down")).when(storage).delete(anyString());
        // Ne doit pas lever d'exception
        photoService.deletePhotoQuietly("photos/x/y.jpg");
    }

    @Test
    void exists_retourneVraiSiClePresente() {
        when(storage.exists("photos/u/1.jpg")).thenReturn(true);
        assertTrue(photoService.exists("photos/u/1.jpg"));
    }

    @Test
    void exists_retourneFauxSiCleNulle() {
        org.junit.jupiter.api.Assertions.assertFalse(photoService.exists(null));
        org.junit.jupiter.api.Assertions.assertFalse(photoService.exists(""));
        org.mockito.Mockito.verifyNoInteractions(storage);
    }
}
