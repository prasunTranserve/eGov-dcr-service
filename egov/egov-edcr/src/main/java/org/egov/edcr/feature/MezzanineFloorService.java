package org.egov.edcr.feature;

import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.HEIGHTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.IN_METER;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED_DESC;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.SQMTRS;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.springframework.stereotype.Service;

@Service
public class MezzanineFloorService extends FeatureProcess {
	private static final String SUBRULE_46 = "46";
	private static final String RULE46_MAXAREA_DESC = "Maximum allowed area of mezzanine floor";
	private static final String RULE46_DIM_DESC = "Minimum height of mezzanine floor";
	private static final String FLOOR = "Floor";
	public static final String HALL_NUMBER = "Hall Number";
	private static final BigDecimal HEIGHT_2_POINT_2 = BigDecimal.valueOf(2.2);

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		String subRule = SUBRULE_46;
		if (pl != null && !pl.getBlocks().isEmpty()) {
			for (Block block : pl.getBlocks()) {
				scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, FLOOR);
				scrutinyDetail.addColumnHeading(4, REQUIRED);
				scrutinyDetail.addColumnHeading(5, PROVIDED);
				scrutinyDetail.addColumnHeading(6, STATUS);
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Mezzanine Floor");
				if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
					BigDecimal totalBuiltupArea = BigDecimal.ZERO;
					for (Floor floor : block.getBuilding().getFloors()) {
						BigDecimal builtupArea = BigDecimal.ZERO;
						for (Occupancy occ : floor.getOccupancies()) {
							if (!occ.getIsMezzanine() && occ.getBuiltUpArea() != null)
								builtupArea = builtupArea.add(occ.getBuiltUpArea().subtract(occ.getDeduction()));
						}
						totalBuiltupArea = totalBuiltupArea.add(builtupArea);
						for (Occupancy eachOccupancy : floor.getOccupancies()) {
							if (eachOccupancy.getIsMezzanine() && floor.getNumber() != 0) {
								if (eachOccupancy.getBuiltUpArea() != null
										&& eachOccupancy.getBuiltUpArea().doubleValue() > 0
										&& eachOccupancy.getTypeHelper() == null) {
									pl.addError(OBJECTNOTDEFINED_DESC,
											getLocaleMessage("msg.error.mezz.occupancy.not.defined", block.getNumber(),
													String.valueOf(floor.getNumber()),
													eachOccupancy.getMezzanineNumber()));
								}
								BigDecimal mezzanineFloorArea = BigDecimal.ZERO;
								if (eachOccupancy.getBuiltUpArea() != null)
									mezzanineFloorArea = eachOccupancy.getBuiltUpArea()
											.subtract(eachOccupancy.getDeduction());

								boolean valid = false;
								BigDecimal oneThirdOfBuiltup = builtupArea.divide(BigDecimal.valueOf(3),
										DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
								if (mezzanineFloorArea.doubleValue() > 0
										&& mezzanineFloorArea.compareTo(oneThirdOfBuiltup) <= 0) {
									valid = true;
								}
								String floorNo = " floor " + floor.getNumber();

								BigDecimal height = eachOccupancy.getHeight();
								if (height.compareTo(BigDecimal.ZERO) == 0) {
									pl.addError(RULE46_DIM_DESC,
											getLocaleMessage(HEIGHTNOTDEFINED,
													"Mezzanine floor " + eachOccupancy.getMezzanineNumber(),
													block.getName(), String.valueOf(floor.getNumber())));
								} else if (height.compareTo(HEIGHT_2_POINT_2) >= 0) {
									setReportOutputDetails(pl, subRule,
											RULE46_DIM_DESC + " " + eachOccupancy.getMezzanineNumber(), floorNo,
											HEIGHT_2_POINT_2 + IN_METER, height + IN_METER,
											Result.Accepted.getResultVal());
								} else {
									setReportOutputDetails(pl, subRule,
											RULE46_DIM_DESC + " " + eachOccupancy.getMezzanineNumber(), floorNo,
											HEIGHT_2_POINT_2 + IN_METER, height + IN_METER,
											Result.Not_Accepted.getResultVal());
								}
								if (valid) {
									setReportOutputDetails(pl, subRule,
											RULE46_MAXAREA_DESC + " " + eachOccupancy.getMezzanineNumber(), floorNo,
											oneThirdOfBuiltup + SQMTRS, mezzanineFloorArea + SQMTRS,
											Result.Accepted.getResultVal());
								} else {
									setReportOutputDetails(pl, subRule,
											RULE46_MAXAREA_DESC + " " + eachOccupancy.getMezzanineNumber(), floorNo,
											oneThirdOfBuiltup + SQMTRS, mezzanineFloorArea + SQMTRS,
											Result.Not_Accepted.getResultVal());
								}
							}
						}
					}
				}
			}
		}
		return pl;
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String floor, String expected,
			String actual, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(FLOOR, floor);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}