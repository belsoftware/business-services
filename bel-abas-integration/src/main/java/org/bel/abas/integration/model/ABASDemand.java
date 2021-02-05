package org.bel.abas.integration.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ABASDemand {
	private String voucherDate;
	private String vousubTypeCode="DMD";
	private String departmentCode="AC";
	private String ulbCode;
	private String voucherReferenceNo;
	private String voucherReferenceDate;
	private String narration;
	private String payerOrPayee;
	private String entryFlag=null;
	private String payModeCode="T";
	private String entryCode="EXS";
	private String financialYear;
	private String createdBy;
	private String LocationDescription="1-1";
	//private String checkSum;
	@JsonProperty("voucherExtDetails")
	private ArrayList<ABASDemandDetail> voucherExtDetails;
}
