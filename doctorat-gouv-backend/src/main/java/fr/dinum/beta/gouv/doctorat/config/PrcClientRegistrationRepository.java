package fr.dinum.beta.gouv.doctorat.config;

import java.net.URI;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.RequestEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.client.RestTemplate;

/**
 * Dépôts des clients OIDC, avec **découverte paresseuse**.
 *
 * <p><strong>Pourquoi paresseux ?</strong> la découverte OIDC effectue un (ou plusieurs)
 * appel(s) réseau vers {@code /.well-known/openid-configuration}.
 * Si cette découverte est faite dans une méthode {@code @Bean}, une erreur chez le fournisseur
 * d'identité fait échouer le démarrage complet de l'application. Or l'exigence est que
 * l'application démarre et fonctionne même si PRC est injoignable ou mal configuré.</p>
 *
 * <p>Ici, la découverte n'a lieu qu'au **premier clic** sur le bouton PRC.
 * Un souci chez PRC ne bloque alors que la tentative de connexion concernée, jamais le boot.</p>
 *
 * <p>Le document est lu via {@link ClientRegistrations#fromOidcConfiguration(java.util.Map)}
 * plutôt que {@code fromIssuerLocation} : ce dernier impose l'égalité stricte entre l'issuer
 * configuré et celui des métadonnées, alors que PRC y publie un identifiant non-URL.</p>
 *
 * <p>Le résultat est mis en cache : la découverte n'est refaite qu'une seule fois par démarrage.</p>
 */
public class PrcClientRegistrationRepository implements ClientRegistrationRepository {

    private static final Logger log = LoggerFactory.getLogger(PrcClientRegistrationRepository.class);

    private final PrcProperties prcProperties;

    /** Verrou pour ne lancer la découverte qu'une seule fois (double-checked locking). */
    private final Object verrou = new Object();

    /** Cache de la découverte ; {@code null} tant que la découverte n'a pas réussi. */
    private volatile ClientRegistration clientEnCache;

    /** Dernier message d'erreur de découverte, pour un diagnostic clair en cas de nouvel échec. */
    private volatile String derniereErreur;

    /**
     * Horodatage de la dernière tentative <strong>échouée</strong> ; {@code 0} si aucune.
     * Sert de cooldown pour ne pas marteler PRC à chaque rechargement de page.
     */
    private volatile long dernierEchec = 0L;

    /** Cooldown après un échec : durée pendant laquelle on n'a pas le droit de retenter. */
    private static final long COOLDOWN_MS = 30_000L;

    public PrcClientRegistrationRepository(PrcProperties prcProperties) {
        this.prcProperties = prcProperties;
    }

    @Override
    public ClientRegistration findByRegistrationId(String registrationId) {

        if (!PrcOAuth2Config.REGISTRATION_ID.equals(registrationId)) {
            return null;
        }

        ClientRegistration client = clientEnCache;
        if (client != null) {
            return client;
        }

        synchronized (verrou) {
            if (clientEnCache != null) {
                return clientEnCache;
            }
            return decouvrirEtMettreEnCache();
        }
    }

    /** Type de réponse du document de découverte OIDC. */
    private static final ParameterizedTypeReference<Map<String, Object>> TYPE_CONFIGURATION =
            new ParameterizedTypeReference<>() { };

    /** Délais de la requête de découverte : le clic utilisateur ne doit pas pendre indéfiniment. */
    private static final int TIMEOUT_MS = 30_000;

    /**
     * Interroge le document de découverte OIDC de PRC et construit le client.
     *
     * <p><strong>Pourquoi ne pas utiliser {@code ClientRegistrations.fromIssuerLocation()} ?</strong>
     * Cette méthode impose l'<em>égalité stricte</em> entre l'issuer configuré et le champ
     * {@code issuer} des métadonnées (Spring Security 7, {@code ClientRegistrations}) :
     * {@code Assert.state(issuer.equals(metadataIssuer), ...)}. Or PRC publie dans ce champ une
     * valeur qui n'est pas l'URL de la découverte, ce qui rendrait toute connexion impossible.
     * On récupère donc le document nous-mêmes puis on passe par
     * {@link ClientRegistrations#fromOidcConfiguration(Map)}, conçu précisément pour ce cas
     * (« issuer différent de l'URL d'accès ») et qui n'effectue aucune comparaison. L'écart est
     * tracé en WARN pour rester traçable.</p>
     *
     * @return le client, ou {@code null} si la découverte a échoué (l'erreur est loguée)
     */
    private ClientRegistration decouvrirEtMettreEnCache() {

        long debut = System.currentTimeMillis();
        String urlMetadata = prcProperties.getIssuerUri() + "/.well-known/openid-configuration";
        try {

            Map<String, Object> configuration = recupererConfiguration(urlMetadata);

            Object issuerMetadata = configuration.get("issuer");
            if (!prcProperties.getIssuerUri().equals(String.valueOf(issuerMetadata))) {
                log.warn("Issuer des métadonnées OIDC (« {} ») différent de l'issuer configuré (« {} »). "
                                + "Normalisation avec l'URL d'accès au document.",
                        issuerMetadata, prcProperties.getIssuerUri());
                // PRC publie un issuer qui n'est pas une URL (identifiant numérique). Spring
                // déduit le registrationId de URI.create(issuer).getHost() (null ici) ce qui
                // échoue avec "registrationId cannot be empty" ; nimbus exige aussi une URI
                // absolue. On remplace le champ par l'URL d'accès, seule valeur exploitable.
                configuration.put("issuer", prcProperties.getIssuerUri());
            }

            ClientRegistration client = ClientRegistrations
                    .fromOidcConfiguration(configuration)
                    .registrationId(PrcOAuth2Config.REGISTRATION_ID)
                    .clientId(prcProperties.getClientId())
                    .clientSecret(prcProperties.getClientSecret())
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    // PRC n'annonce que "anr" ; "openid" est obligatoire pour émettre un id_token,
                    // "profile" permet d'obtenir prénom/nom. Pas de refresh_token : échange unique.
                    // .scope(...) remplace intégralement la liste (sémantique du Builder Spring).
                    .scope("openid", "anr", "profile")
                    .redirectUri(PrcOAuth2Config.REDIRECT_URI)
                    .clientName("Projets Recherche Connect")
                    .build();

            clientEnCache = client;
            derniereErreur = null;
            log.info("Projets Recherche Connect : découverte OIDC réussie en {} ms (issuer {})",
                    System.currentTimeMillis() - debut, prcProperties.getIssuerUri());
            return client;

        } catch (RuntimeException e) {

            // On ne propage PAS : l'application doit rester debout. L'erreur est visible dans
            // les logs et la tentative de connexion échoue proprement (redirection d'erreur).
            derniereErreur = e.getMessage();
            dernierEchec = System.currentTimeMillis();
            log.error("Projets Recherche Connect : échec de la découverte OIDC sur {} "
                    + "après {} ms. Le reste de l'application fonctionne normalement. "
                    + "Vérifiez PRC_ISSUER_URI (l'URL doit renvoyer du JSON, pas du HTML) : {}",
                    prcProperties.getIssuerUri(), System.currentTimeMillis() - debut, e.getMessage());
            log.debug("Détail de l'échec de découverte PRC", e);
            return null;
        }
    }

