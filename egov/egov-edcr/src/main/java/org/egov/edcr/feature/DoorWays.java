package org.egov.edcr.feature;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Result;
import org.egov.common.entity.edcr.Room;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.springframework.stereotype.Service;

@Service
public class DoorWays extends FeatureProcess {

	@Override
	public Map<String, Date> getAmendments() {
		// TODO Auto-generated method stub
		return new LinkedHashMap<>();
	}

	@Override
	public Plan validate(Plan pl) {
		// TODO Auto-generated method stub
		return pl;
	}

	@Override
	public Plan process(Plan pl) {
		boolean flage=true;
		if(flage)
			return pl;
		scrutinyDetail = new ScrutinyDetail();
		scrutinyDetail.setKey("Common_Doorways");
		scrutinyDetail.addColumnHeading(1, RULE_NO);
		scrutinyDetail.addColumnHeading(2, DESCRIPTION);
		scrutinyDetail.addColumnHeading(3, REQUIRED);
		scrutinyDetail.addColumnHeading(4, PROVIDED);
		scrutinyDetail.addColumnHeading(5, STATUS);

		
		for (Block b : pl.getBlocks()) {

			for (Floor floor : b.getBuilding().getFloors()) {
				Map<String, String> details = new HashMap<>();
				details.put(RULE_NO, "");
				details.put(DESCRIPTION, "Doorways Dimensions");
				
				if (floor.getBathRoom() != null) {
					for (Measurement measurements : floor.getBathRoom().getRooms()) {
						if (measurements.getWidth().doubleValue() >= 0.75) {
							details.put(REQUIRED, "Width >= 0.75");
							details.put(PROVIDED, " Width = " + measurements.getWidth().doubleValue());
							details.put(STATUS, Result.Accepted.getResultVal());
							scrutinyDetail.getDetail().add(details);
							pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
						} else {
							details.put(REQUIRED, "Width >= 0.75");
							details.put(PROVIDED, " Width = " + measurements.getWidth().doubleValue());
							details.put(STATUS, Result.Not_Accepted.getResultVal());
							scrutinyDetail.getDetail().add(details);
							pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
						}
					}

				}
				if (floor.getWaterClosets() != null) {
					for (Measurement measurements : floor.getWaterClosets().getRooms()) {
						if (measurements.getWidth().doubleValue() >= 0.75) {
							details.put(REQUIRED, "Width >= 0.75");
							details.put(PROVIDED, " Width = " + measurements.getWidth().doubleValue());
							details.put(STATUS, Result.Accepted.getResultVal());
							scrutinyDetail.getDetail().add(details);
							pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
						} else {
							details.put(REQUIRED, "Width >= 0.75");
							details.put(PROVIDED, " Width = " + measurements.getWidth().doubleValue());
							details.put(STATUS, Result.Not_Accepted.getResultVal());
							scrutinyDetail.getDetail().add(details);
							pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
						}
					}

				}

				if (floor.getRegularRooms() != null) {
					for (Room rooms : floor.getRegularRooms()) {
						for (Measurement measurements : rooms.getRooms()) {

							if (measurements.getWidth().doubleValue() >= 1
									&& measurements.getHeight().doubleValue() >= 2) {
								details.put(REQUIRED, "Height >= 2, Width >= 1");
								details.put(PROVIDED, "Height = " + measurements.getHeight().doubleValue()
										+ ", Width = " + measurements.getWidth().doubleValue());
								details.put(STATUS, Result.Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);
								pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
							} else {
								details.put(REQUIRED, "Height >= 2, Width >= 1");
								details.put(PROVIDED, "Height = " + measurements.getHeight().doubleValue()
										+ ", Width = " + measurements.getWidth().doubleValue());
								details.put(STATUS, Result.Not_Accepted.getResultVal());
								scrutinyDetail.getDetail().add(details);
								pl.getReportOutput().getScrutinyDetails().add(scrutinyDetail);
							}
						}

					}
				}
			}

		}
		return pl;
	}

}
