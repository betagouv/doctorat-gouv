package fr.dinum.beta.gouv.doctorat.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Représente une requête pour créer ou mettre à jour un document dans Albert.
 * 
 */
public class AlbertDocumentRequest {

	@JsonProperty("collection_id")
	private Integer collectionId;

	private Map<String, Object> metadata;

	public AlbertDocumentRequest() {
	}
	
	public AlbertDocumentRequest(Integer collectionId, Map<String, Object> metadata) {
		super();
		this.collectionId = collectionId;
		this.metadata = metadata;
	}

	public AlbertDocumentRequest(Map<String, Object> metadata) {
		this.metadata = metadata;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}

	public void setMetadata(Map<String, Object> metadata) {
		this.metadata = metadata;
	}
	
	public Integer getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(Integer collectionId) {
		this.collectionId = collectionId;
	}
	
}
