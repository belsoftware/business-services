package org.bel.abas.integration.repository;


import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
public class ABASRepository {

	@Autowired
    private JdbcTemplate jdbcTemplate;
	
	@Autowired
	private ObjectMapper mapper;

	@Autowired
	private RestTemplate restTemplate;
    
	public String getULBCode(String tenantId) {
		try {
		String sql = "SELECT ulbcode FROM eg_abas_ulbcode WHERE tenantid=?";
	    String ulbCode = (String) jdbcTemplate.queryForObject(
	            sql, new Object[] { tenantId }, String.class);
	    return ulbCode;
	    }catch(Exception e) {
	    	throw new CustomException("INVALID INPUT","Error in fethcing data");
	    }
	}

	public void saveSharedData(String json, String createdBy, String feature) {
		try {
			String insertSQL = "INSERT INTO eg_abas_shared_data (id, createdtime, jsonstring, createdby, feature) VALUES (?, ?, ?, ?, ?)";
			jdbcTemplate.update(insertSQL, UUID.randomUUID() , new Timestamp(System.currentTimeMillis()) ,json, createdBy, feature);
		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("INVALID INPUT", "Error in fethcing data");
		}
	}
	
	public Map fetchResult(String uri, Object request) {
		
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		Map response = null;
		log.info("URI: "+ uri);
		
		try {
			log.info("Request: "+mapper.writeValueAsString(request));
			response = restTemplate.postForObject(uri, request, Map.class);
		}catch(HttpClientErrorException e) {
			log.error("External Service threw an Exception: ",e.getResponseBodyAsString());
			throw new ServiceCallException(e.getResponseBodyAsString());
		}catch(Exception e) {
			log.error("Exception while searching data : ",e);
		}

		return response;
	}
}
