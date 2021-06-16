package org.bel.abas.integration;

import java.util.TimeZone;

import javax.annotation.PostConstruct;

import org.egov.tracer.config.TracerConfiguration;
import org.springframework.beans.factory.annotation.Value;
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
	
	@Value("${app.timezone}")
    private String timeZone;
	
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
		TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
	}
}
