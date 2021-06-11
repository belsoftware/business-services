package org.bel.abas.integration.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ABASDemandRequest {
	
	@JsonProperty("checkSum")
    private String checkSum = null;

    @JsonProperty("voucherextsysdto")
    private ArrayList<ABASDemand> voucherextsysdto = null;
}