package fr.dinum.beta.gouv.doctorat.service.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Stockage disque local ({@code upload.inscription.dir}).
 * Réservé au dev et aux tests — éphémère sur Scalingo, ne jamais utiliser en rec/prod.
 *
 * <p>Repli transitoire : une clé absolue historique (lignes créées avant OOS)
 * est lue telle quelle au lieu d'être résolue sous le répertoire de base.</p>
 *
 * <p>Le premier segment {@code inscription/} de la clé est ignoré à la résolution :
 * le répertoire de base ({@code ./uploads/inscription}) porte déjà cet espace de noms.
 * L'arborescence locale est ainsi identique au contenu du bucket OOS
 * ({@code {userId}/cv|pieces/...}), sans doublon {@code inscription/inscription}.</p>
 */
@Service
@ConditionalOnProperty(name = "storage.backend", havingValue = "local", matchIfMissing = true)
public class LocalStorageAdapter implements StoragePort {

    private final Path baseDir;

    public LocalStorageAdapter(
            @Value("${upload.inscription.dir:./uploads/inscription}") String uploadDir) {
        this.baseDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, byte[] content, String contentType) {
        try {
            Path target = resolve(key);
            Files.createDirectories(target.getParent());
            Files.write(target, content);
            return key;
        } catch (IOException e) {
            throw new StorageException("Erreur lors du stockage du fichier", e);
        }
    }

    @Override
    public InputStream open(String key) {
        Path target = resolve(key);
        if (!Files.isRegularFile(target)) {
            throw new StorageNotFoundException(key);
        }
        try {
            return Files.newInputStream(target);
        } catch (IOException e) {
            throw new StorageException("Erreur lors de la lecture du fichier", e);
        }
    }

    @Override
    public long size(String key) {
        Path target = resolve(key);
        if (!Files.isRegularFile(target)) {
            throw new StorageNotFoundException(key);
        }
        try {
            return Files.size(target);
        } catch (IOException e) {
            throw new StorageException("Erreur lors de la lecture du fichier", e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return Files.isRegularFile(resolve(key));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        throw new UnsupportedOperationException("URL présignée indisponible en backend local");
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new StorageException("Erreur lors de la suppression du fichier", e);
        }
    }

    private Path resolve(String key) {
        if (key == null || key.isBlank()) {
            throw new StorageNotFoundException(key);
        }
        Path candidate = Path.of(key);
        if (candidate.isAbsolute()) {
            return candidate.normalize();
        }
        String relative = key.replace('\\', '/');
        if (relative.startsWith("inscription/")) {
            relative = relative.substring("inscription/".length());
        }
        if (relative.isBlank()) {
            throw new StorageNotFoundException(key);
        }
        Path target = baseDir.resolve(relative).normalize();
        if (!target.startsWith(baseDir)) {
            throw new StorageException("Clé de fichier invalide");
        }
        return target;
    }
}
