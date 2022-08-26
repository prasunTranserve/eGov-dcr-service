
package org.egov.edcr.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.service.RestCallService;
import org.egov.infra.config.core.ApplicationThreadLocals;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.itextpdf.text.List;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class UserInfoService {
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;
	
	public UserInfoService(RestCallService serviceRequestRepository,MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}

	public StringBuilder getUserSearchUrl() {
		String hostUrl = mdmsConfiguration.getUserServiceHost();
		
		String url = String.format("%s/user/_search", hostUrl);
		return new StringBuilder(url);
	}

	public Object fetchUserInfo(RequestInfo requestInfo, String tenantId, String businessService,String userUUID) {
		StringBuilder searchUrl = getUserSearchUrl();
		Map<String, Object> requestInfoPayload = new HashMap<>();
		java.util.List<String> uuids = new ArrayList<>();
		uuids.add(userUUID);
		requestInfoPayload.put("RequestInfo", requestInfo);
		requestInfoPayload.put("uuid", uuids);
		requestInfoPayload.put("tenantId", tenantId);
		Object result = serviceRequestRepository.fetchResult(searchUrl, requestInfoPayload);
		return result;
	}

	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
}
