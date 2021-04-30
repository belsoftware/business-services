package org.bel.dsssync.cronjob;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bel.dsssync.model.SearcherRequest;
import org.bel.dsssync.service.DssSyncService;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.egov.tracer.model.ServiceCallException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class HourlyJob implements Job {
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	RestTemplate restTemplateORS;

	@Autowired
	DssSyncService dssservice;
	
	@Autowired
	ObjectMapper mapper;
	
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
    
    public static final String jsonpath="$.MdmsRes.ORS.HospitalMapping";
    
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-mm-dd");
	
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
		
		orsIntegration(sdf.format(new Date()), sdf.format(new Date()));
	}
	
	private ModuleDetail getORSMappingRequest(String id) {
		List<MasterDetail> masterDetails = new ArrayList<>();
		masterDetails.add(MasterDetail.builder().name("HospitalMapping").filter("[?(@.id == "+id+")]").build());
		ModuleDetail moduleDtls = ModuleDetail.builder().masterDetails(masterDetails)
				.moduleName("ORS").build();
		return moduleDtls;
	}
	
	public List<Map<String, Object>> getRainmakerData(String defName) {
		StringBuilder uri = new StringBuilder();
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
		JsonNode response = null;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.set("TOKEN", token);
			headers.setContentType(MediaType.APPLICATION_JSON);
			headers.setAccept(Arrays.asList(MediaType.ALL));
			headers.set("Accept-Encoding","*");
			HttpEntity<Object> entity = new HttpEntity<>(request, headers);
			MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
			converter.setSupportedMediaTypes(Arrays.asList(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON));
			restTemplateORS.getMessageConverters().add(0, converter);
			response = restTemplateORS.postForObject(uri.toString(), entity, JsonNode.class); 
			//response = restTemplate.exchange(uri.toString(), HttpMethod.POST, entity, JsonNode.class);
			log.info(""+response);
		} catch (HttpClientErrorException e) {
			e.printStackTrace();
			log.error("External Service threw an Exception: ", e.getResponseBodyAsString());
			throw new ServiceCallException(e.getResponseBodyAsString());
		} catch (Exception e) {
			e.printStackTrace();
			log.error("Exception while fetching from external service: ", e);
		}

		return Optional.ofNullable(response);

	}
	
	private Optional<Object> fetchResultMapping(StringBuilder uri, Object request) {

		Object response = null;
		log.info("URI: "+uri.toString());
		try {
			//RestTemplate restTemplate = new RestTemplate();
			log.info("Request: "+mapper.writeValueAsString(request));
			response = restTemplate.postForObject(uri.toString(), request, Map.class);
		} catch (HttpClientErrorException e) {
			
			//log.error("External Service threw an Exception: ", e);
			throw new ServiceCallException(e.getResponseBodyAsString());
		} catch (Exception e) {
			e.printStackTrace();
			//log.error("Exception while fetching from external service: ", e);
			throw new CustomException("REST_CALL_EXCEPTION : "+uri.toString(),e.getMessage());
		}
		return Optional.ofNullable(response);
	}
	
	public String orsIntegration(String fromDate, String toDate) {
		String result = "Failure";
		/*
		 * JsonObject request = new JsonObject();
		 * request.addProperty("appointment_from_date", fromDate);
		 * request.addProperty("appointment_to_date", toDate);
		 * request.addProperty("user_name", userName); request.addProperty("password",
		 * password); request.addProperty("hospital_type_id", 9);
		 */
		List<Map<String, Object>> data = new ArrayList<>();
		StringBuilder url = new StringBuilder(uri);
		ObjectNode rootNode = mapper.createObjectNode();
		rootNode.put("appointment_from_date", fromDate);
		rootNode.put("appointment_to_date", toDate);
		rootNode.put("user_name", userName);
		rootNode.put("password", password);
		rootNode.put("hospital_type_id", 9);
		Optional<Object> response = fetchResultHeader(url, rootNode);
		try {
			if (response.isPresent()) {
				log.info("NIC O/P 1 : "+response);
				Object parsedResponse = mapper.convertValue(response.get(), Map.class);
				List<Object> dataParsedToList = mapper.convertValue(JsonPath.read(parsedResponse, "$.applist"),
						List.class);
				log.info("2 : "+response);
				for (Object record : dataParsedToList) {
					data.add(mapper.convertValue(record, Map.class));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			throw new CustomException("DATA_RETREIVAL_FAILED", "Failed to retrieve data from the ORS System");
		}

		for (Map<String, Object> record : data) {
			String identifier = ((String) record.get("hospital_id")) + "_"
					+ ((String) record.get("male_successful_appointment"));
			record.put("successful_appointment", Integer.parseInt((String) record.get("successful_appointment")));
			record.put("male_successful_appointment",
					Integer.parseInt((String) record.get("male_successful_appointment")));
			record.put("female_successful_appointment",
					Integer.parseInt((String) record.get("female_successful_appointment")));
			record.put("chhawani_resident_successful_appointment",
					Integer.parseInt((String) record.get("chhawani_resident_successful_appointment")));
			record.put("chhawani_nonresident_successful_appointment",
					Integer.parseInt((String) record.get("chhawani_nonresident_successful_appointment")));
			try {
				record.put("appointment_date", sdf.parse((String) record.get("appointment_date")).getTime());
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				throw new CustomException("INVALID_DATA", "INVALID DATA");
			}

			ModuleDetail orsMappingRequest = getORSMappingRequest((String) record.get("hospital_id"));
			List<ModuleDetail> moduleDetails = new LinkedList<>();
			moduleDetails.add(orsMappingRequest);
			MdmsCriteria mdmsCriteria = MdmsCriteria.builder().moduleDetails(moduleDetails).tenantId("pb").build();
			MdmsCriteriaReq mdmsCriteriaReq = MdmsCriteriaReq.builder().mdmsCriteria(mdmsCriteria)
					.requestInfo(RequestInfo.builder().build()).build();

			StringBuilder mdmsUrl = new StringBuilder().append(mdmsHost).append(mdmsEndPoint);

			Optional<Object> responseMapping = fetchResultMapping(mdmsUrl, mdmsCriteriaReq);
			try {
				if (responseMapping.isPresent()) {
					List<Map<String, Object>> ab = JsonPath.read(responseMapping.get(), jsonpath);
					log.info("" + ab.get(0).get("cb"));
					record.put("tenantId", ab.get(0).get("cb"));
				}

			} catch (Exception e) {
				e.printStackTrace();
				throw new CustomException("DATA_RETREIVAL_FAILED", "Failed to retrieve data");
			}
			// dssservice.putToElasticSearch("orsindex-v1", "general", identifier,
			// jsonObject);
		}
		for (Map<String, Object> record : data) {
			log.info("" + record);
		}
		result= "Success";
		return result;
	}
}
