package fr.dinum.beta.gouv.doctorat.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FresqSearchResult {
	@JsonProperty("content")
	private List<FresqSearchContent> content;
	
	@JsonProperty("totalElements")
	private long totalElements;
	
	@JsonProperty("totalPages")
	private int totalPages;
	
	@JsonProperty("pageNumber")
	private int pageNumber;
	
	@JsonProperty("pageSize")
	private int pageSize;
	
	@JsonProperty("numberOfElements")
	private int numberOfElements;
	
	public List<FresqSearchContent> getContent() {
		return content;
	}
	
	public void setContent(List<FresqSearchContent> content) {
		this.content = content;
	}
	
	public long getTotalElements() {
		return totalElements;
	}
	
	public void setTotalElements(long totalElements) {
		this.totalElements = totalElements;
	}
	
	public int getTotalPages() {
		return totalPages;
	}
	
	public void setTotalPages(int totalPages) {
		this.totalPages = totalPages;
	}
	
	public int getPageNumber() {
		return pageNumber;
	}
	
	public void setPageNumber(int pageNumber) {
		this.pageNumber = pageNumber;
	}
	
	public int getPageSize() {
		return pageSize;
	}
	
	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}
	
	public int getNumberOfElements() {
		return numberOfElements;
	}
	
	public void setNumberOfElements(int numberOfElements) {
		this.numberOfElements = numberOfElements;
	}
}
