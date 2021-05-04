package org.egov.edcr.od;

import java.math.BigDecimal;

import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.constants.DxfFileConstants;

public class FeeCalculationUtill {

	public static void checkShelterFeePrevalidation(Plan pl) {
		OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		
		if(helper==null || helper.getType()==null || helper.getSubtype()==null)
			return;
		
		boolean isShelterFeeRequired = false;
		BigDecimal totalEWSFeeEffectiveArea = BigDecimal.ZERO;

		if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
				.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.SEMI_DETACHED.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.ROW_HOUSING.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.APARTMENT_BUILDING.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.HOUSING_PROJECT.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.WORK_CUM_RESIDENTIAL.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.STUDIO_APARTMENTS.equals(helper.getSubtype().getCode())
				|| DxfFileConstants.MEDIUM_INCOME_HOUSING.equals(helper.getSubtype().getCode())) {
			BigDecimal totalEwsBUA = pl.getTotalEWSAreaInPlot();
			BigDecimal totalBUA = pl.getVirtualBuilding().getTotalFloorArea();
			BigDecimal totalEwsRequiredArea = totalBUA.multiply(new BigDecimal("0.10")).setScale(2,
					BigDecimal.ROUND_HALF_UP);
			long totalNumberOfDu = pl.getPlanInformation().getTotalNoOfDwellingUnits();
			if (totalNumberOfDu > 8) {
				isShelterFeeRequired = true;
				if (totalEwsBUA.compareTo(totalEwsRequiredArea) >= 0) {
					isShelterFeeRequired = false;
					totalEWSFeeEffectiveArea = BigDecimal.ZERO;
				} else {
					if (!DxfFileConstants.YES.equalsIgnoreCase(pl.getPlanInfoProperties().get(
							DxfFileConstants.HAS_PROJECT_PROVIDED_MIN_10_PER_BUA_FOR_EWS_WITHIN_5_KM_FROM_PROJECT_SITE))) {
						isShelterFeeRequired = false;
						totalEWSFeeEffectiveArea = totalEwsRequiredArea.subtract(totalEwsBUA);
					}else {
						isShelterFeeRequired = true;
						totalEWSFeeEffectiveArea = totalEwsRequiredArea.subtract(totalEwsBUA);
					}
				}

			}

		}
	
		pl.getPlanInformation().setShelterFeeRequired(isShelterFeeRequired);
		pl.setTotalEWSFeeEffectiveArea(totalEWSFeeEffectiveArea);

	}
}
