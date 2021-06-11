package org.bel.abas.integration.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ABASPayment {
	private String receiptNumber;
	private String receiptDate;
	private String receiptCategory;
	private String vendorName;
	private String receivedFrom;
	private String departmentName;
	private String mobileNumber;
	private String emailId;
	private String fieldCode="1-1";
	private String ulbCode;
	private String createdBy;
	private String payMode;
	private String ifscCode;
	private String instrumentNo;
	private String instrumentDate;
	private String narration;
	private String checkSum;

	@JsonProperty("receiptFeeDetailList")
	private ArrayList<ReceiptFeeDetail> receiptFeeDetailList;
	
	private String bankName;
}
