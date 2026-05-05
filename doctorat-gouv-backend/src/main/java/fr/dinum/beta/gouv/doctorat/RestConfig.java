package fr.dinum.beta.gouv.doctorat;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestConfig {

    @Bean
    public RestTemplate restTemplate() {
    	RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new LoggingInterceptor());
        return restTemplate;
    }
}
