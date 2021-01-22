package org.egov.edcr.od;

import java.math.BigDecimal;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;

public class DataPreparation {

	public static void updatePlanDetails(Plan pl) {
		updateVirtualBuildingHeight(pl);
		updateSpecialBuilding(pl);

	}

	private static void updateVirtualBuildingHeight(Plan pl) {
		BigDecimal buildingHeight = BigDecimal.ZERO;
		try {
			buildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getBuildingHeight())
					.reduce(BigDecimal::max).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		pl.getVirtualBuilding().setBuildingHeight(buildingHeight);

		BigDecimal declaredBuildingHeight = BigDecimal.ZERO;
		try {
			declaredBuildingHeight = pl.getBlocks().stream()
					.map(block -> block.getBuilding().getDeclaredBuildingHeight()).reduce(BigDecimal::max).get();
		} catch (Exception e) {
			e.printStackTrace();
		}
		pl.getVirtualBuilding().setDeclaredBuildingHeight(declaredBuildingHeight);
	}

	private static void updateSpecialBuilding(Plan pl) {
		boolean specialBuilding = false;

		boolean isAssemblyBuilding = false;
		for (Block block : pl.getBlocks()) {
			if (block.isAssemblyBuilding()) {
				isAssemblyBuilding = true;
				break;
			}
		}

		boolean isHazardousBuildings = false;
		if (DxfFileConstants.YES.equals(pl.getPlanInformation().getBuildingUnderHazardousOccupancyCategory()))
			isHazardousBuildings = true;

		boolean isBuildingCentrallyAirConditioned = false;
		if (DxfFileConstants.YES.equals(pl.getPlanInformation().getBuildingCentrallyAirConditioned())) {
			if (pl.getVirtualBuilding().getTotalExistingBuiltUpArea().compareTo(new BigDecimal("500")) > 0)
				isBuildingCentrallyAirConditioned = true;
		}

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		boolean isSplOccupancy = false;
		if (DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_NON_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_STORAGE_PERISHABLE.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.WHOLESALE_MARKET.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.HOTEL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.FIVE_STAR_HOTEL.equals(occupancyTypeHelper.getSubtype().getCode()))
			isSplOccupancy = true;

		boolean isMixedOccupancies=false;//need to add condition
		
		if(isAssemblyBuilding || isHazardousBuildings || isBuildingCentrallyAirConditioned || isSplOccupancy || isMixedOccupancies)
			specialBuilding=true;
		
		pl.getPlanInformation().setSpecialBuilding(specialBuilding);
	}

}
