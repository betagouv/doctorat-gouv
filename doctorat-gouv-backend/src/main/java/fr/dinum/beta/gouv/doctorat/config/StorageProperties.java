package fr.dinum.beta.gouv.doctorat.config;

import jakarta.annotation.PostConstruct;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Backend de stockage des fichiers d'inscription (CV + pièces complémentaires).
 *
 * <ul>
 *   <li>{@code local} : disque éphémère ({@code upload.inscription.dir}).
 *   Réservé au dev et aux tests. Perdu à chaque restart sur Scalingo.</li>
 *   <li>{@code oos} : OUTSCALE Object Storage (API S3), obligatoire en rec/prod.
 *   Bucket + clés OBLIGATOIRES via {@code OOS_BUCKET / OOS_ACCESS_KEY / OOS_SECRET_KEY}.</li>
 * </ul>
 *
 * <p>Fail-fast au boot : backend inconnu ou backend {@code oos} incomplet =
 * {@link IllegalStateException} avec message explicite (jamais de log des clés).</p>
 */
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Backend actif : {@code local} ou {@code oos}.
     * Via la variable d'environnement {@code STORAGE_BACKEND}.
     */
    private String backend = "local";

    private final Oos oos = new Oos();

    @PostConstruct
    void validate() {
        if (!"local".equalsIgnoreCase(backend) && !"oos".equalsIgnoreCase(backend)) {
            throw new IllegalStateException(
                    "Stockage fichiers : storage.backend='" + backend + "' inconnu"
                            + " (attendu 'local' ou 'oos', via STORAGE_BACKEND)");
        }
        if (isOos() && (isBlank(oos.bucket) || isBlank(oos.accessKey) || isBlank(oos.secretKey))) {
            throw new IllegalStateException(
                    "Stockage fichiers : backend 'oos' incomplet."
                    + " Poser OOS_BUCKET, OOS_ACCESS_KEY et OOS_SECRET_KEY"
                    + " (variables Scalingo en rec/prod, .env en local).");
        }
    }

    public boolean isOos() {
        return "oos".equalsIgnoreCase(backend);
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public Oos getOos() {
        return oos;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public static class Oos {

        /** Endpoint S3 OOS, ex. https://oos.cloudgouv-eu-west-1.outscale.com */
        private String endpoint;

        /** Région OOS, ex. cloudgouv-eu-west-1 */
        private String region;

        /** Bucket dédié à l'environnement (dev / rec / prod séparés) */
        private String bucket;

        /** Access key de la paire dédiée à l'environnement (jamais committée) */
        private String accessKey;

        /** Secret key de la paire dédiée à l'environnement (jamais committée, jamais loggée) */
        private String secretKey;

        /** Durée de validité des URL présignées de lecture (secondes) */
        private int presignTtlSec = 300;

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getAccessKey() {
            return accessKey;
        }

        public void setAccessKey(String accessKey) {
            this.accessKey = accessKey;
        }

        public String getSecretKey() {
            return secretKey;
        }

        public void setSecretKey(String secretKey) {
            this.secretKey = secretKey;
        }

        public int getPresignTtlSec() {
            return presignTtlSec;
        }

        public void setPresignTtlSec(int presignTtlSec) {
            this.presignTtlSec = presignTtlSec;
        }
    }
}
