package org.egov.edcr.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.AdditionalReportDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.ScrutinyDetail;
import org.egov.common.entity.edcr.SetBack;
import org.egov.edcr.config.properties.EdcrApplicationSettings;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.feature.AdditionalFeature;
import org.egov.edcr.od.FloorNumberToWord;
import org.egov.edcr.od.OdishaUtill;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.BarcodeQRCode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public abstract class PermitOrderService {

	@Autowired
	private JasperReportService reportService;

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private MdmsService mdmsService;

	@Autowired
	private NocService nocService;

	private static final Logger LOG = Logger.getLogger(PermitOrderService.class);

	public static final String FRONT_YARD_DESC = "Front Setback";
	public static final String REAR_YARD_DESC = "Rear Setback";
	public static final String SIDE_YARD1_DESC = "Side Setback 1";
	public static final String SIDE_YARD2_DESC = "Side Setback 2";
	public static final String BSMT_FRONT_YARD_DESC = "Basement Front Setback";
	public static final String BSMT_REAR_YARD_DESC = "Basement Rear Setback";
	public static final String BSMT_SIDE_YARD1_DESC = "Basement Side Setback 1";
	public static final String BSMT_SIDE_YARD2_DESC = "Basement Side Setback 2";
	public static final String BSMT_SIDE_YARD_DESC = "Basement Side Setback";
	public static final String SIDE_YARD_DESC = "Side Setback";
	private static final String SIDENUMBER = "Side Number";
	private static final String SIDENUMBER_NAME = "Setback";
	private static final String LEVEL = "Level";
	private static final String BLOCK_WISE_SUMMARY = "Block Wise Summary";
	private static final String TOTAL = "Total";
	private static final String DESCRIPTION = "description";
	private static final String RULE_NO = "RuleNo";
	public static final String BLOCK = "Block";
	public static final String STATUS = "Status";
	public static final String SQM =" SQM";
	public static final String PROVIDED_LIFT_DETAIL = "providedLiftDetail";
	public static final String REQUIRED_LIFT_DETAIL = "requiredLiftDetail";
	public static final String SCRUTINY_DETAIL_PROVIDED = "Provided";
	public static final String SCRUTINY_DETAIL_REQUIRED = "Required";
	public static final String GROUND_FLOOR_NO = "0";
	public static final String DETAIL = "Detail";
	public static final String DESCRIPTION_IN_SCRUTINY_DETAIL = "Description";
	public static final String NO_OF_TREE_PER_PLOT = "No of tree as per plot";
	public static final String PAYMENTS_RESPONSE_FIELD = "Payments";
	
	public static final String TAXHEAD_BPA_SANC_FEES_CODE = "BPA_SANC_FEES";
	public static final String TAXHEAD_BPA_SANC_FEES_NAME = "Sanction Fee";
	public static final String TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE = "BPA_SANC_TEMP_RETENTION_FEE";
	public static final String TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_NAME = "Temporary Retention Fee";
	public static final String TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE = "BPA_SANC_SECURITY_DEPOSIT";
	public static final String TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_NAME = "Security Deposit";
	public static final String TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE = "BPA_SANC_WORKER_WELFARE_CESS";
	public static final String TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_NAME = "Construction Workers Welfare Cess";
	public static final String TAXHEAD_BPA_SANC_PUR_FAR_CODE = "BPA_SANC_PUR_FAR";
	public static final String TAXHEAD_BPA_SANC_PUR_FAR_NAME = "Purchasable FAR";
	public static final String TAXHEAD_BPA_SANC_SHELTER_FEE_CODE = "BPA_SANC_SHELTER_FEE";
	public static final String TAXHEAD_BPA_SANC_SHELTER_FEE_NAME = "Shelter Fee";
	public static final String TAXHEAD_BPA_SANC_SANC_FEE_CODE = "BPA_SANC_SANC_FEE";
	public static final String TAXHEAD_BPA_SANC_SANC_FEE_NAME = "Sanction Fee";
	public static final String TAXHEAD_BPA_SANC_EIDP_FEE_CODE = "BPA_SANC_EIDP_FEE";
	public static final String TAXHEAD_BPA_SANC_EIDP_FEE_NAME = "EIDP FEE";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE = "BPA_SANC_ADJUSTMENT_AMOUNT";
	public static final String TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_NAME = "Other Fee";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE = 
			"BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_NAME = 
			"Building Operation Fee Rework Adjustment Amount";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE="BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_NAME=
			"Land Development Fee Rework Adjustment Amount";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_CODE = "BPA_LAND_DEV_FEE";
	public static final String TAXHEAD_BPA_LAND_DEV_FEE_NAME = "Development Fee";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_CODE = "BPA_BLDNG_OPRN_FEE";
	public static final String TAXHEAD_BPA_BLDNG_OPRN_FEE_NAME = "Fee for Building Operation";
	public static final String ADJUSTED_AMOUNT_ZERO = "0.0";
	public static final String OWNERSHIP_MAJOR_TYPE_INDIVIDUAL = "INDIVIDUAL";
	public static final String OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE = "INSTITUTIONALPRIVATE";
	public static final String OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT = "INSTITUTIONALGOVERNMENT";
	
	

	public abstract InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo);
	@Autowired
	private EdcrApplicationSettings edcrApplicationSettings;
	
	public static String image_logo;
	public Image logo = null;
	{
		try {
			ClassPathResource resource = new ClassPathResource("logo_base64.txt");
			InputStream inputStream = resource.getInputStream();
			//InputStream is = classloader.getResourceAsStream("logo_base64.txt");
			//FileInputStream fis = new FileInputStream("classpath:config/logo_base64.txt");
			String stringTooLong = IOUtils.toString(inputStream, "UTF-8");
			byte[] b = org.apache.commons.codec.binary.Base64.decodeBase64(stringTooLong);
			logo = Image.getInstance(b);
			logo.scaleToFit(90, 90);
			logo.setAlignment(Image.MIDDLE);
			logo.setAlignment(Image.TOP);
			logo.setAlignment(Image.ALIGN_JUSTIFIED);
		} catch (Exception e) {
			throw new ApplicationRuntimeException("Error while loding logo", e);
		}
	}

	public Image getLogo() throws Exception {
		return logo;
	}
	
	public String getServiceType(Plan pl) {
		try {
			 Map<String, String> SERVICE_TYPE = new ConcurrentHashMap<>();
			 SERVICE_TYPE.put("NEW_CONSTRUCTION", "New Construction");
			 SERVICE_TYPE.put("ADDITION_AND_ALTERATION", "Addition and Alteration");
			return SERVICE_TYPE.get(pl.getPlanInformation().getServiceType());
		}catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
	}
	
	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}
	
	public Map<String, Object> getAdditionalDetailsMap(Map bpa) {
		String jsonString = new JSONObject(bpa).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read("additionalDetails");
	}

	public Object fetchPaymentDetails(RequestInfo requestInfo, String consumercode, String tenantId) {
		paymentService.fetchApplicationFeePaymentDetails(requestInfo, consumercode, tenantId);
		paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode, tenantId);
		return null;
	}
	
	public List<Map<String, Object>> getPermitFeeBillAccountDetails(RequestInfo requestInfo, String consumercode,
			String tenantId) {
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		return getBillAccountDetailsFromPaymentResponse(permitFeePaymentDetails);
	}
	
	public List<Map<String, Object>> getBillAccountDetailsFromPaymentResponse(Object permitFeePaymentDetails) {
		List<Map<String, Object>> billAccountDetails = new ArrayList<>();

		int paymentsLength = 1;
		if (Objects.nonNull(permitFeePaymentDetails) && permitFeePaymentDetails instanceof Map
				&& ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			paymentsLength = payments.size();
		}
		String jsonString = new JSONObject((Map) permitFeePaymentDetails).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		billAccountDetails = context.read(
				"$.Payments[" + (paymentsLength - 1) + "].paymentDetails[0].bill.billDetails[0].billAccountDetails");
		return billAccountDetails;
	}
	
	public String getFeeComponentNameFromTaxHeadCode(String taxHeadCode) {
		String taxHeadName = "";
		switch (taxHeadCode) {
		case TAXHEAD_BPA_SANC_FEES_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_FEES_NAME;
			break;
		case TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_TEMP_RETENTION_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_SECURITY_DEPOSIT_NAME;
			break;
		case TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_WORKER_WELFARE_CESS_NAME;
			break;
		case TAXHEAD_BPA_SANC_PUR_FAR_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_PUR_FAR_NAME;
			break;
		case TAXHEAD_BPA_SANC_SHELTER_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_SHELTER_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_SANC_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_SANC_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_EIDP_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_EIDP_FEE_NAME;
			break;
		case TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE:
			taxHeadName = TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_NAME;
			break;
		case TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_CODE:
			taxHeadName = TAXHEAD_BPA_BLDNG_OPRN_FEE_REWORK_ADJUSTMENT_NAME;
			break;
		case TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_CODE:
			taxHeadName = TAXHEAD_BPA_LAND_DEV_FEE_REWORK_ADJUSTMENT_NAME;
			break;
		case TAXHEAD_BPA_LAND_DEV_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_LAND_DEV_FEE_NAME;
			break;
		case TAXHEAD_BPA_BLDNG_OPRN_FEE_CODE:
			taxHeadName = TAXHEAD_BPA_BLDNG_OPRN_FEE_NAME;
			break;
		default:
			taxHeadName = taxHeadCode;
		}
		return taxHeadName;
	}
	
	public Map<String, String> getAllFeeDetailsMap(LinkedHashMap bpaApplication, RequestInfo requestInfo,
			String consumercode, String tenantId) {

		Map<String, String> paymentDetailsMap = new HashMap<>();
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		int PermitFeePaymentsLength = 1;
		if (Objects.nonNull(permitFeePaymentDetails) && permitFeePaymentDetails instanceof Map
				&& ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) permitFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			PermitFeePaymentsLength = payments.size();
		}
		List<Map<String, Object>> permitFeeBillAccountDetails = getBillAccountDetailsFromPaymentResponse(
				permitFeePaymentDetails);
		for (Map<String, Object> billAccountDetail : permitFeeBillAccountDetails) {
			String adjustedAmount = String.valueOf(billAccountDetail.get("adjustedAmount"));
			String taxHeadCode = String.valueOf(billAccountDetail.get("taxHeadCode"));
			paymentDetailsMap.put(taxHeadCode, adjustedAmount);
			if (TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE.equalsIgnoreCase(taxHeadCode)
					&& !ADJUSTED_AMOUNT_ZERO.equals(adjustedAmount)) {	
				paymentDetailsMap.put("modificationReasonSanctionFeeAdjustmentAmount",
					getValue(bpaApplication, "$.additionalDetails.modificationReasonSanctionFeeAdjustmentAmount"));
			}
		}
		String totalPermitFeeAmountPaid = getValue((Map) permitFeePaymentDetails,
				"$.Payments[" + (PermitFeePaymentsLength - 1) + "].paymentDetails[0].totalAmountPaid");
		paymentDetailsMap.put("totalPermitFeeAmountPaid", totalPermitFeeAmountPaid);

		Object applicationFeePaymentDetails = paymentService.fetchApplicationFeePaymentDetails(requestInfo,
				consumercode, tenantId);
		int applicationFeePaymentsLength = 1;
		if (Objects.nonNull(applicationFeePaymentDetails) && applicationFeePaymentDetails instanceof Map
				&& ((Map) applicationFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD) instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) applicationFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD))) {
			List payments = (List) ((Map) applicationFeePaymentDetails).get(PAYMENTS_RESPONSE_FIELD);
			applicationFeePaymentsLength = payments.size();
		}
		List<Map<String, Object>> applicationFeeBillAccountDetails = getBillAccountDetailsFromPaymentResponse(
				applicationFeePaymentDetails);
		for (Map<String, Object> billAccountDetail : applicationFeeBillAccountDetails) {
			String adjustedAmount = String.valueOf(billAccountDetail.get("adjustedAmount"));
			String taxHeadCode = String.valueOf(billAccountDetail.get("taxHeadCode"));
			paymentDetailsMap.put(taxHeadCode, adjustedAmount);
		}
		String totalApplicationFeeAmountPaid = getValue((Map) applicationFeePaymentDetails,
				"$.Payments[" + (applicationFeePaymentsLength - 1) + "].paymentDetails[0].totalAmountPaid");
		paymentDetailsMap.put("totalApplicationFeeAmountPaid", totalApplicationFeeAmountPaid);

		BigDecimal totalApplicationAndPermitFee = new BigDecimal(totalPermitFeeAmountPaid)
				.add(new BigDecimal(totalApplicationFeeAmountPaid)).setScale(2, BigDecimal.ROUND_HALF_UP);
		paymentDetailsMap.put("totalApplicationAndPermitFee", totalApplicationAndPermitFee + "");
		return paymentDetailsMap;
	}

	public String[] getUlbNameAndGradeFromMdms(RequestInfo requestInfo, String tenantId) {
		return mdmsService.getUlbNameAndGradeFromMdms(requestInfo, tenantId);
	}

	public Set<String> getNocsList(RequestInfo requestInfo, String tenantId, String bpaApplicationNo) {
		Object nocResponse = nocService.fetchNocs(requestInfo, tenantId, bpaApplicationNo);
		Set<String> nocviewableNames = new HashSet<>();
		if (Objects.nonNull(nocResponse) && nocResponse instanceof Map && ((Map) nocResponse).get("Noc") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) nocResponse).get("Noc"))) {
			List nocs = (List) ((Map) nocResponse).get("Noc");
			Set<String> nocNames = (Set<String>) nocs.stream().map(noc -> String.valueOf(((Map) noc).get("nocType")))
					.collect(Collectors.toSet());
			for (String nocName : nocNames) {
				switch (nocName) {
				case "FIRE_NOC":
					nocviewableNames.add("Fire recommendations from Fire Prevention Wing");
					break;
				case "NMA_NOC":
					nocviewableNames.add("NOC from National Monuments Authority");
					break;
				case "AAI_NOC":
					nocviewableNames.add("NOC from Airports Authority of India");
					break;
				default:
					nocviewableNames.add(nocName);
				}
			}
		}
		return nocviewableNames;
	}

	private List<AdditionalReportDetail> buildAdditionalReport(Plan plan) {

		List<AdditionalReportDetail> additionalReportDetails = new ArrayList<>();

		AdditionalReportDetail additionalReportDetail1 = new AdditionalReportDetail();
		additionalReportDetail1.setDescription("No.of staircases");
		additionalReportDetail1.setNoDescription("10");
		additionalReportDetail1.setNoOfItems(new BigDecimal(10));
		AdditionalReportDetail additionalReportDetail2 = new AdditionalReportDetail();
		additionalReportDetail2.setDescription("No.of Lifts");
		additionalReportDetail2.setNoDescription("5");
		additionalReportDetail2.setNoOfItems(new BigDecimal(5));
		AdditionalReportDetail additionalReportDetail3 = new AdditionalReportDetail();
		additionalReportDetail3.setDescription("E-vehicle charging station(30%)");
		additionalReportDetail3.setNoDescription("30% of total car parks provided ");
		additionalReportDetail3.setNoOfItems(new BigDecimal(45));
		AdditionalReportDetail additionalReportDetail4 = new AdditionalReportDetail();
		additionalReportDetail4.setDescription("Visitor parking(20% of required car parking)");
		additionalReportDetail4.setNoDescription("29.4");
		additionalReportDetail4.setNoOfItems(new BigDecimal(30));
		AdditionalReportDetail additionalReportDetail5 = new AdditionalReportDetail();
		additionalReportDetail5.setDescription("Plantation(1no of tree per 80Sqm.)");
		additionalReportDetail5.setNoDescription("91");
		additionalReportDetail5.setNoOfItems(new BigDecimal(93));

		additionalReportDetails.add(additionalReportDetail1);
		additionalReportDetails.add(additionalReportDetail2);
		additionalReportDetails.add(additionalReportDetail3);
		additionalReportDetails.add(additionalReportDetail4);
		additionalReportDetails.add(additionalReportDetail5);

		return additionalReportDetails;

	}

	public List<DcrReportBlockDetail> buildBlockWiseExistingInfo(Plan plan) {
		List<DcrReportBlockDetail> dcrReportBlockDetails = new ArrayList<>();

		// List<Block> blocks = plan.getBlocks();
		List<Block> totalBlocksInPlan = new ArrayList<>();
		totalBlocksInPlan.addAll(plan.getBlocks());
		totalBlocksInPlan.addAll(plan.getOuthouse());
		if (!totalBlocksInPlan.isEmpty()) {

			for (Block block : totalBlocksInPlan) {

				Building building = block.getBuilding();
				if (building != null && building.getTotalExistingBuiltUpArea() != null
						&& building.getTotalExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0
						&& building.getTotalExistingFloorArea().compareTo(BigDecimal.ZERO) > 0) {
					DcrReportBlockDetail dcrReportBlockDetail = new DcrReportBlockDetail();
					dcrReportBlockDetail.setBlockNo(block.getNumber());

					List<Floor> floors = building.getFloors();

					if (!floors.isEmpty()) {
						List<DcrReportFloorDetail> dcrReportFloorDetails = new ArrayList<>();
						for (Floor floor : floors) {

							List<Occupancy> occupancies = floor.getOccupancies();

							if (!occupancies.isEmpty()) {

								for (Occupancy occupancy : occupancies) {
									String occupancyName = "";
									if (occupancy.getTypeHelper() != null)
										if (occupancy.getTypeHelper().getSubtype() != null)
											occupancyName = occupancy.getTypeHelper().getSubtype().getName();
										else if (occupancy.getTypeHelper().getType() != null)
											occupancyName = occupancy.getTypeHelper().getType().getName();
									if (occupancy != null
											&& occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
										DcrReportFloorDetail dcrReportFloorDetail = new DcrReportFloorDetail();
										dcrReportFloorDetail.setFloorNo(
												floor.getTerrace() ? "Terrace" : floor.getNumber().toString());
										dcrReportFloorDetail.setOccupancy(occupancyName);
										dcrReportFloorDetail.setBuiltUpArea(occupancy.getExistingBuiltUpArea());
										dcrReportFloorDetail.setFloorArea(occupancy.getExistingFloorArea());
										dcrReportFloorDetail.setCarpetArea(occupancy.getExistingCarpetArea());
										dcrReportFloorDetails.add(dcrReportFloorDetail);
									}
								}

							}

						}
						dcrReportFloorDetails = dcrReportFloorDetails.stream()
								.sorted(Comparator.comparing(DcrReportFloorDetail::getFloorNumberInteger))
								.collect(Collectors.toList());

						dcrReportBlockDetail.setDcrReportFloorDetails(dcrReportFloorDetails);
					}
					dcrReportBlockDetails.add(dcrReportBlockDetail);
				}

			}

		}
		return dcrReportBlockDetails;
	}

	public List<DcrReportBlockDetail> buildBlockWiseProposedInfo(Plan plan) {
		List<DcrReportBlockDetail> dcrReportBlockDetails = new ArrayList<>();
		AdditionalFeature additionalFeature = new AdditionalFeature();
//		List<Block> blocks = plan.getBlocks();
		List<Block> totalBlocksInPlan = new ArrayList<>();
		totalBlocksInPlan.addAll(plan.getBlocks());
		totalBlocksInPlan.addAll(plan.getOuthouse());

		if (!totalBlocksInPlan.isEmpty()) {

			for (Block block : totalBlocksInPlan) {

				Building building = block.getBuilding();
				if (building != null) {
					DcrReportBlockDetail dcrReportBlockDetail = new DcrReportBlockDetail();
					String noOfFloor = additionalFeature.getNoOfFloor(block);
					// TODO: NA to be replaced by sub occupancy of that block-
					dcrReportBlockDetail.setBlockNo(block.getNumber());
					// dcrReportBlockDetail.setBlockNo(block.getNumber() + " (" + noOfFloor + "
					// NA)");//raza
					dcrReportBlockDetail.setCoverageArea(building.getCoverageArea());
					dcrReportBlockDetail.setBuildingHeight(building.getBuildingHeight());
					dcrReportBlockDetail.setDeclaredBuildingHeight(building.getDeclaredBuildingHeight());
					dcrReportBlockDetail.setConstructedArea(building.getTotalConstructedArea());
					List<Floor> floors = building.getFloors();

					if (!floors.isEmpty()) {
						List<DcrReportFloorDetail> dcrReportFloorDetails = new ArrayList<>();
						for (Floor floor : floors) {

							List<Occupancy> occupancies = floor.getOccupancies();

							if (!occupancies.isEmpty()) {

								for (Occupancy occupancy : occupancies) {
									String occupancyName = "";
									if (occupancy.getTypeHelper() != null)
										if (occupancy.getTypeHelper().getSubtype() != null)
											occupancyName = occupancy.getTypeHelper().getSubtype().getName();
										else {
											if (occupancy.getTypeHelper().getType() != null)
												occupancyName = occupancy.getTypeHelper().getType().getName();
										}
									DcrReportFloorDetail dcrReportFloorDetail = new DcrReportFloorDetail();
									String floorNo;
									dcrReportFloorDetail.setFloorNumberInteger(floor.getNumber());
									if (floor.getTerrace())
										floorNo = "Terrace";
									else if (occupancy.getIsMezzanine())
										floorNo = FloorNumberToWord.floorName(floor.getNumber(), floor.getIsStiltFloor(), floor.getIsServiceFloor()) + " (Mezzanine " + floor.getNumber() + ")";
									else
										floorNo = FloorNumberToWord.floorName(floor.getNumber(), floor.getIsStiltFloor(), floor.getIsServiceFloor());
									dcrReportFloorDetail.setFloorNo(floorNo);
									dcrReportFloorDetail.setOccupancy(occupancyName);
									dcrReportFloorDetail.setBuiltUpArea(
											occupancy.getExistingBuiltUpArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getBuiltUpArea()
															.subtract(occupancy.getExistingBuiltUpArea())
													: occupancy.getBuiltUpArea());
									dcrReportFloorDetail.setFloorArea(
											occupancy.getExistingFloorArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getFloorArea()
															.subtract(occupancy.getExistingFloorArea())
													: occupancy.getFloorArea());
									dcrReportFloorDetail.setCarpetArea(
											occupancy.getExistingCarpetArea().compareTo(BigDecimal.ZERO) > 0
													? occupancy.getCarpetArea()
															.subtract(occupancy.getExistingCarpetArea())
													: occupancy.getCarpetArea());
									if (dcrReportFloorDetail.getBuiltUpArea().compareTo(BigDecimal.ZERO) > 0) {
										dcrReportFloorDetails.add(dcrReportFloorDetail);
									}
								}

							}

						}
						dcrReportFloorDetails = dcrReportFloorDetails.stream()
								.sorted(Comparator.comparing(DcrReportFloorDetail::getFloorNumberInteger))
								.collect(Collectors.toList());

						dcrReportBlockDetail.setDcrReportFloorDetails(dcrReportFloorDetails);
					}
					dcrReportBlockDetails.add(dcrReportBlockDetail);
				}

			}

		}
		return dcrReportBlockDetails;
	}

	public Image getQrCode(String ownersCsv, String permitNo, String approvalDate, String edcrNo) {
		String qrCodeInformation = "Applicant Name: %s, Permit Order Number : %s, Permit Order Date : %s, eDCR Scrutiny Number: %s";
		qrCodeInformation = String.format(qrCodeInformation, ownersCsv, permitNo, approvalDate, edcrNo);
		BarcodeQRCode qrCode = new BarcodeQRCode(qrCodeInformation, 1, 1, null);
		Image codeQrImage = null;
		try {
			codeQrImage = qrCode.getImage();
		} catch (BadElementException e) {
			LOG.error("BadElementException while generating qr code image", e);
		}
		codeQrImage.scaleToFit(90, 90);
		codeQrImage.setAlignment(Image.MIDDLE);
		codeQrImage.setAlignment(Image.TOP);
		codeQrImage.setAlignment(Image.ALIGN_JUSTIFIED);
		return codeQrImage;
	}

	public String getTotalPlotAreaValueV2(Plan pl) {
		// " - Total plot area: Ac1.830Dec. (" + plotArea + " Sqm.)\n", fontPara1Bold
		StringBuilder result = new StringBuilder(" - Total plot area: ");
		BigDecimal totalPlotArea = pl.getPlanInformation().getTotalPlotArea();
		if(totalPlotArea == null || totalPlotArea.compareTo(BigDecimal.ZERO)<=0)
			totalPlotArea = pl.getPlot().getPlotBndryArea();
		BigDecimal totalPlotAreaInAcr = totalPlotArea.divide(new BigDecimal("4046.2"), 3, BigDecimal.ROUND_HALF_UP);
		result.append(totalPlotAreaInAcr + " Acre ( " + totalPlotArea + SQM + " ) ");
		return result.toString();
	}

	public List<Chunk> getTotalCDPRoadAffectedArea(Plan pl) {
		List<Chunk> affectedAreas = new ArrayList<>();
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		for (org.egov.common.entity.edcr.AffectedLandArea affectedLandArea : pl.getAffectedLandAreas()) {
			// affected area
			if (affectedLandArea.getMeasurements() != null && !affectedLandArea.getMeasurements().isEmpty()) {
				BigDecimal area = affectedLandArea.getMeasurements().stream().map(l -> l.getArea())
						.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
				Chunk chunk = new Chunk(
						" - " + affectedLandArea.getName() + " affected area: " + area + SQM + "\n",
						fontPara1Bold);
				affectedAreas.add(chunk);
			}
		}
		return affectedAreas;
	}

	public String getRoadAffectedArea(Plan pl) {
		String roadAffectedArea = BigDecimal.ZERO + "";
		try {
			for (org.egov.common.entity.edcr.AffectedLandArea affectedLandArea : pl.getAffectedLandAreas()) {
				// affected area
				if (affectedLandArea.getMeasurements() != null && !affectedLandArea.getMeasurements().isEmpty()) {
					BigDecimal area = affectedLandArea.getMeasurements().stream().map(l -> l.getArea())
							.reduce(BigDecimal::add).orElse(BigDecimal.ZERO).setScale(2, BigDecimal.ROUND_HALF_UP);
					roadAffectedArea = area + "";
				}
			}
		} catch (Exception ex) {
			LOG.error("error while extracting road affected area for permit letter");
		}
		return roadAffectedArea;
	}
	
	public BigDecimal getGiftedArea(Plan pl) {
		return pl.getPlot().getPlotBndryDeductionArea();
	}
	
	public static Map<String, BigDecimal> getSetBackData(Plan plan, Block block) {
		SetBack setBack = block.getSetBacks().get(0);

		// these are provided setbacks-
		BigDecimal frontSetbackProvided = BigDecimal.ZERO;
		BigDecimal rearSetbackProvided = BigDecimal.ZERO;
		BigDecimal leftSetbackProvided = BigDecimal.ZERO;
		BigDecimal rightSetbackProvided = BigDecimal.ZERO;

		if (setBack != null) {
			frontSetbackProvided = setBack.getFrontYard() != null ? setBack.getFrontYard().getMinimumDistance()
					: BigDecimal.ZERO;
			rearSetbackProvided = setBack.getRearYard() != null ? setBack.getRearYard().getMinimumDistance()
					: BigDecimal.ZERO;
			leftSetbackProvided = setBack.getSideYard1() != null ? setBack.getSideYard1().getMinimumDistance()
					: BigDecimal.ZERO;
			rightSetbackProvided = setBack.getSideYard2() != null ? setBack.getSideYard2().getMinimumDistance()
					: BigDecimal.ZERO;
		}

		Map<String, BigDecimal> setBackData = new HashMap<>();
		setBackData.put("frontSetbackProvided", frontSetbackProvided);
		setBackData.put("rearSetbackProvided", rearSetbackProvided);
		setBackData.put("leftSetbackProvided", leftSetbackProvided);
		setBackData.put("rightSetbackProvided", rightSetbackProvided);

		// these are required setbacks-
		BigDecimal frontSetbackRequired = BigDecimal.ZERO;
		BigDecimal rearSetbackRequired = BigDecimal.ZERO;
		BigDecimal leftSetbackRequired = BigDecimal.ZERO;
		BigDecimal rightSetbackRequired = BigDecimal.ZERO;
		setBackData.put("frontSetbackRequired", frontSetbackRequired);
		setBackData.put("rearSetbackRequired", rearSetbackRequired);
		setBackData.put("leftSetbackRequired", leftSetbackRequired);
		setBackData.put("rightSetbackRequired", rightSetbackRequired);
		return setBackData;
	}
	
	public String getStairCount(Plan pl) {
		String count = DxfFileConstants.NA;
		//TODO required stair
		StringBuilder requiredStairCount = new StringBuilder();
		StringBuilder stairDetail = new StringBuilder();
		try {
			for (Block block : pl.getBlocks()) {
				Optional<Integer> maxStairCount = block.getBuilding().getFloors().stream()
						.map(floor -> floor.getGeneralStairs() == null ? 0 : floor.getGeneralStairs().size())
						.reduce(Integer::max);
				if (maxStairCount.isPresent())
					stairDetail.append( ", "+"B"+block.getName() + "-" + maxStairCount.get());
			}
		} catch (Exception ex) {
			LOG.error("error while extracting the stair count for permit letter", ex);
		}
		if (!stairDetail.toString().isEmpty()) {
			count = stairDetail.toString();
			count = count.replaceFirst(", ", "");
		}
		return count;
	}
	
	public String getRequiredStairCount(Plan plan) {
		String count = DxfFileConstants.NA;
		StringBuilder requiredStairs = new StringBuilder();
		try {
			for (Block block : plan.getBlocks()) {
				int requiredStair = org.egov.edcr.feature.GeneralStair.requiredGenralStairPerFloor(plan, block);
				requiredStairs.append(", B" + block.getName() + "-" + requiredStair);
			}
			if (!requiredStairs.toString().isEmpty())
				count = requiredStairs.toString().replaceFirst(", ", "");
		} catch (Exception ex) {
			LOG.error("error while extracting required no of stairs for permit letter", ex);
		}
		return count;
	}
	
	public Map<String, String> getLiftDetails(Plan plan) {
		String detail = DxfFileConstants.NA;
		StringBuilder providedLiftDetail = new StringBuilder();
		StringBuilder requiredLiftDetail = new StringBuilder();
		Map<String, String> liftDetail = new HashMap<>();
		try {
			for (Block block : plan.getBlocks()) {
				java.util.List<ScrutinyDetail> scrutinyDetails = OdishaUtill.getScrutinyDetailsFromPlan(plan,
						"Block_" + block.getNumber() + "_" + "General Lift");
				if (!CollectionUtils.isEmpty(scrutinyDetails)) {
					// use ground floor detail-
					Optional<Map<String, String>> groundFloorDetail = scrutinyDetails.get(0).getDetail().stream()
							.filter(floorDetail -> floorDetail.get("Floor").equals(GROUND_FLOOR_NO)).findFirst();
					if (groundFloorDetail.isPresent() && StringUtils.isNotEmpty(groundFloorDetail.get().get(SCRUTINY_DETAIL_REQUIRED))
							&& StringUtils.isNotEmpty(groundFloorDetail.get().get(SCRUTINY_DETAIL_PROVIDED))) {
						requiredLiftDetail
								.append( ", "+"B"+block.getName() + "-" + groundFloorDetail.get().get(SCRUTINY_DETAIL_REQUIRED));
						providedLiftDetail
								.append( ", "+"B"+block.getName() + "-" + groundFloorDetail.get().get(SCRUTINY_DETAIL_PROVIDED));
					}

				}
			}
		} catch (Exception ex) {
			LOG.error("error while extracting the lift count for permit letter", ex);
		}
		String addProvidedLiftDetail = !providedLiftDetail.toString().isEmpty()
				? liftDetail.put(PROVIDED_LIFT_DETAIL, providedLiftDetail.toString().replaceFirst(", ", ""))
				: liftDetail.put(PROVIDED_LIFT_DETAIL, detail);
		String addRequiredLiftDetail = !requiredLiftDetail.toString().isEmpty()
				? liftDetail.put(REQUIRED_LIFT_DETAIL, requiredLiftDetail.toString().replaceFirst(", ", ""))
				: liftDetail.put(REQUIRED_LIFT_DETAIL, detail);
		return liftDetail;
	}
	
	public String getNoOfTreesRequired(Plan plan) {
		String noOfTreesRequired = DxfFileConstants.NA;
		try {
			java.util.List<ScrutinyDetail> scrutinyDetails = OdishaUtill.getScrutinyDetailsFromPlan(plan,
					"Common_Plantation Tree Cover");
			if (!CollectionUtils.isEmpty(scrutinyDetails)) {
				ScrutinyDetail scrutinyDetail = scrutinyDetails.get(0);
				Optional<Map<String, String>> treeDetail = scrutinyDetail.getDetail().stream()
						.filter(detail -> StringUtils.isNotEmpty(detail.get(DESCRIPTION_IN_SCRUTINY_DETAIL))
								&& NO_OF_TREE_PER_PLOT.equalsIgnoreCase(detail.get(DESCRIPTION_IN_SCRUTINY_DETAIL)))
						.findFirst();
				if (treeDetail.isPresent() && StringUtils.isNotEmpty(treeDetail.get().get(SCRUTINY_DETAIL_REQUIRED)))
					noOfTreesRequired = treeDetail.get().get(SCRUTINY_DETAIL_REQUIRED);
			}
		} catch (Exception ex) {
			LOG.error("error while extracting no of trees required for permit letter", ex);
		}
		return noOfTreesRequired;
	}

	public String getNoOfTreesProvided(Plan plan) {
		String noOfTreesProvided = DxfFileConstants.NA;
		try {
			int noOfCutTree = plan.getPlantation().getCutTreeCount();
			int noOfExistingTree = plan.getPlantation().getExistingTreeCount();
			int noOfPlantedTree = plan.getPlantation().getPlantedTreeCount();
			noOfTreesProvided = noOfExistingTree - noOfCutTree + noOfPlantedTree + "";
		} catch (Exception ex) {
			LOG.error("error while extracting no of trees provided for permit letter", ex);
		}
		return noOfTreesProvided;
	}
	
	public String getHeight(Plan plan) {
		String height = DxfFileConstants.NA;
		StringBuilder heights = new StringBuilder();
		try {
			for (Block block : plan.getBlocks()) {
				heights.append(", B" + block.getName() + "-" + block.getBuilding().getBuildingHeight());
			}
			if (!heights.toString().isEmpty())
				height = heights.toString().replaceFirst(", ", "");
		} catch (Exception ex) {
			LOG.error("error while extracting height og blocks for permit letter", ex);
		}
		return height;
	}
	
	public String getCorrespondenceAddress(LinkedHashMap bpaApplication) {
		String correspondenceAddress = "";
		try {
			String ownershipMajorType = getOwnershipMajorType(bpaApplication);
			String jsonPathForCorrespondenceAddress = "";
			if (StringUtils.isNotEmpty(ownershipMajorType)) {
				switch (ownershipMajorType) {
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE:
					// only one owner allowed from UI-
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
					break;
				case OWNERSHIP_MAJOR_TYPE_INDIVIDUAL:
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[?(@.isPrimaryOwner==true)].correspondenceAddress";
					break;
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT:
					// only one owner allowed from UI-
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
					break;
				default:
					LOG.info("unsupported ownershipMajorType:" + ownershipMajorType);
				}
			} else {
				String ownershipCategory = getOwnershipCategory(bpaApplication);
				if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[?(@.isPrimaryOwner==true)].correspondenceAddress";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
					jsonPathForCorrespondenceAddress = "$.landInfo.owners[0].correspondenceAddress";
				else
					LOG.info("unsupported ownershipCategory:" + ownershipCategory);
			}
			correspondenceAddress = getValue(bpaApplication, jsonPathForCorrespondenceAddress).replace("[", "")
					.replace("]", "").replace("\"", "");
		} catch (Exception ex) {
			LOG.error("error while extracting corresponding address of owner", ex);
		}
		return correspondenceAddress;
	}

	public String getNameOfOwner(LinkedHashMap bpaApplication) {
		String ownerName = "";
		try {
			// first look for ownershipMajorType.If not available then look for
			// ownershipCategory
			String ownershipMajorType = getOwnershipMajorType(bpaApplication);
			String jsonPathForOwnerName = "";
			if (StringUtils.isNotEmpty(ownershipMajorType)) {
				switch (ownershipMajorType) {
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE:
					// only one owner allowed from UI-
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
					break;
				case OWNERSHIP_MAJOR_TYPE_INDIVIDUAL:
					jsonPathForOwnerName = "$.landInfo.owners.*.name";
					break;
				case OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT:
					// only one owner allowed from UI-
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
					break;
				default:
					LOG.info("unsupported ownershipMajorType:" + ownershipMajorType);
				}
			} else {
				String ownershipCategory = getOwnershipCategory(bpaApplication);
				if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_PRIVATE))
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INDIVIDUAL))
					jsonPathForOwnerName = "$.landInfo.owners.*.name";
				else if (ownershipCategory.contains(OWNERSHIP_MAJOR_TYPE_INSTITUTIONAL_GOVERNMENT))
					jsonPathForOwnerName = "$.landInfo.additionalDetails.institutionName";
				else
					LOG.info("unsupported ownershipCategory:" + ownershipCategory);
			}
			ownerName = getValue(bpaApplication, jsonPathForOwnerName).replace("[", "").replace("]", "").replace("\"",
					"");

		} catch (Exception ex) {
			LOG.error("error while extracting owner name", ex);
		}
		return ownerName;
	}

	private String getOwnershipMajorType(LinkedHashMap bpaApplication) {
		String ownershipMajorType = "";
		try {
			ownershipMajorType = getValue(bpaApplication, "$.landInfo.ownerShipMajorType");
		} catch (Exception ex) {
			LOG.error("exception while extracting ownershipMajorType");
		}
		return ownershipMajorType;
	}

	private String getOwnershipCategory(LinkedHashMap bpaApplication) {
		String ownershipCategory = "";
		try {
			ownershipCategory = getValue(bpaApplication, "$.landInfo.ownershipCategory");
		} catch (Exception ex) {
			LOG.error("exception while extracting ownerShipCategory");
		}
		return ownershipCategory;
	}
}