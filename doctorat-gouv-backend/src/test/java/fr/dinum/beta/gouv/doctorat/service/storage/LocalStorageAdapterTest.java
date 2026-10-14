package fr.dinum.beta.gouv.doctorat.service.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Adapter local : aucune dépendance réseau, répertoire temporaire par test.
 */
class LocalStorageAdapterTest {

    @TempDir
    private Path base;

    private LocalStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new LocalStorageAdapter(base.resolve("uploads/inscription").toString());
    }

    @Test
    void store_puisOpen_etSize_arrondiComplet() throws IOException {
        byte[] contenu = "contenu-pdf".getBytes(StandardCharsets.UTF_8);
        String key = "inscription/user-1/cv/uuid_cv.pdf";

        assertEquals(key, adapter.store(key, contenu, "application/pdf"));
        assertTrue(adapter.exists(key));
        assertEquals(contenu.length, adapter.size(key));
        try (InputStream in = adapter.open(key)) {
            assertArrayEquals(contenu, in.readAllBytes());
        }
    }

    @Test
    void store_ignorePremierSegmentInscription() {
        String key = "inscription/user-1/pieces/uuid_p.pdf";
        adapter.store(key, new byte[] { 1 }, "application/pdf");

        // Pas de doublon inscription/inscription : l'arbre local reflete le bucket.
        assertTrue(Files.isRegularFile(base.resolve("uploads/inscription/user-1/pieces/uuid_p.pdf")));
        assertFalse(Files.exists(base.resolve("uploads/inscription/inscription")));
    }

    @Test
    void open_absent_leveNotFound() {
        assertThrows(StorageNotFoundException.class, () -> adapter.open("inscription/x/cv/a.pdf"));
        assertThrows(StorageNotFoundException.class, () -> adapter.size("inscription/x/cv/a.pdf"));
        assertFalse(adapter.exists("inscription/x/cv/a.pdf"));
    }

    @Test
    void delete_supprimeEtResteIdempotent() {
        String key = "inscription/user-1/cv/uuid_cv.pdf";
        adapter.store(key, new byte[] { 1 }, "application/pdf");

        adapter.delete(key);

        assertFalse(adapter.exists(key));
        adapter.delete(key);
    }

    @Test
    void cheminAbsoluHistorique_luTelQuel() throws IOException {
        Path historique = base.resolve("ancien.pdf");
        Files.write(historique, new byte[] { 2 });

        try (InputStream in = adapter.open(historique.toString())) {
            assertArrayEquals(new byte[] { 2 }, in.readAllBytes());
        }
    }

    @Test
    void tentativeTraversee_rejetee() {
        assertThrows(StorageException.class,
            () -> adapter.store("../echappe.pdf", new byte[] { 1 }, "application/pdf"));
        assertFalse(adapter.exists("../echappe.pdf"));
        assertFalse(Files.exists(base.resolve("echappe.pdf")));
    }

    @Test
    void presignGet_nonSupporteEnLocal() {
        assertThrows(UnsupportedOperationException.class,
            () -> adapter.presignGet("inscription/x/cv/a.pdf", Duration.ofMinutes(5)));
    }
}
