package org.bel.abas.integration.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bel.abas.integration.config.ApplicationProperties;
import org.bel.abas.integration.model.PaymentModeEnum;
import org.bel.abas.integration.repository.ABASRepository;
import org.egov.common.contract.request.RequestInfo;
import org.egov.mdms.model.MasterDetail;
import org.egov.mdms.model.MdmsCriteria;
import org.egov.mdms.model.MdmsCriteriaReq;
import org.egov.mdms.model.ModuleDetail;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AbasIntegUtil {
	
	@Autowired
	private ApplicationProperties appProps;
	
	@Autowired
	private ABASRepository serviceRequestRepository;
	
	public final String BS_TAXHEAD_SERVICE_PATH = "$.MdmsRes.BillingService.TaxHeadMaster.*.service";
	
	public final String BS_GLCODE_PATH = "$.MdmsRes.BillingService.GLCode.*.glcode";
	
	public final String BILLINGSERVICE = "BillingService";
	public final String TAXHEADMASTER = "TaxHeadMaster";
	public final String GLCODE = "GLCode";
	
	public SimpleDateFormat sd = new SimpleDateFormat("dd/MM/yyyy");
	
	public Charset UTF_8 = StandardCharsets.UTF_8;
	
	public byte[] digest(byte[] input) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
		byte[] result = md.digest(input);
		return result;
	}

	public String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
	
	public int getFiscalYear(Calendar calendarDate) {
        int month = calendarDate.get(Calendar.MONTH);
        int year = calendarDate.get(Calendar.YEAR);
        return (month > Calendar.MARCH) ? year : year - 1;
    }
	
	public Map<String, String> abasPaymentModeMap = new HashMap<String, String>(){{
		put(PaymentModeEnum.CASH.toString(),"C");
		put(PaymentModeEnum.CHEQUE.toString(),"Q");
		put(PaymentModeEnum.DD.toString(),"D");
		put(PaymentModeEnum.ONLINE.toString(),"W");
		put(PaymentModeEnum.CARD.toString(),"W");
		put(PaymentModeEnum.OFFLINE_NEFT.toString(),"W");
		put(PaymentModeEnum.OFFLINE_RTGS.toString(),"W");
		put(PaymentModeEnum.ONLINE_NEFT.toString(),"W");
		put(PaymentModeEnum.ONLINE_RTGS.toString(),"W");
		put(PaymentModeEnum.POSTAL_ORDER.toString(),"W");
		
	}};
	
	public MdmsCriteriaReq prepareMdMsRequest(String tenantId, String moduleName, List<String> names, String filter,
			RequestInfo requestInfo) {

		List<MasterDetail> masterDetails = new ArrayList<>();
		names.forEach(name -> {
			masterDetails.add(MasterDetail.builder().name(name).filter(filter).build());
		});

		ModuleDetail moduleDetail = ModuleDetail.builder().moduleName(moduleName).masterDetails(masterDetails).build();
		List<ModuleDetail> moduleDetails = new ArrayList<>();
		moduleDetails.add(moduleDetail);
		MdmsCriteria mdmsCriteria = MdmsCriteria.builder().tenantId(tenantId).moduleDetails(moduleDetails).build();
		return MdmsCriteriaReq.builder().requestInfo(requestInfo).mdmsCriteria(mdmsCriteria).build();
	}

	
	public DocumentContext getAttributeValues(MdmsCriteriaReq mdmsReq) {
		StringBuilder uri = new StringBuilder(appProps.getMdmsHost()).append(appProps.getMdmsEndpoint());

		try {
			return JsonPath.parse(serviceRequestRepository.fetchResult(uri.toString(), mdmsReq));
		} catch (Exception e) {
			log.error("Error while fetching MDMS data", e);
			throw new CustomException("INVALID_INPUT", "Invalid Input Data");
		}
	}
	
	public String getGLCodeFromTaxHead(String taxHeadCode, RequestInfo requestInfo, String tenantId) {
    	try {
    		MdmsCriteriaReq mdmsReqTaxHead = prepareMdMsRequest(tenantId, BILLINGSERVICE, Arrays.asList(TAXHEADMASTER), "[?(@.code == '"+taxHeadCode+"')]",
    				requestInfo);
    		DocumentContext mdmsDataTaxHead = getAttributeValues(mdmsReqTaxHead);
    		List<String> taxHeadServices = mdmsDataTaxHead.read(BS_TAXHEAD_SERVICE_PATH);
    		if(taxHeadServices.size()>0) {
	    		log.info("taxHeadServices "+taxHeadServices.get(0));
	    		MdmsCriteriaReq mdmsReqGLCode = prepareMdMsRequest(tenantId, BILLINGSERVICE, Arrays.asList(GLCODE), "[?(@.code == '"+taxHeadServices.get(0)+"')]",
	    				requestInfo);
	    		DocumentContext mdmsDataGLCode = getAttributeValues(mdmsReqGLCode);
	    		List<String> glCodes = mdmsDataGLCode.read(BS_GLCODE_PATH);
	    		if(glCodes.size()>0) {
	    			return glCodes.get(0);
	    		}
    		}
    	}
    	catch(Exception e) {
    		e.printStackTrace();
    	}
		return null;
    }
}
