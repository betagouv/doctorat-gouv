package fr.dinum.beta.gouv.doctorat.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Validation fail-fast du backend de stockage (sans contexte Spring).
 */
class StoragePropertiesTest {

    private StorageProperties local() {
        StorageProperties properties = new StorageProperties();
        properties.setBackend("local");
        return properties;
    }

    private StorageProperties oosComplet() {
        StorageProperties properties = new StorageProperties();
        properties.setBackend("oos");
        properties.getOos().setEndpoint("https://oos.cloudgouv-eu-west-1.outscale.com");
        properties.getOos().setRegion("cloudgouv-eu-west-1");
        properties.getOos().setBucket("doctorat-gouv-rec");
        properties.getOos().setAccessKey("AK_TEST");
        properties.getOos().setSecretKey("SK_TEST");
        return properties;
    }

    @Test
    void local_sansCles_neLevePas() {
        assertDoesNotThrow(() -> local().validate());
    }

    @Test
    void oos_complet_neLevePas() {
        assertDoesNotThrow(() -> oosComplet().validate());
    }

    @Test
    void backendInconnu_leve() {
        StorageProperties properties = local();
        properties.setBackend("ftp");

        IllegalStateException e = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(e.getMessage().contains("STORAGE_BACKEND"));
    }

    @Test
    void oos_sansBucket_leve() {
        StorageProperties properties = oosComplet();
        properties.getOos().setBucket(" ");

        assertThrows(IllegalStateException.class, properties::validate);
    }

    @Test
    void oos_sansCles_leveSansExposerLesCles() {
        StorageProperties properties = oosComplet();
        properties.getOos().setAccessKey("");
        properties.getOos().setSecretKey(null);

        IllegalStateException e = assertThrows(IllegalStateException.class, properties::validate);
        assertTrue(e.getMessage().contains("OOS_BUCKET"));
    }

    @Test
    void isOos_insensibleALaCasse() {
        StorageProperties properties = local();
        properties.setBackend("OOS");
        properties.getOos().setBucket("b");
        properties.getOos().setAccessKey("a");
        properties.getOos().setSecretKey("s");

        assertTrue(properties.isOos());
        assertDoesNotThrow(properties::validate);
    }
}
