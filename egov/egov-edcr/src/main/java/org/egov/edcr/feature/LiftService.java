/*
 * eGov  SmartCity eGovernance suite aims to improve the internal efficiency,transparency,
 * accountability and the service delivery of the government  organizations.
 *
 *  Copyright (C) <2019>  eGovernments Foundation
 *
 *  The updated version of eGov suite of products as by eGovernments Foundation
 *  is available at http://www.egovernments.org
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see http://www.gnu.org/licenses/ or
 *  http://www.gnu.org/licenses/gpl.html .
 *
 *  In addition to the terms of the GPL license to be adhered to in using this
 *  program, the following additional terms are to be complied with:
 *
 *      1) All versions of this program, verbatim or modified must carry this
 *         Legal Notice.
 *      Further, all user interfaces, including but not limited to citizen facing interfaces,
 *         Urban Local Bodies interfaces, dashboards, mobile applications, of the program and any
 *         derived works should carry eGovernments Foundation logo on the top right corner.
 *
 *      For the logo, please refer http://egovernments.org/html/logo/egov_logo.png.
 *      For any further queries on attribution, including queries on brand guidelines,
 *         please contact contact@egovernments.org
 *
 *      2) Any misrepresentation of the origin of the material is prohibited. It
 *         is required that all modified versions of this material be marked in
 *         reasonable ways as different from the original version.
 *
 *      3) This license does not grant any rights to any user of the program
 *         with regards to rights under trademark law for use of the trade names
 *         or trademarks of eGovernments Foundation.
 *
 *  In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */

