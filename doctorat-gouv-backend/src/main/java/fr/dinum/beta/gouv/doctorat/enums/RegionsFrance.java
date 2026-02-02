package fr.dinum.beta.gouv.doctorat.enums;

import java.util.*;
import java.util.stream.Collectors;

public class RegionsFrance {

    /** Mapping département → région */
    private static final Map<String, String> DEPT_TO_REGION = Map.ofEntries(
        Map.entry("01", "Auvergne-Rhône-Alpes"),
        Map.entry("02", "Hauts-de-France"),
        Map.entry("03", "Auvergne-Rhône-Alpes"),
        Map.entry("04", "Provence-Alpes-Côte d'Azur"),
        Map.entry("05", "Provence-Alpes-Côte d'Azur"),
        Map.entry("06", "Provence-Alpes-Côte d'Azur"),
        Map.entry("07", "Auvergne-Rhône-Alpes"),
        Map.entry("08", "Grand Est"),
        Map.entry("09", "Occitanie"),
        Map.entry("10", "Grand Est"),
        Map.entry("11", "Occitanie"),
        Map.entry("12", "Occitanie"),
        Map.entry("13", "Provence-Alpes-Côte d'Azur"),
        Map.entry("14", "Normandie"),
        Map.entry("15", "Auvergne-Rhône-Alpes"),
        Map.entry("16", "Nouvelle-Aquitaine"),
        Map.entry("17", "Nouvelle-Aquitaine"),
        Map.entry("18", "Centre-Val de Loire"),
        Map.entry("19", "Nouvelle-Aquitaine"),

        Map.entry("2A", "Corse"),
        Map.entry("2B", "Corse"),

        Map.entry("21", "Bourgogne-Franche-Comté"),
        Map.entry("22", "Bretagne"),
        Map.entry("23", "Nouvelle-Aquitaine"),
        Map.entry("24", "Nouvelle-Aquitaine"),
        Map.entry("25", "Bourgogne-Franche-Comté"),
        Map.entry("26", "Auvergne-Rhône-Alpes"),
        Map.entry("27", "Normandie"),
        Map.entry("28", "Centre-Val de Loire"),
        Map.entry("29", "Bretagne"),

        Map.entry("30", "Occitanie"),
        Map.entry("31", "Occitanie"),
        Map.entry("32", "Occitanie"),
        Map.entry("33", "Nouvelle-Aquitaine"),
        Map.entry("34", "Occitanie"),
        Map.entry("35", "Bretagne"),
        Map.entry("36", "Centre-Val de Loire"),
        Map.entry("37", "Centre-Val de Loire"),
        Map.entry("38", "Auvergne-Rhône-Alpes"),
        Map.entry("39", "Bourgogne-Franche-Comté"),

        Map.entry("40", "Nouvelle-Aquitaine"),
        Map.entry("41", "Centre-Val de Loire"),
        Map.entry("42", "Auvergne-Rhône-Alpes"),
        Map.entry("43", "Auvergne-Rhône-Alpes"),
        Map.entry("44", "Pays de la Loire"),
        Map.entry("45", "Centre-Val de Loire"),
        Map.entry("46", "Occitanie"),
        Map.entry("47", "Nouvelle-Aquitaine"),
        Map.entry("48", "Occitanie"),
        Map.entry("49", "Pays de la Loire"),

        Map.entry("50", "Normandie"),
        Map.entry("51", "Grand Est"),
        Map.entry("52", "Grand Est"),
        Map.entry("53", "Pays de la Loire"),
        Map.entry("54", "Grand Est"),
        Map.entry("55", "Grand Est"),
        Map.entry("56", "Bretagne"),
        Map.entry("57", "Grand Est"),
        Map.entry("58", "Bourgogne-Franche-Comté"),
        Map.entry("59", "Hauts-de-France"),

        Map.entry("60", "Hauts-de-France"),
        Map.entry("61", "Normandie"),
        Map.entry("62", "Hauts-de-France"),
        Map.entry("63", "Auvergne-Rhône-Alpes"),
        Map.entry("64", "Nouvelle-Aquitaine"),
        Map.entry("65", "Occitanie"),
        Map.entry("66", "Occitanie"),
        Map.entry("67", "Grand Est"),
        Map.entry("68", "Grand Est"),
        Map.entry("69", "Auvergne-Rhône-Alpes"),

        Map.entry("70", "Bourgogne-Franche-Comté"),
        Map.entry("71", "Bourgogne-Franche-Comté"),
        Map.entry("72", "Pays de la Loire"),
        Map.entry("73", "Auvergne-Rhône-Alpes"),
        Map.entry("74", "Auvergne-Rhône-Alpes"),
        Map.entry("75", "Île-de-France"),
        Map.entry("76", "Normandie"),
        Map.entry("77", "Île-de-France"),
        Map.entry("78", "Île-de-France"),
        Map.entry("79", "Nouvelle-Aquitaine"),

        Map.entry("80", "Hauts-de-France"),
        Map.entry("81", "Occitanie"),
        Map.entry("82", "Occitanie"),
        Map.entry("83", "Provence-Alpes-Côte d'Azur"),
        Map.entry("84", "Provence-Alpes-Côte d'Azur"),
        Map.entry("85", "Pays de la Loire"),
        Map.entry("86", "Nouvelle-Aquitaine"),
        Map.entry("87", "Nouvelle-Aquitaine"),
        Map.entry("88", "Grand Est"),
        Map.entry("89", "Bourgogne-Franche-Comté"),

        Map.entry("90", "Bourgogne-Franche-Comté"),
        Map.entry("91", "Île-de-France"),
        Map.entry("92", "Île-de-France"),
        Map.entry("93", "Île-de-France"),
        Map.entry("94", "Île-de-France"),
        Map.entry("95", "Île-de-France"),

        Map.entry("971", "Guadeloupe"),
        Map.entry("972", "Martinique"),
        Map.entry("973", "Guyane"),
        Map.entry("974", "La Réunion"),
        Map.entry("976", "Mayotte")
    );

    /** Retourne la région à partir d’un code postal */
    public static String regionFromCodePostal(String cp) {
        if (cp == null || cp.length() < 2) return null;

        cp = cp.trim();

        // Outre-mer : 971xx, 972xx, 973xx, 974xx, 976xx
        if (cp.startsWith("97") || cp.startsWith("98")) {
            String dept = cp.substring(0, 3);
            return DEPT_TO_REGION.get(dept);
        }

        // Corse : 20xxx → 2A ou 2B
        if (cp.startsWith("20")) {
            int thirdDigit = Integer.parseInt(cp.substring(2, 3));
            return thirdDigit <= 1 ? DEPT_TO_REGION.get("2A") : DEPT_TO_REGION.get("2B");
        }

        // Métropole : 2 premiers chiffres
        String dept = cp.substring(0, 2);
        return DEPT_TO_REGION.get(dept);
    }

    /** Retourne la liste des départements appartenant à une région */
    public static List<String> departementsFromRegion(String region) {
        if (region == null) return List.of();

        return DEPT_TO_REGION.entrySet().stream()
                .filter(e -> e.getValue().equalsIgnoreCase(region.trim()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /** Retourne toutes les régions distinctes */
    public static List<String> allRegions() {
        return DEPT_TO_REGION.values().stream()
                .distinct()
                .sorted()
                .toList();
    }
}

