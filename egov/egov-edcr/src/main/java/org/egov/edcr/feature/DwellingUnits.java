package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.constants.OdishaUlbs;
import org.springframework.stereotype.Service;

@Service
public class DwellingUnits extends FeatureProcess {

	@Override
	public Plan process(Plan pl) {

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())) {
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, BLOCK);
			scrutinyDetail.addColumnHeading(3, DESCRIPTION);
			// scrutinyDetail.addColumnHeading(4, REQUIRED);
			scrutinyDetail.addColumnHeading(5, PROVIDED);
			// scrutinyDetail.addColumnHeading(6, STATUS);
			scrutinyDetail.setKey("Common_Dwelling Units Summary");

			// validate area ->

			for (Block b : pl.getBlocks()) {

				for (Floor floor : b.getBuilding().getFloors()) {
					boolean ewsflage = false;
					boolean ligflage = false;
					boolean mig1flage = false;
					boolean mig2flage = false;

					for (FloorUnit ews : floor.getEwsUnit()) {
						if (ews.getArea().compareTo(new BigDecimal("30")) > 0) {
							ewsflage = true;
							break;
						}
					}
					for (FloorUnit lig : floor.getLigUnit()) {
						if (lig.getArea().compareTo(new BigDecimal("60")) > 0) {
							ligflage = true;
							break;
						}
					}

					for (FloorUnit mig1 : floor.getMig1Unit()) {
						if (mig1.getArea().compareTo(new BigDecimal("160")) > 0) {
							mig1flage = true;
							break;
						}
					}

					for (FloorUnit mig2 : floor.getMig2Unit()) {
						if (mig2.getArea().compareTo(new BigDecimal("200")) > 0) {
							mig2flage = true;
							break;
						}
					}

					if (ewsflage)
						pl.addError("ewsunit " + b.getNumber() + floor.getNumber(),
								"Maximum Allowed area of each EWS Dwelling Units in block " + b.getNumber() + " floor "
										+ floor.getNumber() + " is 30 Sqm");

					if (ligflage)
						pl.addError("ligunit " + b.getNumber() + floor.getNumber(),
								"Maximum Allowed area of each LIG Dwelling Units in block " + b.getNumber() + " floor "
										+ floor.getNumber() + " is 60 Sqm");

					if (mig1flage)
						pl.addError("mig1unit " + b.getNumber() + floor.getNumber(),
								"Maximum Allowed area of each MIG1 Dwelling Units in block " + b.getNumber() + " floor "
										+ floor.getNumber() + " is 160 Sqm");

					if (mig2flage)
						pl.addError("mig2unit " + b.getNumber() + floor.getNumber(),
								"Maximum Allowed area of each MIG2 Dwelling Units in block " + b.getNumber() + " floor "
										+ floor.getNumber() + " is 200 Sqm");

				}
			}

			int multiplicand = 0;
			BigDecimal plotArea = pl.getPlot().getArea();
			//SPARIT
			//OdishaUlbs ulb = OdishaUlbs.getUlb(pl.getThirdPartyUserTenantld());
			
			
