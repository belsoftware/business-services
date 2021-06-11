package org.bel.abas.integration.controller;

import org.bel.abas.integration.repository.ABASRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1/")
public class AbasIntegController {

	@Autowired
    private ABASRepository abasRepository;
	
	@RequestMapping(value = { "_transactionReversal"}, method = RequestMethod.POST)
	public ResponseEntity<String>  deleteDeathImport(@RequestParam String json) {
		abasRepository.saveSharedData(json,"ABAS","TRANSACTION_REVERSAL_RECEIVE");
        return new ResponseEntity<>("Saved " , HttpStatus.OK);
    }
}
