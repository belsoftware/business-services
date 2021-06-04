package org.bel.abas.integration.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ReceiptFeeDetail {
	
	private String receiptHead;
	private String receptAmount;
	private String financialYear;
	private String demColCode;
}
