package fr.dinum.beta.gouv.doctorat.dto;

public class VectorSearchHit {

	private Long sujetId;
	private double score;
	private String blocType;
	private String contenuMatche;

	public VectorSearchHit() {}

	public VectorSearchHit(Long sujetId, double score, String blocType, String contenuMatche) {
		this.sujetId = sujetId;
		this.score = score;
		this.blocType = blocType;
		this.contenuMatche = contenuMatche;
	}

	public Long getSujetId() { return sujetId; }
	public void setSujetId(Long sujetId) { this.sujetId = sujetId; }

	public double getScore() { return score; }
	public void setScore(double score) { this.score = score; }

	public String getBlocType() { return blocType; }
	public void setBlocType(String blocType) { this.blocType = blocType; }

	public String getContenuMatche() { return contenuMatche; }
	public void setContenuMatche(String contenuMatche) { this.contenuMatche = contenuMatche; }
}
