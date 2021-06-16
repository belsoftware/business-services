package org.bel.abas.integration.consumer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import org.bel.abas.integration.contract.PaymentRequest;
import org.bel.abas.integration.model.ABASPayment;
import org.bel.abas.integration.model.ABASPaymentRequest;
import org.bel.abas.integration.model.BillAccountDetail;
import org.bel.abas.integration.model.BillDetail;
import org.bel.abas.integration.model.Payment;
import org.bel.abas.integration.model.PaymentDetail;
import org.bel.abas.integration.model.ReceiptFeeDetail;
import org.bel.abas.integration.repository.ABASRepository;
import org.bel.abas.integration.utils.AbasIntegUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PaymentListener {
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private ABASRepository abasRepository;
    
    @Autowired
    private AbasIntegUtil util;
    
    @KafkaListener(topics = "${kafka.topics.payment.create.name}")
    public void listen(final HashMap<String, Object> record) {
		try {
			ABASPaymentRequest request = new ABASPaymentRequest();
			ArrayList<ABASPayment> abasPayments = new ArrayList<ABASPayment>();
			PaymentRequest paymentRequest = objectMapper.convertValue(record, PaymentRequest.class);
			Payment payment = paymentRequest.getPayment();
			ABASPayment abasPayment = new ABASPayment();
			abasPayment.setReceiptNumber(payment.getPaymentDetails().get(0).getReceiptNumber());
			abasPayment.setReceiptDate(util.sd.format(new Date(payment.getPaymentDetails().get(0).getReceiptDate())));
			abasPayment.setVendorName(payment.getPaidBy());
			abasPayment.setReceivedFrom(payment.getPaidBy());
			abasPayment.setMobileNumber(payment.getMobileNumber());
			abasPayment.setEmailId(payment.getPayerEmail());
			abasPayment.setUlbCode(abasRepository.getULBCode(payment.getTenantId()));
			abasPayment.setCreatedBy(payment.getAuditDetails().getCreatedBy());
			abasPayment.setPayMode(util.abasPaymentModeMap.get(payment.getPaymentMode().toString()));
			abasPayment.setIfscCode(payment.getIfscCode());
			abasPayment.setInstrumentNo(payment.getInstrumentNumber());
			abasPayment.setInstrumentDate(util.sd.format(new Date(payment.getInstrumentDate())));
			abasPayment.setNarration("Receipt Voucher for Receipt No. "+payment.getPaymentDetails().get(0).getReceiptNumber());
			abasPayment.setCheckSum(util.bytesToHex(util.digest((abasPayment.getCreatedBy() +"|"+abasPayment.getUlbCode()).getBytes(util.UTF_8))));
			
			int year = util.getFiscalYear(Calendar.getInstance());
			ArrayList<ReceiptFeeDetail> abasPaymentDetails = new ArrayList<ReceiptFeeDetail>();
			for (PaymentDetail paymentDetail : payment.getPaymentDetails()) {
				for(BillDetail billDetail : paymentDetail.getBill().getBillDetails()) {
					for(BillAccountDetail billAccountDetail : billDetail.getBillAccountDetails()) {
						ReceiptFeeDetail receiptFeeDetail = new ReceiptFeeDetail();
						receiptFeeDetail.setReceiptHead(util.getGLCodeFromTaxHead(billAccountDetail.getTaxHeadCode(),
								paymentRequest.getRequestInfo(), payment.getTenantId()));
						receiptFeeDetail.setReceptAmount(billAccountDetail.getAmount().toString());
						receiptFeeDetail.setFinancialYear(year + "-" + (year + 1));
						receiptFeeDetail.setDemColCode(null);
						abasPaymentDetails.add(receiptFeeDetail);
					}
				}
			}
			abasPayment.setReceiptFeeDetailList(abasPaymentDetails);
			abasPayments.add(abasPayment);
			request.setVoucherextsysdto(abasPayments);
			String json = new Gson().toJson(request);
			abasRepository.saveSharedData(json, "BEL","PAYMENT_SEND");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
    }
}
