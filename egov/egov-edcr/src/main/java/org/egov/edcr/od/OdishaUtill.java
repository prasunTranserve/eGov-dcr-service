package org.egov.edcr.od;

import java.math.BigDecimal;
import java.util.Map;

import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Measurement;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.Room;
import org.egov.edcr.constants.DxfFileConstants;

public class OdishaUtill {

	private static final BigDecimal MINIMUM_NUMBER_OF_OCCUPANTS_OR_USERS_FOR_ASSEMBLY_BUILDING = new BigDecimal("50");

	public static boolean isAssemblyBuildingCriteria(Plan pl) {
		boolean isAssemblyBuilding = false;
		OccupancyTypeHelper occupancyTypeHelper = pl.getVirtualBuilding().getMostRestrictiveFarHelper();

		if (DxfFileConstants.OC_PUBLIC_SEMI_PUBLIC_OR_INSTITUTIONAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_COMMERCIAL.equals(occupancyTypeHelper.getType().getCode())
				|| DxfFileConstants.OC_TRANSPORTATION.equals(occupancyTypeHelper.getType().getCode())) {

			BigDecimal providedNumberOfOccupantsOrUser = pl.getPlanInformation().getNumberOfOccupantsOrUsers();

			if (providedNumberOfOccupantsOrUser
					.compareTo(MINIMUM_NUMBER_OF_OCCUPANTS_OR_USERS_FOR_ASSEMBLY_BUILDING) >= 0) {

				if (DxfFileConstants.AUDITORIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BANQUET_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CINEMA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CLUB.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSIC_PAVILIONS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.COMMUNITY_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCIENCE_CENTRE_OR_MUSEUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONFERNCE_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CONVENTION_HALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SCULPTURE_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.CULTURAL_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.EXHIBITION_CENTER.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.GYMNASIA.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MARRIAGE_HALL_OR_KALYAN_MANDAP
								.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MULTIPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.MUSUEM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PLACE_OF_WORKSHIP.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.PUBLIC_LIBRARIES.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RECREATION_BLDG.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SPORTS_COMPLEX.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.STADIUM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.THEATRE.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RELIGIOUS_BUILDING.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RESTAURANT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOPPING_MALL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SHOWROOM.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.SUPERMARKETS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.FOOD_COURTS.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.AIRPORT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.METRO_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.BUS_TERMINAL.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.ISBT.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.RAILWAY_STATION.equals(occupancyTypeHelper.getSubtype().getCode())
						|| DxfFileConstants.TRUCK_TERMINAL.equals(occupancyTypeHelper.getSubtype().getCode())) {
					isAssemblyBuilding = true;
				}
			}
		}
		pl.getPlanInformation().setAssemblyBuilding(isAssemblyBuilding);
		return isAssemblyBuilding;
	}

	public static BigDecimal getMaxBuildingHeight(Plan pl) {
		BigDecimal buildingHeight = BigDecimal.ZERO;
		buildingHeight = pl.getBlocks().stream().map(block -> block.getBuilding().getBuildingHeight())
				.reduce(BigDecimal::max).get();
		return buildingHeight;
	}

