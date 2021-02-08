package org.bel.dsssync.web.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import org.bel.dsssync.model.MigrationParams;
import org.egov.common.contract.request.RequestInfo;


@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RequestInfoWrapper {

	@JsonProperty("RequestInfo")
	private RequestInfo requestInfo;
	
	@JsonProperty("migrationParams")
	private MigrationParams migrationParams;
	
}
