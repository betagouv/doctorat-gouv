package fr.dinum.beta.gouv.doctorat.service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

/**
 * Service dédié à la création de documents Albert à partir de sujets de thèse.
 */
@Service
public class AlbertDocumentService {
	
  private static final Logger log = LoggerFactory.getLogger(AlbertDocumentService.class);

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper = new ObjectMapper();

	@Value("${albert.api.key}")
	private String apiKey;

	@Value("${albert.collection.name}")
	private String collectionName;
	
	@Value("${albert.collection.id}")
	private Integer collectionId;

	@Value("${albert.base-url:https://albert.api.etalab.gouv.fr/v1}")
	private String baseUrl;

  public AlbertDocumentService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  public Long uploadDocumentToCollection(
      int collectionId,
      byte[] fileBytes,
      String fileName,
      String contentType,
      Map<String, Object> metadata // can be null
  ) {
    String url = baseUrl + "/documents";

    HttpHeaders headers = new HttpHeaders();
    headers.setBearerAuth(apiKey);
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);

    MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();

    // (1) REQUIRED: file part name must be "file"
    ByteArrayResource filePart = new ByteArrayResource(fileBytes) {
      @Override
      public String getFilename() {
        return fileName;
      }
    };
    HttpHeaders fileHeaders = new HttpHeaders();
    fileHeaders.setContentType(MediaType.parseMediaType(contentType != null ? contentType : "application/pdf"));
    HttpEntity<ByteArrayResource> fileEntity = new HttpEntity<>(filePart, fileHeaders);
    multipartBody.add("file", fileEntity);

    // (2) REQUIRED: collection_id as form field
    multipartBody.add("collection_id", String.valueOf(collectionId));

    // (3) OPTIONAL: metadata (send as JSON string if your API supports it)
    if (metadata != null && !metadata.isEmpty()) {
      try {
        multipartBody.add("metadata", objectMapper.writeValueAsString(metadata));
      } catch (Exception e) {
        throw new RuntimeException("Failed to serialize metadata to JSON", e);
      }
    }

    HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(multipartBody, headers);

    ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

    if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null || response.getBody().get("id") == null) {
      throw new RuntimeException("Upload failed: HTTP " + response.getStatusCode() + " body=" + response.getBody());
    }

    Object id = response.getBody().get("id");
    return (id instanceof Number) ? ((Number) id).longValue() : Long.parseLong(String.valueOf(id));
  }
  
  public Long createDocument(PropositionThese sujet) {

	    Map<String, Object> metadata = new HashMap<>();
	    metadata.put("id_interne", sujet.getId());
	    metadata.put("matricule", sujet.getMatricule());
	    metadata.put("titre", sujet.getTheseTitre());
	    metadata.put("etablissement", sujet.getEtablissementLibelle());

	    // Génération d’un vrai PDF textuel
	    byte[] pdfBytes = generatePdfForSujet(sujet);

	    return uploadDocumentToCollection(
	        collectionId,
	        pdfBytes,
	        "sujet-" + sujet.getId() + ".pdf",
	        "application/pdf",
	        metadata
	    );
	}

  private byte[] generatePdfForSujet(PropositionThese sujet) {
	    try (ByteArrayOutputStream out = new ByteArrayOutputStream();
	         PDDocument doc = new PDDocument()) {

	        PDPage page = new PDPage();
	        doc.addPage(page);

	        PDPageContentStream content = new PDPageContentStream(doc, page);

	        content.beginText();
	        content.setFont(PDType1Font.HELVETICA_BOLD, 16);
	        content.newLineAtOffset(50, 750);
	        content.showText("Sujet de thèse");
	        content.endText();

	        content.beginText();
	        content.setFont(PDType1Font.HELVETICA, 12);
	        content.newLineAtOffset(50, 720);
	        content.showText("Titre : " + sujet.getTheseTitre());
	        content.endText();

	        content.beginText();
	        content.setFont(PDType1Font.HELVETICA, 12);
	        content.newLineAtOffset(50, 700);
	        content.showText("Matricule : " + sujet.getMatricule());
	        content.endText();

	        content.beginText();
	        content.setFont(PDType1Font.HELVETICA, 12);
	        content.newLineAtOffset(50, 680);
	        content.showText("Établissement : " + sujet.getEtablissementLibelle());
	        content.endText();

	        content.close();

	        doc.save(out);
	        return out.toByteArray();

	    } catch (Exception e) {
	        throw new RuntimeException("Erreur lors de la génération du PDF", e);
	    }
	}



  // Example usage helper
  public Long uploadExample() {
    Map<String, Object> metadata = new HashMap<>();
    metadata.put("id_interne", 1);
    metadata.put("matricule", "p72621");
    metadata.put("titre", "Organisation 3D des chromosomes ...");
    metadata.put("etablissement", "Université de Montpellier");

    byte[] pdfBytes = (
    	    "%PDF-1.4\n" +
    	    "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
    	    "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
    	    "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R >> endobj\n" +
    	    "4 0 obj << /Length 44 >> stream\n" +
    	    "BT /F1 24 Tf 72 100 Td (Hello Albert) Tj ET\n" +
    	    "endstream endobj\n" +
    	    "xref\n" +
    	    "0 5\n" +
    	    "0000000000 65535 f \n" +
    	    "0000000010 00000 n \n" +
    	    "0000000060 00000 n \n" +
    	    "0000000110 00000 n \n" +
    	    "0000000200 00000 n \n" +
    	    "trailer << /Size 5 /Root 1 0 R >>\n" +
    	    "startxref\n" +
    	    "300\n" +
    	    "%%EOF"
    	).getBytes(StandardCharsets.UTF_8);

    return uploadDocumentToCollection(211775, pdfBytes, "these.pdf", "application/pdf", metadata);
  }
}