	public static void validateServiceFloor(Plan pl, Block b, Floor f) {
		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
		boolean isServiceFloor = false;
		String color = DxfFileConstants.COLOR_SERVICE_FLOOR;// 39
		
		
		BigDecimal noOfFloorsAboveGround = BigDecimal.ZERO;
		for (Floor floor : b.getBuilding().getFloors()) {
			if (floor.getNumber() != null && floor.getNumber() >= 0) {
				noOfFloorsAboveGround = noOfFloorsAboveGround.add(BigDecimal.valueOf(1));
			}
		}
		
		boolean hasTerrace = b.getBuilding().getFloors().stream()
				.anyMatch(floor -> floor.getTerrace().equals(Boolean.TRUE));

		noOfFloorsAboveGround = hasTerrace ? noOfFloorsAboveGround.subtract(BigDecimal.ONE)
				: noOfFloorsAboveGround;
		
		BigDecimal totalArea=BigDecimal.ZERO;
		BigDecimal height=BigDecimal.ZERO;
		for (Room room : f.getRegularRooms()) {
			for (Measurement measurement : room.getRooms()) {
				if (heightOfRoomFeaturesColor.get(color) == measurement.getColorCode()) {
					isServiceFloor = true;
					totalArea=totalArea.add(measurement.getArea());
					height=measurement.getHeight();
					break;
				}
			}
			if (isServiceFloor) {
				if (noOfFloorsAboveGround.compareTo(new BigDecimal("4")) <= 0)
					pl.addError("SERVICE_FLOOR", "Service Floor not allowed in less then 5 story building");
				if (f.getNumber() <= 0)
					pl.addError("SERVICE_FLOOR1", "Service Floor not allowed on floor 0 or besment");
			}
		}
		f.setIsServiceFloor(isServiceFloor);
		totalArea=roundUp(totalArea);
		f.setTotalServiceArea(totalArea);
		height=roundUp(height);
		f.setServiceFloorHeight(height);
	}

	public static void validateStilledFloor(Plan pl, Block b, Floor f) {
		Map<String, Integer> heightOfRoomFeaturesColor = pl.getSubFeatureColorCodesMaster().get("HeightOfRoom");
		boolean isStiltFloor = false;
		String color = DxfFileConstants.COLOR_STILT_FLOOR;// 38
		BigDecimal totalStilledArea=BigDecimal.ZERO;
		BigDecimal flrHeight=BigDecimal.ZERO;
		for (Room room : f.getRegularRooms()) {
			for (Measurement measurement : room.getRooms()) {
				if (heightOfRoomFeaturesColor.get(color) == measurement.getColorCode()) {
					isStiltFloor = true;
					totalStilledArea=totalStilledArea.add(measurement.getArea());
					flrHeight=measurement.getHeight();
				}
			}
			if (isStiltFloor && f.getNumber() < 0) {
				pl.addError("STILT_FLOOR", "Stilt Floor can not be in besment.");
			}
		}
		
		
		
		// deducted building height need to test and verify
		if(isStiltFloor) {
			if (b.getBuilding().getDeclaredBuildingHeight().compareTo(new BigDecimal("15"))<0 ) {
				if(flrHeight.compareTo(new BigDecimal("2.4")) == 0)
					b.getBuilding().setBuildingHeight(b.getBuilding().getBuildingHeight().subtract(new BigDecimal("2.4")));;
			}else {
				b.getBuilding().setBuildingHeight(b.getBuilding().getBuildingHeight().subtract(f.getHeight()));;
			}
		}

		f.setIsStiltFloor(isStiltFloor);
		totalStilledArea=roundUp(totalStilledArea);
		f.setTotalStiltArea(totalStilledArea);
		flrHeight=roundUp(flrHeight);
		f.setStiltFloorHeight(flrHeight);
	}
	
	public static BigDecimal roundUp(BigDecimal number) {
		number=number.setScale(2, BigDecimal.ROUND_HALF_UP);
		return number;
	}
	
	
	public static void setPlanInfoBlkWise(Plan pl,String key) {
		
		for(Block block:pl.getBlocks()) {
			String value=pl.getPlanInfoProperties().get(key+"_"+block.getNumber());
	    	try {
	    		BigDecimal numValue=new BigDecimal(value);
	    		block.setNumberOfOccupantsOrUsersOrBedBlk(numValue);
				if(numValue.compareTo(BigDecimal.ZERO)<=0)
					pl.addError("NUMBER_OF_OCCUPANTS_OR_USERS_"+block.getNumber(), "Number Of Occupants/Users/Bed is not defined in block "+block.getNumber());
	    	}catch (Exception e) {
				pl.addError("NUMBER_OF_OCCUPANTS_OR_USERS_"+block.getNumber(), "Number Of Occupants/Users/Bed is invalid in block "+block.getNumber());
			}
		}
	}
}





