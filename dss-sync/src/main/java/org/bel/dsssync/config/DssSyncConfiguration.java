package org.bel.dsssync.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.egov.tracer.config.TracerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.util.Collections;
import java.util.TimeZone;


@Import({TracerConfiguration.class})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Component
public class DssSyncConfiguration {


    @Value("${app.timezone}")
    private String timeZone;

    @PostConstruct
    public void initialize() {
        TimeZone.setDefault(TimeZone.getTimeZone(timeZone));
    }

    @Bean
    @Autowired
    public MappingJackson2HttpMessageConverter jacksonConverter(ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setSupportedMediaTypes(Collections.singletonList(MediaType.ALL)); 
        return converter;
    }


    @Value("${egov.collectionservice.host}")
    private String collectionserviceHost;
    
    @Value("${egov.tlservice.path}")
    private String tlserviceHost;
    
    @Value("${egov.waterService.host}")
    private String waterServiceHost;
    
    @Value("${egov.dss.limit}")
    private Integer dssSearchLimit;
    
    @Value("${egov.dss.offset}")
    private Integer dssSearchOffset;
    
    @Value("${egov.dss.collectionsBreakingLimit}")
    private Integer collectionsBreakingLimit;
    
    @Value("${egov.dss.tlBreakingLimit}")
    private Integer tlBreakingLimit;
    
    @Value("${egov.dss.waterBreakingLimit}")
    private Integer waterBreakingLimit;
    
    @Value("${egov.dss.sewerageBreakingLimit}")
    private Integer sewerageBreakingLimit;
    
    @Value("${egov.dss.leaseBreakingLimit}")
    private Integer leaseBreakingLimit;
    
    @Value("${egov.elasticSearch.path}")
    private String elasticSearch;

    @Value("${egov.propertyService.host}")
    private String propertyServiceHost;
    
    @Value("${egov.locationService.host}")
    private String locationServiceHost;
    
    @Value("${egov.dss.pgrServiceSearchLimit}")
    private Integer pgrServiceSearchLimit;
    
    @Value("${egov.dss.pgrBreakingLimit}")
    private Integer pgrBreakingLimit;
    
    @Value("${egov.dss.leaseServiceSearchLimit}")
    private Integer leaseServiceSearchLimit;
    
    @Value("${egov.dss.sewerageServiceSearchLimit}")
    private Integer sewerageServiceSearchLimit;
    
    @Value("${egov.dss.waterServiceSearchLimit}")
    private Integer waterServiceSearchLimit;
    
    @Value("${egov.dss.collectionsServiceSearchLimit}")
    private Integer collectionsServiceSearchLimit;
    
    @Value("${egov.dss.tlServiceSearchLimit}")
    private Integer tlServiceSearchLimit;
    
    @Value("${egov.sewerageService.host}")
    private String sewerageServiceHost;
    
  
}