    /**
     * Récupère et valide le document de découverte OIDC.
     *
     * @throws IllegalStateException si la réponse est vide ou sans champ {@code issuer}
     */
    private Map<String, Object> recupererConfiguration(String urlMetadata) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(TIMEOUT_MS);
        requestFactory.setReadTimeout(TIMEOUT_MS);
        RestTemplate rest = new RestTemplate(requestFactory);

        RequestEntity<Void> request = RequestEntity.get(URI.create(urlMetadata)).build();
        Map<String, Object> configuration = rest.exchange(request, TYPE_CONFIGURATION).getBody();

        if (configuration == null || configuration.get("issuer") == null) {
            throw new IllegalStateException("Document de découverte OIDC invalide (« issuer » absent) : " + urlMetadata);
        }
        return configuration;
    }

    /**
     * Déclenche la découverte OIDC si elle n'a pas encore eu lieu, puis indique si le client
     * est utilisable.
     *
     * <p>À la différence de {@link #isReady()}, cette méthode peut <strong>débloquer</strong>
     * le premier appel : la découverte paresseuse est normalement enclenchée par Spring Security
     * via {@link #findByRegistrationId(String)}, c'est-à-dire seulement <em>après</em> la
     * redirection vers {@code /oauth2/authorization/prc}. Un contrôle sur {@code isReady()} seul
     * bloquerait donc la connexion tant qu'aucune tentative n'aurait eu lieu.</p>
     *
     * @return {@code true} si le client est prêt (découverte réussie ou déjà en cache)
     */
    public boolean preparer() {
        return findByRegistrationId(PrcOAuth2Config.REGISTRATION_ID) != null;
    }

    /**
     * Variante de {@link #preparer()} à usage <strong>passif</strong> : appelée par
     * {@code GET /api/prc/statut} au chargement de la page de connexion.
     *
     * <p><strong>Pourquoi un cooldown ?</strong> cette méthode est exécutée à chaque
     * affichage de la page. Sans garde-fou, un PRC injoignable déclencherait une tentative
     * réseau (jusqu'à {@value #COOLDOWN_MS} ms) à chaque rechargement. Après un échec,
     * on retourne donc {@code false} pendant {@value #COOLDOWN_MS} ms sans toucher au réseau.</p>
     *
     * <p>À la différence de {@link #preparer()} (action utilisateur explicite — le clic
     * sur le bouton), le cooldown n'est <strong>jamais</strong> appliqué ici : l'utilisateur
     * qui clique doit toujours être servi immédiatement.</p>
     *
     * @return {@code true} si le client est prêt (cache, ou découverte réussie maintenant)
     */
    public boolean preparerAuChargement() {

        if (clientEnCache != null) {
            return true; // déjà prêt : aucun coût
        }

        long depuisEchec = System.currentTimeMillis() - dernierEchec;
        if (dernierEchec > 0 && depuisEchec < COOLDOWN_MS) {
            log.debug("Découverte PRC en cooldown : nouvelle tentative dans {} ms",
                    COOLDOWN_MS - depuisEchec);
            return false;
        }

        return preparer();
    }

    /**
     * @return le dernier message d'erreur de découverte, ou {@code null} si aucune tentative
     *         n'a échoué (utile pour le diagnostic)
     */
    public String getDerniereErreur() {
        return derniereErreur;
    }

    /**
     * Indique si un client OIDC valide a été découvert (prêt à l'utilisation).
     * Utile pour masquer le bouton frontend lorsque la découverte OIDC a échoué.
     */
    public boolean isReady() {
        return clientEnCache != null;
    }
}