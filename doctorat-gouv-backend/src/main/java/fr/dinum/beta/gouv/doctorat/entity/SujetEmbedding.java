package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sujet_embedding")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SujetEmbedding {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "proposition_these_id", nullable = false)
	private Long propositionTheseId;

	@Column(name = "bloc_type", nullable = false, length = 30)
	private String blocType;

	@Column(name = "contenu", columnDefinition = "TEXT", nullable = false)
	private String contenu;

	@Column(name = "embedding", columnDefinition = "vector(1024)", nullable = false)
	private float[] embedding;

	@Column(name = "date_indexation", nullable = false)
	private LocalDateTime dateIndexation;

	@Column(name = "version_modele", length = 50)
	private String versionModele;

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public Long getPropositionTheseId() { return propositionTheseId; }
	public void setPropositionTheseId(Long propositionTheseId) { this.propositionTheseId = propositionTheseId; }

	public String getBlocType() { return blocType; }
	public void setBlocType(String blocType) { this.blocType = blocType; }

	public String getContenu() { return contenu; }
	public void setContenu(String contenu) { this.contenu = contenu; }

	public float[] getEmbedding() { return embedding; }
	public void setEmbedding(float[] embedding) { this.embedding = embedding; }

	public LocalDateTime getDateIndexation() { return dateIndexation; }
	public void setDateIndexation(LocalDateTime dateIndexation) { this.dateIndexation = dateIndexation; }

	public String getVersionModele() { return versionModele; }
	public void setVersionModele(String versionModele) { this.versionModele = versionModele; }
}
