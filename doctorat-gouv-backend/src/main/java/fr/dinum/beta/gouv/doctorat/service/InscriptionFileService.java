package fr.dinum.beta.gouv.doctorat.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service de stockage des fichiers uploadés lors de l'inscription (CV + pièces complémentaires).
 * Valide le format (PDF) et la taille, puis sauvegarde sur disque dans le répertoire configuré
 * (upload.inscription.dir).
 */
@Service
public class InscriptionFileService {

    private static final long CV_MAX_SIZE = 1_048_576L;       // 1 Mo
    private static final long PIECE_MAX_SIZE = 5_242_880L;    // 5 Mo
    private static final int MAX_PIECES = 5;

    private final String uploadDir;

    public InscriptionFileService(
            @Value("${upload.inscription.dir:./uploads/inscription}") String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String storeCv(String userId, MultipartFile file) {
        validatePdf(file, CV_MAX_SIZE, "CV");
        Path dir = Path.of(uploadDir, userId, "cv");
        return store(file, dir);
    }

    public List<String> storePieces(String userId, List<MultipartFile> files) {
        List<MultipartFile> safe = (files == null) ? List.of() : files;
        if (safe.size() > MAX_PIECES) {
            throw new IllegalArgumentException("Maximum " + MAX_PIECES + " pièces complémentaires autorisées");
        }
        List<String> paths = new ArrayList<>();
        Path dir = Path.of(uploadDir, userId, "pieces");
        for (MultipartFile file : safe) {
            validatePdf(file, PIECE_MAX_SIZE, "Pièce complémentaire");
            paths.add(store(file, dir));
        }
        return paths;
    }

    private void validatePdf(MultipartFile file, long maxSize, String label) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(label + " : aucun fichier fourni");
        }
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        boolean isPdf = "application/pdf".equalsIgnoreCase(file.getContentType()) || name.endsWith(".pdf");
        if (!isPdf) {
            throw new IllegalArgumentException(label + " : le format PDF est requis");
        }
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException(label + " : la taille du fichier est trop importante");
        }
    }

    private String store(MultipartFile file, Path dir) {
        try {
            Path absoluteDir = dir.toAbsolutePath();
            Files.createDirectories(absoluteDir);
            String safeName = System.currentTimeMillis() + "_"
                    + file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_");
            Path target = absoluteDir.resolve(safeName);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return target.toString();
        } catch (IOException e) {
            throw new RuntimeException("Erreur lors du stockage du fichier", e);
        }
    }
}
