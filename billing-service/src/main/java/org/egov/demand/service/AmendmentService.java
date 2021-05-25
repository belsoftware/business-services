package org.egov.demand.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.egov.common.contract.request.RequestInfo;
import org.egov.demand.amendment.model.Amendment;
import org.egov.demand.amendment.model.AmendmentCriteria;
import org.egov.demand.amendment.model.AmendmentRequest;
import org.egov.demand.amendment.model.AmendmentUpdate;
import org.egov.demand.amendment.model.AmendmentUpdateRequest;
import org.egov.demand.amendment.model.State;
import org.egov.demand.amendment.model.enums.AmendmentStatus;
import org.egov.demand.config.ApplicationProperties;
import org.egov.demand.model.ApportionDemandResponse;
import org.egov.demand.model.AuditDetails;
import org.egov.demand.model.BillV2.BillStatus;
import org.egov.demand.model.Demand;
import org.egov.demand.model.DemandApportionRequest;
import org.egov.demand.model.DemandCriteria;
import org.egov.demand.model.DemandDetail;
import org.egov.demand.repository.AmendmentRepository;
import org.egov.demand.repository.BillRepositoryV2;
import org.egov.demand.repository.ServiceRequestRepository;
import org.egov.demand.util.Util;
import org.egov.demand.web.contract.DemandRequest;
import org.egov.demand.web.validator.AmendmentValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

@Service
public class AmendmentService {
	
	@Autowired
	private Util util;
	
	@Autowired
	private ApplicationProperties props;
	
	@Autowired
	private DemandService demandService;
	
	@Autowired
	private BillRepositoryV2 billRepositoryV2;
	
	@Autowired
	private AmendmentValidator amendmentValidator;
	
	@Autowired
	private AmendmentRepository amendmentRepository;
	
	@Autowired
	private ServiceRequestRepository serviceRequestRepository;
	
	@Autowired
	private ObjectMapper mapper;
	/**
	 * Search amendment based on criteria
	 * 
	 * @param amendmentCriteria
	 */
	public List<Amendment> search(AmendmentCriteria amendmentCriteria, RequestInfo requestInfo) {

		amendmentValidator.validateAmendmentCriteriaForSearch(amendmentCriteria);
		if (!StringUtils.isEmpty(amendmentCriteria.getMobileNumber())) {

			DemandCriteria demandCriteria = DemandCriteria.builder()
					.businessService(amendmentCriteria.getBusinessService())
					.mobileNumber(amendmentCriteria.getMobileNumber())
					.tenantId(amendmentCriteria.getTenantId())
					.build();
			
			List<Demand> demands = demandService.getDemands(demandCriteria, requestInfo);
			if (!CollectionUtils.isEmpty(demands)) {
				if (!CollectionUtils.isEmpty(amendmentCriteria.getConsumerCode()))
					amendmentCriteria.getConsumerCode()
							.retainAll(demands.stream().map(Demand::getConsumerCode).collect(Collectors.toSet()));
				else
					amendmentCriteria.getConsumerCode()
							.addAll(demands.stream().map(Demand::getConsumerCode).collect(Collectors.toSet()));
			}
			if (CollectionUtils.isEmpty(amendmentCriteria.getConsumerCode())
					&& ObjectUtils.isEmpty(amendmentCriteria.getAmendmentId()))
				return new ArrayList<>();
		}
		return amendmentRepository.getAmendments(amendmentCriteria);
	}

	/**
	 * 
	 * @param amendmentRequest
	 */
	public Amendment create(AmendmentRequest amendmentRequest) {
		
		RequestInfo requestInfo = amendmentRequest.getRequestInfo();
		Amendment amendment = amendmentRequest.getAmendment();
		
		amendmentValidator.validateAmendmentForCreate(amendmentRequest);
		amendmentValidator.enrichAmendmentForCreate(amendmentRequest);
		if (props.getIsAmendmentworkflowEnabed()) {
			
			State state = util.callWorkFlow(amendment.getWorkflow(), requestInfo);
			amendment.setStatus(AmendmentStatus.fromValue(state.getApplicationStatus()));
		}
		amendmentRepository.saveAmendment(amendmentRequest);
		if (!props.getIsAmendmentworkflowEnabed()) {
			updateDemandWithAmendmentTax(requestInfo, amendment);
		}
		return amendmentRequest.getAmendment();
	}
	
	
	/**
	 * update method for amendment, used only with workflow. if workflow is not available then method is not called
	 * 
	 * @param amendmentUpdateRequest
	 * @param isRequestForWorkflowUpdate
	 */
	public Amendment updateAmendment(AmendmentUpdateRequest amendmentUpdateRequest) {
		
		RequestInfo requestInfo = amendmentUpdateRequest.getRequestInfo();
		AmendmentUpdate amendmentUpdate = amendmentUpdateRequest.getAmendmentUpdate();
		Amendment amendmentFromSearch = amendmentValidator.validateAndEnrichAmendmentForUpdate(amendmentUpdateRequest);
		
		/*
		 * Workflow update
		 */
		if (props.getIsAmendmentworkflowEnabed()) {
			State resultantState = util.callWorkFlow(amendmentUpdate.getWorkflow(), requestInfo);
			amendmentUpdate.getWorkflow().setState(resultantState);
			amendmentUpdate.setStatus(AmendmentStatus.fromValue(resultantState.getApplicationStatus()));
		}
		
		/*
		 * amendment update 
		 */
		amendmentRepository.updateAmendment(Arrays.asList(amendmentUpdate));
		
		if (amendmentUpdate.getStatus().equals(AmendmentStatus.ACTIVE)) {
			updateDemandWithAmendmentTax(requestInfo, amendmentFromSearch);
		}
		return search(amendmentUpdate.toSearchCriteria(), requestInfo).get(0);
	}