package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.Lift;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class LiftService extends FeatureProcess {

	private static final String SUBRULE_48_DESC = "Minimum number of lifts for block %s";
	private static final String SUBRULE_48 = "48";
	private static final String REMARKS = "Remarks";
	private static final String SUBRULE_48_DESCRIPTION = "Minimum number of lifts";
	private static final String SUBRULE_40A_3 = "40A-3";
	private static final String SUBRULE_118 = "118";
	private static final String SUBRULE_118_DESCRIPTION = "Minimum dimension Of lift %s on floor %s";
	private static final String SUBRULE_118_DESC = "Minimum dimension Of lift";

	@Override
	public Plan validate(Plan plan) {
		for (Block block : plan.getBlocks()) {
			if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
				for (Floor floor : block.getBuilding().getFloors()) {
					List<Lift> lifts = floor.getLifts();
					if (lifts != null && !lifts.isEmpty()) {
						for (Lift lift : lifts) {
							List<Measurement> liftPolyLines = lift.getLifts();
							if (liftPolyLines != null && !liftPolyLines.isEmpty()) {
								validateDimensions(plan, block.getNumber(), floor.getNumber(),
										lift.getNumber().toString(), liftPolyLines);
							}
						}
					}
				}
			}
		}
		return plan;
	}

	@Override
	public Plan process(Plan plan) {
		// validate(plan);
		if (plan != null && !plan.getBlocks().isEmpty()) {
			for (Block block : plan.getBlocks()) {
				scrutinyDetail = new ScrutinyDetail();
				scrutinyDetail.addColumnHeading(1, RULE_NO);
				scrutinyDetail.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail.addColumnHeading(3, REQUIRED);
				scrutinyDetail.addColumnHeading(4, PROVIDED);
				scrutinyDetail.addColumnHeading(5, STATUS);
				scrutinyDetail.addColumnHeading(6, REMARKS);
				scrutinyDetail.setKey("Block_" + block.getNumber() + "_" + "Lift - Minimum Required");

				ScrutinyDetail scrutinyDetail1 = new ScrutinyDetail();
				scrutinyDetail1.addColumnHeading(1, RULE_NO);
				scrutinyDetail1.addColumnHeading(2, DESCRIPTION);
				scrutinyDetail1.addColumnHeading(3, REQUIRED);
				scrutinyDetail1.addColumnHeading(4, PROVIDED);
				scrutinyDetail1.addColumnHeading(5, STATUS);
				scrutinyDetail1.addColumnHeading(6, REMARKS);
				scrutinyDetail1.setKey("Block_" + block.getNumber() + "_" + "Lift - Minimum Dimension");

				OccupancyTypeHelper mostRestrictiveFarHelper = plan.getVirtualBuilding() != null
						? plan.getVirtualBuilding().getMostRestrictiveFarHelper()
						: null;
				BigDecimal expectedLiftCount = BigDecimal.ZERO;

				if (block.getBuilding() != null && !block.getBuilding().getOccupancies().isEmpty()) {
					boolean valid = false;

					if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.A.equals(mostRestrictiveFarHelper.getType().getCode())) {
						if (DxfFileConstants.A_P.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_S.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_R.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
									expectedLiftCount);

						} else if (DxfFileConstants.A_AB.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_HP.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							if (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("10")) > 0) {
								expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
										expectedLiftCount);

							}

						} else if (DxfFileConstants.A_WCR.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
									expectedLiftCount);

						} else if (DxfFileConstants.A_SA.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							if (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("10")) > 0) {
								expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
										expectedLiftCount);

							}

						} else if (DxfFileConstants.A_DH.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_D.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
									expectedLiftCount);

						} else if (DxfFileConstants.A_E.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_LIH.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_MIH.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							if (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("10")) > 0) {
								expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
										expectedLiftCount);

							}

						} else if (DxfFileConstants.A_H.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_SH.equals(mostRestrictiveFarHelper.getSubtype().getCode())
								|| DxfFileConstants.A_SQ.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {
							expectedLiftCount = calculateExpectedLiftCountForResidentialOccupancy(block,
									expectedLiftCount);

						}

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.B.equals(mostRestrictiveFarHelper.getType().getCode())) {
						if (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("10")) > 0) {
							expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

						}

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.C.equals(mostRestrictiveFarHelper.getType().getCode())) {
						if (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("10")) > 0) {
							expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

						}

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.D.equals(mostRestrictiveFarHelper.getType().getCode())) {
						expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.E.equals(mostRestrictiveFarHelper.getType().getCode())) {
						expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.F.equals(mostRestrictiveFarHelper.getType().getCode())) {
						expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.G.equals(mostRestrictiveFarHelper.getType().getCode())) {
						expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

					} else if (null != mostRestrictiveFarHelper
							&& DxfFileConstants.H.equals(mostRestrictiveFarHelper.getType().getCode())) {
						expectedLiftCount = calculateExpectedLiftCountForOtherOccupancy(block, expectedLiftCount);

					}

					if (BigDecimal.valueOf(Double.valueOf(block.getNumberOfLifts()))
							.compareTo(expectedLiftCount) >= 0) {
						valid = true;
					}

					if (valid) {
						setReportOutputDetails(plan, SUBRULE_48, SUBRULE_48_DESCRIPTION, expectedLiftCount.toString(),
								block.getNumberOfLifts(), Result.Accepted.getResultVal(), "", scrutinyDetail);
					} else {
						setReportOutputDetails(plan, SUBRULE_48, SUBRULE_48_DESCRIPTION, expectedLiftCount.toString(),
								block.getNumberOfLifts(), Result.Not_Accepted.getResultVal(), "", scrutinyDetail);
					}

				}

			}
		}

		return plan;
	}

	private BigDecimal calculateExpectedLiftCountForOtherOccupancy(Block block, BigDecimal expectedLiftCount) {
		if (!block.getBuilding().getFloors().isEmpty()) {
			BigDecimal expectedLiftCountPerFloor = BigDecimal.ZERO;
			Set<BigDecimal> liftCountSet = new HashSet<BigDecimal>();
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getNumber() > 2) {
					expectedLiftCountPerFloor = floor.getArea().divide(new BigDecimal("1000")).setScale(0,
							BigDecimal.ROUND_UP);
					liftCountSet.add(expectedLiftCountPerFloor);

				}

			}
			expectedLiftCount = Collections.max(liftCountSet);
			if ((expectedLiftCount.compareTo(new BigDecimal("2")) < 0)
					&& (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("21")) > 0)) {
				expectedLiftCount = new BigDecimal("2");
			}

		}
		return expectedLiftCount;
	}

	private BigDecimal calculateExpectedLiftCountForResidentialOccupancy(Block block, BigDecimal expectedLiftCount) {
		if (!block.getBuilding().getFloors().isEmpty()) {
			int totalNumberOfUnits = 0;
			for (Floor floor : block.getBuilding().getFloors()) {
				List<FloorUnit> floorUnits = floor.getUnits();
				if (floor.getNumber() > 2 && !CollectionUtils.isEmpty(floorUnits)) {
					totalNumberOfUnits = totalNumberOfUnits + floorUnits.size();

				}

			}
			expectedLiftCount = BigDecimal.valueOf(totalNumberOfUnits).divide(new BigDecimal("20")).setScale(0,
					BigDecimal.ROUND_UP);
			if ((expectedLiftCount.compareTo(new BigDecimal("2")) < 0)
					&& (block.getBuilding().getBuildingHeight().compareTo(new BigDecimal("21")) > 0)) {
				expectedLiftCount = new BigDecimal("2");
			}

		}
		return expectedLiftCount;
	}

	private void setReportOutputDetails(Plan plan, String ruleNo, String ruleDesc, String expected, String actual,
			String status, String remarks, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		details.put(REMARKS, remarks);
		scrutinyDetail.getDetail().add(details);
		plan.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private void validateDimensions(Plan plan, String blockNo, int floorNo, String liftNo,
			List<Measurement> liftPolylines) {
		int count = 0;
		for (Measurement m : liftPolylines) {
			if (m.getInvalidReason() != null && m.getInvalidReason().length() > 0) {
				count++;
			}
		}
		if (count > 0) {
			plan.addError(String.format(DxfFileConstants.LAYER_LIFT_WITH_NO, blockNo, floorNo, liftNo),
					count + " number of lift polyline not having only 4 points in layer "
							+ String.format(DxfFileConstants.LAYER_LIFT_WITH_NO, blockNo, floorNo, liftNo));

		}
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
