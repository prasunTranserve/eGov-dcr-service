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

import org.egov.common.entity.edcr.AccessoryBlock;
import org.egov.common.entity.edcr.CulDeSacRoad;
import org.egov.common.entity.edcr.Lane;
import org.egov.common.entity.edcr.NonNotifiedRoad;
import org.egov.common.entity.edcr.NotifiedRoad;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.edcr.utility.DcrConstants;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ApprovedConstruction extends FeatureProcess {

	private static final String SUBRULE_88_1_DESC = "Maximum area of accessory block %s";
	private static final String SUBRULE_88_3_DESC = "Maximum height of accessory block %s";

	private static final String SUBRULE_88_1 = "88-1";
	private static final String SUBULE_88_3 = "88-3";
	private static final String SUBRULE_88_4 = "88-4";
	private static final String SUBRULE_88_5 = "88-5";

	private static final String MIN_DIS_NOTIFIED_ROAD_FROM_ACC_BLDG = "Minimum distance from accessory block to notified road";
	private static final String MIN_DIS_NON_NOTIFIED_ROAD_FROM_ACC_BLDG = "Minimum distance from accessory building to non notified road";
	private static final String MIN_DIS_CULDESAC_ROAD_FROM_ACC_BLDG = "Minimum distance from accessory building to culdesac road";
	private static final String MIN_DIS_LANE_ROAD_FROM_ACC_BLDG = "Minimum distance from accessory building to lane road";
	private static final String SUBRULE_88_5_DESC = "Minimum distance from accessory block %s to plot boundary";

	@Override
	public Plan validate(Plan plan) {
		return plan;
	}




	@Override
	public Plan process(Plan plan) {
		return plan;
	}

	private void setReportOutputDetails(Plan plan, String ruleNo, String ruleDesc, String expected, String actual,
			String status, ScrutinyDetail scrutinyDetail) {
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, ruleNo);
		details.put(DESCRIPTION, ruleDesc);
		details.put(REQUIRED, expected);
		details.put(PROVIDED, actual);
		details.put(STATUS, status);
		scrutinyDetail.getDetail().add(details);
		plan.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
