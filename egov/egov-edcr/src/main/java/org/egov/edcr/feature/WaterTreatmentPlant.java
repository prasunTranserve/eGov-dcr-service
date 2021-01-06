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

import static org.egov.edcr.utility.DcrConstants.OBJECTDEFINED_DESC;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED;
import static org.egov.edcr.utility.DcrConstants.OBJECTNOTDEFINED_DESC;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.constants.DxfFileConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class WaterTreatmentPlant extends FeatureProcess {
	private static final String SUB_RULE_53_5_DESCRIPTION = "Liquid waste management treatment plant ";
	private static final String SUB_RULE_53_5 = "53-5";
	private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10000);

	@Override
	public Plan validate(Plan pl) {
		HashMap<String, String> errors = new HashMap<>();
		if (pl != null && pl.getUtility() != null) { // liquid waste treatment plant defined or not
			if (pl.getUtility().getLiquidWasteTreatementPlant().isEmpty()) {
				errors.put(SUB_RULE_53_5_DESCRIPTION, edcrMessageSource.getMessage(OBJECTNOTDEFINED,
						new String[] { SUB_RULE_53_5_DESCRIPTION }, LocaleContextHolder.getLocale()));
				pl.addErrors(errors);

			}

		}

		return pl;
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
		scrutinyDetail.setKey("Common_Water Treatment Plant");
		processLiquidWasteTreatment(pl);
		return pl;
	}

	private void processLiquidWasteTreatment(Plan pl) {
		BigDecimal wasteWaterQuantity = BigDecimal.ZERO;
		wasteWaterQuantity = new BigDecimal(
				(String) pl.getPlanInfoProperties().get(DxfFileConstants.WASTE_WATER_QUANTITY));
		if (wasteWaterQuantity.compareTo(TEN_THOUSAND) >= 0) {
			if (!pl.getUtility().getLiquidWasteTreatementPlant().isEmpty()) {
				setReportOutputDetailsWithoutOccupancy(pl, SUB_RULE_53_5, SUB_RULE_53_5_DESCRIPTION, "",
						OBJECTDEFINED_DESC, Result.Accepted.getResultVal());
				return;
			} else {
				setReportOutputDetailsWithoutOccupancy(pl, SUB_RULE_53_5, SUB_RULE_53_5_DESCRIPTION, "",
						OBJECTNOTDEFINED_DESC, Result.Not_Accepted.getResultVal());
				return;
			}

		}

	}

	private void setReportOutputDetailsWithoutOccupancy(Plan pl, String ruleNo, String ruleDesc, String expected,
			String actual, String status) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
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
