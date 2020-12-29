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
			
			//validate area ->
			
			int multiplicand=0;
			BigDecimal plotArea=BigDecimal.ZERO;
			if(plotArea.compareTo(new BigDecimal("4000"))<0) {
				multiplicand=300;
			}else if(plotArea.compareTo(new BigDecimal("4000"))>=0 && plotArea.compareTo(new BigDecimal("10000"))<0) {
				multiplicand=250;
			}else {
				multiplicand=200;
			}
			
			BigDecimal perArchSqm=new BigDecimal("4046");
			int archcount=0;
			if(plotArea.remainder(perArchSqm).compareTo(BigDecimal.ZERO)==0) {
				archcount=plotArea.divide(perArchSqm).intValue();
			}else {
				archcount=plotArea.divide(perArchSqm).intValue();
				archcount++;
			}
			long maxAllowed=multiplicand*archcount;
			
			long totalProvided=0;
			
			for(Block block:pl.getBlocks()) {
				for(Floor floor:block.getBuilding().getFloors()) {
					if(floor.getEwsUnit()!=null)
						totalProvided=totalProvided+floor.getEwsUnit().size();
					if(floor.getLigUnit()!=null)
						totalProvided=totalProvided+floor.getLigUnit().size();
				}
			}
			
			//show this on report
			

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
