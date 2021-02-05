package org.bel.abas.integration.consumer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;

import org.bel.abas.integration.contract.DemandRequest;
import org.bel.abas.integration.model.ABASDemand;
import org.bel.abas.integration.model.ABASDemandDetail;
import org.bel.abas.integration.model.ABASDemandRequest;
import org.bel.abas.integration.model.Demand;
import org.bel.abas.integration.model.DemandDetail;
import org.bel.abas.integration.repository.ABASRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import ch.qos.logback.classic.pattern.Abbreviator;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DemandListener {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ABASRepository abasRepository;
    
    @KafkaListener(topics = "${kafka.topics.abas.demand.insert}")
    public void listen(final HashMap<String, Object> record) {
    	SimpleDateFormat sd = new SimpleDateFormat("dd/MM/yyyy");
    	try{
    		ABASDemandRequest request = new ABASDemandRequest();
    		ArrayList<ABASDemand> abasDemands = new ArrayList<ABASDemand>();
    		DemandRequest demandRequest = objectMapper.convertValue(record, DemandRequest.class);
    		for(Demand demand : demandRequest.getDemands()) {
    			ABASDemand abasDemand = new ABASDemand();
    			abasDemand.setVoucherDate(sd.format(new Date()));
    			abasDemand.setUlbCode(abasRepository.getULBCode(demand.getTenantId()));
    			abasDemand.setVoucherReferenceNo(demand.getConsumerCode());
    			abasDemand.setVoucherReferenceDate(sd.format(new Date(demand.getAuditDetails().getCreatedTime())));
    			abasDemand.setNarration("Demand Voucher for "+demand.getConsumerType());
    			abasDemand.setPayerOrPayee(demand.getPayer().getName());
    			int year = getFiscalYear(Calendar.getInstance());
    			abasDemand.setFinancialYear(year + "-" + (year + 1));
    			abasDemand.setCreatedBy(demand.getPayer().getId().toString());
    			//abasDemand.setCheckSum(abasDemand.getCreatedBy()+" | "+abasDemand.getUlbCode());
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
    		request.setCheckSum(abasDemands.get(0).getCreatedBy() +" | "+abasDemands.get(0).getUlbCode());
    		request.setVoucherextsysdto(abasDemands);
    		log.info(new Gson().toJson(request));
    	}
		catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    public int getFiscalYear(Calendar calendarDate) {
        int month = calendarDate.get(Calendar.MONTH);
        int year = calendarDate.get(Calendar.YEAR);
        return (month > Calendar.MARCH) ? year : year - 1;
    }

}
