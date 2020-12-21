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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.stereotype.Service;

@Service
public class Basement extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(Basement.class);
	private static final String RULE_46_6A = "46-6a";
	private static final String RULE_46_6C = "46-6c";
	public static final String BASEMENT_REQUIRED = "Basement required";
	public static final String BASEMENT_DESCRIPTION_ONE = "Height from the floor to the soffit of the roof slab or ceiling";
	public static final String BASEMENT_DESCRIPTION_TWO = "Minimum height of the ceiling of upper basement above ground level";

	@Override
	public Plan validate(Plan pl) {
		OccupancyTypeHelper helper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();
		BigDecimal plotArea = pl.getPlot().getArea();

		for (Block block : pl.getBlocks()) {
			int totalNoOfBasement = noOfBasement(pl, block);
			int maxAllowedBasement = 0;
			BigDecimal maxAreaAllowed = BigDecimal.ZERO;
			if (DxfFileConstants.PLOTTED_DETACHED_OR_INDIVIDUAL_RESIDENTIAL_BUILDING
					.equals(helper.getSubtype().getCode())
					|| DxfFileConstants.SEMI_DETACHED.equals(helper.getSubtype().getCode())
					|| DxfFileConstants.ROW_HOUSING.equals(helper.getSubtype().getCode())) {
				maxAllowedBasement = 1;
				if (plotArea.compareTo(new BigDecimal("500")) <= 0) {
					if (block.getBuilding().getCoverageArea() != null)
						maxAreaAllowed = block.getBuilding().getCoverageArea().multiply(new BigDecimal("0.5"));
				}

				if (totalNoOfBasement > maxAllowedBasement)
					pl.addError("Basement error", "Maximum one basement is allowed");

				if (totalAreaOfBasement(block).compareTo(maxAreaAllowed) > 0)
					pl.addError("Basement ARea", "Maximum of 50% of the covered area is alowed");
			} else if (DxfFileConstants.OC_RESIDENTIAL.equals(helper.getType().getCode())
					|| DxfFileConstants.PUBLIC_AND_SEMI_PUBLIC_USE_ZONES.equals(helper.getType().getCode())
					|| DxfFileConstants.PUBLIC_UTILITY_BLDG.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_INDUSTRIAL_ZONE.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_EDUCATION.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_TRANSPORTATION.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_AGRICULTURE.equals(helper.getType().getCode())
					|| DxfFileConstants.OC_MIXED_USE.equals(helper.getType().getCode())) {

				if (plotArea.compareTo(new BigDecimal("500")) < 0) {
					if (totalNoOfBasement > 0)
						pl.addError("Basement error", "basement is not allowed");
				} else if (plotArea.compareTo(new BigDecimal("500")) >= 0
						&& plotArea.compareTo(new BigDecimal("1000")) <= 0) {
					if (totalNoOfBasement > 1)
						pl.addError("Basement error", "Maximum one basement is not allowed");
				}
			}

			if (DxfFileConstants.OC_COMMERCIAL.equals(helper.getType().getCode())) {
				maxAllowedBasement = 1;
				if (plotArea.compareTo(new BigDecimal("500")) <= 0) {
					if (totalNoOfBasement > 1)
						pl.addError("Basement error", "Maximum one basement is allowed");

					if (block.getBuilding().getCoverageArea() != null)
						maxAreaAllowed = block.getBuilding().getCoverageArea().multiply(new BigDecimal("0.5"));

					if (totalAreaOfBasement(block).compareTo(maxAreaAllowed) > 0)
						pl.addError("Basement ARea", "Maximum of 50% of the covered area is alowed");
				} else if (plotArea.compareTo(new BigDecimal("500")) >= 0
						&& plotArea.compareTo(new BigDecimal("1000")) <= 0) {
					if (totalNoOfBasement > 1)
						pl.addError("Basement error", "Maximum one basement is allowed");
				}

			}

		}

		return pl;
	}

	private BigDecimal totalAreaOfBasement(Block block) {
		BigDecimal area = BigDecimal.ZERO;

		for (Floor floor : block.getBuilding().getFloors()) {
			area = area.add(floor.getArea());
		}

		return area;
	}

	private int noOfBasement(Plan pl, Block block) {
		int count = 0;
		for (Floor floor : block.getBuilding().getFloors()) {
			if (floor.getNumber() < 0)
				count++;
		}
		return count;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);
		HashMap<String, String> errors = new HashMap<>();
		OccupancyTypeHelper mostRestrictiveFarHelper = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;
		for (Block b : pl.getBlocks()) {

			ScrutinyDetail scrutinyDetail = new ScrutinyDetail();
			scrutinyDetail.setKey("Block_" + b.getNumber() + "_" + "Basement");
			scrutinyDetail.addColumnHeading(1, RULE_NO);
			scrutinyDetail.addColumnHeading(2, DESCRIPTION);
			scrutinyDetail.addColumnHeading(3, REQUIRED);
			scrutinyDetail.addColumnHeading(4, PROVIDED);
			scrutinyDetail.addColumnHeading(5, STATUS);

			Map<String, String> details = new HashMap<>();
			BigDecimal minLength = BigDecimal.ZERO;

			if (b.getBuilding() != null && b.getBuilding().getFloors() != null
					&& !b.getBuilding().getFloors().isEmpty()) {
				for (Floor f : b.getBuilding().getFloors()) {

					if (f != null && f.getNumber() == -1) {

						// apply rule

						if (f.getHeightFromTheFloorToCeiling() != null
								&& !f.getHeightFromTheFloorToCeiling().isEmpty()) {

							minLength = f.getHeightFromTheFloorToCeiling().stream().reduce(BigDecimal::min).get();

							if (minLength.compareTo(BigDecimal.valueOf(2.5)) >= 0) {
								details.put(RULE_NO, RULE_46_6A);
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_ONE);
								details.put(REQUIRED, ">= 2.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);

							} else {
								details = new HashMap<>();
								details.put(RULE_NO, RULE_46_6A);
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_ONE);
								details.put(REQUIRED, ">= 2.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Not_Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);
							}
						}
						minLength = BigDecimal.ZERO;
						if (f.getHeightOfTheCeilingOfUpperBasement() != null
								&& !f.getHeightOfTheCeilingOfUpperBasement().isEmpty()) {

							minLength = f.getHeightOfTheCeilingOfUpperBasement().stream().reduce(BigDecimal::min).get();

							BigDecimal minRequired = BigDecimal.ZERO;
							BigDecimal maxRequired = new BigDecimal("1.5");

							if (DxfFileConstants.OC_COMMERCIAL.equals(mostRestrictiveFarHelper.getType().getCode())
									|| DxfFileConstants.MULTI_LEVEL_CAR_PARKING.equals(
											mostRestrictiveFarHelper.getSubtype().getCode())
									|| isGroundFloorStilt(b)) {
								minRequired = new BigDecimal("0.3");
							} else {
								minRequired = new BigDecimal("0.9");
							}

							if (minLength.compareTo(minRequired) >= 0 && minLength.compareTo(maxRequired) < 0) {
								details = new HashMap<>();
								details.put(RULE_NO, RULE_46_6C);
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_TWO);
								details.put(REQUIRED, "Between " + minRequired.toString() + " to 1.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);

							} else {
								details = new HashMap<>();
								details.put(RULE_NO, RULE_46_6C);
								details.put(DESCRIPTION, BASEMENT_DESCRIPTION_TWO);
								details.put(REQUIRED, "Between " + minRequired.toString() + " to 1.5");
								details.put(PROVIDED, minLength.toString());
								details.put(STATUS, Result.Not_Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);
							}
						}

					}

				}
			}
			pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		}

		if (errors.size() > 0)
			pl.addErrors(errors);

		return pl;
	}

	private boolean isGroundFloorStilt(Block block) {
		if (block.getBuilding() != null && block.getBuilding().getFloors() != null) {
			for (Floor floor : block.getBuilding().getFloors()) {
				if (floor.getNumber() == 0 && floor.getIsStiltFloor())
					return true;

			}
		}
		return false;
	}

//	private int allowedNoOfBesment(Plan pl,OccupancyTypeHelper occupancyTypeHelper) {
//		BigDecimal plot=pl.getPlot().getArea();
//	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}