package fr.dinum.beta.gouv.doctorat.enums;

public enum DomaineScientifique {
	MATHEMATIQUES("1", "Mathématiques et leurs interactions"), 
	PHYSIQUE("2", "Physique"),
	TERRE_UNIVERS("3", "Sciences de la terre et de l'univers, espace"), 
	CHIMIE("4", "Chimie"),
	BIO_MEDECINE("5", "Biologie, médecine et santé"), 
	SHS("6", "Sciences humaines et humanités"),
	SOCIETE("7", "Sciences de la société"), 
	INGENIEUR("8", "Sciences pour l'ingénieur"),
	STIC("9", "Sciences et technologies de l'information et de la communication"),
	AGRONOMIE("10", "Sciences agronomiques et écologiques");

	private final String code;
	private final String label;

	DomaineScientifique(String code, String label) {
		this.code = code;
		this.label = label;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public static String labelFromCode(String code) {
		for (DomaineScientifique d : values()) {
			if (d.code.equals(code))
				return d.label;
		}
		return null; // ou throw exception
	}
	
	public static String codeFromLabel(String label) {
	    for (DomaineScientifique d : values()) {
	        if (d.getLabel().equalsIgnoreCase(label.trim())) {
	            return d.getCode();
	        }
	    }
	    return null; // ou exception si tu veux
	}

}
