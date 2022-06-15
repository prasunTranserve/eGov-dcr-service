package org.egov.commons.mdms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MdmsConfiguration {

    @Value("${mdms.host:}")
    private String mdmsHost;

    @Value("${mdms.searchurl:}")
    private String mdmsSearchUrl;

    @Value("${mdms.enable:false}")
    private Boolean mdmsEnabled;
    
    @Value("${egov.collection.service.host:}")
    private String collectionServiceHost;
    
    @Value("${egov.noc.service.host:}")
    private String nocHost;

    public String getMdmsHost() {
        return mdmsHost;
    }

    public void setMdmsHost(String mdmsHost) {
        this.mdmsHost = mdmsHost;
    }

    public String getMdmsSearchUrl() {
        return mdmsSearchUrl;
    }

    public void setMdmsSearchUrl(String mdmsSearchUrl) {
        this.mdmsSearchUrl = mdmsSearchUrl;
    }

    public Boolean getMdmsEnabled() {
        return mdmsEnabled;
    }

    public void setMdmsEnabled(Boolean mdmsEnabled) {
        this.mdmsEnabled = mdmsEnabled;
    }

	public String getCollectionServiceHost() {
		return collectionServiceHost;
	}

	public void setCollectionServiceHost(String collectionServiceHost) {
		this.collectionServiceHost = collectionServiceHost;
	}

	public String getNocHost() {
		return nocHost;
	}

	public void setNocHost(String nocHost) {
		this.nocHost = nocHost;
	}

    
}
