package org.egov.edcr.service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.AdditionalReportDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.Plan;
import org.egov.edcr.config.properties.EdcrApplicationSettings;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.feature.AdditionalFeature;
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
		return  DxfFileConstants.getServiceTypeList().get(pl.getPlanInformation().getServiceType());
	}
	
	/**
	 * extract value of a node from map
	 * 
	 * @param dataMap data
	 * @param key     jsonpath to extract from data
	 * @return
	 */
	public String getValue(Map dataMap, String key) {
		String jsonString = new JSONObject(dataMap).toString();
		DocumentContext context = JsonPath.using(Configuration.defaultConfiguration()).parse(jsonString);
		return context.read(key) + "";
	}

	public Object fetchPaymentDetails(RequestInfo requestInfo, String consumercode, String tenantId) {
		paymentService.fetchApplicationFeePaymentDetails(requestInfo, consumercode, tenantId);
		paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode, tenantId);
		return null;
	}

	public String[] getSanctionFeeAndCWWC(RequestInfo requestInfo, String consumercode, String tenantId) {
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		int paymentsLength = 1;
		if (Objects.nonNull(permitFeePaymentDetails) && permitFeePaymentDetails instanceof Map
				&& ((Map) permitFeePaymentDetails).get("Payments") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) permitFeePaymentDetails).get("Payments"))) {
			List payments = (List) ((Map) permitFeePaymentDetails).get("Payments");
			paymentsLength = payments.size();
		}

		String sanctionFeeAmount = getValue((Map) permitFeePaymentDetails, "$.Payments[" + (paymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_SANC_FEE')].adjustedAmount");
		String constructionWelfareCess = getValue((Map) permitFeePaymentDetails, "$.Payments[" + (paymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_WORKER_WELFARE_CESS')].adjustedAmount");

		sanctionFeeAmount = sanctionFeeAmount.replace("[", "").replace("]", "");
		constructionWelfareCess = constructionWelfareCess.replace("[", "").replace("]", "");

		String[] sanctionFeeAndCWWC = new String[2];
		sanctionFeeAndCWWC[0] = sanctionFeeAmount;
		sanctionFeeAndCWWC[1] = constructionWelfareCess;
		return sanctionFeeAndCWWC;
	}

	public String[] getAllFeeDetails(RequestInfo requestInfo, String consumercode, String tenantId) {
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		int PermitFeePaymentsLength = 1;
		if (Objects.nonNull(permitFeePaymentDetails) && permitFeePaymentDetails instanceof Map
				&& ((Map) permitFeePaymentDetails).get("Payments") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) permitFeePaymentDetails).get("Payments"))) {
			List payments = (List) ((Map) permitFeePaymentDetails).get("Payments");
			PermitFeePaymentsLength = payments.size();
		}

		String sanctionFeeAmount = getValue((Map) permitFeePaymentDetails, "$.Payments[" + (PermitFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_SANC_FEE')].adjustedAmount");
		String constructionWelfareCess = getValue((Map) permitFeePaymentDetails, "$.Payments["
				+ (PermitFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_WORKER_WELFARE_CESS')].adjustedAmount");
		String shelterFees = getValue((Map) permitFeePaymentDetails, "$.Payments[" + (PermitFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_SHELTER_FEE')].adjustedAmount");
		String purchasedFarFees = getValue((Map) permitFeePaymentDetails, "$.Payments[" + (PermitFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_PUR_FAR')].adjustedAmount");
		String EIDPFees = getValue((Map) permitFeePaymentDetails, "$.Payments[" + (PermitFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_EIDP_FEE')].adjustedAmount");
		String totalPermitFeeAmountPaid = getValue((Map) permitFeePaymentDetails,
				"$.Payments[" + (PermitFeePaymentsLength - 1) + "].paymentDetails[0].totalAmountPaid");
		String temporaryRetentionFee = getValue((Map) permitFeePaymentDetails, "$.Payments["
				+ (PermitFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_TEMP_RETENTION_FEE')].adjustedAmount");

		sanctionFeeAmount = sanctionFeeAmount.replace("[", "").replace("]", "");
		constructionWelfareCess = constructionWelfareCess.replace("[", "").replace("]", "");
		shelterFees = shelterFees.replace("[", "").replace("]", "");
		purchasedFarFees = purchasedFarFees.replace("[", "").replace("]", "");
		EIDPFees = EIDPFees.replace("[", "").replace("]", "");
		totalPermitFeeAmountPaid = totalPermitFeeAmountPaid.replace("[", "").replace("]", "");
		temporaryRetentionFee = temporaryRetentionFee.replace("[", "").replace("]", "");
		String[] allFeeDetails = new String[11];
		allFeeDetails[0] = sanctionFeeAmount;
		allFeeDetails[1] = constructionWelfareCess;
		allFeeDetails[2] = shelterFees;
		allFeeDetails[3] = purchasedFarFees;
		allFeeDetails[4] = EIDPFees;
		allFeeDetails[5] = totalPermitFeeAmountPaid;

		Object applicationFeePaymentDetails = paymentService.fetchApplicationFeePaymentDetails(requestInfo,
				consumercode, tenantId);
		int applicationFeePaymentsLength = 1;
		if (Objects.nonNull(applicationFeePaymentDetails) && applicationFeePaymentDetails instanceof Map
				&& ((Map) applicationFeePaymentDetails).get("Payments") instanceof List
				&& !CollectionUtils.isEmpty((List) ((Map) applicationFeePaymentDetails).get("Payments"))) {
			List payments = (List) ((Map) applicationFeePaymentDetails).get("Payments");
			applicationFeePaymentsLength = payments.size();
		}
		String developmentFeeAmount = getValue((Map) applicationFeePaymentDetails, "$.Payments["
				+ (applicationFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_LAND_DEV_FEE')].adjustedAmount");
		String buildingOperationFee = getValue((Map) applicationFeePaymentDetails, "$.Payments["
				+ (applicationFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_BLDNG_OPRN_FEE')].adjustedAmount");
		String totalApplicationFeeAmountPaid = getValue((Map) applicationFeePaymentDetails,
				"$.Payments[" + (applicationFeePaymentsLength - 1) + "].paymentDetails[0].totalAmountPaid");
		developmentFeeAmount = developmentFeeAmount.replace("[", "").replace("]", "");
		buildingOperationFee = buildingOperationFee.replace("[", "").replace("]", "");
		totalApplicationFeeAmountPaid = totalApplicationFeeAmountPaid.replace("[", "").replace("]", "");
		developmentFeeAmount = developmentFeeAmount.isEmpty() ? "0.0" : developmentFeeAmount;
		allFeeDetails[6] = developmentFeeAmount;
		allFeeDetails[7] = buildingOperationFee;
		allFeeDetails[8] = totalApplicationFeeAmountPaid;
		BigDecimal totalApplicationAndPermitFee = new BigDecimal(totalPermitFeeAmountPaid)
				.add(new BigDecimal(totalApplicationFeeAmountPaid)).setScale(2, BigDecimal.ROUND_HALF_UP);
		allFeeDetails[9] = totalApplicationAndPermitFee + "";
		allFeeDetails[10] = temporaryRetentionFee;
		return allFeeDetails;
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
								.sorted(Comparator.comparing(DcrReportFloorDetail::getFloorNo))
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
									if (floor.getTerrace())
										floorNo = "Terrace";
									else if (occupancy.getIsMezzanine())
										floorNo = floor.getNumber() + " (Mezzanine " + floor.getNumber() + ")";
									else
										floorNo = String.valueOf(floor.getNumber());
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
								.sorted(Comparator.comparing(DcrReportFloorDetail::getFloorNo))
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
		BigDecimal totalPlotAreaInAcr = totalPlotArea.divide(new BigDecimal("4046.2"), 3, BigDecimal.ROUND_HALF_UP);
		result.append(totalPlotAreaInAcr + " Acre ( " + totalPlotArea + DxfFileConstants.SQM + " ) ");
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
						" - " + affectedLandArea.getName() + " affected area: " + area + DxfFileConstants.SQM + "\n",
						fontPara1Bold);
				affectedAreas.add(chunk);
			}
		}
		return affectedAreas;
	}
	
	public BigDecimal getGiftedArea(Plan pl) {
		return pl.getPlot().getPlotBndryDeductionArea();
	}
}
