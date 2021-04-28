package org.bel.dsssync.cronjob;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bel.dsssync.model.SearcherRequest;
import org.bel.dsssync.service.DssSyncService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HourlyJob implements Job {
	
	@Autowired
	RestTemplate restTemplate;

	@Autowired
	DssSyncService dssservice;
	
	@Value("${egov.searcher.host}")
	public String searcherHost;

	@Value("${egov.searcher.endpoint}")
	public String searcherEndpoint;
	
	@Value("${bel.ors.uri}")
	public String uri;
	
	@Value("${bel.ors.username}")
	public String userName;
	
	@Value("${bel.ors.password}")
	public String password;
	
	@Value("${bel.ors.token}")
	public String token;
	
	@Value("${egov.mdms.host}")
    private String mdmsHost;

    @Value("${egov.mdms.search.endpoint}")
    private String mdmsEndPoint;
	
	@Override
	public void execute(JobExecutionContext jobExecutionContext) {
		log.info(" dss-sync cron started ");
		int totalCitizensRegistered=0;
		List<Map<String, Object>> dataCitizens = getRainmakerData("citizensRegistered");
		for (Map<String, Object> record : dataCitizens) {
				if (record.get("day").equals("Week0")) {
					totalCitizensRegistered = (int)Math.round( (Double)record.get("count"));
				}
		}
		JsonObject  jsonObject = new JsonObject();
		jsonObject.addProperty("citizenCount", totalCitizensRegistered);
		jsonObject.addProperty("tenantId", "all");
		dssservice.putToElasticSearch("dss-citizen-count", "_doc", "all", jsonObject);
		
		List<Map<String, Object>> dataCitizensTenant = getRainmakerData("citizensRegisteredInTenant");
		for (Map<String, Object> record : dataCitizensTenant) {
			JsonObject jsonObjectTenant = new JsonObject();
			jsonObjectTenant.addProperty("citizenCount", (int) Math.round((Double) record.get("count")));
			jsonObjectTenant.addProperty("tenantId", (String) record.get("tenantid"));
			dssservice.putToElasticSearch("dss-citizen-count", "_doc", (String) record.get("tenantid"), jsonObjectTenant);
		}
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
		ObjectMapper mapper = new ObjectMapper();
		
		//String json = "{\"applist\":[{\"successful_appointment\":\"3\",\"male_successful_appointment\":\"1\",\"status_code\":\"1\",\"female_successful_appointment\":\"2\",\"appointment_date\":\"2021-03-16\",\"chhawani_resident_successful_appointment\":\"0\",\"hospital_id\":\"91070526\",\"chhawani_nonresident_successful_appointment\":\"3\",\"hospital_name\":\"Cantonment General Hospital,  Delhi Cantt\",\"failed_appointment\":\"0\"},{\"successful_appointment\":\"2\",\"male_successful_appointment\":\"2\",\"status_code\":\"1\",\"female_successful_appointment\":\"0\",\"appointment_date\":\"2021-03-16\",\"chhawani_resident_successful_appointment\":\"0\",\"hospital_id\":\"34037\",\"chhawani_nonresident_successful_appointment\":\"2\",\"hospital_name\":\"Cantonment General Hospital Lucknow Cantt\",\"failed_appointment\":\"0\"}]}";
		//List<Object> dataParsedToList = mapper.convertValue(JsonPath.read(json, "$.applist"), List.class);
		
		JsonObject request = new JsonObject();
		request.addProperty("appointment_from_date", sdf.format(new Date()));
		request.addProperty("appointment_to_date", sdf.format(new Date()));
		request.addProperty("user_name", userName);
		request.addProperty("password", password);
		request.addProperty("hospital_type_id", 9);
		List<Map<String, Object>> data = new ArrayList<>();
		StringBuilder url= new StringBuilder(uri);
		Optional<Object> response = fetchResultHeader(url, request);
		try {
			if(response.isPresent()) {
				Object parsedResponse = mapper.convertValue(response.get(), Map.class);
				List<Object> dataParsedToList = mapper.convertValue(JsonPath.read(parsedResponse, "$.applist"), List.class);
				for (Object record : dataParsedToList) {
					data.add(mapper.convertValue(record, Map.class));
				}
			}

		} catch (Exception e) {
			throw new CustomException("DATA_RETREIVAL_FAILED", "Failed to retrieve data from the ORS System");
		}
		
		for (Map<String, Object> record : data) {
			String identifier=((String) record.get("hospital_id"))
					+"_"+((String) record.get("male_successful_appointment"));
			record.put("successful_appointment",Integer.parseInt((String) record.get("successful_appointment")));
			record.put("male_successful_appointment",Integer.parseInt((String) record.get("male_successful_appointment")));
			record.put("female_successful_appointment",Integer.parseInt((String) record.get("female_successful_appointment")));
			record.put("chhawani_resident_successful_appointment",Integer.parseInt((String) record.get("chhawani_resident_successful_appointment")));
			record.put("chhawani_nonresident_successful_appointment",Integer.parseInt((String) record.get("chhawani_nonresident_successful_appointment")));
			try {
				record.put("appointment_date",sdf.parse((String) record.get("appointment_date")).getTime());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			record.put("tenantId","pb.agra");
			
			//dssservice.putToElasticSearch("orsindex-v1", "general", identifier, jsonObject);
		}
		for (Map<String, Object> record : data) {
			log.info(""+record);
		}
	}
	
	public List<Map<String, Object>> getRainmakerData(String defName) {
		StringBuilder uri = new StringBuilder();
		ObjectMapper mapper = new ObjectMapper();
		List<Map<String, Object>> data = new ArrayList<>();
		SearcherRequest request = preparePlainSearchReq(uri, defName);
		Optional<Object> response = fetchResult(uri, request);
		try {
			if(response.isPresent()) {
				Object parsedResponse = mapper.convertValue(response.get(), Map.class);
				List<Object> dataParsedToList = mapper.convertValue(JsonPath.read(parsedResponse, "$.data"), List.class);
				for (Object record : dataParsedToList) {
					data.add(mapper.convertValue(record, Map.class));
				}
			}

		} catch (Exception e) {
			throw new CustomException("EMAILER_DATA_RETREIVAL_FAILED", "Failed to retrieve data from the db");
		}
		return data;

	}
	
	public SearcherRequest preparePlainSearchReq(StringBuilder uri, String defName) {
		uri.append(searcherHost);
		String endPoint = searcherEndpoint.replace("{searchName}", defName);
		uri.append(endPoint);
		HashMap<String, Object> param = new HashMap<>();
		param.put("intervalinsecs",604800000 );
		SearcherRequest searcherRequest = SearcherRequest.builder().requestInfo(new RequestInfo()).searchCriteria(param)
				.build();
		return searcherRequest;
	}
	
	public Optional<Object> fetchResult(StringBuilder uri, Object request) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		Object response = null;
		try {
			response = restTemplate.postForObject(uri.toString(), request, JsonNode.class);
		} catch (HttpClientErrorException e) {
			log.error("External Service threw an Exception: ", e);
			throw new ServiceCallException(e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error("Exception while fetching from external service: ", e);
		}

		return Optional.ofNullable(response);

	}
	
	public Optional<Object> fetchResultHeader(StringBuilder uri, Object request) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
		Object response = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("TOKEN", token);
			HttpEntity<Object> entity = new HttpEntity<>(request, headers);
			restTemplate.postForObject(uri.toString(), entity, String.class); 
			response = restTemplate.postForObject(uri.toString(), request, JsonNode.class);
		} catch (HttpClientErrorException e) {
			log.error("External Service threw an Exception: ", e);
			throw new ServiceCallException(e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error("Exception while fetching from external service: ", e);
		}

		return Optional.ofNullable(response);

	}
}
