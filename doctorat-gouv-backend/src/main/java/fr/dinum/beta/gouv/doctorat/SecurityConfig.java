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
	            // Autorise librement l'accès à ton endpoint
	            .requestMatchers("/api/propositions-these/**").permitAll()
	            .requestMatchers("/api/filters/**").permitAll()
	            // Les autres endpoints peuvent être protégés
	            .anyRequest().authenticated()
	        )
	        .headers(headers -> headers
	            .contentSecurityPolicy(csp -> csp
	                .policyDirectives("default-src 'self'; " +
	                                  "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
	                                  "style-src 'self' 'unsafe-inline'; " +
	                                  "img-src 'self' data:; " +
	                                  "connect-src 'self' http://localhost:4200 http://localhost:8080;")
	            )
	        );

	    return http.build();
	}

}
