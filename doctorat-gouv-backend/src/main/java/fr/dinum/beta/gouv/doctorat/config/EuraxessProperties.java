package fr.dinum.beta.gouv.doctorat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration du flux EURAXESS.
 */
@Configuration
@ConfigurationProperties(prefix = "euraxess")
public class EuraxessProperties {

	private String organisationIdKey;
	private String datasourceKey;
	private String contactEmail;
	private boolean validationEnabled = false;

	public String getOrganisationIdKey() {
		return organisationIdKey;
	}

	public void setOrganisationIdKey(String organisationIdKey) {
		this.organisationIdKey = organisationIdKey;
	}

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public void setDatasourceKey(String datasourceKey) {
		this.datasourceKey = datasourceKey;
	}

	public String getContactEmail() {
		return contactEmail;
	}

	public void setContactEmail(String contactEmail) {
		this.contactEmail = contactEmail;
	}

	public boolean isValidationEnabled() {
		return validationEnabled;
	}

	public void setValidationEnabled(boolean validationEnabled) {
		this.validationEnabled = validationEnabled;
	}

}
