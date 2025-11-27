package fr.dinum.beta.gouv.doctorat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;


@SpringBootApplication
@EnableScheduling
public class DoctoratGouvBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(DoctoratGouvBackendApplication.class, args);
	}

}
