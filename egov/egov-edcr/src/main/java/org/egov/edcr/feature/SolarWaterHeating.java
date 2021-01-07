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

import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.FloorUnit;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
public class SolarWaterHeating extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(SolarWaterHeating.class);

	private static final String RULE_51 = "51";
	private static final String RULE_51_DESCRIPTION = "Solar Water Heating";
	public static final BigDecimal MINIMUM_AREA_200 = BigDecimal.valueOf(200);

	@Override
	public Plan validate(Plan pl) {
		HashMap<String, String> errors = new HashMap<>();
		if (pl != null && pl.getUtility() != null) { // Solar Water Heating system defined or not
			String subOccupancyCode = pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype().getCode();
			if (checkOccupancyTypeForSolarWaterHeating(subOccupancyCode)
					&& pl.getUtility().getSolarWaterHeatingSystems().isEmpty()) {
				errors.put(RULE_51_DESCRIPTION, edcrMessageSource.getMessage(OBJECTNOTDEFINED,
						new String[] { RULE_51_DESCRIPTION }, LocaleContextHolder.getLocale()));
				pl.addErrors(errors);
			}

		}

		return pl;

	}

	public boolean isOccupancyTypeNotApplicable(OccupancyTypeHelper occupancyTypeHelper) {
		boolean isNotApplicable = false;

		if (DxfFileConstants.B_M.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_SFH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_B.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_R.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_IIR.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_AB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_F.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_C.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_CBO.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_CNS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_P.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_D.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_GG.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_G.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_GS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_HR.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_BLH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_P1.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_P2.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_CMS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_RES.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_LS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_SC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_SM.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_S.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_WST1.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_WST2.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_ST.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_SUP.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_WH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_WM.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_MC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_WB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.B_ME.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_A.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_C.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_CL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_MP.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_CH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_O.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_OAH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_SC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_C1H.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_C2H.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_SCC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_CC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_EC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_G.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_ML.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_M.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_PW.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_PL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_REB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_SPC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_S.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_T.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_AB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_GO.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_LSGO.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_RB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_SWC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_CI.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_D.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_YC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_DC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_GSGH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_RT.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_MTH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_MB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_NH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_PLY.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_RC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_VHAB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_RTI.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_PS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_FS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_J.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.C_PO.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_BCC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_BTC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_BCG.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_PDSS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_PTPA.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_PUB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_SS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_TEL.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_WPS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_SSY.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_EDD.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_IB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_NPI.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_ITB.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_SI.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_L.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_FF.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.E_SF.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_A.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_AS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_MS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_BS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_BT.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_I.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_RS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_TS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_MLCP.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_PP.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_TP.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.G_TT.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_AF.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_AG.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_ARF.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_FH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_CH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_NGH.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_PDS.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_H.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.H_SC.equals(occupancyTypeHelper.getSubtype().getCode())
				|| DxfFileConstants.D_BCG.equals(occupancyTypeHelper.getSubtype().getCode())) {
			isNotApplicable = true;
		}

		return isNotApplicable;
	}

	@Override
	public Plan process(Plan pl) {
		validate(pl);

		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		scrutinyDetail.setKey("Common_Solar Water Heating");
		String subRule = "";
		String subRuleDesc = RULE_51_DESCRIPTION;
		BigDecimal expectedTankCapacity = BigDecimal.ZERO;
		BigDecimal actualTankCapacity = BigDecimal.ZERO;
		int totalNumberOfStudents = 0;
		int totalNumberOfBeds = 0;
		boolean isValid = false;
		OccupancyTypeHelper mostRestrictiveFarHelper = pl.getVirtualBuilding() != null
				? pl.getVirtualBuilding().getMostRestrictiveFarHelper()
				: null;

		if (isOccupancyTypeNotApplicable(mostRestrictiveFarHelper)) {
			return pl;
		}

		actualTankCapacity = new BigDecimal(
				(String) pl.getPlanInfoProperties().get(DxfFileConstants.SOLOR_WATER_HEATING_IN_LTR));

		totalNumberOfStudents = new Integer(
				(String) pl.getPlanInfoProperties().get(DxfFileConstants.NUMBER_OF_STUDENTS));

		totalNumberOfBeds = new Integer((String) pl.getPlanInfoProperties().get(DxfFileConstants.NUMBER_OF_BEDS));

		if (mostRestrictiveFarHelper != null
				&& checkOccupancyTypeForSolarWaterHeating(mostRestrictiveFarHelper.getSubtype().getCode())) {
			if (DxfFileConstants.A_P.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_S.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_R.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_AB.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_HP.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_WCR.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_SA.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_DH.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Plotted
																										// Detached,Semi-detached,Row
																										// housing,Apartment
																										// Building,Housing
																										// Project,work-cum-residential,Studio
																										// Apartments,Dharmasala
				expectedTankCapacity = setUpTankCapacity(pl, expectedTankCapacity);

			} else if (DxfFileConstants.A_D.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Dormitory
				expectedTankCapacity = new BigDecimal(totalNumberOfStudents)
						.multiply(new BigDecimal(10).setScale(0, BigDecimal.ROUND_HALF_UP));

			} else if (DxfFileConstants.A_E.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_LIH.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_MIH.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// EWS,Low
				expectedTankCapacity = setUpTankCapacity(pl, expectedTankCapacity);

			} else if (DxfFileConstants.A_H.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Hostel
				expectedTankCapacity = new BigDecimal(totalNumberOfStudents)
						.multiply(new BigDecimal(10).setScale(0, BigDecimal.ROUND_HALF_UP));

			} else if (DxfFileConstants.A_SH.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.A_SQ.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Shelter
				expectedTankCapacity = setUpTankCapacity(pl, expectedTankCapacity);

			} else if (DxfFileConstants.B_H.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Hotel,
				expectedTankCapacity = new BigDecimal(totalNumberOfBeds)
						.multiply(new BigDecimal(10).setScale(0, BigDecimal.ROUND_HALF_UP));

			} else if (DxfFileConstants.B_5S.equals(mostRestrictiveFarHelper.getSubtype().getCode())) { // 5 Star Hotel
				for (Block block : pl.getBlocks()) {
					if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
						int totalNumberOfUnits = 0;
						for (Floor floor : block.getBuilding().getFloors()) {
							List<FloorUnit> floorUnits = floor.getUnits();
							if (!CollectionUtils.isEmpty(floorUnits)) {
								totalNumberOfUnits = totalNumberOfUnits + floorUnits.size();
							}

						}
						expectedTankCapacity = new BigDecimal(totalNumberOfUnits)
								.multiply(new BigDecimal(15).setScale(0, BigDecimal.ROUND_HALF_UP));

					}
				}

			} else if (DxfFileConstants.B_SCR.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Shop Cum
				expectedTankCapacity = setUpTankCapacity(pl, expectedTankCapacity);

			} else if (DxfFileConstants.B_GH.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Guest
																										// Houses
				expectedTankCapacity = new BigDecimal(200);

			} else if (DxfFileConstants.B_FC.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Food Court
				expectedTankCapacity = new BigDecimal(200);

			} else if (DxfFileConstants.C_B.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Banquet
																										// Hall
				expectedTankCapacity = new BigDecimal(200);

			} else if (DxfFileConstants.C_MH.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Marriage
																										// Hall/Kalyan
																										// Mandap
				expectedTankCapacity = new BigDecimal(200);

			} else if (DxfFileConstants.C_P.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Police/Army/Barrack
				expectedTankCapacity = new BigDecimal(200);

			} else if (DxfFileConstants.C_HC.equals(mostRestrictiveFarHelper.getSubtype().getCode())
					|| DxfFileConstants.C_H.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Health
																										// center,
																										// Hospital
				expectedTankCapacity = new BigDecimal(totalNumberOfBeds)
						.multiply(new BigDecimal(10).setScale(0, BigDecimal.ROUND_HALF_UP));

			} else if (DxfFileConstants.C_L.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Lab
				expectedTankCapacity = new BigDecimal(100);

			} else if (DxfFileConstants.F.equals(mostRestrictiveFarHelper.getType().getCode())) {
				if (DxfFileConstants.F_RTC.equals(mostRestrictiveFarHelper.getSubtype().getCode())) {// Research and
																										// Training
																										// Center
					expectedTankCapacity = new BigDecimal(200);

				} else {// Other sub-occupancies for Education
					expectedTankCapacity = new BigDecimal(totalNumberOfStudents)
							.multiply(new BigDecimal(10).setScale(0, BigDecimal.ROUND_HALF_UP));

				}

			}
			if (actualTankCapacity.compareTo(expectedTankCapacity) >= 0) {
				isValid = true;
			}
			processSolarWaterTankCapacity(pl, "Compulsary", subRule, subRuleDesc, expectedTankCapacity, isValid);
		}
		return pl;
	}

	private BigDecimal setUpTankCapacity(Plan pl, BigDecimal expectedTankCapacity) {
		for (Block block : pl.getBlocks()) {
			if (block.getBuilding() != null && !block.getBuilding().getFloors().isEmpty()) {
				BigDecimal expectedMinimumArea = MINIMUM_AREA_200;
				BigDecimal plinthArea = block.getBuilding().getCoverageArea();
				if (plinthArea.compareTo(expectedMinimumArea) >= 0) {
					int totalNumberOfUnits = 0;
					for (Floor floor : block.getBuilding().getFloors()) {
						List<FloorUnit> floorUnits = floor.getUnits();
						if (!CollectionUtils.isEmpty(floorUnits)) {
							totalNumberOfUnits = totalNumberOfUnits + floorUnits.size();
						}

					}
					expectedTankCapacity = expectedTankCapacity
							.add(new BigDecimal(totalNumberOfUnits).multiply(new BigDecimal(100)))
							.setScale(0, BigDecimal.ROUND_HALF_UP);
				}

			}
		}
		return expectedTankCapacity;
	}

	private void processSolarWaterTankCapacity(Plan planDetail, String rule, String subRule, String subRuleDesc,
			BigDecimal expectedTankCapacity, Boolean valid) {
		if (expectedTankCapacity.compareTo(BigDecimal.valueOf(0)) > 0) {
			if (valid) {
				setReportOutputDetails(planDetail, subRule, "RAINWATER_HARVESTING_TANK_CAPACITY",
						expectedTankCapacity.toString(),
						planDetail.getPlanInfoProperties().get(DxfFileConstants.SOLOR_WATER_HEATING_IN_LTR) != null
								? planDetail.getPlanInfoProperties().get(DxfFileConstants.SOLOR_WATER_HEATING_IN_LTR)
								: "0" + " litre",
						Result.Accepted.getResultVal());
			} else {
				setReportOutputDetails(planDetail, subRule, "RAINWATER_HARVESTING_TANK_CAPACITY",
						expectedTankCapacity.toString() + "IN_LITRE",
						planDetail.getPlanInfoProperties().get(DxfFileConstants.SOLOR_WATER_HEATING_IN_LTR) != null
								? planDetail.getPlanInfoProperties().get(DxfFileConstants.SOLOR_WATER_HEATING_IN_LTR)
								: "0" + "  litre",
						Result.Not_Accepted.getResultVal());
			}
		}
	}

	private void setReportOutputDetails(Plan pl, String ruleNo, String ruleDesc, String expected, String actual,
			String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, "");
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	private boolean checkOccupancyTypeForSolarWaterHeating(String subOccupancyType) {

		if (subOccupancyType.equals(DxfFileConstants.A_P.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_S.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_R.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_AB.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_HP.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_WCR.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_SA.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_DH.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_D.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_E.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_LIH.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_MIH.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_H.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_SH.toString())
				|| subOccupancyType.equals(DxfFileConstants.A_SQ.toString())
				|| subOccupancyType.equals(DxfFileConstants.B_H.toString())
				|| subOccupancyType.equals(DxfFileConstants.B_5S.toString())
				|| subOccupancyType.equals(DxfFileConstants.B_SCR.toString())
				|| subOccupancyType.equals(DxfFileConstants.B_GH.toString())
				|| subOccupancyType.equals(DxfFileConstants.B_FC.toString())
				|| subOccupancyType.equals(DxfFileConstants.C_B.toString())
				|| subOccupancyType.equals(DxfFileConstants.C_MH.toString())
				|| subOccupancyType.equals(DxfFileConstants.C_P.toString())
				|| subOccupancyType.equals(DxfFileConstants.C_HC.toString())
				|| subOccupancyType.equals(DxfFileConstants.C_H.toString())
				|| subOccupancyType.equals(DxfFileConstants.C_L.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_CC.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_CI.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_C.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_CTI.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_NS.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_PS.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_H.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_HS.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_PLS.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_CR.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_SMC.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_AA.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_TC.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_STC.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_TI.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_VI.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_MC.toString())
				|| subOccupancyType.equals(DxfFileConstants.F_RTC.toString())) {

			return true;
		}

		return false;
	}

	// CGCL End

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
