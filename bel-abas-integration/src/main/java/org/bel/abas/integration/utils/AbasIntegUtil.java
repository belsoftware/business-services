package org.bel.abas.integration.utils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import org.bel.abas.integration.model.PaymentModeEnum;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class AbasIntegUtil {
	
	public SimpleDateFormat sd = new SimpleDateFormat("dd/MM/yyyy");
	
	public Charset UTF_8 = StandardCharsets.UTF_8;
	
	public byte[] digest(byte[] input) {
		MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
		byte[] result = md.digest(input);
		return result;
	}

	public String bytesToHex(byte[] bytes) {
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(String.format("%02x", b));
		}
		return sb.toString();
	}
	
	public int getFiscalYear(Calendar calendarDate) {
        int month = calendarDate.get(Calendar.MONTH);
        int year = calendarDate.get(Calendar.YEAR);
        return (month > Calendar.MARCH) ? year : year - 1;
    }
	
	public Map<String, String> abasPaymentModeMap = new HashMap<String, String>(){{
		put(PaymentModeEnum.CASH.toString(),"C");
		put(PaymentModeEnum.CHEQUE.toString(),"Q");
		put(PaymentModeEnum.DD.toString(),"D");
		put(PaymentModeEnum.ONLINE.toString(),"W");
		
	}};
}
