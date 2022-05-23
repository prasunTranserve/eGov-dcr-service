package org.egov.edcr.od;

import java.math.BigDecimal;

import org.egov.common.entity.ApplicationType;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;

public class DataPreparation {

	public static void updatePlanDetails(Plan pl) {
		updateNmaData(pl);
		updateVirtualBuildingHeight(pl);
		updateSpecialBuilding(pl);
		updateBusinessService(pl);
		updateSubOccupancy(pl);
	}
	
	private static void updateNmaData(Plan pl) {
		StringBuffer numberOfStoreys=new StringBuffer();
		StringBuffer buildingHeightExcludingMumty=new StringBuffer();
		StringBuffer floorAreaInSquareMetresStoreyWise=new StringBuffer();
		
		for(Block block:pl.getBlocks()) {
			if(block.getBuilding().getFloors()!=null) {
				numberOfStoreys.append("block "+block.getNumber()+" "+block.getBuilding().getFloors().size()+" \r\n");
				for(Floor floor:block.getBuilding().getFloors()) {
					StringBuffer occupancyArea=new StringBuffer();
					for(Occupancy occupancy:floor.getOccupancies()) {
						if(occupancy.getTypeHelper()!=null & occupancy.getTypeHelper().getType()!=null)
						occupancyArea.append(occupancy.getTypeHelper().getType().getName()+" "+ occupancy.getBuiltUpArea()+" \r\n");
					}
					floorAreaInSquareMetresStoreyWise.append("block "+block.getNumber()+" floor "+floor.getNumber()+" "+occupancyArea.toString()+" \r\n");
				}
			}
			buildingHeightExcludingMumty.append("block"+block.getNumber()+" "+block.getBuilding().getBuildingHeight()+" \r\n");
		}
		pl.getPlanInformation().setNumberOfStoreys(numberOfStoreys.toString());
		pl.getPlanInformation().setBuildingHeightExcludingMumty(buildingHeightExcludingMumty.toString());
		pl.getPlanInformation().setBuildingHeightIncludingMumty(buildingHeightExcludingMumty.toString());
		pl.getPlanInformation().setFloorAreaInSquareMetresStoreyWise(floorAreaInSquareMetresStoreyWise.toString());
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
			if (pl.getVirtualBuilding().getTotalBuitUpArea().compareTo(new BigDecimal("500")) > 0)
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

		boolean isMixedOccupancies = false;// need to add condition

		if (isAssemblyBuilding || isHazardousBuildings || isBuildingCentrallyAirConditioned || isSplOccupancy
				|| isMixedOccupancies)
			specialBuilding = true;

		pl.getPlanInformation().setSpecialBuilding(specialBuilding ? DxfFileConstants.YES : DxfFileConstants.NO);
	}

	private static void updateBusinessService(Plan pl) {

		try {
			Double buildingHeight = pl.getVirtualBuilding().getBuildingHeight().doubleValue();
			Double plotArea = pl.getPlot().getArea().doubleValue();
			boolean isSpecialBuilding = DxfFileConstants.YES.equals(pl.getPlanInformation().getSpecialBuilding()) ? true
					: false;
			if(ApplicationType.OCCUPANCY_CERTIFICATE.equals(pl.getApplicationType()))
				setBusinessServiceOC(pl, buildingHeight, plotArea, isSpecialBuilding);
			else
				setBusinessService(pl, buildingHeight, plotArea, isSpecialBuilding);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (pl.getPlanInformation().getBusinessService() == null
				|| pl.getPlanInformation().getBusinessService().trim().isEmpty()) {
			pl.addError("BusinessService", "Not able to find BusinessService Type.");

		}
	}

	private static void setBusinessService(Plan pl, Double buildingHeight, Double plotArea, boolean isSpecialBuilding) {
		if (null != buildingHeight && null != plotArea) {
			if (!isSpecialBuilding) {
				if ((buildingHeight <= 10) || (plotArea <= 500)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_PA_MODULE_CODE);
				}
				if ((buildingHeight > 10 && buildingHeight <= 15) || (plotArea > 500 && plotArea <= 4047)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_PO_MODULE_CODE);
				}
				if ((buildingHeight > 15 && buildingHeight <= 30) || (plotArea > 4047 && plotArea <= 10000)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_PM_MODULE_CODE);
				}
				if ((buildingHeight > 30) || (plotArea > 10000)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_DP_BP_MODULE_CODE);
				}

			} else {
				if (buildingHeight <= 15) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_PO_MODULE_CODE);
				} else if (buildingHeight > 15 && buildingHeight <= 30) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_PM_MODULE_CODE);
				} else if (buildingHeight > 30) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_DP_BP_MODULE_CODE);
				}

			}
		}
	}
	
	private static void setBusinessServiceOC(Plan pl, Double buildingHeight, Double plotArea, boolean isSpecialBuilding) {
		if (null != buildingHeight && null != plotArea) {
			if (!isSpecialBuilding) {
				if ((buildingHeight <= 10) || (plotArea <= 500)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_PA_MODULE_CODE);
					if(isPlanApplicableForAccreditedWorkflow(pl, buildingHeight, plotArea)) {
						pl.getPlanInformation().setBusinessService(pl.getPlanInformation().getBusinessService().concat("|"+DxfFileConstants.BPA_APPROVAL_BY_AN_ACCREDITED_PERSON));
					}
				}
				if ((buildingHeight > 10 && buildingHeight <= 15) || (plotArea > 500 && plotArea <= 4047)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_PO_MODULE_CODE);
				}
				if ((buildingHeight > 15 && buildingHeight <= 30) || (plotArea > 4047 && plotArea <= 10000)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_PM_MODULE_CODE);
				}
				if ((buildingHeight > 30) || (plotArea > 10000)) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_DP_BP_MODULE_CODE);
				}

			} else {
				if (buildingHeight <= 15) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_PO_MODULE_CODE);
				} else if (buildingHeight > 15 && buildingHeight <= 30) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_PM_MODULE_CODE);
				} else if (buildingHeight > 30) {
					pl.getPlanInformation().setBusinessService(DxfFileConstants.BPA_OC_DP_BP_MODULE_CODE);
				}

			}
		}
	}
	
	private static void updateSubOccupancy(Plan pl) {
		OccupancyTypeHelper occupancyTypeHelper=pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		if(occupancyTypeHelper!=null && occupancyTypeHelper.getSubtype()!=null)
			pl.getPlanInformation().setSubOccupancy(occupancyTypeHelper.getSubtype().getName());
	}
	
	private static boolean isPlanApplicableForAccreditedWorkflow(Plan pl, Double buildingHeight, Double plotArea) {
		boolean flage=false;
		if(DxfFileConstants.YES.equals(pl.getPlanInformation().getApprovedLayoutDeclaration()) && (buildingHeight <= 10) && (plotArea <= 500) && !isBasmentPersent(pl)) {
			flage=true;
		}
		return flage;
	}
	
	private static boolean isBasmentPersent(Plan pl) {
		for(Block block:pl.getBlocks()) {
			if(block.getBuilding().getFloorNumber(-1)!=null)
				return true;
		}
		return false;
	}
}