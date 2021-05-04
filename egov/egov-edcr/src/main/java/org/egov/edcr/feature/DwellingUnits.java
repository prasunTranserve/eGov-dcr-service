package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class DwellingUnits extends FeatureProcess {

	@Override
	public Plan process(Plan pl) {

		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_RESIDENTIAL.equals(occupancyTypeHelper.getType().getCode())) {
			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, FLOOR);
			scrutinyDetail.addColumnHeading(4, REQUIRED);
			scrutinyDetail.addColumnHeading(5, PROVIDED);
			scrutinyDetail.addColumnHeading(6, STATUS);
			scrutinyDetail.setKey("Common_Dwelling Units");

			// validate area ->

			for (Block b : pl.getBlocks()) {
				for (Floor floor : b.getBuilding().getFloors()) {
					boolean ewsflage = false;
					boolean ligflage = false;
					boolean mig1flage =false;
					boolean mig2flage =false;
					
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
					
					if(ewsflage) 
						pl.addError("ewsunit "+b.getNumber()+floor.getNumber(), "Maximum Allowed area of each EWS Dwelling Units in block "+b.getNumber() +" floor "+floor.getNumber()+" is 30 Sqm");
					
					if(ligflage) 
						pl.addError("ligunit "+b.getNumber()+floor.getNumber(), "Maximum Allowed area of each LIG Dwelling Units in block "+b.getNumber() +" floor "+floor.getNumber()+" is 60 Sqm");
					
					if(mig1flage)
						pl.addError("mig1unit "+b.getNumber()+floor.getNumber(), "Maximum Allowed area of each MIG1 Dwelling Units in block "+b.getNumber() +" floor "+floor.getNumber()+" is 160 Sqm");
					
					if(mig2flage)
						pl.addError("mig2unit "+b.getNumber()+floor.getNumber(), "Maximum Allowed area of each MIG2 Dwelling Units in block "+b.getNumber() +" floor "+floor.getNumber()+" is 200 Sqm");
					

			}
		}

		int multiplicand = 0;
		BigDecimal plotArea = pl.getPlot().getArea();
		if (plotArea.compareTo(new BigDecimal("4000")) < 0) {
			multiplicand = 300;
		} else if (plotArea.compareTo(new BigDecimal("4000")) >= 0 && plotArea.compareTo(new BigDecimal("10000")) < 0) {
			multiplicand = 250;
		} else {
			multiplicand = 200;
		}

		BigDecimal perArchSqm = new BigDecimal("4046");
		int archcount = 0;
		if (plotArea.remainder(perArchSqm).compareTo(BigDecimal.ZERO) == 0) {
			archcount = plotArea.divide(perArchSqm,2, BigDecimal.ROUND_HALF_UP).intValue();
		} else {
			archcount = plotArea.divide(perArchSqm,2, BigDecimal.ROUND_HALF_UP).intValue();
			archcount++;
		}
		long maxAllowed = multiplicand * archcount;

		long totalProvided = 0;

		for (Block block : pl.getBlocks()) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getEwsUnit() != null)
					totalProvided = totalProvided + floor.getEwsUnit().size();
				if (floor.getLigUnit() != null)
					totalProvided = totalProvided + floor.getLigUnit().size();
			}
		}

		// show this on report

		if (totalProvided > maxAllowed) {
			pl.addError("maxAllowed du allowed", "Max " + maxAllowed + "  Dwelling unit allowed in provided plot.");
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
