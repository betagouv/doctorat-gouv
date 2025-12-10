package fr.dinum.beta.gouv.doctorat.model;

import java.util.List;

import fr.dinum.beta.gouv.doctorat.entity.PropositionThese;

public class AdumResponse {
	private List<PropositionThese> propositions;

	public List<PropositionThese> getPropositions() {
		return propositions;
	}

	public void setPropositions(List<PropositionThese> propositions) {
		this.propositions = propositions;
	}
}
