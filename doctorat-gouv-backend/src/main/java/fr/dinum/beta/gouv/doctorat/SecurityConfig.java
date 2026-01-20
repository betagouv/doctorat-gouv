package fr.dinum.beta.gouv.doctorat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // API publiques
                .requestMatchers("/api/propositions-these/**").permitAll()
                .requestMatchers("/api/filters/**").permitAll()
                .requestMatchers("/api/contact/**").permitAll()

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
                			    "img-src 'self' data:; " +
                			    "style-src 'self' 'unsafe-inline'; " +
                			    "script-src 'self' 'unsafe-inline' blob:; " +
                			    "font-src 'self' data:; " +
                			    "worker-src 'self' blob:; " +
                			    "connect-src 'self' https://doctorat-gouv-dev.osc-secnum-fr1.scalingo.io"
                			)

                )
            );

        return http.build();
    }
}

