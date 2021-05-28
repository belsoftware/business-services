package org.egov.service;

import org.egov.tracer.model.CustomException;
import org.egov.util.ApportionConstants;
import org.egov.web.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class TranslationService {


    private TaxHeadMasterService taxHeadMasterService;


    @Autowired
    public TranslationService(TaxHeadMasterService taxHeadMasterService) {
        this.taxHeadMasterService = taxHeadMasterService;
    }


    public ApportionRequestV2 translate(Bill bill){

        String businessService = bill.getBusinessService();
        BigDecimal amountPaid = bill.getAmountPaid();
        Boolean isAdvanceAllowed = bill.getIsAdvanceAllowed();

        ApportionRequestV2 apportionRequestV2 = ApportionRequestV2.builder().amountPaid(amountPaid).businessService(businessService)
                                                .isAdvanceAllowed(isAdvanceAllowed).build();

        List<BillDetail> billDetails = bill.getBillDetails();

        for(BillDetail billDetail : billDetails){

            TaxDetail taxDetail = TaxDetail.builder().fromPeriod(billDetail.getFromPeriod()).amountToBePaid(billDetail.getAmount())
                                  .amountPaid((billDetail.getAmountPaid() == null) ? BigDecimal.ZERO : billDetail.getAmountPaid())
                                  .entityId(billDetail.getId())
                                  .build();

            billDetail.getBillAccountDetails().forEach(billAccountDetail -> {
                Bucket bucket = Bucket.builder().amount(billAccountDetail.getAmount())
                                .adjustedAmount((billAccountDetail.getAdjustedAmount()==null) ? BigDecimal.ZERO : billAccountDetail.getAdjustedAmount())
                                .taxHeadCode(billAccountDetail.getTaxHeadCode())
                                .priority(billAccountDetail.getOrder())
                                .entityId(billAccountDetail.getId())
                                .build();
                taxDetail.addBucket(bucket);
            });

            apportionRequestV2.addTaxDetail(taxDetail);
        }

        return apportionRequestV2;

    }



    public ApportionRequestV2 translate(List<Demand> demands,Object mdmsData) {

        // Group by businessService before calling this function
        String businessService = demands.get(0).getBusinessService();


        Map<String,Integer> codeToOrderMap = taxHeadMasterService.getCodeToOrderMap(businessService,mdmsData);

        // FIX ME
        BigDecimal amountPaid = BigDecimal.ZERO;
        Boolean isAdvanceAllowed = taxHeadMasterService.isAdvanceAllowed(businessService,mdmsData);


        ApportionRequestV2 apportionRequestV2 = ApportionRequestV2.builder().amountPaid(amountPaid).businessService(businessService)
                .isAdvanceAllowed(isAdvanceAllowed).build();

        Map<String,String> errorMap = new HashMap<>();

        for(Demand demand : demands){

            TaxDetail taxDetail = TaxDetail.builder().fromPeriod(demand.getTaxPeriodFrom()).entityId(demand.getId()).build();

            BigDecimal amountToBePaid = BigDecimal.ZERO;
            BigDecimal collectedAmount = BigDecimal.ZERO;

            for(DemandDetail demandDetail : demand.getDemandDetails()){

                Integer priority = codeToOrderMap.get(demandDetail.getTaxHeadMasterCode());

                if(priority == null)
                    errorMap.put("INVALID_TAXHEAD_CODE","Order is null or taxHead is not found for code: "+demandDetail.getTaxHeadMasterCode());

                Bucket bucket = Bucket.builder().amount(demandDetail.getTaxAmount())
                        .adjustedAmount((demandDetail.getCollectionAmount()==null) ? BigDecimal.ZERO : demandDetail.getCollectionAmount())
                        .taxHeadCode(demandDetail.getTaxHeadMasterCode())
                        .priority(priority)
                        .entityId(demandDetail.getId())
                        .build();
                taxDetail.addBucket(bucket);


                amountToBePaid = amountToBePaid.add(demandDetail.getTaxAmount());
                collectedAmount = collectedAmount.add(demandDetail.getCollectionAmount());
            }

            taxDetail.setAmountPaid(collectedAmount);
            taxDetail.setAmountToBePaid(amountToBePaid);

            apportionRequestV2.addTaxDetail(taxDetail);

        }

        if(!CollectionUtils.isEmpty(errorMap))
            throw new CustomException(errorMap);

        return apportionRequestV2;
    }


	public ApportionRequestV2 translateAmends(List<Demand> demands, Object mdmsData) {


        // Group by businessService before calling this function
        String businessService = demands.get(0).getBusinessService();


        Map<String,Integer> codeToOrderMap = taxHeadMasterService.getCodeToOrderMap(businessService,mdmsData);

        // FIX ME
        BigDecimal amountPaid = new BigDecimal(0);
        Boolean isAdvanceAllowed = taxHeadMasterService.isAdvanceAllowed(businessService,mdmsData);
        BigDecimal negAmounts = BigDecimal.ZERO;
        BigDecimal negAdjAmounts = BigDecimal.ZERO;

        ApportionRequestV2 apportionRequestV2 = ApportionRequestV2.builder().amountPaid(amountPaid).businessService(businessService)
                .isAdvanceAllowed(false).isAmend(true).build();

        Map<String,String> errorMap = new HashMap<>();

        for(Demand demand : demands){

            TaxDetail taxDetail = TaxDetail.builder().fromPeriod(demand.getTaxPeriodFrom()).entityId(demand.getId()).build();

            BigDecimal amountToBePaid = BigDecimal.ZERO;
            BigDecimal collectedAmount = BigDecimal.ZERO;

            for(DemandDetail demandDetail : demand.getDemandDetails()){

                Integer priority = codeToOrderMap.get(demandDetail.getTaxHeadMasterCode());

                if(priority == null)
                    errorMap.put("INVALID_TAXHEAD_CODE","Order is null or taxHead is not found for code: "+demandDetail.getTaxHeadMasterCode());

                Bucket bucket = Bucket.builder().amount(demandDetail.getTaxAmount())
                        .adjustedAmount((demandDetail.getCollectionAmount()==null) ? BigDecimal.ZERO : demandDetail.getCollectionAmount())
                        .taxHeadCode(demandDetail.getTaxHeadMasterCode())
                        .priority(priority)
                        .entityId(demandDetail.getId())
                        .build();
                
                //Adding all the negative amounts and adding as advance
                if(bucket.getAmount().compareTo(BigDecimal.ZERO)<0 && !ApportionConstants.ADDITIONAL_TAXES.contains(bucket.getTaxHeadCode())) {
                	negAmounts = negAmounts.add(bucket.getAmount());
                	negAdjAmounts = negAdjAmounts.add(bucket.getAdjustedAmount());
                	 if( !bucket.getTaxHeadCode().contains("ADVANCE")) 
                	bucket.setAdjustedAmount(bucket.getAmount());
                	

                }
                
                

                if(bucket.getAmount().compareTo(BigDecimal.ZERO)>0) {
                amountToBePaid = amountToBePaid.add(demandDetail.getTaxAmount());
                collectedAmount = collectedAmount.add(demandDetail.getCollectionAmount());
                }
                taxDetail.addBucket(bucket);
            }

            taxDetail.setAmountPaid(collectedAmount);
            taxDetail.setAmountToBePaid(amountToBePaid);
            
            BigDecimal amountPaidNeg = negAmounts.subtract(negAdjAmounts);
            //considering negative amounts as amount paid which will be used for apportioning among all the tax heads
            apportionRequestV2.setAmountPaid(negAmounts.subtract(negAdjAmounts).negate());
            List<Bucket> buckets1 = taxDetail.getBuckets();
            for (Bucket bucket : buckets1) {
            	 if( bucket.getTaxHeadCode().contains("ADVANCE")) {
            		 if(negAmounts.compareTo(BigDecimal.ZERO)!=0) {
            			 if(amountPaidNeg.abs().compareTo(bucket.getAmount().abs())>0) {
                    		 bucket.setAmount(amountPaidNeg);
            			 }
            			
            		 if(amountToBePaid.subtract(collectedAmount).compareTo(BigDecimal.ZERO)>0 )
            			if( amountToBePaid.subtract(collectedAmount).compareTo(bucket.getAmount().subtract(bucket.getAdjustedAmount()).abs())>=0) {
            				bucket.setAdjustedAmount(bucket.getAmount());
            			}
            			else
            				bucket.setAdjustedAmount(bucket.getAdjustedAmount().add(amountToBePaid.subtract(collectedAmount).negate()));
            		 }
            	 }
			}
            apportionRequestV2.addTaxDetail(taxDetail);

        }

        if(!CollectionUtils.isEmpty(errorMap))
            throw new CustomException(errorMap);

        return apportionRequestV2;
    
	}





    }
