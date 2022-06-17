
package org.egov.edcr.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.egov.commons.mdms.config.MdmsConfiguration;
import org.egov.commons.mdms.model.MasterDetail;
import org.egov.commons.mdms.model.MdmsCriteria;
import org.egov.commons.mdms.model.MdmsCriteriaReq;
import org.egov.commons.mdms.model.ModuleDetail;
import org.egov.commons.service.RestCallService;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.stereotype.Service;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

@Service
public class MdmsService {
	private RestCallService serviceRequestRepository;
	private MdmsConfiguration mdmsConfiguration;

	public MdmsService(RestCallService serviceRequestRepository, MdmsConfiguration mdmsConfiguration) {
		this.serviceRequestRepository = serviceRequestRepository;
		this.mdmsConfiguration = mdmsConfiguration;
	}

	public StringBuilder getMdmsSearchUrl() {
		return new StringBuilder().append(mdmsConfiguration.getMdmsHost()).append(mdmsConfiguration.getMdmsSearchUrl());
	}

	public Object fetchTenantsMaster(RequestInfo requestInfo, String tenantId) {
		MdmsCriteria mdmsCriteria = new MdmsCriteria();
		ModuleDetail moduleDetail = new ModuleDetail();
		moduleDetail.setModuleName("tenant");
		MasterDetail masterDetail = new MasterDetail();
		masterDetail.setName("tenants");
		masterDetail.setFilter("[?(@.code=='" + tenantId + "')]");
		List<MasterDetail> masterDetails = new ArrayList<>();
		masterDetails.add(masterDetail);

		moduleDetail.setMasterDetails(masterDetails);
		List<ModuleDetail> moduleDetails = new ArrayList<>();
		moduleDetails.add(moduleDetail);
		mdmsCriteria.setModuleDetails(moduleDetails);
		mdmsCriteria.setTenantId("od");
		MdmsCriteriaReq mdmsCriteriaReq = new MdmsCriteriaReq();
		mdmsCriteriaReq.setMdmsCriteria(mdmsCriteria);
		mdmsCriteriaReq.setRequestInfo(requestInfo);
		Object result = serviceRequestRepository.fetchResult(getMdmsSearchUrl(), mdmsCriteriaReq);
		return result;
	}

	public String[] getUlbNameAndGradeFromMdms(RequestInfo requestInfo, String tenantId) {
		Object tenantsMaster = fetchTenantsMaster(requestInfo, tenantId);
		String ulbGrade = getValue((Map) tenantsMaster, "$.MdmsRes.tenant.tenants[0].city.ulbGrade");
		String ulbName = getValue((Map) tenantsMaster, "$.MdmsRes.tenant.tenants[0].city.name");
		String[] ulbNameAndGrade = new String[2];
		ulbNameAndGrade[0] = ulbName;
		ulbNameAndGrade[1] = ulbGrade;
		return ulbNameAndGrade;
	}

	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
}
