package org.egov.edcr.service;

//import static org.egov.edcr.constants.DxfFileConstants.NA;
//import static org.egov.edcr.constants.DxfFileConstants.YES;
//import static org.egov.edcr.constants.DxfFileConstants.NO;
import static org.egov.edcr.constants.DxfFileConstants.*;
import static org.egov.edcr.constants.DxfFileConstants.IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY;
import static org.egov.edcr.constants.DxfFileConstants.NO;
import static org.egov.edcr.constants.DxfFileConstants.YES;
import static org.egov.infra.utils.PdfUtils.appendFiles;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.egov.common.entity.edcr.EdcrPdfDetail;
import org.egov.common.entity.edcr.OccupancyTypeHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.PlanFeature;
import org.egov.common.entity.edcr.PlanInformation;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.contract.ComparisonRequest;
import org.egov.edcr.contract.EdcrRequest;
import org.egov.edcr.entity.Amendment;
import org.egov.edcr.entity.AmendmentDetails;
import org.egov.edcr.entity.ApplicationType;
import org.egov.edcr.entity.EdcrApplication;
import org.egov.edcr.entity.EdcrApplicationDetail;
import org.egov.edcr.entity.OcComparisonDetail;
import org.egov.edcr.feature.Coverage;
import org.egov.edcr.feature.FeatureProcess;
import org.egov.edcr.od.DataPreparation;
import org.egov.edcr.od.FeeCalculationUtill;
import org.egov.edcr.od.NocAndDocumentsUtill;
import org.egov.edcr.od.OdishaUtill;
import org.egov.edcr.utility.DcrConstants;
import org.egov.infra.custom.CustomImplProvider;
import org.egov.infra.filestore.entity.FileStoreMapper;
import org.egov.infra.filestore.service.FileStoreService;
import org.egov.infra.validation.exception.ValidationError;
import org.egov.infra.validation.exception.ValidationException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Service
public class PlanService {
	private static final Logger LOG = Logger.getLogger(PlanService.class);
	@Autowired
	private PlanFeatureService featureService;
	@Autowired
	private FileStoreService fileStoreService;
	@Autowired
	private CustomImplProvider specificRuleService;
	@Autowired
	private EdcrApplicationDetailService edcrApplicationDetailService;
	@Autowired
	private EdcrPdfDetailService edcrPdfDetailService;
	@Autowired
	private ExtractService extractService;
	@Autowired
	private EdcrApplicationService edcrApplicationService;
	@Autowired
	private OcComparisonService ocComparisonService;
	@Autowired
	private OcComparisonDetailService ocComparisonDetailService;
	@Autowired
	private ShortenedReportService shortenedReportService;

