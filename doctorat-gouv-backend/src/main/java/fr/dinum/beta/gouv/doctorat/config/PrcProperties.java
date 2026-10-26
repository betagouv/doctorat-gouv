package fr.dinum.beta.gouv.doctorat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de l'authentification externe Projets Recherche Connect (PRC).
 *
 * <p><strong>Entièrement optionnelle.</strong> Contrairement à {@code spring.security.oauth2.client.*},
 * cette classe n'utilise aucun placeholder dans les fichiers {@code .properties} : les valeurs sont
 * lues directement dans l'environnement du processus par la <em>liaison détendue</em> de Spring Boot.
 * Un placeholder non résolu (ex. {@code ${PRC_CLIENT_ID}} sans variable) ferait échouer le démarrage,
 * ce qu'on veut explicitement éviter : l'application doit démarrer sans aucune variable PRC.</p>
 *
 * <p>Variables lues :</p>
 * <ul>
 *   <li>{@code PRC_ISSUER_URI} → {@code prc.issuer-uri} : URL de découverte OIDC. <strong>Sa seule
 *       présence active PRC</strong> (voir {@link #isActive()}) ;</li>
 *   <li>{@code PRC_CLIENT_ID} → {@code prc.client-id} ;</li>
 *   <li>{@code PRC_CLIENT_SECRET} → {@code prc.client-secret}.</li>
 * </ul>
 */
@Configuration
@ConfigurationProperties(prefix = "prc")
public class PrcProperties {

	/** URL de découverte OIDC de PRC (issuer). Vide = PRC inactif. */
	private String issuerUri = "";

	/** Client ID fourni par PRC. */
	private String clientId = "";

	/** Client secret fourni par PRC. */
	private String clientSecret = "";

	/**
	 * PRC est actif si et seulement si l'issuer est renseigné.
	 *
	 * <p>En l'absence de configuration, le bouton « Continuer avec Projets Recherche Connect »
	 * est masqué côté Angular et {@code /api/prc/login} répond 503.</p>
	 */
	public boolean isActive() {
		return issuerUri != null && !issuerUri.isBlank();
	}

	public String getIssuerUri() {
		return issuerUri;
	}

	public void setIssuerUri(String issuerUri) {
		this.issuerUri = issuerUri;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}
}
