package fr.dinum.beta.gouv.doctorat.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FresqEtablissementResult {
	@JsonProperty("datas")
	private List<FresqEtablissement> datas;
	
	@JsonProperty("nextPageOffset")
	private Integer nextPageOffset;
	
	public List<FresqEtablissement> getDatas() {
		return datas;
	}
	
	public void setDatas(List<FresqEtablissement> datas) {
		this.datas = datas;
	}
	
	public Integer getNextPageOffset() {
		return nextPageOffset;
	}
	
	public void setNextPageOffset(Integer nextPageOffset) {
		this.nextPageOffset = nextPageOffset;
	}
	
	public boolean isEmpty() {
		return datas == null || datas.isEmpty();
	}
}
