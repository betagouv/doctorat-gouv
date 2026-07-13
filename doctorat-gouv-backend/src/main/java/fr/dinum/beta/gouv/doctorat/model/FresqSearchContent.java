package fr.dinum.beta.gouv.doctorat.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FresqSearchContent {
	@JsonProperty("recordId")
	private String recordId;
	
	@JsonProperty("collectionId")
	private String collectionId;
	
	@JsonProperty("bucketId")
	private String bucketId;
	
	@JsonProperty("data")
	private Map<String, Object> data;
	
	public String getRecordId() {
		return recordId;
	}
	
	public void setRecordId(String recordId) {
		this.recordId = recordId;
	}
	
	public String getCollectionId() {
		return collectionId;
	}
	
	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}
	
	public String getBucketId() {
		return bucketId;
	}
	
	public void setBucketId(String bucketId) {
		this.bucketId = bucketId;
	}
	
	public Map<String, Object> getData() {
		return data;
	}
	
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
}
