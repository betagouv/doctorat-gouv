package fr.dinum.beta.gouv.doctorat.security;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ApiKeyFilter.class);

    @Value("${API_KEY_EXPORT:}")
    private String expectedApiKey;

    @Value("${EURAXESS_API_KEY:}")
    private String euraxessApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-KEY");
        String expectedKey = expectedKeyFor(request);

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Requête vers {} sans header X-API-KEY", request.getRequestURI());
            sendUnauthorized(response, "Header X-API-KEY manquant");
            return;
        }

        if (!apiKey.equals(expectedKey)) {
            log.warn("Requête vers {} avec API Key invalide", request.getRequestURI());
            sendUnauthorized(response, "API Key invalide");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String expectedKeyFor(HttpServletRequest request) {
        return isEuraxessFeed(request) ? euraxessApiKey : expectedApiKey;
    }

    private static boolean isEuraxessFeed(HttpServletRequest request) {
        return request.getRequestURI().equals("/api/euraxess/feed");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + message + "\"}");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !(uri.startsWith("/api/export/") || isEuraxessFeed(request));
    }
}
