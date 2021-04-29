package org.bel.dsssync.web.controllers;

import org.bel.dsssync.cronjob.HourlyJob;
import org.bel.dsssync.model.SearchCriteria;
import org.bel.dsssync.service.DssSyncService;
import org.bel.dsssync.web.models.RequestInfoWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/v1")
public class DssSyncController {

	@Autowired
	private DssSyncService dssSyncService;
	
	@Autowired
	private HourlyJob job;

	@RequestMapping(value = { "/_migrate"}, method = RequestMethod.POST)
    public String migrate(@RequestBody RequestInfoWrapper requestInfoWrapper,
                                                        @ModelAttribute SearchCriteria criteria
            ) {
        String response = dssSyncService.migrate(criteria, requestInfoWrapper);
        return response;
    }
	
	@RequestMapping(value = { "/_orsIntegration"}, method = RequestMethod.POST)
    public String orsIntegration(@ModelAttribute SearchCriteria criteria) {
        String response = job.orsIntegration(criteria.getFromDateStr(), criteria.getToDateStr());
        return response;
    }
}