	/**
	 * Method to update demand after an amendment is ACTIVE
	 * 
	 * if no demands found then ignored
	 * 
	 * @param requestInfo
	 * @param amendment
	 */
	public void updateDemandWithAmendmentTax(RequestInfo requestInfo, Amendment amendment) {
		
		
		DemandCriteria demandCriteria = DemandCriteria.builder()
				.consumerCode(Stream.of(amendment.getConsumerCode()).collect(Collectors.toSet()))
				.businessService(amendment.getBusinessService())
				.tenantId(amendment.getTenantId())
				//.isPaymentCompleted(false)
				.build();
		
		List<Demand> demands = demandService.getDemands(demandCriteria, requestInfo);
		if(!CollectionUtils.isEmpty(demands)) {
			
			AuditDetails auditDetails = util.getAuditDetail(requestInfo);
			if (demands.size() > 1)
				Collections.sort(demands, Comparator.comparing(Demand::getTaxPeriodFrom)
						.thenComparing(Demand::getTaxPeriodTo).reversed());
			Demand demand = demands.get(0);
			amendment.getDemandDetails().forEach(detail -> {
			
				detail.setAuditDetails(auditDetails);
				detail.setDemandId(demand.getId());
				detail.setTenantId(demand.getTenantId());
			});
			BigDecimal totalValue = BigDecimal.ZERO;
			for (DemandDetail amenddemandDetail : amendment.getDemandDetails()) {
				BigDecimal amendedAmount = amenddemandDetail.getTaxAmount().subtract(amenddemandDetail.getCollectionAmount());
				if(amendedAmount.compareTo(BigDecimal.ZERO)<0) {
				for (DemandDetail demandDetail : demand.getDemandDetails()) {
					if(demandDetail.getTaxHeadMasterCode().equalsIgnoreCase(amenddemandDetail.getTaxHeadMasterCode())) {
					BigDecimal demandAmount = demandDetail.getTaxAmount().subtract(demandDetail.getCollectionAmount());
					if(demandAmount.compareTo(BigDecimal.ZERO)>0) {
						if(amendedAmount.abs().compareTo(demandAmount)>0) {
							demandDetail.setCollectionAmount(demandDetail.getCollectionAmount().add(demandAmount));
							amenddemandDetail.setCollectionAmount(amenddemandDetail.getCollectionAmount().add(demandAmount).negate());
							totalValue = totalValue.add(amendedAmount);
							amendedAmount = amendedAmount.subtract(demandAmount);
							
						}
						else {
							demandDetail.setCollectionAmount(demandDetail.getCollectionAmount().add(amendedAmount.abs()));
							amenddemandDetail.setCollectionAmount(amenddemandDetail.getCollectionAmount().add(amendedAmount));

							amendedAmount = BigDecimal.ZERO;
						}
					}
					}
				}
				}
				
			}
			demand.getDemandDetails().addAll(amendment.getDemandDetails());
			DemandApportionRequest apportionRequest = DemandApportionRequest.builder().requestInfo(requestInfo)
					.demands(demands).tenantId(amendment.getTenantId()).build();
			Object response = serviceRequestRepository.fetchResult(util.getApportionURL(), apportionRequest);
			ApportionDemandResponse apportionDemandResponse = mapper.convertValue(response,
					ApportionDemandResponse.class);
			enrichAdvanceTaxHead(apportionDemandResponse.getDemands());

			demandService.update(new DemandRequest(requestInfo, apportionDemandResponse.getDemands()), null);

			AmendmentUpdate amendmentUpdate = AmendmentUpdate.builder()
					.additionalDetails(amendment.getAdditionalDetails()).amendmentId(amendment.getAmendmentId())
					.tenantId(amendment.getTenantId()).status(AmendmentStatus.CONSUMED).amendedDemandId(demand.getId())
					.auditDetails(auditDetails).build();

			amendmentRepository.updateAmendment(Arrays.asList(amendmentUpdate));
			billRepositoryV2.updateBillStatus(
					demands.stream().map(Demand::getConsumerCode).collect(Collectors.toList()), BillStatus.EXPIRED);
						 
		}
	}

	public void enrichAdvanceTaxHead(List<Demand> demands) {
		demands.forEach(demand -> {
			demand.getDemandDetails().forEach(demandDetail ->  {
					if (StringUtils.isEmpty(demandDetail.getId())
							&& demandDetail.getTaxHeadMasterCode().contains("ADVANCE")) {
						demandDetail.setId(UUID.randomUUID().toString());
						demandDetail.setTenantId(demand.getTenantId());
						demandDetail.setDemandId(demand.getId());
					}
				});
			});
		
	}
	
}
