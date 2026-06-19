package fr.dinum.beta.gouv.doctorat.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

@Component
public class BlocExtractor {

	private static final int MAX_CHARS = 2500;

	public List<BlocSujet> extraireBlocs(PropositionThese sujet) {
		List<BlocSujet> blocs = new ArrayList<>();

		ajouterBloc(blocs, sujet.getId(), "titre",
			join(sujet.getTheseTitre(), sujet.getTheseTitreAnglais()));

		ajouterBloc(blocs, sujet.getId(), "resume",
			join(sujet.getResume(), sujet.getResumeAnglais()));

		ajouterBloc(blocs, sujet.getId(), "objectif", sujet.getObjectif());
		ajouterBloc(blocs, sujet.getId(), "contexte", sujet.getContexte());

		ajouterBloc(blocs, sujet.getId(), "mots_cles",
			joinMap(sujet.getMotsCles(), sujet.getMotsClesAnglais()));

		ajouterBloc(blocs, sujet.getId(), "profil",
			join(sujet.getProfilRecherche(), sujet.getProfilRechercheAnglais()));

		ajouterBloc(blocs, sujet.getId(), "localisation",
			join(sujet.getUniteRechercheVille(),
			     sujet.getUniteRechercheCodePostal(),
			     sujet.getEtablissementVille(),
			     sujet.getEtablissementCodePostal()));

		return blocs;
	}

	private void ajouterBloc(List<BlocSujet> blocs, Long sujetId, String type, String contenu) {
		if (contenu == null || contenu.isBlank()) return;
		if (contenu.length() > MAX_CHARS) contenu = contenu.substring(0, MAX_CHARS);
		blocs.add(new BlocSujet(sujetId, type, contenu));
	}

	private String join(String... valeurs) {
		return Stream.of(valeurs)
			.filter(v -> v != null && !v.isBlank())
			.collect(Collectors.joining("\n\n"));
	}

	private String joinMap(Map<String, String>... maps) {
		return Stream.of(maps)
			.filter(m -> m != null)
			.flatMap(m -> m.values().stream())
			.filter(v -> v != null && !v.isBlank())
			.collect(Collectors.joining(", "));
	}

	public static class BlocSujet {
		private final Long sujetId;
		private final String type;
		private final String contenu;

		public BlocSujet(Long sujetId, String type, String contenu) {
			this.sujetId = sujetId;
			this.type = type;
			this.contenu = contenu;
		}

		public Long getSujetId() { return sujetId; }
		public String getType() { return type; }
		public String getContenu() { return contenu; }
	}
}
