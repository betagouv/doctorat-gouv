package fr.dinum.beta.gouv.doctorat.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import fr.dinum.beta.gouv.doctorat.dto.PropositionTheseDto;
import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;
import fr.dinum.beta.gouv.doctorat.mapper.PropositionTheseMapper;
import fr.dinum.beta.gouv.doctorat.repository.PropositionTheseRepository;
import jakarta.persistence.criteria.Predicate;

@Service
public class PropositionTheseService {

	private final PropositionTheseRepository propositionTheseRepository;

	public PropositionTheseService(PropositionTheseRepository propositionTheseRepository) {
		this.propositionTheseRepository = propositionTheseRepository;
	}

	public List<PropositionTheseDto> search(Map<String, String> filters) {
		List<PropositionThese> entities = propositionTheseRepository.findAll(buildSpecification(filters));
		return entities.stream().map(PropositionTheseMapper::toDto) // appel statique
				.toList();
	}

	private Specification<PropositionThese> buildSpecification(Map<String, String> filters) {
		return (root, query, cb) -> {
			Predicate predicate = cb.conjunction();

			filters.forEach((key, value) -> {
				if (value != null && !value.isBlank()) {
					switch (key) {
					case "discipline" -> predicate.getExpressions().add(cb.equal(root.get("specialite"), value));
					case "localisation" ->
						predicate.getExpressions().add(cb.equal(root.get("uniteRechercheVille"), value));
					case "laboratoire" -> predicate.getExpressions()
							.add(cb.like(cb.lower(root.get("uniteRechercheLibelle")), "%" + value.toLowerCase() + "%"));
					case "ecole" -> predicate.getExpressions().add(cb.equal(root.get("ecoleDoctoralNumero"), value));
					case "financement" -> predicate.getExpressions().add(cb.equal(root.get("financementEtat"), value));
					case "contrat" ->
						predicate.getExpressions().add(cb.equal(root.get("candidatureEnLignePossible"), value));
					default -> {
						// ignore unknown filters
					}
					}
				}
			});

			return predicate;
		};
	}
}
