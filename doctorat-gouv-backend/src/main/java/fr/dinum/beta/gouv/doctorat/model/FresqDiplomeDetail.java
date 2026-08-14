package fr.dinum.beta.gouv.doctorat.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FresqDiplomeDetail {
	@JsonProperty("id")
	private String id;
	
	@JsonProperty("last_modified")
	private Long lastModified;
	
	@JsonProperty("date_creation")
	private Long dateCreation;
	
	@JsonProperty("data")
	private Map<String, Object> data;
	
	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}
	
	public Long getLastModified() {
		return lastModified;
	}
	
	public void setLastModified(Long lastModified) {
		this.lastModified = lastModified;
	}
	
	public Long getDateCreation() {
		return dateCreation;
	}
	
	public void setDateCreation(Long dateCreation) {
		this.dateCreation = dateCreation;
	}
	
	public Map<String, Object> getData() {
		return data;
	}
	
	public void setData(Map<String, Object> data) {
		this.data = data;
	}
}
