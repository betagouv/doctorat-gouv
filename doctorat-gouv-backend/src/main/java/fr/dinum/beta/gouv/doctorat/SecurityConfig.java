package fr.dinum.beta.gouv.doctorat;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fr.dinum.beta.gouv.doctorat.config.PrcProperties;
import fr.dinum.beta.gouv.doctorat.security.ApiKeyFilter;
import fr.dinum.beta.gouv.doctorat.security.JwtAuthenticationFilter;
import fr.dinum.beta.gouv.doctorat.security.PrcLoginFailureHandler;
import fr.dinum.beta.gouv.doctorat.security.PrcLoginSuccessHandler;

@Configuration
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;
    private final JwtAuthenticationFilter jwtFilter;

    /** PRC est optionnel : injecté en ObjectProvider pour ne pas casser les contextes de test. */
    private final ObjectProvider<PrcProperties> prcProperties;

    /** Handlers OAuth2 PRC : absents des slices de test → ObjectProvider comme ci-dessus. */
    private final ObjectProvider<PrcLoginSuccessHandler> prcSuccessHandler;
    private final ObjectProvider<PrcLoginFailureHandler> prcFailureHandler;

    public SecurityConfig(ApiKeyFilter apiKeyFilter, JwtAuthenticationFilter jwtFilter,
                          ObjectProvider<PrcProperties> prcProperties,
                          ObjectProvider<PrcLoginSuccessHandler> prcSuccessHandler,
                          ObjectProvider<PrcLoginFailureHandler> prcFailureHandler) {
        this.apiKeyFilter = apiKeyFilter;
        this.jwtFilter = jwtFilter;
        this.prcProperties = prcProperties;
        this.prcSuccessHandler = prcSuccessHandler;
        this.prcFailureHandler = prcFailureHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> {})
            .csrf(csrf -> csrf.disable())
            .addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // API publiques
                .requestMatchers("/api/propositions-these/**").permitAll()
                .requestMatchers("/api/filters/**").permitAll()
                .requestMatchers("/api/contact/**").permitAll()
                .requestMatchers("/api/euraxess/**").permitAll()
                .requestMatchers("/api/inscription").permitAll()
                .requestMatchers("/api/inscription/complet").permitAll()
                .requestMatchers("/api/connexion").permitAll()
                // PRC : point d'entrée du parcours SSO et statut (pour masquer le bouton)
                .requestMatchers("/api/prc/login").permitAll()
                .requestMatchers("/api/prc/statut").permitAll()
                .requestMatchers("/sitemap.xml").permitAll()

                // API protégées (JWT requis)
                .requestMatchers("/api/me").authenticated()
                .requestMatchers("/api/candidat/**").authenticated()
                .requestMatchers("/api/directeur-these/**").authenticated()
                .requestMatchers("/api/deconnexion").authenticated()
                .requestMatchers("/api/fichiers/**").authenticated()

                // Fichiers statiques Angular
                .requestMatchers(
                    "/", 
                    "/index.html",
                    "/favicon.ico",
                    "/assets/**",
                    "/**/*.js",
                    "/**/*.css",
                    "/**/*.png",
                    "/**/*.jpg",
                    "/**/*.woff2",
                    "/**/*.woff",
                    "/**/*.ttf",
                    "/browser/**"
                ).permitAll()

                // Toutes les autres routes → Angular doit les gérer
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
            	    .contentSecurityPolicy(csp -> csp
            	        .policyDirectives(
                            "default-src 'self'; " +
                            "img-src 'self' data: blob: https://stats.beta.gouv.fr; " +
            	            "style-src 'self' 'unsafe-inline'; " +
            	            "script-src 'self' 'unsafe-inline' blob: https://stats.beta.gouv.fr; " +
            	            "font-src 'self' data:; " +
            	            "worker-src 'self' blob:; " +
            	            "connect-src 'self' https://doctorat-gouv-dev.osc-secnum-fr1.scalingo.io"
            	            + " https://app.doctorat.gouv.fr/"
            	            + " https://stats.beta.gouv.fr;"
            	        )
            	    )
            	);

        // PRC optionnel : on ne branche le flux OAuth2 que s'il est réellement configuré.
        // Sinon aucun ClientRegistrationRepository n'existe et .oauth2Login() échouerait.
        PrcProperties prc = prcProperties.getIfAvailable();
        if (prc != null && prc.isActive()) {
            PrcLoginSuccessHandler successHandler = prcSuccessHandler.getIfAvailable();
            PrcLoginFailureHandler failureHandler = prcFailureHandler.getIfAvailable();
            http.oauth2Login(oauth2 -> oauth2
                    // URL de retour enregistrée chez PRC (voir PrcOAuth2Config.REDIRECT_URI)
                    .loginProcessingUrl("/login/oauth2/code/prc")
                    // Succès : compte (création JIT ou liaison) + notre JWT + redirection Angular
                    .successHandler(successHandler)
                    // Échec : /connexion?erreur=prc (message clair, jamais de page blanche)
                    .failureHandler(failureHandler));
        }

        return http.build();
    }
}
