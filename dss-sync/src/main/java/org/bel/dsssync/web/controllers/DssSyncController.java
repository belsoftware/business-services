package org.bel.dsssync.web.controllers;

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

	@RequestMapping(value = { "/_migrate"}, method = RequestMethod.POST)
    public String migrate(@RequestBody RequestInfoWrapper requestInfoWrapper,
                                                        @ModelAttribute SearchCriteria criteria
            ) {
        String response = dssSyncService.migrate(criteria, requestInfoWrapper);
        return response;
    }
}
