package org.bel.abas.integration.model;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ABASDemandDetail {
	
	private BigDecimal voucherAmount ;
	private String acHeadCode;
}
