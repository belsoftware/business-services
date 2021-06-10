package org.bel.abas.integration.consumer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.bel.abas.integration.contract.DemandRequest;
import org.bel.abas.integration.model.ABASDemand;
import org.bel.abas.integration.model.ABASDemandDetail;
import org.bel.abas.integration.model.ABASDemandRequest;
import org.bel.abas.integration.model.Demand;
import org.bel.abas.integration.model.DemandDetail;
import org.bel.abas.integration.repository.ABASRepository;
import org.bel.abas.integration.utils.AbasIntegUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemandListener {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ABASRepository abasRepository;
    
    @Autowired
    private AbasIntegUtil util;
    
    @KafkaListener(topics = "${kafka.topics.abas.demand.insert}")
    public void listen(final HashMap<String, Object> record) {
    	
    	try{
    		ABASDemandRequest request = new ABASDemandRequest();
    		ArrayList<ABASDemand> abasDemands = new ArrayList<ABASDemand>();
    		DemandRequest demandRequest = objectMapper.convertValue(record, DemandRequest.class);
    		for(Demand demand : demandRequest.getDemands()) {
    			ABASDemand abasDemand = new ABASDemand();
    			abasDemand.setVoucherDate(util.sd.format(new Date()));
    			abasDemand.setUlbCode(abasRepository.getULBCode(demand.getTenantId()));
    			abasDemand.setVoucherReferenceNo(demand.getConsumerCode());
    			abasDemand.setVoucherReferenceDate(util.sd.format(new Date(demand.getAuditDetails().getCreatedTime())));
    			abasDemand.setNarration("Demand Voucher for "+demand.getConsumerType());
    			abasDemand.setPayerOrPayee(demand.getPayer().getName());
    			//int year = util.getFiscalYear(Calendar.getInstance());
    			Calendar cal= Calendar.getInstance();
    			cal.setTimeInMillis(demand.getTaxPeriodFrom());
    			int year = cal.get(Calendar.YEAR);
    			abasDemand.setFinancialYear(year+ "-"+ (year+1));
    			abasDemand.setCreatedBy(demand.getPayer().getId().toString());
    			//abasDemand.setCheckSum(util.bytesToHex(util.digest((abasDemands.get(0).getCreatedBy() +"|"+abasDemands.get(0).getUlbCode()).getBytes(util.UTF_8))));
    			ArrayList<ABASDemandDetail> abasDemandDetails = new ArrayList<ABASDemandDetail>();
    			for(DemandDetail demandDetail : demand.getDemandDetails()) {
    				ABASDemandDetail abasDemandDetail = new ABASDemandDetail();
    				abasDemandDetail.setAcHeadCode(demandDetail.getTaxHeadMasterCode());
    				abasDemandDetail.setVoucherAmount(demandDetail.getTaxAmount());
    				abasDemandDetails.add(abasDemandDetail);
    			}
    			abasDemand.setVoucherExtDetails(abasDemandDetails);
    			abasDemands.add(abasDemand);
    		}
    		request.setCheckSum(util.bytesToHex(util.digest((abasDemands.get(0).getCreatedBy() +"|"+abasDemands.get(0).getUlbCode()).getBytes(util.UTF_8))));
    		request.setVoucherextsysdto(abasDemands);
    		String json =new Gson().toJson(request);
    		abasRepository.saveSharedData(json,"BEL","DEMAND_SEND");
    	}
		catch (Exception e) {
			e.printStackTrace();
		}
    }
}
