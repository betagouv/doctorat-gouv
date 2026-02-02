package fr.dinum.beta.gouv.doctorat.dto;

public class Attachment {
	private final String name;
	private final String base64;

	public Attachment(String name, String base64) {
		this.name = name;
		this.base64 = base64;
	}

	public String getName() {
		return name;
	}

	public String getBase64() {
		return base64;
	}
}
