package org.egov.collection.consumer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.collection.model.Payment;
import org.egov.collection.model.PaymentDetail;
import org.egov.collection.model.PaymentRequest;
import org.egov.collection.producer.CollectionProducer;
import org.egov.collection.web.contract.Bill;
import org.egov.common.contract.request.RequestInfo;
import org.egov.collection.config.ApplicationProperties;

import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.encoder.org.apache.commons.lang.StringUtils;

import static org.egov.collection.config.CollectionServiceConstants.*;

@Slf4j
@Component
public class CollectionNotificationConsumer{

    @Autowired
    private ObjectMapper objectMapper;
    // url shortner

    @Value("${egov.url.shortner.host}")
    private String urlShortnerHost;

    @Value("${egov.url.shortner.endpoint}")
    private String urlShortnerEndpoint;
    
    @Autowired
    private ApplicationProperties applicationProperties;

    @Autowired
    private CollectionProducer producer;
    
    public static final String PAYMENT_MSG_LOCALIZATION_CODE = "coll.notif.payment.receipt.link";

    @Autowired
    private RestTemplate restTemplate;

    @KafkaListener(topics = { "${kafka.topics.payment.create.name}", "${kafka.topics.payment.receiptlink.name}" })
    public void listen(HashMap<String, Object> record, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic){
        try{
            PaymentRequest req = objectMapper.convertValue(record, PaymentRequest.class);
            sendNotification(req);
        }catch(Exception e){
            log.error("Exception while reading from the queue: ", e);
        }
    }

    private void sendNotification(PaymentRequest paymentRequest){
        Payment payment = paymentRequest.getPayment();
        for (PaymentDetail paymentDetail : payment.getPaymentDetails()) {
            String mobNo = payment.getMobileNumber();
            String paymentStatus = (payment.getPaymentStatus().toString() == null ? "NEW" : payment.getPaymentStatus().toString());
            Bill bill = paymentDetail.getBill();
            String message = buildSmsBody(bill, paymentDetail, paymentRequest.getRequestInfo(), paymentStatus);
            String templateId =fetchTemplateIdFromLocalization(paymentRequest.getRequestInfo(), bill.getTenantId(), COLLECTION_LOCALIZATION_MODULE, PAYMENT_MSG_LOCALIZATION_CODE);
			
            if (!StringUtils.isEmpty(message)) {
                HashMap<String, Object> request = new HashMap<>();
                request.put("mobileNumber", mobNo);
                request.put("message", message);

				request.put("templateId", templateId);
                producer.producer(applicationProperties.getSmsTopic(), request);
            } else {
                log.error("Message not configured! No notification will be sent.");
            }
        }

    }

    private String buildSmsBody(Bill bill, PaymentDetail paymentDetail, RequestInfo requestInfo, String paymentStatus){
        String message = null;
        String content = null;
        switch(paymentStatus.toUpperCase()){
            case "NEW":
                content = fetchContentFromLocalization(requestInfo, paymentDetail.getTenantId(), COLLECTION_LOCALIZATION_MODULE, WF_MT_STATUS_OPEN_CODE);
                break;
            case "DEPOSITED":
                content = fetchContentFromLocalization(requestInfo, paymentDetail.getTenantId(), COLLECTION_LOCALIZATION_MODULE, WF_MT_STATUS_DEPOSITED_CODE);
                break;
            case "CANCELLED":
                content = fetchContentFromLocalization(requestInfo, paymentDetail.getTenantId(), COLLECTION_LOCALIZATION_MODULE, WF_MT_STATUS_CANCELLED_CODE);
                break;
            case "DISHONOURED":
                content = fetchContentFromLocalization(requestInfo, paymentDetail.getTenantId(), COLLECTION_LOCALIZATION_MODULE, WF_MT_STATUS_DISHONOURED_CODE);
                break;
            default:
                break;
        }
        if(!StringUtils.isEmpty(content)){
            StringBuilder link = new StringBuilder();
            link.append(applicationProperties.getUiHost() + "/citizen").append("/otpLogin?mobileNo=").append(bill.getMobileNumber()).append("&redirectTo=")
                    .append(applicationProperties.getUiRedirectUrl()).append("&params=").append(paymentDetail.getTenantId() + "," + paymentDetail.getReceiptNumber());

            content = content.replaceAll("<rcpt_link>", getShortenedUrl(link.toString()));

            String moduleName = fetchContentFromLocalization(requestInfo, paymentDetail.getTenantId(),
                    BUSINESSSERVICE_LOCALIZATION_MODULE, formatCodes(paymentDetail.getBusinessService()));

            if(StringUtils.isEmpty(moduleName))
                moduleName = "Adhoc Tax";

            content = content.replaceAll("<owner_name>", bill.getPayerName());

            if(content.contains("<amount_paid>"))
                content = content.replaceAll("<amount_paid>", paymentDetail.getTotalAmountPaid().toString());
            content = content.replaceAll("<fin_year>", fetchFinYear(bill.getBillDetails().get(0).getFromPeriod(), bill.getBillDetails().get(0).getToPeriod()));
			content = content.replaceAll("<rcpt_no>", paymentDetail.getReceiptNumber());
            content = content.replaceAll("<tax_name>", moduleName);
            content = content.replaceAll("<unique_id>", bill.getConsumerCode());
            content = content.replaceAll("<fin_year>", fetchFinYear(bill.getBillDetails().get(0).getFromPeriod(), bill.getBillDetails().get(0).getToPeriod()));
			
            message = content;
        }
        return message;
    }
    
