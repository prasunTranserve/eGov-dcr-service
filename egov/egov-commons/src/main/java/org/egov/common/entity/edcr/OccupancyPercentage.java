package org.egov.common.entity.edcr;

import java.math.BigDecimal;

public class OccupancyPercentage {
	
	private String occupancy;
	
	private String subOccupancy;
	
	private BigDecimal totalBuildUpArea;
	
	private BigDecimal percentage;

	public String getOccupancy() {
		return occupancy;
	}

	public void setOccupancy(String occupancy) {
		this.occupancy = occupancy;
	}

	public String getSubOccupancy() {
		return subOccupancy;
	}

	public void setSubOccupancy(String subOccupancy) {
		this.subOccupancy = subOccupancy;
	}

	public BigDecimal getTotalBuildUpArea() {
		return totalBuildUpArea;
	}

	public void setTotalBuildUpArea(BigDecimal totalBuildUpArea) {
		this.totalBuildUpArea = totalBuildUpArea;
	}

	public BigDecimal getPercentage() {
		return percentage;
	}

	public void setPercentage(BigDecimal percentage) {
		this.percentage = percentage;
	}

	
}
