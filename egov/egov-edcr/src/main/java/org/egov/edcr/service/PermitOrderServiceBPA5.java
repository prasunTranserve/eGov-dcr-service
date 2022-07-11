package org.egov.edcr.service;

import java.io.InputStream;
import java.util.LinkedHashMap;

import org.egov.common.entity.edcr.Plan;
import org.egov.infra.microservice.models.RequestInfo;

public class PermitOrderServiceBPA5 extends PermitOrderServiceBPA1 {

	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo) {
		return super.generateReport(plan, bpaApplication, requestInfo);
	}

}