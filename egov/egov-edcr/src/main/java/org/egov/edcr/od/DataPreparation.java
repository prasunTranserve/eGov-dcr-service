package org.egov.edcr.od;

import java.math.BigDecimal;

import org.egov.common.entity.edcr.Plan;

public class DataPreparation {
	
	public static void updatePlanDetails(Plan pl) {
		updateVirtualBuildingHeight(pl);
		
	}
	
	private static void updateVirtualBuildingHeight(Plan pl) {
		BigDecimal buildingHeight = BigDecimal.ZERO;
		try {
			buildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getBuildingHeight())
					.reduce(BigDecimal::max).get();
		}catch (Exception e) {
			e.printStackTrace();
		}
		pl.getVirtualBuilding().setBuildingHeight(buildingHeight);
		
		
		BigDecimal declaredBuildingHeight = BigDecimal.ZERO;
		try {
			declaredBuildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getDeclaredBuildingHeight())
					.reduce(BigDecimal::max).get();
		}catch (Exception e) {
			e.printStackTrace();
		}
		pl.getVirtualBuilding().setDeclaredBuildingHeight(declaredBuildingHeight);
	}

}
