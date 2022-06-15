
package org.egov.edcr.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.service.RestCallService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.microservice.models.RequestInfo;
import org.jfree.util.Log;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class PaymentService {
	private static Logger LOG = Logger.getLogger(PaymentService.class);
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;
	
	public PaymentService(RestCallService serviceRequestRepository, MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}

	public StringBuilder getPaymentsSearchUrl(String businessservice) {
		StringBuilder uri = new StringBuilder().append("%s/collection-services/payments/").append(businessservice)
				.append("/_search");
		String hostUrl = mdmsConfiguration.getBpaCalculatorHost();
		String url = String.format(uri.toString(), hostUrl);
		return new StringBuilder(url);
	}

	public Object fetchApplicationFeePaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrl("BPA.NC_APP_FEE").append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
	}

	public Object fetchPermitFeePaymentDetails(RequestInfo requestInfo, String consumerCode, String tenantId) {
		StringBuilder searchUrl = getPaymentsSearchUrl("BPA.NC_SAN_FEE").append("?consumerCodes=").append(consumerCode)
				.append("&tenantId=").append(tenantId);
		Map<String, Object> requestInfoPayload = new HashMap<>();
		requestInfoPayload.put("RequestInfo", requestInfo);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		LOG.info(result);
		return result;
	}

	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
}
