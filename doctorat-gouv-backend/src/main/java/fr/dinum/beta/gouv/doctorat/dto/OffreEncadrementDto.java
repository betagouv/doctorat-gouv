package fr.dinum.beta.gouv.doctorat.dto;

/**
 * Fiche offre d'encadrement : l'offre + les données publiques du chercheur
 * (résolues depuis son profil, sans colonne supplémentaire en base).
 */
public class OffreEncadrementDto {

    private PropositionTheseDto offre;
    private DirecteurOffreDto directeur;

    public PropositionTheseDto getOffre() { return offre; }
    public void setOffre(PropositionTheseDto offre) { this.offre = offre; }

    public DirecteurOffreDto getDirecteur() { return directeur; }
    public void setDirecteur(DirecteurOffreDto directeur) { this.directeur = directeur; }
}
