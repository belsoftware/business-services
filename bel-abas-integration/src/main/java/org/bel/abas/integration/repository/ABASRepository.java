package org.bel.abas.integration.repository;


import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Repository
public class ABASRepository {

	@Autowired
    private JdbcTemplate jdbcTemplate;
    
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
}
