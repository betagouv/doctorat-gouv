package fr.dinum.beta.gouv.doctorat.service.storage;

import java.io.InputStream;
import java.time.Duration;

/**
 * Port de stockage des fichiers d'inscription (CV + pièces complémentaires).
 *
 * <p>Deux implémentations, choisies par {@code storage.backend} :
 * <ul>
 *   <li>{@code local} : {@link LocalStorageAdapter} (disque, dev/test).</li>
 *   <li>{@code oos} : {@link OosS3StorageAdapter} (OUTSCALE OOS, rec/prod).</li>
 * </ul>
 *
 * <p>Les valeurs persistées en base ({@code cvFilename}, {@code piece_path}) sont des
 * <strong>clés objet</strong> ({@code inscription/{userId}/cv|pieces/{uuid}_{nom}.pdf}),
 * jamais des chemins disque. Les lignes historiques en chemin absolu local restent
 * lisibles par l'adapter local (repli transitoire, dev uniquement).</p>
 */
public interface StoragePort {

    /**
     * Stocke le contenu sous la clé donnée et retourne cette clé (à persister en base).
     */
    String store(String key, byte[] content, String contentType);

    /**
     * Ouvre un flux de lecture. L'appelant doit le fermer.
     * @throws StorageNotFoundException si la clé n'existe pas
     */
    InputStream open(String key);

    /**
     * Taille en octets (pour {@code Content-Length}).
     * @throws StorageNotFoundException si la clé n'existe pas
     */
    long size(String key);

    /**
     * Vrai si la clé existe et est lisible. Ne lève jamais d'exception.
     */
    boolean exists(String key);

    /**
     * URL présignée de lecture directe, valable {@code ttl}.
     * @throws StorageNotFoundException si la clé n'existe pas
     * @throws UnsupportedOperationException en backend local
     */
    String presignGet(String key, Duration ttl);

    /**
     * Supprime la clé (idempotent, usage RGPD).
     */
    void delete(String key);

    /**
     * Nom affichable d'une clé objet ou d'un chemin historique :
     * nom de fichier seul, sans préfixe technique (horodatage historique ou UUID).
     */
    static String displayName(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String name = key.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        return name.replaceFirst("^(\\d+|[0-9a-fA-F-]{36})_", "");
    }
}
