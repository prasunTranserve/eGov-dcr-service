package org.egov.edcr.feature;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyType;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.springframework.stereotype.Service;

@Service
public class Helipad extends FeatureProcess {

	private static final Logger LOG = Logger.getLogger(Helipad.class);

	@Override
	public Plan validate(Plan pl) {
		return pl;
	}

	@Override
	public Plan process(Plan pl) {

		boolean status = false;
		String provided = "";

		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Helipad Provision");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		for (Block b : pl.getBlocks()) {

			if (b.getBuilding().getBuildingHeight().doubleValue() >= 200) {

				BigDecimal helipadDetails = getHelipad(pl);
				if (helipadDetails.doubleValue() > 0) {
					status = true;
					provided = "Present";
				} else {

					provided = "Not Present";
				}
				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, "");
				details.put(DESCRIPTION, "Provision for Helipad");
				details.put(REQUIRED, "Required");
				details.put(PROVIDED, provided);

				details.put(STATUS, status ? Result.Verify.getResultVal() : Result.Not_Accepted.getResultVal());
				scrutinyDetail.getDetail().add(details);

			} else if (b.getBuilding().getBuildingHeight().doubleValue() < 200) {
				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, "");
				details.put(DESCRIPTION, "Provision for Helipad");
				details.put(REQUIRED, " NA");
				details.put(PROVIDED, " -");

				details.put(STATUS, Result.Accepted.getResultVal());
				scrutinyDetail.getDetail().add(details);
			}

			
		}
		pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
		return pl;

	}

	private BigDecimal getHelipad(Plan pl) {
		BigDecimal helipad = BigDecimal.ZERO;
		// helipad=pl.getUtility().getWaterTankCapacity().doubleValue();
		return helipad;
	}

	@Override
	public Map<String, Date> getAmendments() {
		return new LinkedHashMap<>();
	}

}