//			if(ulb.isSparitFlag() && pl.getVirtualBuilding().getBuildingHeight().compareTo(new BigDecimal( "15"))>0) {
//				System.out.println("Sparit  implementation3:");
//				multiplicand=0;
//			}
//			else {
//				
//			if (plotArea.compareTo(new BigDecimal("4000")) < 0) {
//				
//				multiplicand = 300;
//			} else if (plotArea.compareTo(new BigDecimal("4000")) >= 0
//					&& plotArea.compareTo(new BigDecimal("10000")) < 0) {
//				multiplicand = 250;
//			} else {
//				multiplicand = 200;
//			}
//			  }
			
			
			if (plotArea.compareTo(new BigDecimal("4000")) < 0) {
				multiplicand = 300;
			} else if (plotArea.compareTo(new BigDecimal("4000")) >= 0
					&& plotArea.compareTo(new BigDecimal("10000")) < 0) {
				multiplicand = 250;
			} else {
				multiplicand = 200;
			}

			BigDecimal perArchSqm = new BigDecimal("4046");
			int archcount = 0;
			if (plotArea.remainder(perArchSqm).compareTo(BigDecimal.ZERO) == 0) {
				archcount = plotArea.divide(perArchSqm, 2, BigDecimal.ROUND_HALF_UP).intValue();
			} else {
				archcount = plotArea.divide(perArchSqm, 2, BigDecimal.ROUND_HALF_UP).intValue();
				archcount++;
			}
			long maxAllowed = multiplicand * archcount;

			long totalProvided = 0;

			for (Block block : pl.getBlocks()) {
				int ews = 0;
				int lig = 0;
				int mig1 = 0;
				int mig2 = 0;
				int other = 0;
				int room = 0;
				for (Floor floor : block.getBuilding().getFloors()) {
					if (floor.getEwsUnit() != null)
						totalProvided = totalProvided + floor.getEwsUnit().size();
					if (floor.getLigUnit() != null)
						totalProvided = totalProvided + floor.getLigUnit().size();

					ews = ews + floor.getEwsUnit().size();
					lig = lig + floor.getLigUnit().size();
					mig1 = mig1 + floor.getMig1Unit().size();
					mig2 = mig2 + floor.getMig2Unit().size();
					other = other + floor.getOthersUnit().size();
					room = room + floor.getRoomUnit().size();
				}
				int totalUnit = ews + lig + mig1 + mig2 + other + room;

				// retrict minimum one du unit in each resitandisal block
				OccupancyTypeHelper buildingTypeHelper = block.getBuilding().getMostRestrictiveFarHelper();
				if (DxfFileConstants.OC_RESIDENTIAL.equals(buildingTypeHelper.getType().getCode())) {
					if (totalUnit == 0) {
						pl.addError("Mini_1_unit" + block.getNumber(),
								"Minimum one Dwelling unit required for block " + block.getNumber());
					}

				}

				// EWS
				if (ews > 0) {
					Map<String, String> ewsDetails = new HashMap<>();
					ewsDetails.put(RULE_NO, "");
					ewsDetails.put(DESCRIPTION, "EWS");
					ewsDetails.put(BLOCK, block.getName());
					ewsDetails.put(REQUIRED, "NA");
					ewsDetails.put(PROVIDED, ews + "");
					ewsDetails.put(STATUS, Result.Verify.getResultVal());
					scrutinyDetail.getDetail().add(ewsDetails);
				}

				// lig
				if (lig > 0) {
					Map<String, String> ligDetails = new HashMap<>();
					ligDetails.put(RULE_NO, "");
					ligDetails.put(DESCRIPTION, "LIG");
					ligDetails.put(BLOCK, block.getName());
					ligDetails.put(REQUIRED, "NA");
					ligDetails.put(PROVIDED, lig + "");
					ligDetails.put(STATUS, Result.Verify.getResultVal());
					scrutinyDetail.getDetail().add(ligDetails);
				}

				// mig1
				if (mig1 > 0) {
					Map<String, String> mig1Details = new HashMap<>();
					mig1Details.put(RULE_NO, "");
					mig1Details.put(DESCRIPTION, "MIG1");
					mig1Details.put(BLOCK, block.getName());
					mig1Details.put(REQUIRED, "NA");
					mig1Details.put(PROVIDED, mig1 + "");
					mig1Details.put(STATUS, Result.Verify.getResultVal());
					scrutinyDetail.getDetail().add(mig1Details);
				}

				// mig2
				if (mig2 > 0) {
					Map<String, String> mig2Details = new HashMap<>();
					mig2Details.put(RULE_NO, "");
					mig2Details.put(DESCRIPTION, "MIG1");
					mig2Details.put(BLOCK, block.getName());
					mig2Details.put(REQUIRED, "NA");
					mig2Details.put(PROVIDED, mig2 + "");
					mig2Details.put(STATUS, Result.Verify.getResultVal());
					scrutinyDetail.getDetail().add(mig2Details);
				}

				// other
				if (other > 0) {
					Map<String, String> otherDetails = new HashMap<>();
					otherDetails.put(RULE_NO, "");
					otherDetails.put(DESCRIPTION, "Other");
					otherDetails.put(BLOCK, block.getName());
					otherDetails.put(REQUIRED, "NA");
					otherDetails.put(PROVIDED, other + "");
					otherDetails.put(STATUS, Result.Verify.getResultVal());
					scrutinyDetail.getDetail().add(otherDetails);
				}

				// room
				if (room > 0) {
					Map<String, String> roomDetails = new HashMap<>();
					roomDetails.put(RULE_NO, "");
					roomDetails.put(DESCRIPTION, "Room");
					roomDetails.put(BLOCK, block.getName());
					roomDetails.put(REQUIRED, "NA");
					roomDetails.put(PROVIDED, room + "");
					roomDetails.put(STATUS, Result.Verify.getResultVal());
					scrutinyDetail.getDetail().add(roomDetails);
				}

			}

			// show this on report

			if (totalProvided > maxAllowed) {
				pl.addError("maxAllowed du allowed", "Max " + maxAllowed + " Dwelling unit allowed in provided plot.");
			}

			if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
					.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.SEMI_DETACHED.equals(occupancyTypeHelper.getSubtype().getCode())
					|| DxfFileConstants.HOUSING_PROJECT.equals(occupancyTypeHelper.getSubtype().getCode())) {
				if (pl.getPlanInformation().getTotalNoOfDwellingUnits() > 8)
					pl.addError("wrng_oc", "Found " + pl.getPlanInformation().getTotalNoOfDwellingUnits()
							+ " Dwelling Units, More than 8 Dwelling Units not allowed in this sub-occupancy. Please change project  Sub-Occupancy to Apartment");
			}

			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}

		return pl;

	}

	@Override
	public Map<String, Date> getAmendments() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Plan validate(Plan pl) {
		// TODO Auto-generated method stub
		return null;
	}

}
