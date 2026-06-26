package fr.dinum.beta.gouv.doctorat.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "scaleway_query_log")
@NoArgsConstructor
public class ScalewayQueryLog {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "query", columnDefinition = "TEXT", nullable = false)
	private String query;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public ScalewayQueryLog(String query, LocalDateTime createdAt) {
		this.query = query;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }

	public String getQuery() { return query; }
	public void setQuery(String query) { this.query = query; }

	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
