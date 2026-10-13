package fr.dinum.beta.gouv.doctorat.service.storage;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.config.StorageProperties;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

/**
 * Stockage OUTSCALE Object Storage (API S3). Obligatoire en rec/prod.
 *
 * <p>Points d'attention OOS (cf. doc OUTSCALE, tracés dans {@code docs/secu/004-*} :</p>
 * <ul>
 *   <li>Checksums : ni le client ni les requêtes n'envoient de checksum par défaut
 *   grâce à {@code AWS_REQUEST_CHECKSUM_CALCULATION=WHEN_REQUIRED} /
 *   {@code AWS_RESPONSE_CHECKSUM_VALIDATION=WHEN_REQUIRED} (contournement officiel du
 *   {@code BadRequest "trailing checksum is not supported"}). À poser en variables
 *   Scalingo (rec/prod) et en {@code .env} local. La validation des réponses est
 *   aussi désactivée ici, OOS ne renvoyant pas les checksums AWS récents.</li>
 *   <li>Adressage virtuel d'hôte ({@code bucket.endpoint}), supporté par OOS.</li>
 *   <li>Chiffrement SSE-S3 systématique à l'écriture.</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(name = "storage.backend", havingValue = "oos")
public class OosS3StorageAdapter implements StoragePort, DisposableBean {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    public OosS3StorageAdapter(StorageProperties properties) {
        // StorageProperties a déjà validé (fail-fast) : bucket + clés obligatoires.
        StaticCredentialsProvider credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(
                        properties.getOos().getAccessKey(),
                        properties.getOos().getSecretKey()));
        URI endpoint = URI.create(properties.getOos().getEndpoint());
        Region region = Region.of(properties.getOos().getRegion());
        this.bucket = properties.getOos().getBucket();
        this.s3 = S3Client.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(false)
                        .build())
                .build();
        this.presigner = S3Presigner.builder()
                .endpointOverride(endpoint)
                .region(region)
                .credentialsProvider(credentials)
                .serviceConfiguration(S3Configuration.builder()
                        .checksumValidationEnabled(false)
                        .build())
                .build();
    }

    @Override
    public String store(String key, byte[] content, String contentType) {
        requireKey(key);
        try {
            s3.putObject(PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) content.length)
                    .serverSideEncryption(ServerSideEncryption.AES256)
                    .build(), RequestBody.fromBytes(content));
            return key;
        } catch (S3Exception e) {
            throw new StorageException("Erreur lors du stockage du fichier (" + e.statusCode() + ")", e);
        }
    }

    @Override
    public InputStream open(String key) {
        requireKey(key);
        try {
            return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (NoSuchKeyException e) {
            throw new StorageNotFoundException(key);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new StorageNotFoundException(key);
            }
            throw new StorageException("Erreur lors de la lecture du fichier (" + e.statusCode() + ")", e);
        }
    }

    @Override
    public long size(String key) {
        requireKey(key);
        try {
            return s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
                    .contentLength();
        } catch (NoSuchKeyException e) {
            throw new StorageNotFoundException(key);
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                throw new StorageNotFoundException(key);
            }
            throw new StorageException("Erreur lors de la lecture du fichier (" + e.statusCode() + ")", e);
        }
    }

    @Override
    public boolean exists(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        try {
            s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public String presignGet(String key, Duration ttl) {
        requireKey(key);
        if (!exists(key)) {
            throw new StorageNotFoundException(key);
        }
        return presigner.presignGetObject(GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder().bucket(bucket).key(key).build())
                .build()).url().toString();
    }

    @Override
    public void delete(String key) {
        requireKey(key);
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception e) {
            throw new StorageException("Erreur lors de la suppression du fichier (" + e.statusCode() + ")", e);
        }
    }

    @Override
    public void destroy() {
        presigner.close();
        s3.close();
    }

    private static void requireKey(String key) {
        if (key == null || key.isBlank()) {
            throw new StorageNotFoundException(key);
        }
    }
}