	public Plan process(EdcrApplication dcrApplication, String applicationType) {
		Map<String, String> cityDetails = specificRuleService.getCityDetails();

		Date asOnDate = null;
		if (dcrApplication.getPermitApplicationDate() != null) {
			asOnDate = dcrApplication.getPermitApplicationDate();
		} else if (dcrApplication.getApplicationDate() != null) {
			asOnDate = dcrApplication.getApplicationDate();
		} else {
			asOnDate = new Date();
		}

		AmendmentService repo = (AmendmentService) specificRuleService.find("amendmentService");
		Amendment amd = repo.getAmendments();
		long start = System.currentTimeMillis();
		Plan plan = null;
		long end = 0, execution = 0;
		boolean isAborted = false;
		try {
			plan = extractService.extract(dcrApplication.getSavedDxfFile(), amd, asOnDate,
					featureService.getFeatures());
			isAborted = checkAbortCaseTrue(plan);
			end = System.currentTimeMillis();
			execution = end - start;
			LOG.info("Total time taken to extract plan : " + execution);
			if (!isAborted) {
				plan.setApplicationType(
						org.egov.common.entity.ApplicationType.valueOf(dcrApplication.getApplicationType().name()));
				plan.setMdmsMasterData(dcrApplication.getMdmsMasterData());
				plan.setThirdPartyUserTenantld(dcrApplication.getThirdPartyUserTenant());
				// plan.getErrors().clear();
				updateOdPlanInfo(plan);
				plan.getPlanInformation().setServiceType(dcrApplication.getServiceType());
				start = System.currentTimeMillis();
				plan = applyRules(plan, amd, cityDetails);
				end = System.currentTimeMillis();
				execution = end - start;
				LOG.info("Total time taken to complete scrutiny : " + execution);
				// update Noc and documentList
				NocAndDocumentsUtill.updateNoc(plan);
				NocAndDocumentsUtill.updateDocuments(plan);
				FeeCalculationUtill.checkShelterFeePrevalidation(plan);
				DataPreparation.updatePlanDetails(plan);
				OdishaUtill.computeOccupancyPercentage(plan);
			}

		} catch (Exception e) {
			LOG.error(e.getMessage());
			e.printStackTrace();
			isAborted = true;
		}
		if (isAborted)
			plan = getAbortedSupportPlan(plan);
		
		if(dcrApplication.getIsRevisionApplication()!=null)
			plan.getPlanInformation().setIsRevisionApplication(dcrApplication.getIsRevisionApplication());
		
		String comparisonDcrNumber = dcrApplication.getEdcrApplicationDetails().get(0).getComparisonDcrNumber();
		if (ApplicationType.PERMIT.getApplicationTypeVal()
				.equalsIgnoreCase(dcrApplication.getApplicationType().getApplicationType())
				|| (ApplicationType.OCCUPANCY_CERTIFICATE.getApplicationTypeVal()
						.equalsIgnoreCase(dcrApplication.getApplicationType().getApplicationType())
						&& StringUtils.isBlank(comparisonDcrNumber))) {
			InputStream reportStream = generateReport(plan, amd, dcrApplication);
			start = System.currentTimeMillis();
			saveOutputReport(dcrApplication, reportStream, plan);
			end = System.currentTimeMillis();
			execution = end - start;
			LOG.info("Total time taken to persist scrutiny report in temp storage : " + execution);
		} else if (ApplicationType.OCCUPANCY_CERTIFICATE.getApplicationTypeVal()
				.equalsIgnoreCase(dcrApplication.getApplicationType().getApplicationType())
				&& StringUtils.isNotBlank(comparisonDcrNumber)) {
			ComparisonRequest comparisonRequest = new ComparisonRequest();
			EdcrApplicationDetail edcrApplicationDetail = dcrApplication.getEdcrApplicationDetails().get(0);
			comparisonRequest.setEdcrNumber(edcrApplicationDetail.getComparisonDcrNumber());
			comparisonRequest.setTenantId(edcrApplicationDetail.getApplication().getThirdPartyUserTenant());
			edcrApplicationDetail.setPlan(plan);

			OcComparisonDetail processCombinedStatus = ocComparisonService.processCombinedStatus(comparisonRequest,
					edcrApplicationDetail);

			dcrApplication.setDeviationStatus(processCombinedStatus.getStatus());
			start = System.currentTimeMillis();
			InputStream reportStream = generateReport(plan, amd, dcrApplication);
			end = System.currentTimeMillis();
			execution = end - start;
			LOG.info("Total time taken to genrate pdf report :" + execution);
			start = System.currentTimeMillis();
			saveOutputReport(dcrApplication, reportStream, plan);
			end = System.currentTimeMillis();
			execution = end - start;
			LOG.info("Total time taken to persist scrutiny report in temp storage : " + execution);
			final List<InputStream> pdfs = new ArrayList<>();
			Path path = fileStoreService.fetchAsPath(
					dcrApplication.getEdcrApplicationDetails().get(0).getReportOutputId().getFileStoreId(),
					"Digit DCR");
			byte[] convertedDigitDcr = null;
			try {
				convertedDigitDcr = Files.readAllBytes(path);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ByteArrayInputStream dcrReport = new ByteArrayInputStream(convertedDigitDcr);
			pdfs.add(dcrReport);

			if (plan.getMainDcrPassed()) {
				OcComparisonDetail ocComparisonE = ocComparisonService.processCombined(processCombinedStatus,
						edcrApplicationDetail);

				final String fileName = ocComparisonE.getOcdcrNumber() + "-" + ocComparisonE.getDcrNumber()
						+ "-comparison" + ".pdf";
				final FileStoreMapper fileStoreMapper = fileStoreService.store(ocComparisonE.getOutput(), fileName,
						"application/pdf", DcrConstants.FILESTORE_MODULECODE);
				ocComparisonE.setOcComparisonReport(fileStoreMapper);
				if (StringUtils.isNotBlank(dcrApplication.getEdcrApplicationDetails().get(0).getDcrNumber())) {
					ocComparisonE.setOcdcrNumber(dcrApplication.getEdcrApplicationDetails().get(0).getDcrNumber());
				}
				ocComparisonDetailService.saveAndFlush(ocComparisonE);

				Path ocPath = fileStoreService.fetchAsPath(ocComparisonE.getOcComparisonReport().getFileStoreId(),
						"Digit DCR");
				byte[] convertedComparison = null;
				try {
					convertedComparison = Files.readAllBytes(ocPath);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ByteArrayInputStream comparisonReport = new ByteArrayInputStream(convertedComparison);
				pdfs.add(comparisonReport);
			}

			final byte[] data = appendFiles(pdfs);
			InputStream targetStream = new ByteArrayInputStream(data);
			saveOutputReport(dcrApplication, targetStream, plan);
			updateFinalReport(dcrApplication.getEdcrApplicationDetails().get(0).getReportOutputId());
		}
		return plan;
	}

	private boolean checkAbortCaseTrue(Plan pl) {
		boolean flage = false;
		if (pl == null || pl.getBlocks() == null || pl.getBlocks().size() == 0
				|| pl.getErrors().get("PLOT_BOUNDARY") != null)
			flage = true;
		if (pl.getPlanInfoProperties() == null || pl.getPlanInfoProperties().size() < 2)
			flage = true;
		return flage;
	}

	private Plan getAbortedSupportPlan(Plan plan) {
		Plan pl = new Plan();
		if (plan != null)
			pl.getErrors().putAll(plan.getErrors());
		PlanInformation planInformation = new PlanInformation();
		pl.setPlanInformation(planInformation);
		pl.addError("1", "Your drawing may be rejected due to one of the following reasons:");
		pl.addError("2", "Plan Info Not present.");
		pl.addError("3", "Building Footprint Layer Not Present.");
		pl.addError("4", "Built Up Area Layer Not Present.");
		pl.addError("5", "If Built Up area Layer Color code is not defined as per drawing manual.");

		return pl;
	}

	public void updateOdPlanInfo(Plan pl) {

//    	//NUMBER_OF_OCCUPANTS_OR_USERS_OR_BED_BLK_%s 
//    	OdishaUtill.setPlanInfoBlkWise(pl, DxfFileConstants.NUMBER_OF_OCCUPANTS_OR_USERS_OR_BED_BLK);

		// IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY
		String buildingUnderHazardousOccupancyCategory = pl.getPlanInfoProperties()
				.get(IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY);

		if (YES.equalsIgnoreCase(buildingUnderHazardousOccupancyCategory)
				|| NO.equalsIgnoreCase(buildingUnderHazardousOccupancyCategory)) {
			pl.getPlanInformation().setBuildingUnderHazardousOccupancyCategory(buildingUnderHazardousOccupancyCategory);
		} else {
			pl.addError("IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY",
					"IS_BUILDING_UNDER_HAZARDOUS_OCCUPANCY_CATEGORY is not defined in plan info.");
		}

		// PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT

		// NUMBER_OF_OCCUPANTS_OR_USERS
		try {
			pl.getPlanInformation().setBenchmarkValuePerAcre(pl.getPlanInfoProperties().get(
					DxfFileConstants.PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT) != null
							? new BigDecimal(pl.getPlanInfoProperties().get(
									DxfFileConstants.PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT))
							: BigDecimal.ZERO);
		} catch (Exception e) {
			if (!NA.equals(pl.getPlanInfoProperties().get(
					DxfFileConstants.PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT)))
				pl.addError("PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT",
						"PER_ACRE_BENCHMARK_VALUE_OF_LAND_NEEDED_IF_PROJECT_IS_HAVING_PURCHASABLE_FAR_COMPONENT is invalid in planinfo layer.");
		}

//    	//TOTAL_NUMBER_OF_DWELLING_UNITS 
//    	try {
//			pl.getPlanInformation().setTotalNoOfDwellingUnits(pl.getPlanInfoProperties().get(DxfFileConstants.TOTAL_NUMBER_OF_DWELLING_UNITS)!=null?new BigDecimal(pl.getPlanInfoProperties().get(DxfFileConstants.TOTAL_NUMBER_OF_DWELLING_UNITS)):BigDecimal.ZERO);
//    	}catch (Exception e) {
//			//For NA
//    		pl.getPlanInformation().setTotalNoOfDwellingUnits(BigDecimal.ZERO);
//		}

		// APPROVED_LAYOUT_DECLARATION
		String approvedLayoutDeclaration = pl.getPlanInfoProperties().get(APPROVED_LAYOUT_DECLARATION);

		if (YES.equalsIgnoreCase(approvedLayoutDeclaration) || NO.equalsIgnoreCase(approvedLayoutDeclaration)) {
			pl.getPlanInformation().setApprovedLayoutDeclaration(approvedLayoutDeclaration);
		} else {
			pl.addError("APPROVED_LAYOUT_DECLARATION", "APPROVED_LAYOUT_DECLARATION is not defined in plan info.");
		}

		// DOES_THE_PROJECT_REQUIRE_NOC_FROM_AAI_AS_PER_THE_COLOUR_CODED_ZONE_MAPS
		String nocFromAAI = pl.getPlanInfoProperties()
				.get(DOES_THE_PROJECT_REQUIRE_NOC_FROM_AAI_AS_PER_THE_COLOUR_CODED_ZONE_MAPS);

		if (YES.equalsIgnoreCase(nocFromAAI) || NO.equalsIgnoreCase(nocFromAAI)) {
			pl.getPlanInformation().setNocFromAAI(nocFromAAI);
		} else {
			pl.addError("DOES_THE_PROJECT_REQUIRE_NOC_FROM_AAI_AS_PER_THE_COLOUR_CODED_ZONE_MAPS",
					"DOES_THE_PROJECT_REQUIRE_NOC_FROM_AAI_AS_PER_THE_COLOUR_CODED_ZONE_MAPS is not defined in plan info.");
		}

		// IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_CENTRALLY_PROTECTED_MONUMENT
		String isProjectNearOfCentrallyProtectedMonument = pl.getPlanInfoProperties()
				.get(IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_CENTRALLY_PROTECTED_MONUMENT);

		if (YES.equalsIgnoreCase(isProjectNearOfCentrallyProtectedMonument)
				|| NO.equalsIgnoreCase(isProjectNearOfCentrallyProtectedMonument)) {
			pl.getPlanInformation()
					.setIsProjectNearOfCentrallyProtectedMonument(isProjectNearOfCentrallyProtectedMonument);
		} else {
			pl.addError("IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_CENTRALLY_PROTECTED_MONUMENT",
					"IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_CENTRALLY_PROTECTED_MONUMENT is not defined in plan info.");
		}

		// IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_STATE_PROTECTED_MONUMENT
		String isProjectNearOfStateProtectedMonument = pl.getPlanInfoProperties()
				.get(IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_STATE_PROTECTED_MONUMENT);

		if (YES.equalsIgnoreCase(isProjectNearOfStateProtectedMonument)
				|| NO.equalsIgnoreCase(isProjectNearOfStateProtectedMonument)) {
			pl.getPlanInformation().setIsProjectNearOfStateProtectedMonument(isProjectNearOfStateProtectedMonument);
		} else {
			pl.addError("IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_STATE_PROTECTED_MONUMENT",
					"IS_THE_PROJECT_LOCATED_WITHIN_300_METERS_DISTANCE_OF_THE_STATE_PROTECTED_MONUMENT is not defined in plan info.");
		}

		// IS_THE_PROJECT_LOCATED_WITHIN_200_METERS_FROM_STRATEGIC_BUILDINGS
		String ProjectNearOfStrategicBuildings = pl.getPlanInfoProperties()
				.get(IS_THE_PROJECT_LOCATED_WITHIN_200_METERS_FROM_STRATEGIC_BUILDINGS);

		if (YES.equalsIgnoreCase(ProjectNearOfStrategicBuildings)
				|| NO.equalsIgnoreCase(ProjectNearOfStrategicBuildings)) {
			pl.getPlanInformation().setProjectNearOfStrategicBuildings(ProjectNearOfStrategicBuildings);
		} else {
			pl.addError("IS_THE_PROJECT_LOCATED_WITHIN_200_METERS_FROM_STRATEGIC_BUILDINGS",
					"IS_THE_PROJECT_LOCATED_WITHIN_200_METERS_FROM_STRATEGIC_BUILDINGS is not defined in plan info.");
		}

		// IS_PROPOSED_CONSTRUCTION_NEXT_TO_FLOOD_EMBANKMENT_AND_DOES_APPLICANT_WANT_TO_HAVE_DIRECT_ACCESS_FROM_THE_EMBANKMENT_ROAD
		String isProposedConstructionNextToFloodEmbankment = pl.getPlanInfoProperties().get(
				IS_PROPOSED_CONSTRUCTION_NEXT_TO_FLOOD_EMBANKMENT_AND_DOES_APPLICANT_WANT_TO_HAVE_DIRECT_ACCESS_FROM_THE_EMBANKMENT_ROAD);

		if (YES.equalsIgnoreCase(isProposedConstructionNextToFloodEmbankment)
				|| NO.equalsIgnoreCase(isProposedConstructionNextToFloodEmbankment)) {
			pl.getPlanInformation()
					.setIsProposedConstructionNextToFloodEmbankment(isProposedConstructionNextToFloodEmbankment);
		} else {
			pl.addError(
					"IS_PROPOSED_CONSTRUCTION_NEXT_TO_FLOOD_EMBANKMENT_AND_DOES_APPLICANT_WANT_TO_HAVE_DIRECT_ACCESS_FROM_THE_EMBANKMENT_ROAD",
					"IS_PROPOSED_CONSTRUCTION_NEXT_TO_FLOOD_EMBANKMENT_AND_DOES_APPLICANT_WANT_TO_HAVE_DIRECT_ACCESS_FROM_THE_EMBANKMENT_ROAD is not defined in plan info.");
		}

		// IS_KISAM_OF_LAND_RECORDED_AS_AGRICULTURE_IN_RECORD_OF_RIGHTS
		String isKisamOfLandRecordedAsAgricultureInRecordOfRights = pl.getPlanInfoProperties()
				.get(IS_KISAM_OF_LAND_RECORDED_AS_AGRICULTURE_IN_RECORD_OF_RIGHTS);

		if (YES.equalsIgnoreCase(isKisamOfLandRecordedAsAgricultureInRecordOfRights)
				|| NO.equalsIgnoreCase(isKisamOfLandRecordedAsAgricultureInRecordOfRights)) {
			pl.getPlanInformation().setIsKisamOfLandRecordedAsAgricultureInRecordOfRights(
					isKisamOfLandRecordedAsAgricultureInRecordOfRights);
		} else {
			pl.addError("IS_KISAM_OF_LAND_RECORDED_AS_AGRICULTURE_IN_RECORD_OF_RIGHTS",
					"IS_KISAM_OF_LAND_RECORDED_AS_AGRICULTURE_IN_RECORD_OF_RIGHTS is not defined in plan info.");
		}

		// IS_THE_PROJECT_ADJACENT_TO_HIGHWAY_AND_HAVING_DIRECT_ACCESS_TO_IT
		String isTheProjectAdjacentToHighwayAndHavingDirectAccessToIt = pl.getPlanInfoProperties()
				.get(IS_THE_PROJECT_ADJACENT_TO_HIGHWAY_AND_HAVING_DIRECT_ACCESS_TO_IT);

		if (YES.equalsIgnoreCase(isTheProjectAdjacentToHighwayAndHavingDirectAccessToIt)
				|| NO.equalsIgnoreCase(isTheProjectAdjacentToHighwayAndHavingDirectAccessToIt)) {
			pl.getPlanInformation().setIsTheProjectAdjacentToHighwayAndHavingDirectAccessToIt(
					isTheProjectAdjacentToHighwayAndHavingDirectAccessToIt);
		} else {
			pl.addError("IS_THE_PROJECT_ADJACENT_TO_HIGHWAY_AND_HAVING_DIRECT_ACCESS_TO_IT",
					"IS_THE_PROJECT_ADJACENT_TO_HIGHWAY_AND_HAVING_DIRECT_ACCESS_TO_IT is not defined in plan info.");
		}

		// IS_THE_PROJECT_CLOSE_TO_THE_COASTAL_REGION
		String isTheProjectCloseToTheCoastalRegion = pl.getPlanInfoProperties()
				.get(IS_THE_PROJECT_CLOSE_TO_THE_COASTAL_REGION);

		if (YES.equalsIgnoreCase(isTheProjectCloseToTheCoastalRegion)
				|| NO.equalsIgnoreCase(isTheProjectCloseToTheCoastalRegion)) {
			pl.getPlanInformation().setIsTheProjectCloseToTheCoastalRegion(isTheProjectCloseToTheCoastalRegion);
		} else {
			pl.addError("IS_THE_PROJECT_CLOSE_TO_THE_COASTAL_REGION",
					"IS_THE_PROJECT_CLOSE_TO_THE_COASTAL_REGION is not defined in plan info.");
		}

		// STAR_RATING_FOR_HOTEL_PROJECT
		String startRatingForHotel = pl.getPlanInfoProperties().get(DxfFileConstants.STAR_RATING_FOR_HOTEL_PROJECT);
		try {
			if (startRatingForHotel != null && !DxfFileConstants.NA.equals(startRatingForHotel)) {
				pl.getPlanInformation().setStartRatingForHotel(Integer.parseInt(startRatingForHotel));
			} else {
				pl.getPlanInformation().setStartRatingForHotel(0);
			}
		} catch (Exception e) {
			pl.getPlanInformation().setStartRatingForHotel(0);
		}

		// DOES_HOSPITAL_HAVE_CRITICAL_CARE_UNIT
		String doesHospitalHaveCriticalCareUnit = pl.getPlanInfoProperties().get(DOES_HOSPITAL_HAVE_CRITICAL_CARE_UNIT);
		pl.getPlanInformation().setDoesHospitalHaveCriticalCareUnit(
				doesHospitalHaveCriticalCareUnit == null ? DxfFileConstants.NA : doesHospitalHaveCriticalCareUnit);

		// IS_SECURITY_DEPOSIT_REQUIRED

		if (DxfFileConstants.YES.equals(pl.getPlanInfoProperties().get(IS_SECURITY_DEPOSIT_REQUIRED)))
			pl.getPlanInformation().setSecurityDepositRequired(true);
		else
			pl.getPlanInformation().setSecurityDepositRequired(false);

		// DISTANCE_OF_DA_PARKING_SPACE_FROM_BUILDING_ENTRANCE
		try {
			pl.getParkingDetails().setDistFromDAToMainEntrance(new BigDecimal(
					pl.getPlanInfoProperties().get(DISTANCE_OF_DA_PARKING_SPACE_FROM_BUILDING_ENTRANCE)));
		} catch (Exception e) {
			pl.getParkingDetails().setDistFromDAToMainEntrance(BigDecimal.ZERO);
		}

		// TOTAL_PARKING_AREA_IF_PROJECT_HAS_OFF_SITE_PARKING_PROVISION_WITHIN_300_METERS_FROM_PROJECT_SITE
		try {
			pl.getPlanInformation().setOffSiteParkingprovisionsArea(new BigDecimal(pl.getPlanInfoProperties().get(
					TOTAL_PARKING_AREA_IF_PROJECT_HAS_OFF_SITE_PARKING_PROVISION_WITHIN_300_METERS_FROM_PROJECT_SITE)));
		} catch (Exception e) {
			pl.getPlanInformation().setOffSiteParkingprovisionsArea(BigDecimal.ZERO);
		}

		// ARCHITECT_OR_TECHNICAL_PERSON_NAME
		String licensee = pl.getPlanInfoProperties().get(ARCHITECT_OR_TECHNICAL_PERSON_NAME);
		if (licensee != null && !licensee.isEmpty() && licensee.trim().length() > 0
				&& !DxfFileConstants.YES.equals(licensee) && !DxfFileConstants.NO.equals(licensee)
				&& !DxfFileConstants.NO.equals(licensee)) {
			pl.setArchitectInformation(licensee);
		} else {
			pl.addError("licensee", "ARCHITECT_OR_TECHNICAL_PERSON_NAME is mandatory in plan info.");
		}

		// IS_BUILDING_CENTRALLY_AIR_CONDITIONED
		String buildingCentrallyAirConditioned = pl.getPlanInfoProperties().get(IS_BUILDING_CENTRALLY_AIR_CONDITIONED);
		if (YES.equals(buildingCentrallyAirConditioned) || NO.equals(buildingCentrallyAirConditioned)) {
			pl.getPlanInformation().setBuildingCentrallyAirConditioned(buildingCentrallyAirConditioned);
		} else {
			pl.addError("buildingCentrallyAirConditioned",
					"IS_BUILDING_CENTRALLY_AIR_CONDITIONED is mandatory in plan info.");
		}

		// DOES_PROJECT_HAVE_MORE_THAN_10000_LITRES_OF_WASTE_WATER_DISCHARGE_PER_DAY
		String wasteWaterDischargePerDay = pl.getPlanInfoProperties()
				.get(DOES_PROJECT_HAVE_MORE_THAN_10000_LITRES_OF_WASTE_WATER_DISCHARGE_PER_DAY);
		if (YES.equals(wasteWaterDischargePerDay) || NO.equals(wasteWaterDischargePerDay)) {
			pl.getPlanInformation().setWasteWaterDischargePerDay(wasteWaterDischargePerDay);
		} else {
			pl.addError("wasteWaterDischargePerDay",
					"DOES_PROJECT_HAVE_MORE_THAN_10000_LITRES_OF_WASTE_WATER_DISCHARGE_PER_DAY is mandatory in plan info.");
		}

		// IS_LAND_REGULARIZED
		String isLandRegularized = pl.getPlanInfoProperties().get(IS_LAND_REGULARIZED);
		if (YES.equals(isLandRegularized) || NO.equals(isLandRegularized)) {
			pl.getPlanInformation().setIsLandRegularized(isLandRegularized);
		} else {
			pl.addError("isLandRegularized", "IS_LAND_REGULARIZED is mandatory in plan info.");
		}

		// IS_DRIVEWAY_PROVIDING_ACCESS_TO_REAR_SIDE_OR_ANY_OTHER_SIDE_OTHER_THAN_FRONT_OF_THE_BUILDING
		String isDrivewayForRearSide = pl.getPlanInfoProperties().get(IS_LAND_REGULARIZED);
		if (YES.equals(isDrivewayForRearSide) || NO.equals(isDrivewayForRearSide)) {
			// NA
		} else {
			pl.addError("isDrivewayForRearSide",
					"IS_DRIVEWAY_PROVIDING_ACCESS_TO_REAR_SIDE_OR_ANY_OTHER_SIDE_OTHER_THAN_FRONT_OF_THE_BUILDING is mandatory in plan info.");
		}

		// TOTAL_CONNECTED_LOAD_OF_THE_PROPOSED_PROJECT_IN_W
		String totalConnectedLoadOfTheProposedProjectInW = pl.getPlanInfoProperties()
				.get(TOTAL_CONNECTED_LOAD_OF_THE_PROPOSED_PROJECT_IN_W);
		try {
			pl.getPlanInformation().setTotalConnectedLoadOfTheProposedProjectInW(
					new BigDecimal(totalConnectedLoadOfTheProposedProjectInW));
		} catch (Exception e) {
			pl.addError("totalConnectedLoadOfTheProposedProjectInW",
					"TOTAL_CONNECTED_LOAD_OF_THE_PROPOSED_PROJECT_IN_W is mandatory in plan info.");
		}

		// MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W
		String minimumGenerationCapacityOfTheRooftopSolarPvSystemInW = pl.getPlanInfoProperties()
				.get(MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W);
		try {
			pl.getPlanInformation().setMinimumGenerationCapacityOfTheRooftopSolarPvSystemInW(
					new BigDecimal(minimumGenerationCapacityOfTheRooftopSolarPvSystemInW));
		} catch (Exception e) {
			if (!NA.equals(minimumGenerationCapacityOfTheRooftopSolarPvSystemInW)) {
				pl.addError("minimumGenerationCapacityOfTheRooftopSolarPvSystemInW",
						"MINIMUM_GENERATION_CAPACITY_OF_THE_ROOFTOP_SOLAR_PV_SYSTEM_IN_W is mandatory in plan info.");
			}
		}

		// CAPACITY_OF_SOLAR_WATER_HEATING_SYSTEM_IN_LPD
		String capacityOfSolarWaterHeatingSystemInLpd = pl.getPlanInfoProperties()
				.get(CAPACITY_OF_SOLAR_WATER_HEATING_SYSTEM_IN_LPD);
		try {
			pl.getPlanInformation()
					.setCapacityOfSolarWaterHeatingSystemInLpd(new BigDecimal(capacityOfSolarWaterHeatingSystemInLpd));
		} catch (Exception e) {
			if (!NA.equals(capacityOfSolarWaterHeatingSystemInLpd)) {
				pl.addError("capacityOfSolarWaterHeatingSystemInLpd",
						"CAPACITY_OF_SOLAR_WATER_HEATING_SYSTEM_IN_LPD is mandatory in plan info.");
			}
		}

		// PROJECT_VALUE_IN_INR_IF_EIDP_FEE_IS_APPLICABLE_FOR_PROJECT
		String projectValueForEIDP = pl.getPlanInfoProperties()
				.get(PROJECT_VALUE_IN_INR_IF_EIDP_FEE_IS_APPLICABLE_FOR_PROJECT);
		try {
			pl.getPlanInformation().setProjectValueForEIDP(
					new BigDecimal(projectValueForEIDP));
		} catch (Exception e) {
			if (!NA.equals(projectValueForEIDP)) {
				pl.addError("projectValueForEIDP",
						"PROJECT_VALUE_IN_INR_IF_EIDP_FEE_IS_APPLICABLE_FOR_PROJECT is mandatory in plan info.");
			}
		}

		// IS_THE_PROJECT_BY_STATE_GOVT_OR_CENTRAL_GOVT_OR_GOVT_UNDERTAKING
		String isProjectUndertakingByGovt = pl.getPlanInfoProperties()
				.get(IS_THE_PROJECT_BY_STATE_GOVT_OR_CENTRAL_GOVT_OR_GOVT_UNDERTAKING);
		if (YES.equals(isProjectUndertakingByGovt) || NO.equals(isProjectUndertakingByGovt)) {
			pl.getPlanInformation().setIsProjectUndertakingByGovt(isProjectUndertakingByGovt);
		} else {
			pl.addError("isProjectUndertakingByGovt",
					"IS_THE_PROJECT_BY_STATE_GOVT_OR_CENTRAL_GOVT_OR_GOVT_UNDERTAKING is mandatory in plan info.");
		}

		// NUMBER_OF_TEMPORARY_STRUCTURES_IF_PRESENT_AT_THE_SITE

		String numberOfTemporaryStructures = pl.getPlanInfoProperties()
				.get(NUMBER_OF_TEMPORARY_STRUCTURES_IF_PRESENT_AT_THE_SITE);
		if (numberOfTemporaryStructures == null) {
			pl.addError("numberOfTemporaryStructures",
					"NUMBER_OF_TEMPORARY_STRUCTURES_IF_PRESENT_AT_THE_SITE is mandatory in plan info.");
		} else if (NA.equals(numberOfTemporaryStructures)) {
			pl.getPlanInformation().setIsRetentionFeeApplicable(Boolean.FALSE);
			pl.getPlanInformation().setNumberOfTemporaryStructures(BigDecimal.ZERO);
		} else {
			BigDecimal count = new BigDecimal(numberOfTemporaryStructures);
			if (count.compareTo(BigDecimal.ZERO) > 0) {
				pl.getPlanInformation().setIsRetentionFeeApplicable(Boolean.TRUE);
				pl.getPlanInformation().setNumberOfTemporaryStructures(count);
			} else {
				pl.getPlanInformation().setIsRetentionFeeApplicable(Boolean.FALSE);
				pl.getPlanInformation().setNumberOfTemporaryStructures(BigDecimal.ZERO);
			}
		}
		
		//ADDITIONAL_TDR_IF_APPLICABLE
		String additionalTdr=pl.getPlanInfoProperties()
				.get(ADDITIONAL_TDR_IF_APPLICABLE_M2);
		if(additionalTdr==null) {
			pl.addError(ADDITIONAL_TDR_IF_APPLICABLE_M2, "ADDITIONAL_TDR_IF_APPLICABLE_M2 is mandatory in plan info.");
		}else if(NA.equals(additionalTdr)) {
			pl.getPlanInformation().setAdditionalTdr(BigDecimal.ZERO);
		}else {
			BigDecimal count = BigDecimal.ZERO;
			try {
				count = new BigDecimal(additionalTdr);
			}catch (Exception e) {
				pl.addError(ADDITIONAL_TDR_IF_APPLICABLE_M2, "ADDITIONAL_TDR_IF_APPLICABLE_M2 value is not accepted.");
			}
			if (count.compareTo(BigDecimal.ZERO) > 0) {
				pl.getPlanInformation().setAdditionalTdr(count);
			}
		}
	}
	

	public void savePlanDetail(Plan plan, EdcrApplicationDetail detail) {

		if (LOG.isInfoEnabled())
			LOG.info("*************Before serialization******************");
		File f = new File("plandetail.txt");
		try (FileOutputStream fos = new FileOutputStream(f); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
			ObjectMapper mapper = new ObjectMapper();
			mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
			mapper.writeValue(f, plan);
			detail.setPlanDetailFileStore(
					fileStoreService.store(f, f.getName(), "text/plain", DcrConstants.APPLICATION_MODULE_TYPE));
			oos.flush();
		} catch (IOException e) {
			LOG.error("Unable to serialize!!!!!!", e);
		}
		if (LOG.isInfoEnabled())
			LOG.info("*************Completed serialization******************");

	}

	private Plan applyRules(Plan plan, Amendment amd, Map<String, String> cityDetails) {

		// check whether valid amendments are present
		int index = -1;
		AmendmentDetails[] a = null;
		int length = amd.getDetails().size();
		if (!amd.getDetails().isEmpty()) {
			index = amd.getIndex(plan.getApplicationDate());
			a = new AmendmentDetails[amd.getDetails().size()];
			amd.getDetails().toArray(a);
		}

		for (PlanFeature ruleClass : featureService.getFeatures()) {
			String str = ruleClass.getRuleClass().getSimpleName();
			try {
				FeatureProcess rule = null;
				str = str.substring(0, 1).toLowerCase() + str.substring(1);
				LOG.info("Looking for bean " + str);
				// when amendments are not present
				if (amd.getDetails().isEmpty() || index == -1)
					rule = (FeatureProcess) specificRuleService.find(ruleClass.getRuleClass().getSimpleName());
				// when amendments are present
				else {
					if (index >= 0) {
						// find amendment specific beans
						for (int i = index; i < length; i++) {
							if (a[i].getChanges().keySet().contains(ruleClass.getRuleClass().getSimpleName())) {
								String strNew = str + "_" + a[i].getDateOfBylawString();
								rule = (FeatureProcess) specificRuleService.find(strNew);
								if (rule != null)
									break;
							}
						}
						// when amendment specific beans not found
						if (rule == null) {
							rule = (FeatureProcess) specificRuleService.find(ruleClass.getRuleClass().getSimpleName());
						}

					}

				}

				if (rule != null) {
					LOG.info("Looking for bean resulted in " + rule.getClass().getSimpleName());
					rule.process(plan);
					LOG.info("Completed Process " + rule.getClass().getSimpleName() + "  " + new Date());
				}

				// check occupancy type is present or not
				if (ruleClass.getRuleClass().isAssignableFrom(Coverage.class)) {
					OccupancyTypeHelper occupancyTypeHelper = plan.getVirtualBuilding()
							.getMostRestrictiveFarHelper() != null
									? plan.getVirtualBuilding().getMostRestrictiveFarHelper()
									: null;
					if (occupancyTypeHelper == null && !isOccupancyTypeHelperValid(occupancyTypeHelper)) {
						plan.addError("buildupArea-occ", DxfFileConstants.BLT_UP_AREA_ERROR_MSG);
						return plan;
					}
				}

				if (plan.getErrors().containsKey(DxfFileConstants.OCCUPANCY_ALLOWED_KEY)
						|| plan.getErrors().containsKey("units not in meters")
						|| plan.getErrors().containsKey(DxfFileConstants.OCCUPANCY_PO_NOT_ALLOWED_KEY))
					return plan;

			} catch (Exception e) {
				e.printStackTrace();
				plan.addError("Error " + str, "Error occured while processing " + str + " !");
			}
		}
		return plan;
	}

	private boolean isOccupancyTypeHelperValid(OccupancyTypeHelper occupancyTypeHelper) {
		boolean flage = false;
		if (occupancyTypeHelper != null && occupancyTypeHelper.getType() != null
				&& occupancyTypeHelper.getType().getCode() != null && occupancyTypeHelper.getSubtype() != null
				&& occupancyTypeHelper.getSubtype().getCode() != null)
			flage = true;
		return flage;
	}

	private InputStream generateReport(Plan plan, Amendment amd, EdcrApplication dcrApplication) {

		String beanName = "PlanReportService";
		PlanReportService service = null;
		int index = -1;
		AmendmentDetails[] amdArray = null;
		InputStream reportStream = null;
		int length = amd.getDetails().size();
		if (!amd.getDetails().isEmpty()) {
			index = amd.getIndex(plan.getApplicationDate());
			amdArray = new AmendmentDetails[amd.getDetails().size()];
			amd.getDetails().toArray(amdArray);
		}

		try {
			beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);

			if (amd.getDetails().isEmpty() || index == -1)
				service = (PlanReportService) specificRuleService.find(beanName);
			else if (index >= 0) {
				for (int i = index; i < length; i++) {

					service = (PlanReportService) specificRuleService
							.find(beanName + "_" + amdArray[i].getDateOfBylawString());
					if (service != null)
						break;
				}
			}
			if (service == null) {
				service = (PlanReportService) specificRuleService.find(beanName);
			}

			reportStream = service.generateReport(plan, dcrApplication);

		} catch (BeansException e) {
			LOG.error("No Bean Defined for the Rule " + beanName);
		}

		return reportStream;
	}

	@Transactional
	public void saveOutputReport(EdcrApplication edcrApplication, InputStream reportOutputStream, Plan plan) {

		List<EdcrApplicationDetail> edcrApplicationDetails = edcrApplicationDetailService
				.fingByDcrApplicationId(edcrApplication.getId());
		final String fileName = edcrApplication.getApplicationNumber() + "-v" + edcrApplicationDetails.size() + ".pdf";

		final FileStoreMapper fileStoreMapper = fileStoreService.store(reportOutputStream, fileName, "application/pdf",
				DcrConstants.FILESTORE_MODULECODE);

		buildDocuments(edcrApplication, null, fileStoreMapper, plan);

		PlanInformation planInformation = plan.getPlanInformation();
		edcrApplication.getEdcrApplicationDetails().get(0).setPlanInformation(planInformation);
		edcrApplicationDetailService.saveAll(edcrApplication.getEdcrApplicationDetails());
	}
	
	public void buildDocuments(EdcrApplication edcrApplication, FileStoreMapper dxfFile, FileStoreMapper reportOutput,
			Plan plan) {

		if (dxfFile != null) {
			EdcrApplicationDetail edcrApplicationDetail = new EdcrApplicationDetail();

			edcrApplicationDetail.setDxfFileId(dxfFile);
			edcrApplicationDetail.setApplication(edcrApplication);
			for (EdcrApplicationDetail edcrApplicationDetail1 : edcrApplication.getEdcrApplicationDetails()) {
				edcrApplicationDetail.setPlan(edcrApplicationDetail1.getPlan());
			}
			List<EdcrApplicationDetail> edcrApplicationDetails = new ArrayList<>();
			edcrApplicationDetails.add(edcrApplicationDetail);
			edcrApplication.setSavedEdcrApplicationDetail(edcrApplicationDetail);
			edcrApplication.setEdcrApplicationDetails(edcrApplicationDetails);
		}

		if (reportOutput != null) {
			EdcrApplicationDetail edcrApplicationDetail = edcrApplication.getEdcrApplicationDetails().get(0);

			if (plan.getEdcrPassed()) {
				edcrApplicationDetail.setStatus("Accepted");
				edcrApplication.setStatus("Accepted");
			} else {
				edcrApplicationDetail.setStatus("Not Accepted");
				edcrApplication.setStatus("Not Accepted");
			}
			edcrApplicationDetail.setCreatedDate(new Date());
			edcrApplicationDetail.setReportOutputId(reportOutput);
			if (plan.getEdcrPassed())
				edcrApplicationDetail.setShortenedreportOutputId(generateShortenedReport(plan, edcrApplication));
			List<EdcrApplicationDetail> edcrApplicationDetails = new ArrayList<>();
			edcrApplicationDetails.add(edcrApplicationDetail);
			savePlanDetail(plan, edcrApplicationDetail);

			ArrayList<org.egov.edcr.entity.EdcrPdfDetail> edcrPdfDetails = new ArrayList<>();

			if (plan.getEdcrPdfDetails() != null && !plan.getEdcrPdfDetails().isEmpty()) {
				for (EdcrPdfDetail edcrPdfDetail : plan.getEdcrPdfDetails()) {
					org.egov.edcr.entity.EdcrPdfDetail pdfDetail = new org.egov.edcr.entity.EdcrPdfDetail();
					pdfDetail.setLayer(edcrPdfDetail.getLayer());
					pdfDetail.setFailureReasons(edcrPdfDetail.getFailureReasons());
					pdfDetail.setStandardViolations(edcrPdfDetail.getStandardViolations());

					File convertedPdf = edcrPdfDetail.getConvertedPdf();
					if (convertedPdf != null) {
						FileStoreMapper fileStoreMapper = fileStoreService.store(convertedPdf, convertedPdf.getName(),
								"application/pdf", "Digit DCR");
						pdfDetail.setConvertedPdf(fileStoreMapper);
					}
				}
			}

			if (!edcrPdfDetails.isEmpty()) {
				for (org.egov.edcr.entity.EdcrPdfDetail edcrPdfDetail : edcrPdfDetails) {
					edcrPdfDetail.setEdcrApplicationDetail(edcrApplicationDetail);
				}

				edcrPdfDetailService.saveAll(edcrPdfDetails);
			}

			edcrApplication.setEdcrApplicationDetails(edcrApplicationDetails);
		}
	}

	public Plan extractPlan(EdcrRequest edcrRequest, MultipartFile dxfFile) {
		File planFile = edcrApplicationService.savePlanDXF(dxfFile);

		Date asOnDate = new Date();

		AmendmentService repo = (AmendmentService) specificRuleService.find(AmendmentService.class.getSimpleName());
		Amendment amd = repo.getAmendments();

		Plan plan = extractService.extract(planFile, amd, asOnDate, featureService.getFeatures());
		if (StringUtils.isNotBlank(edcrRequest.getApplicantName()))
			plan.getPlanInformation().setApplicantName(edcrRequest.getApplicantName());
		else
			plan.getPlanInformation().setApplicantName(DxfFileConstants.ANONYMOUS_APPLICANT);

		return plan;
	}

	private void updateFinalReport(FileStoreMapper fileStoreMapper) {
		try {
			Path path = fileStoreService.fetchAsPath(fileStoreMapper.getFileStoreId(), "Digit DCR");

			PDDocument doc = PDDocument.load(new File(path.toString()));
			for (int i = 0; i < doc.getNumberOfPages(); i++) {
				PDPage page = doc.getPage(i);
				PDPageContentStream contentStream = new PDPageContentStream(doc, page,
						PDPageContentStream.AppendMode.APPEND, true);
				/*
				 * if (i == 0) { contentStream.setNonStrokingColor(Color.white);
				 * contentStream.addRect(275, 720, 60, 20); contentStream.fill();
				 * contentStream.setNonStrokingColor(Color.black); contentStream.beginText();
				 * contentStream.newLineAtOffset(275, 720);
				 * contentStream.setFont(PDType1Font.TIMES_BOLD, 12); if
				 * ("Not Accepted".equalsIgnoreCase(status)) {
				 * contentStream.setNonStrokingColor(Color.RED); } else {
				 * contentStream.setNonStrokingColor(0,127,0); } contentStream.showText(status);
				 * contentStream.endText(); }
				 */
				// page coordinate
				contentStream.setNonStrokingColor(Color.white);
				contentStream.addRect(230, 20, 80, 40);
				contentStream.fill();

				contentStream.setNonStrokingColor(Color.black);
				contentStream.beginText();

				contentStream.newLineAtOffset(248, 23);

				contentStream.setFont(PDType1Font.TIMES_ROMAN, 10);
				String text = (i + 1) + " of " + doc.getNumberOfPages();
				contentStream.showText(text);
				contentStream.endText();
				contentStream.close();
			}
			doc.save(new File(path.toString()));
			doc.close();
		} catch (IOException e) {
			throw new ValidationException(Arrays.asList(new ValidationError("error", e.getMessage())));
		}
	}
	
	public FileStoreMapper generateShortenedReport(Plan plan, EdcrApplication dcrApplication) {
		try {
			InputStream reportOutputStream=shortenedReportService.generateReport(plan, dcrApplication);
			final String fileName = dcrApplication.getApplicationNumber() + "-"+DxfFileConstants.SHORTENED_SCRUTINY_REPORT + ".pdf";

			final FileStoreMapper fileStoreMapper = fileStoreService.store(reportOutputStream, fileName, "application/pdf",
					DcrConstants.FILESTORE_MODULECODE);
			
			return fileStoreMapper;
		}catch (Exception e) {
			LOG.error("while genrating shortened report", e);
		}
		return null;
	}
}