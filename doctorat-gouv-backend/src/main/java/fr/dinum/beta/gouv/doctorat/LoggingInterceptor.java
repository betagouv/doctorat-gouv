package fr.dinum.beta.gouv.doctorat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

public class LoggingInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory.getLogger(LoggingInterceptor.class);

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        log.info(">>> HTTP REQUEST");
        log.info("URI: {}", request.getURI());
        log.info("Method: {}", request.getMethod());
        log.info("Headers: {}", request.getHeaders());
        log.info("Body: {}", new String(body, StandardCharsets.UTF_8));

        ClientHttpResponse response = execution.execute(request, body);

        log.info(">>> HTTP RESPONSE");
        log.info("Status: {}", response.getStatusCode());

        return response;
    }
}

