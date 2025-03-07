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

import static org.egov.edcr.constants.DxfFileConstants.A_SA;
import static org.egov.edcr.constants.DxfFileConstants.B;
import static org.egov.edcr.constants.DxfFileConstants.D;
import static org.egov.edcr.constants.DxfFileConstants.G;
import static org.egov.edcr.utility.DcrConstants.DECIMALDIGITS_MEASUREMENTS;
import static org.egov.edcr.utility.DcrConstants.ROUNDMODE_MEASUREMENTS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.utility.DcrConstants;
import org.springframework.stereotype.Service;

@Service
public class PlantationGreenStrip extends FeatureProcess {

	private static final String RULE_32 = "32";
	public static final String PLANTATION_TREECOVER_DESCRIPTION = "Plantation Area";

	@Override
	public Plan validate(Plan pl) {
		return null;
	}

	@Override
	public Plan process(Plan pl) {

		validate(pl);
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Plantation");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);
		Map<String, String> details = new HashMap<>();
		details.put(RULE_NO, RULE_32);
		details.put(DESCRIPTION, PLANTATION_TREECOVER_DESCRIPTION);

		BigDecimal totalArea = BigDecimal.ZERO;
		BigDecimal plotArea = BigDecimal.ZERO;
		BigDecimal plantationPer = BigDecimal.ZERO;
		String type = "";
		String subType = "";

		for(Block block:pl.getBlocks()) {
			for (Measurement m :block.getPlantationGreenStripes()) {
				totalArea = totalArea.add(m.getArea());
			}
		}

		if (pl.getPlot() != null)
			plotArea = pl.getPlot().getArea();

		if (pl.getVirtualBuilding() != null && pl.getVirtualBuilding().getMostRestrictiveFarHelper() != null
				&& pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype() != null) {
			type = pl.getVirtualBuilding().getMostRestrictiveFarHelper().getType().getCode();
			subType = pl.getVirtualBuilding().getMostRestrictiveFarHelper().getSubtype().getCode();
		}

		if (totalArea.intValue() > 0 && plotArea != null && plotArea.intValue() > 0)
			plantationPer = totalArea.divide(plotArea, DECIMALDIGITS_MEASUREMENTS, ROUNDMODE_MEASUREMENTS);
		
		BigDecimal required=BigDecimal.ZERO;
		if(DxfFileConstants.OPEN_SPACE_USE_ZONE.equals(pl.getPlanInformation().getLandUseZone())) {
			required=new BigDecimal("0.50");
		}else {
			if(DxfFileConstants.FARM_HOUSE.equals(subType)
				|| DxfFileConstants.COUNTRY_HOMES.equals(subType)) {
				required=new BigDecimal("0.65");
			}
		}
		boolean isAcepted=false;
		if(plantationPer.compareTo(required) >= 0)
			isAcepted=true;
		
		if(required.compareTo(BigDecimal.ZERO)>0)
			details.put(REQUIRED, required.multiply(new BigDecimal(100)).toString() + "%");
		else
			details.put(REQUIRED, DxfFileConstants.NA);
		details.put(PROVIDED, plantationPer.multiply(new BigDecimal(100)).toString() + "%");
		details.put(STATUS, isAcepted?Result.Accepted.getResultVal():Result.Not_Accepted.getResultVal());
		scrutinyDetail.getDetail().add(details);
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
	
		return pl;

	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}
}
