package org.egov.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class ApportionConstants {

    public static final String DEFAULT = "DEFAULT";

    public static final String MDMS_BILLING_SERVICE = "BillingService";

    public static final String MDMS_TAXHEAD  = "TaxHeadMaster";

    public static final String MDMS_BUSINESSSERVICE  = "BusinessService";

    public static final String ADVANCE_TAXHEAD_JSONPATH_CODE = "$.MdmsRes.BillingService.TaxHeadMaster[?(@.category=='ADVANCE_COLLECTION' && @.service==\"{}\")].code";

    public static final String ADVANCE_BUSINESSSERVICE_JSONPATH_CODE = "$.MdmsRes.BillingService.BusinessService[?(@.code==\"{}\")].isAdvanceAllowed";

    public static final String TAXHEAD_JSONPATH_CODE = "$.MdmsRes.BillingService.TaxHeadMaster[?(@.service==\"{}\")]";

    public static final String MDMS_ORDER_KEY  = "order";

    public static final String MDMS_TAXHEADCODE_KEY  = "code";
    
    public static final String PT = "PT";
    
    public static final String PT_UNIT_USAGE_EXEMPTION = "PT_UNIT_USAGE_EXEMPTION";

	public static final String PT_OWNER_EXEMPTION = "PT_OWNER_EXEMPTION";

	public static final String PT_TIME_REBATE = "PT_TIME_REBATE";

	public static final String PT_TIME_PENALTY = "PT_TIME_PENALTY";

	public static final String PT_TIME_INTEREST = "PT_TIME_INTEREST";

	public static final String PT_ADVANCE_CARRYFORWARD = "PT_ADVANCE_CARRYFORWARD";

	public static final String PT_FIRE_CESS = "PT_FIRE_CESS";

	public static final String PT_CANCER_CESS = "PT_CANCER_CESS";

	public static final String PT_ADHOC_PENALTY = "PT_ADHOC_PENALTY";

	public static final String PT_ADHOC_REBATE = "PT_ADHOC_REBATE";
	
	public static final String PT_ROUNDOFF = "PT_ROUNDOFF";
	
	public static final String PT_DEMANDNOTICE_CHARGE = "PT_DEMANDNOTICE_CHARGE";

	
    public static final List<String> ADDITIONAL_TAXES = Collections.unmodifiableList(Arrays
			.asList(PT_FIRE_CESS, PT_UNIT_USAGE_EXEMPTION, PT_UNIT_USAGE_EXEMPTION, PT_ADHOC_PENALTY, PT_CANCER_CESS, PT_OWNER_EXEMPTION, PT_TIME_REBATE,
					PT_TIME_PENALTY, PT_TIME_INTEREST,PT_FIRE_CESS, PT_CANCER_CESS, PT_ADHOC_PENALTY, PT_ADHOC_REBATE,PT_ROUNDOFF, PT_DEMANDNOTICE_CHARGE));


    public ApportionConstants() {
    }
}
