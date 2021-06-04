package org.bel.abas.integration;

import javax.annotation.PostConstruct;

import org.bel.abas.integration.repository.ABASRepository;
import org.egov.tracer.config.TracerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootApplication(scanBasePackages = "org.bel.abas.integration")
@EnableAutoConfiguration
@Import({TracerConfiguration.class})
public class BelABASIntegrationApplication {
	@Autowired
	private ABASRepository abasRepository;

	public static void main(String[] args) {
		SpringApplication.run(BelABASIntegrationApplication.class, args);
	}
	
	@Bean
    public ObjectMapper objectMapper(){
        return new ObjectMapper()
                .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
    }
	
	@PostConstruct
	private void start() {
		System.out.println("in");
		//abasRepository.saveSharedData("1234555 json", "BEL");
	}
}
