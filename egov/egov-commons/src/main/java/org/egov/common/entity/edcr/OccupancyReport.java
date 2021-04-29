package org.egov.common.entity.edcr;

import java.math.BigDecimal;

public class OccupancyReport {
	
	private String occupancy;
	
	private String subOccupancy;
	
	private BigDecimal percentage;


	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}

	public String getSubOccupancy() {
		return subOccupancy;
	}

	public void setSubOccupancy(String subOccupancy) {
		this.subOccupancy = subOccupancy;
	}

	public String getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(String occupancy) {
		this.occupancy = occupancy;
	}

	
}
