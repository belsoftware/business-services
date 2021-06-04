package org.bel.abas.integration.model;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ABASPaymentRequest {

    @JsonProperty("voucherextsysdto")
    private ArrayList<ABASPayment> voucherextsysdto = null;
}
