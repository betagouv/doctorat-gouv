package fr.dinum.beta.gouv.doctorat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import fr.dinum.beta.gouv.doctorat.security.ApiKeyFilter;
import fr.dinum.beta.gouv.doctorat.security.JwtAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final ApiKeyFilter apiKeyFilter;
    private final JwtAuthenticationFilter jwtFilter;

    public SecurityConfig(ApiKeyFilter apiKeyFilter, JwtAuthenticationFilter jwtFilter) {
        this.apiKeyFilter = apiKeyFilter;
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
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
                .requestMatchers("/api/connexion").permitAll()
                .requestMatchers("/sitemap.xml").permitAll()

                // API protégées (JWT requis)
                .requestMatchers("/api/me").authenticated()
                .requestMatchers("/api/deconnexion").authenticated()

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
            	            "img-src 'self' data: https://stats.beta.gouv.fr; " +
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

        return http.build();
    }
}