    /**
	 * Fix : Add template id 
	 * @param requestInfo
	 * @param tenantId
	 * @param module
	 * @param code
	 * @return
	 */
	private String fetchTemplateIdFromLocalization(RequestInfo requestInfo, String tenantId, String module, String code) {
        List<String> codes = new ArrayList<>(); 
        List<String> templateIds = new ArrayList<>();
        String templateId = null;
        Object result = null;
        String locale = "";
        if(requestInfo.getMsgId().contains("|"))
            locale = requestInfo.getMsgId().split("[\\|]")[1];
        if(StringUtils.isEmpty(locale))
            locale = applicationProperties.getFallBackLocale();
        StringBuilder uri = new StringBuilder();
        uri.append(applicationProperties.getLocalizationHost()).append(applicationProperties.getLocalizationEndpoint());
        uri.append("?tenantId=").append(tenantId.split("\\.")[0]).append("&locale=").append(locale).append("&module=").append(module);

        Map<String, Object> request = new HashMap<>();
        request.put("RequestInfo", requestInfo);
		try {
			result = restTemplate.postForObject(uri.toString(), request, Map.class);
			codes = JsonPath.read(result, LOCALIZATION_CODES_JSONPATH);
			templateIds = JsonPath.read(result, LOCALIZATION_TEMPLATEID_JSONPATH);
			if (null != result && !CollectionUtils.isEmpty(codes) && !CollectionUtils.isEmpty(templateIds) && codes.size() == templateIds.size()) {
				for (int i = 0; i < codes.size(); i++) {
					if(codes.get(i).equals(code)) templateId = templateIds.get(i);
				}
			}
		} catch (Exception e) {
			log.error("Exception while fetching from localization: " + e);
		}
		
		return templateId;
	}

    private String fetchContentFromLocalization(RequestInfo requestInfo, String tenantId, String module, String code){
        String message = null;
        List<String> codes = new ArrayList<>();
        List<String> messages = new ArrayList<>();
        Object result = null;
        String locale = "";
        if(requestInfo.getMsgId().contains("|"))
            locale = requestInfo.getMsgId().split("[\\|]")[1];
        if(StringUtils.isEmpty(locale))
            locale = applicationProperties.getFallBackLocale();
        StringBuilder uri = new StringBuilder();
        uri.append(applicationProperties.getLocalizationHost()).append(applicationProperties.getLocalizationEndpoint());
        uri.append("?tenantId=").append(tenantId.split("\\.")[0]).append("&locale=").append(locale).append("&module=").append(module);

        Map<String, Object> request = new HashMap<>();
        request.put("RequestInfo", requestInfo);
        try {
            result = restTemplate.postForObject(uri.toString(), request, Map.class);
            codes = JsonPath.read(result, LOCALIZATION_CODES_JSONPATH);
            messages = JsonPath.read(result, LOCALIZATION_MSGS_JSONPATH);
        } catch (Exception e) {
            log.error("Exception while fetching from localization: " + e);
        }
        if(CollectionUtils.isEmpty(messages)){
            throw new CustomException("LOCALIZATION_NOT_FOUND", "Localization not found for the code: " + code);
        }
        for(int index = 0; index < codes.size(); index++){
            if(codes.get(index).equals(code)){
                message = messages.get(index);
            }
        }
        return message;
    }

    private String formatCodes(String code){
        String regexForSpecialCharacters  = "[$&+,:;=?@#|'<>.^*()%!-]";
        code = code.replaceAll(regexForSpecialCharacters, "");
        code = code.replaceAll(" ", "_");

        return BUSINESSSERVICELOCALIZATION_CODE_PREFIX + code.toUpperCase();
    }
    
	/**
	 * Method to shortent the url
	 * returns the same url if shortening fails
	 * @param url
	 */
	public String getShortenedUrl(String url){

		HashMap<String,String> body = new HashMap<>();
		body.put("url",url);
		StringBuilder builder = new StringBuilder(this.urlShortnerHost);
		builder.append(this.urlShortnerEndpoint);
		String res = restTemplate.postForObject(builder.toString(), body, String.class);

		if(StringUtils.isEmpty(res)){
			log.error("URL_SHORTENING_ERROR","Unable to shorten url: "+url); ;
			return url;
		}
		else return res;
	}
	
	/**
	 * Method to fetch year format from from and to period of the bill
	 *
	 * @param fromPeriod
	 * @param toPeriod
	 * @return
	 */
	private String fetchFinYear(Long fromPeriod, Long toPeriod) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(fromPeriod);
		int fromYear = calendar.get(Calendar.YEAR);
		calendar.setTimeInMillis(toPeriod);
		int toYear = calendar.get(Calendar.YEAR);
		if((toYear - fromYear) == 0)
			return toYear + "";
		return fromYear + "-" + (toYear % 1000);
	}
}
