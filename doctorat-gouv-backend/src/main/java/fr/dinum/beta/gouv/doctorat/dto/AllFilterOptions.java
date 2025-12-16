package fr.dinum.beta.gouv.doctorat.dto;

import java.util.List;

/**
 * Représente le jeu complet d’options de filtres.
 * Chaque champ est une liste de valeurs uniques.
 */
public record AllFilterOptions(
        List<String> discipline,
        List<String> thematique,
        List<String> localisation,
        List<String> laboratoire,
        List<String> ecole
) {}
