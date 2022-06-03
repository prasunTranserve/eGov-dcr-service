package org.egov.edcr.service;

import static ar.com.fdvs.dj.domain.constants.Stretching.RELATIVE_TO_BAND_HEIGHT;

import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.AdditionalReportDetail;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.Building;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.Floor;
import org.egov.common.entity.edcr.Occupancy;
import org.egov.common.entity.edcr.OccupancyPercentage;
import org.egov.common.entity.edcr.OccupancyReport;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.VirtualBuilding;
import org.egov.common.entity.edcr.VirtualBuildingReport;
import org.egov.edcr.entity.PaymentTable;
import org.egov.edcr.feature.AdditionalFeature;
import org.egov.infra.microservice.models.RequestInfo;
import org.json.simple.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.itextpdf.text.Image;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

import ar.com.fdvs.dj.core.DJConstants;
import ar.com.fdvs.dj.core.layout.ClassicLayoutManager;
import ar.com.fdvs.dj.domain.DJCalculation;
import ar.com.fdvs.dj.domain.DJDataSource;
import ar.com.fdvs.dj.domain.DynamicReport;
import ar.com.fdvs.dj.domain.Style;
import ar.com.fdvs.dj.domain.builders.ColumnBuilder;
import ar.com.fdvs.dj.domain.builders.ColumnBuilderException;
import ar.com.fdvs.dj.domain.builders.FastReportBuilder;
import ar.com.fdvs.dj.domain.constants.Border;
import ar.com.fdvs.dj.domain.constants.Page;
import ar.com.fdvs.dj.domain.entities.Subreport;
import ar.com.fdvs.dj.domain.entities.columns.AbstractColumn;

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

	public abstract  InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo);
	
//	{
//		FastReportBuilder drb = new FastReportBuilder();
//		StringBuilder reportBuilder = new StringBuilder();
//
//		final Style titleStyle = new Style("titleStyle");
//		titleStyle.setFont(new Font(50, Font._FONT_TIMES_NEW_ROMAN, true));
//		titleStyle.setHorizontalAlign(HorizontalAlign.CENTER);
//
//		titleStyle.setFont(new Font(2, Font._FONT_TIMES_NEW_ROMAN, false));
//
//		if (plan.getVirtualBuilding() != null && !plan.getVirtualBuilding().getOccupancyTypes().isEmpty()) {
//			List<String> occupancies = new ArrayList<>();
//			plan.getVirtualBuilding().getOccupancyTypes().forEach(occ -> {
//				if (occ.getType() != null)
//					occupancies.add(occ.getType().getName());
//			});
//			Set<String> distinctOccupancies = new HashSet<>(occupancies);
////			plan.getPlanInformation()
////					.setOccupancy(distinctOccupancies.stream().map(String::new).collect(Collectors.joining(",")));
//			if (plan.getVirtualBuilding().getMostRestrictiveFarHelper() != null
//					&& plan.getVirtualBuilding().getMostRestrictiveFarHelper().getType() != null)
//				plan.getPlanInformation()
//						.setOccupancy(plan.getVirtualBuilding().getMostRestrictiveFarHelper().getType().getName());
//		}
//
//		StringBuilder nocs = new StringBuilder();
//		if (plan.getNoObjectionCertificates() != null && plan.getNoObjectionCertificates().size() > 0) {
//			int i = 1;
//			for (Map.Entry<String, String> entry : plan.getNoObjectionCertificates().entrySet()) {
//				nocs.append(String.valueOf(i)).append(". ");
//				nocs.append(entry.getValue());
//				nocs.append("\n");
//				i++;
//			}
//		}
//
//		drb.setPageSizeAndOrientation(new Page(842, 595, true));
//
//		final JRDataSource ds = new JRBeanCollectionDataSource(Collections.singletonList(plan));
//		final Map<String, Object> valuesMap = new HashMap<>();
//		String ulbName = null;
////		if (ulbName == null || ulbName.trim().isEmpty())
//		ulbName = UlbNameConstants.ulbName(plan.getThirdPartyUserTenantld());
//		valuesMap.put("ulbName", ulbName);
//		valuesMap.put("nocString", nocs.toString());
//		valuesMap.put("nocs", plan.getNoObjectionCertificates());
//		valuesMap.put("far", plan.getFarDetails() != null ? plan.getFarDetails().getProvidedFar() : "");
//		valuesMap.put("coverage", plan.getCoverage());
//		valuesMap.put("totalFloorArea",
//				plan.getVirtualBuilding() != null ? plan.getVirtualBuilding().getTotalFloorArea()
//						: BigDecimal.valueOf(0));
//		valuesMap.put("totalBuiltUpArea",
//				plan.getVirtualBuilding() != null ? plan.getVirtualBuilding().getTotalBuitUpArea()
//						: BigDecimal.valueOf(0));
//		valuesMap.put("blockCount",
//				plan.getBlocks() != null && !plan.getBlocks().isEmpty() ? plan.getBlocks().size() : 0);
//		valuesMap.put("surrenderRoadArea", plan.getTotalSurrenderRoadArea());
//
//		List<DcrReportBlockDetail> blockDetails = new ArrayList<>();
//
//		List<DcrReportBlockDetail> existingBlockDetails = buildBlockWiseExistingInfo(plan);
//		VirtualBuildingReport virtualBuildingReport = buildVirtualBuilding(plan.getVirtualBuilding());
//		List<OccupancyReport> occupanciesReport = buildSubOccupanciesReport(
//				plan.getPlanInformation().getOccupancyPercentages());
//		List<AdditionalReportDetail> additionalReportDetails = buildAdditionalReport(plan);
//
//		List<DcrReportBlockDetail> proposedBlockDetails = buildBlockWiseProposedInfo(plan);
//
//		// Add proposed block details
//		for (DcrReportBlockDetail dcrReportBlockDetail : proposedBlockDetails) {
//			blockDetails.add(dcrReportBlockDetail);
//			drb.addConcatenatedReport(getBlkDetails(dcrReportBlockDetail, true));
//			valuesMap.put("Block No " + dcrReportBlockDetail.getBlockNo(),
//					dcrReportBlockDetail.getDcrReportFloorDetails());
//		}
//
//		// No of staircases and other details-
//		drb.addConcatenatedReport(getAdditionalDetailsV2());
//		valuesMap.put("AdditionalDetails2", additionalReportDetails);
//
//		drb.addConcatenatedReport(getFarAndParkingDetails());
//		List<Map<String, String>> farAndParkingDetails = new ArrayList<>();
//		Map<String, String> farDetail = new HashMap<>();
//		farDetail.put("key1", "F.A.R.");
//		farDetail.put("key2", "6.00 (Max. Permissible) 2.00(Base FAR )");
//		farDetail.put("key3", "ACHIEVED 2.984(0.984 Purchasable FAR)");
//		farAndParkingDetails.add(farDetail);
//		Map<String, String> heightDetail = new HashMap<>();
//		heightDetail.put("key1", "Height");
//		heightDetail.put("key2", "44.7 Mtr");
//		heightDetail.put("key3", null);
//		farAndParkingDetails.add(heightDetail);
//		Map<String, String> parkingDetail = new HashMap<>();
//		parkingDetail.put("key1", "Parking (30%)");
//		parkingDetail.put("key2", "Basement-4787.55+ Stilt- 456.2 + Ground (Open Parking )-1554.2");
//		parkingDetail.put("key3", "Total =6798.03Sqm");
//		farAndParkingDetails.add(parkingDetail);
//		valuesMap.put("FarAndParkingDetails", farAndParkingDetails);
//
//		drb.addConcatenatedReport(getSetBacks());
//		List<Map<String, String>> setBackDetails = new ArrayList<>();
//		Map<String, String> frontSetBack = new HashMap<>();
//		frontSetBack.put("setBackName", "Front Set back");
//		frontSetBack.put("requiredSetback", "11 & 4");
//		frontSetBack.put("providedSetback", "12.80 & 11");
//		setBackDetails.add(frontSetBack);
//		Map<String, String> rearSetBack = new HashMap<>();
//		rearSetBack.put("setBackName", "Rear Set back");
//		rearSetBack.put("requiredSetback", "11 & 3");
//		rearSetBack.put("providedSetback", "13.00 & 3.20");
//		setBackDetails.add(rearSetBack);
//		Map<String, String> leftSide = new HashMap<>();
//		leftSide.put("setBackName", "Left side");
//		leftSide.put("requiredSetback", "11 & 2.5");
//		leftSide.put("providedSetback", "14.25 & 3.0");
//		setBackDetails.add(leftSide);
//		Map<String, String> rightSide = new HashMap<>();
//		rightSide.put("setBackName", "Right side");
//		rightSide.put("requiredSetback", "11 & 2.5");
//		rightSide.put("providedSetback", "16.8 & 6.66");
//		setBackDetails.add(rightSide);
//		valuesMap.put("SetbackDetails", setBackDetails);
//
//		drb.addConcatenatedReport(getNocs());
//		valuesMap.put("Nocs", Collections.EMPTY_LIST);
//
//		drb.addConcatenatedReport(getConditions());
//		valuesMap.put("Conditions", Collections.EMPTY_LIST);
//
//		List<PaymentTable> applicationFeeDetails = buildApplicationFeeDetails(plan);
//		drb.addConcatenatedReport(getPaymentDetails("Application Fee Components", "Application Fee Details", true));
//		valuesMap.put("Application Fee Details", applicationFeeDetails);
//
//		List<PaymentTable> permitFeeDetails = buildPermitFeeDetails(plan);
//		drb.addConcatenatedReport(getPaymentDetails("Permit Fee Components", "Permit Fee Details", false));
//		valuesMap.put("Permit Fee Details", permitFeeDetails);
//
//		drb.addConcatenatedReport(getPaymentTotal());
//		valuesMap.put("paymentTotal", Collections.EMPTY_LIST);
//
//		drb.addConcatenatedReport(getOtherConditionsDeclaration());
//		valuesMap.put("OtherConditionsDeclaration", Collections.EMPTY_LIST);
//
//		drb.addConcatenatedReport(getOtherConditions());
//		valuesMap.put("OtherConditions", Collections.EMPTY_LIST);
//
//		// recent parameters-
//		valuesMap.put("permitNo", "BP/CTC/000044");
//		valuesMap.put("dated", "10/05/2022");
//		valuesMap.put("applicationNo", "BP-CTC-2022-05-10-000201");
//		valuesMap.put("ulbName", "Cuttack");
//		valuesMap.put("ulbGrade", "Municipal Corporation");
//		valuesMap.put("permissionUnder",
//				"Permission Under Sub-Section (3) of the Section-16 of the Orissa Development Authorities\r\n"
//						+ "Act’1982(Orissa Act,1982) is hereby granted in favour of Smt/Sri");
//		valuesMap.put("ownerName", "Prasun Kumar");
//		valuesMap.put("forConstructionOf", " for construction of");
//		valuesMap.put("noOfFloors", "[G+1]");
//		valuesMap.put("occupancyType", "Residential Plotted building");
//		valuesMap.put("inRespectOf", " in respect of Plot No");
//		valuesMap.put("plotNo", "32/1");
//		valuesMap.put("khataNoText", ", Khata No. ");
//		valuesMap.put("khataNo", "560");
//		valuesMap.put("villageText", ", Village/Mouza");
//		valuesMap.put("village", "Andarpur");
//		valuesMap.put("ofText", "  in the Development Plan area of ");
//		valuesMap.put("subjectTo", " with the following parameters and conditions;");
//		valuesMap.put("totalPlotArea", "4864");
//		valuesMap.put("roadAffectedArea", "0");
//		valuesMap.put("roadWidth", "9.14");
//
//		LOG.info("Generate Report.......");
//		List<ScrutinyDetail> scrutinyDetails = plan.getReportOutput().getScrutinyDetails();
//
//		Set<String> common = new TreeSet<>();
//		Map<String, ScrutinyDetail> allMap = new HashMap<>();
//		Map<String, Set<String>> blocks = new TreeMap<>();
//		for (ScrutinyDetail sd : scrutinyDetails) {
//			LOG.info(sd.getKey());
//			LOG.info(sd.getHeading());
//			String[] split = {};
//			if (sd.getKey() != null)
//				split = sd.getKey().split("_");
//			if (split.length == 2) {
//				common.add(split[1]);
//				allMap.put(split[1], sd);
//
//			} else if (split.length == 3) {
//				if (blocks.get(split[1]) == null) {
//					Set<String> features = new TreeSet<>();
//					features.add(split[2]);
//					blocks.put(split[1], features);
//				} else {
//					blocks.get(split[1]).add(split[2]);
//				}
//				allMap.put(split[1] + split[2], sd);
//			}
//		}
//		int i = 0;
//		drb.setTemplateFile("/reports/templates/002_Merged_BDA_20220519.jrxml");
//		drb.setMargins(0, 0, 60, 55);
//		final DynamicReport dr = drb.build();
//		InputStream exportPdf = null;
//		try {
//			JasperPrint generateJasperPrint = DynamicJasperHelper.generateJasperPrint(dr, new ClassicLayoutManager(),
//					ds, valuesMap);
//			JasperExportManager.exportReportToPdfFile(generateJasperPrint,
//					"C:\\Users\\Prasun.Kumar\\code\\od2\\eGov-dcr-service-edcr_sub_report-Branch\\eGov-dcr-service\\egov\\egov-edcr\\src\\main\\resources\\reports\\templates\\001_LatestPermit_20220520.pdf");
//			exportPdf = reportService.exportPdf(generateJasperPrint);
//		} catch (IOException | JRException e) {
//			LOG.error("Error occurred when generating Jasper report", e);
//		}
//		return exportPdf;
//
//	}

	public Image getLogo(String imageUrl) throws Exception {
		Image logo1 = Image.getInstance(new URL(imageUrl));
		logo1.scaleToFit(90, 90);
		logo1.setAlignment(Image.MIDDLE);
		logo1.setAlignment(Image.TOP);
		logo1.setAlignment(Image.ALIGN_JUSTIFIED);
		return logo1;
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
		int paymentsLength=1;
		if(Objects.nonNull(permitFeePaymentDetails)&& permitFeePaymentDetails instanceof Map && ((Map)permitFeePaymentDetails).get("Payments") instanceof List
				&& !CollectionUtils.isEmpty((List)((Map)permitFeePaymentDetails).get("Payments"))) {
			List payments =	(List)((Map)permitFeePaymentDetails).get("Payments");
			paymentsLength=payments.size();
		}
		 
		String sanctionFeeAmount = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(paymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_SANC_FEE')].adjustedAmount");
		String constructionWelfareCess = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(paymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_WORKER_WELFARE_CESS')].adjustedAmount");
				
		sanctionFeeAmount=sanctionFeeAmount.replace("[", "").replace("]", "");
		constructionWelfareCess=constructionWelfareCess.replace("[", "").replace("]", "");
		
		String[] sanctionFeeAndCWWC=new String[2];
		sanctionFeeAndCWWC[0]=sanctionFeeAmount;
		sanctionFeeAndCWWC[1]=constructionWelfareCess;
		return sanctionFeeAndCWWC;
	}
	
	public String[] getAllFeeDetails(RequestInfo requestInfo, String consumercode, String tenantId) {
		Object permitFeePaymentDetails = paymentService.fetchPermitFeePaymentDetails(requestInfo, consumercode,
				tenantId);
		int PermitFeePaymentsLength=1;
		if(Objects.nonNull(permitFeePaymentDetails)&& permitFeePaymentDetails instanceof Map && ((Map)permitFeePaymentDetails).get("Payments") instanceof List
				&& !CollectionUtils.isEmpty((List)((Map)permitFeePaymentDetails).get("Payments"))) {
			List payments =	(List)((Map)permitFeePaymentDetails).get("Payments");
			PermitFeePaymentsLength=payments.size();
		}
		 
		String sanctionFeeAmount = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_SANC_FEE')].adjustedAmount");
		String constructionWelfareCess = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_WORKER_WELFARE_CESS')].adjustedAmount");
		String shelterFees = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_SHELTER_FEE')].adjustedAmount");
		String purchasedFarFees = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_PUR_FAR')].adjustedAmount");
		String EIDPFees = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_EIDP_FEE')].adjustedAmount");
		String totalPermitFeeAmountPaid = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].totalAmountPaid");
		String temporaryRetentionFee = getValue((Map) permitFeePaymentDetails,
				"$.Payments["+(PermitFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_SANC_TEMP_RETENTION_FEE')].adjustedAmount");

		
		sanctionFeeAmount=sanctionFeeAmount.replace("[", "").replace("]", "");
		constructionWelfareCess=constructionWelfareCess.replace("[", "").replace("]", "");
		shelterFees=shelterFees.replace("[", "").replace("]", "");
		purchasedFarFees=purchasedFarFees.replace("[", "").replace("]", "");
		EIDPFees=EIDPFees.replace("[", "").replace("]", "");
		totalPermitFeeAmountPaid = totalPermitFeeAmountPaid.replace("[", "").replace("]", "");
		temporaryRetentionFee = temporaryRetentionFee.replace("[", "").replace("]", "");
		String[] allFeeDetails=new String[11];
		allFeeDetails[0]=sanctionFeeAmount;
		allFeeDetails[1]=constructionWelfareCess;
		allFeeDetails[2]=shelterFees;
		allFeeDetails[3]=purchasedFarFees;
		allFeeDetails[4]=EIDPFees;
		allFeeDetails[5]=totalPermitFeeAmountPaid;
		
		Object applicationFeePaymentDetails = paymentService.fetchApplicationFeePaymentDetails(requestInfo, consumercode, tenantId);
		int applicationFeePaymentsLength=1;
		if(Objects.nonNull(applicationFeePaymentDetails)&& applicationFeePaymentDetails instanceof Map && ((Map)applicationFeePaymentDetails).get("Payments") instanceof List
				&& !CollectionUtils.isEmpty((List)((Map)applicationFeePaymentDetails).get("Payments"))) {
			List payments =	(List)((Map)applicationFeePaymentDetails).get("Payments");
			applicationFeePaymentsLength=payments.size();
		}
		String developmentFeeAmount = getValue((Map) applicationFeePaymentDetails, "$.Payments["
				+ (applicationFeePaymentsLength - 1)
				+ "].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_LAND_DEV_FEE')].adjustedAmount");
		String buildingOperationFee = getValue((Map) applicationFeePaymentDetails,
				"$.Payments["+(applicationFeePaymentsLength-1)+"].paymentDetails[0].bill.billDetails[0].billAccountDetails[?(@.taxHeadCode == 'BPA_BLDNG_OPRN_FEE')].adjustedAmount");
		String totalApplicationFeeAmountPaid = getValue((Map) applicationFeePaymentDetails,
				"$.Payments["+(applicationFeePaymentsLength-1)+"].paymentDetails[0].totalAmountPaid");
		developmentFeeAmount = developmentFeeAmount.replace("[", "").replace("]", "");
		buildingOperationFee = buildingOperationFee.replace("[", "").replace("]", "");
		totalApplicationFeeAmountPaid = totalApplicationFeeAmountPaid.replace("[", "").replace("]", "");
		developmentFeeAmount = developmentFeeAmount.isEmpty() ? "0.0" : developmentFeeAmount;
		allFeeDetails[6]=developmentFeeAmount;
		allFeeDetails[7]=buildingOperationFee;
		allFeeDetails[8]=totalApplicationFeeAmountPaid;
		BigDecimal totalApplicationAndPermitFee = new BigDecimal(totalPermitFeeAmountPaid)
				.add(new BigDecimal(totalApplicationFeeAmountPaid))
				.setScale(2, BigDecimal.ROUND_HALF_UP);
		allFeeDetails[9]=totalApplicationAndPermitFee+"";
		allFeeDetails[10]=temporaryRetentionFee;
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

	private List<PaymentTable> buildApplicationFeeDetails(Plan plan) {
		List<PaymentTable> list = new ArrayList<>();
		list.add(new PaymentTable("Fee for building operation", new BigDecimal(1225), new BigDecimal(1225)));
		list.add(new PaymentTable("Development fees", new BigDecimal(0), new BigDecimal(0)));
		return list;
	}

	private List<PaymentTable> buildPermitFeeDetails(Plan plan) {
		List<PaymentTable> list = new ArrayList<>();
		list.add(new PaymentTable("Sanction fees", new BigDecimal(2475), new BigDecimal(2475)));
		list.add(new PaymentTable("Construction workfer welfare cess(CWWC)", new BigDecimal(32334),
				new BigDecimal(32334)));
		list.add(new PaymentTable("Other fees", new BigDecimal(100), new BigDecimal(100)));
		list.add(new PaymentTable("Shelter fees", new BigDecimal(0), new BigDecimal(0)));
		list.add(new PaymentTable("Charges for purchasable FAR area", new BigDecimal(0), new BigDecimal(0)));
		list.add(new PaymentTable("EIDP fees", new BigDecimal(0), new BigDecimal(0)));
		return list;
	}

	private Subreport getTotalAreaDetails(VirtualBuildingReport virtualBuildingReport) {
		try {

			FastReportBuilder frb = new FastReportBuilder();
			AbstractColumn builtUpArea = ColumnBuilder.getNew()
					.setColumnProperty("totalBuitUpArea", BigDecimal.class.getName()).setTitle("Built Up Area in m²")
					.setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn floorArea = ColumnBuilder.getNew()
					.setColumnProperty("totalFloorArea", BigDecimal.class.getName()).setTitle("Floor Area in m²")
					.setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn carpetArea = ColumnBuilder.getNew()
					.setColumnProperty("totalCarpetArea", BigDecimal.class.getName()).setTitle("Carpet Area in m²")
					.setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

			AbstractColumn coverageArea = ColumnBuilder.getNew()
					.setColumnProperty("totalCoverageArea", BigDecimal.class.getName()).setTitle("Covered Area in m²")
					.setWidth(120).setStyle(reportService.getTotalNumberStyle()).build();

			frb.addColumn(builtUpArea);
			frb.addColumn(floorArea);
			frb.addColumn(carpetArea);
			frb.addColumn(coverageArea);

			frb.setTitle("Total Area");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(new DJDataSource("Total Area Details", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getAdditionalDetails(AdditionalReportDetail additionalReportDetails) {

		try {
			Style style = new Style();

			FastReportBuilder frb = new FastReportBuilder();
			AbstractColumn description = ColumnBuilder.getNew().setColumnProperty("description", String.class.getName())
					.setTitle("Description").setWidth(120).setStyle(style).build();

			AbstractColumn noDescription = ColumnBuilder.getNew()
					.setColumnProperty("noDescription", String.class.getName()).setTitle("Additional Info")
					.setWidth(120).setStyle(style).build();

			AbstractColumn noOfItems = ColumnBuilder.getNew().setColumnProperty("noOfItems", BigDecimal.class.getName())
					.setTitle("No.").setWidth(120).setStyle(style).build();

			frb.addColumn(description);
			frb.addColumn(noDescription);
			frb.addColumn(noOfItems);

			frb.setTitle("Additional Info");
			frb.setTitleStyle(reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setTopMargin(5);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);

			sub.setDatasource(new DJDataSource("Additional Details", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));

			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;

	}

	private Subreport getBlkDetails(DcrReportBlockDetail dcrReportBlockDetail, boolean isProposed) {
		try {

			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn floor = ColumnBuilder.getNew().setColumnProperty("floorNo", String.class.getName())
					.setTitle("Floor").setWidth(160).setHeaderStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn builtUpArea = ColumnBuilder.getNew()
					.setColumnProperty("builtUpArea", BigDecimal.class.getName()).setTitle("Covered area").setWidth(160)
					.setStyle(reportService.getNumberStyle()).build();
			frb.addGlobalFooterVariable(builtUpArea, DJCalculation.SUM, reportService.getTotalNumberStyle());

			AbstractColumn occupancy = ColumnBuilder.getNew().setColumnProperty("occupancy", String.class.getName())
					.setTitle("Occupancy/Sub Occupancy").setWidth(160)
					.setHeaderStyle(reportService.getBldgDetlsHeaderStyle()).build();

			/*
			 * AbstractColumn dwellingUnits =
			 * ColumnBuilder.getNew().setColumnProperty("dwellingUnits",
			 * BigDecimal.class.getName())
			 * .setTitle("No. of Dwelling Units").setWidth(120).setStyle(reportService.
			 * getNumberStyle()).build(); frb.addGlobalFooterVariable(dwellingUnits,
			 * DJCalculation.SUM, reportService.getTotalNumberStyle());
			 */

			/*
			 * AbstractColumn carpetArea = ColumnBuilder.getNew()
			 * .setColumnProperty("carpetArea",
			 * BigDecimal.class.getName()).setTitle("Carpet Area in m²")
			 * .setWidth(120).setStyle(reportService.getNumberStyle()).build();
			 * frb.addGlobalFooterVariable(carpetArea, DJCalculation.SUM,
			 * reportService.getTotalNumberStyle());
			 */

			frb.addColumn(floor);
			frb.addColumn(builtUpArea);
			frb.addColumn(occupancy);
			// frb.addColumn(dwellingUnits);
//			frb.addColumn(carpetArea);

			if (dcrReportBlockDetail.getBlockNo() != null) {
				frb.setTitle("Block No " + dcrReportBlockDetail.getBlockNo() + " - Proposed Details");
			}

			Style titleStyle = reportService.getTitleStyle();
			titleStyle.setBorder(Border.THIN());
			frb.setTitleStyle(titleStyle);
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(0);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(titleStyle, reportService.getSubTitleStyle(), reportService.getColumnHeaderStyle(),
					reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			frb.setGrandTotalLegend(TOTAL);
			frb.setGrandTotalLegendStyle(reportService.getNumberStyle());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			if (isProposed) {
				sub.setDatasource(new DJDataSource("Block No " + dcrReportBlockDetail.getBlockNo(),
						DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			} else
				sub.setDatasource(new DJDataSource("Existing Block No " + dcrReportBlockDetail.getBlockNo(),
						DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getAdditionalDetailsV2() {
		try {
			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn description = ColumnBuilder.getNew().setColumnProperty("description", String.class.getName())
					.setTitle("Description").setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn noDescription = ColumnBuilder.getNew()
					.setColumnProperty("noDescription", String.class.getName()).setTitle("Additional Info")
					.setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn noOfItems = ColumnBuilder.getNew().setColumnProperty("noOfItems", BigDecimal.class.getName())
					.setTitle("No.").setWidth(160).setStyle(reportService.getTotalNumberStyle()).build();

			frb.addColumn(description);
			frb.addColumn(noDescription);
			frb.addColumn(noOfItems);

			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(0);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource("AdditionalDetails2", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getPaymentDetails(String title, String paymentDatasourceName, boolean isHeadingsRequired) {
		try {
			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn detailName = ColumnBuilder.getNew().setColumnProperty("detailName", String.class.getName())
					.setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn demandAmount = ColumnBuilder.getNew()
					.setColumnProperty("demandAmount", BigDecimal.class.getName()).setWidth(160)
					.setStyle(reportService.getNumberStyle()).build();

			AbstractColumn amountPaid = ColumnBuilder.getNew()
					.setColumnProperty("amountPaid", BigDecimal.class.getName()).setWidth(160)
					.setStyle(reportService.getNumberStyle()).build();

			if (isHeadingsRequired) {
				detailName.setTitle("Details of Fees and charges");
				demandAmount.setTitle("Amount in Rupees");
				amountPaid.setTitle("Adjusted Amount");
			}

			frb.addColumn(detailName);
			frb.addColumn(demandAmount);
			frb.addColumn(amountPaid);

			Style columnHeaderStyle = reportService.getColumnHeaderStyle();
			Style detailStyle = reportService.getDetailStyle();
			/*
			 * if(paymentDatasourceName.equals("Application Fee Details")) {
			 * columnHeaderStyle.setBackgroundColor(Color.decode("#C3CD26"));
			 * detailStyle.setBackgroundColor(Color.decode("#5ECD26"));
			 * columnHeaderStyle.setTextColor(Color.decode("#C3CD26"));
			 * columnHeaderStyle.setBackgroundColor(null); } else
			 * if(paymentDatasourceName.equals("Application Fee Details")){
			 * columnHeaderStyle.setBackgroundColor(Color.decode("#26CACD"));
			 * detailStyle.setBackgroundColor(Color.decode("#CD5926")); }
			 */

			frb.setTitle(title);
			Style titleStyle = reportService.getTitleStyle();
			titleStyle.setBorder(Border.THIN());
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(0);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(titleStyle, reportService.getSubTitleStyle(), columnHeaderStyle, detailStyle);
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource(paymentDatasourceName, DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getFarAndParkingDetails() {
		try {
			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn column1 = ColumnBuilder.getNew().setColumnProperty("key1", String.class.getName())
					.setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn column2 = ColumnBuilder.getNew().setColumnProperty("key2", String.class.getName())
					.setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn column3 = ColumnBuilder.getNew().setColumnProperty("key3", String.class.getName())
					.setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			frb.addColumn(column1);
			frb.addColumn(column2);
			frb.addColumn(column3);

			frb.setTitle("Grand Total FAR Area - 15871.93 Sqm.");
			frb.setSubtitle("Grand Total BUA - 15871.93 Sqm.");
			Style titleStyle = reportService.getTitleStyle();
			titleStyle.setBorder(Border.THIN());
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(0);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(titleStyle, titleStyle, reportService.getColumnHeaderStyle(),
					reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource("FarAndParkingDetails", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getSetBacks() {
		try {
			FastReportBuilder frb = new FastReportBuilder();

			AbstractColumn column1 = ColumnBuilder.getNew().setColumnProperty("setBackName", String.class.getName())
					.setTitle("Item").setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle()).build();

			AbstractColumn column2 = ColumnBuilder.getNew().setColumnProperty("requiredSetback", String.class.getName())
					.setTitle("Required(in Mtr)").setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle())
					.build();

			AbstractColumn column3 = ColumnBuilder.getNew().setColumnProperty("providedSetback", String.class.getName())
					.setTitle("Provided (in Mtr)").setWidth(160).setStyle(reportService.getBldgDetlsHeaderStyle())
					.build();

			frb.addColumn(column1);
			frb.addColumn(column2);
			frb.addColumn(column3);

			frb.setTitle("Set backs approved to be provided");
			Style titleStyle = reportService.getTitleStyle();
			titleStyle.setBorder(Border.THIN());
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(20);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(titleStyle, titleStyle, reportService.getColumnHeaderStyle(),
					reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource("SetbackDetails", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getNocs() {
		FastReportBuilder frb = new FastReportBuilder();
		String title = "NOCs/ Clearances submitted:\\n1.NOC from Airport Authority of India\\n2.Environmental Clearance from SEIAA\\n3.Fire recommendations from Fire Prevention Wing\\n4.NOC from CGWA.";
		frb.setWhenNoData(title, reportService.getTitleStyle());
		frb.setHeaderHeight(5);
		frb.setLeftMargin(25);
		frb.setTopMargin(10);
		frb.setBottomMargin(0);
		frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
				reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
		frb.setAllowDetailSplit(false);
		frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
		DynamicReport build = frb.build();
		Subreport sub = new Subreport();
		sub.setDynamicReport(build);
		Style style = new Style();
		style.setStretchWithOverflow(true);
		style.setStreching(RELATIVE_TO_BAND_HEIGHT);
		sub.setStyle(style);
		sub.setDatasource(new DJDataSource("Nocs", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
		sub.setLayoutManager(new ClassicLayoutManager());
		return sub;
	}

	private Subreport getPaymentTotal() {
		try {
			FastReportBuilder frb = new FastReportBuilder();
			String title = "Total Fees Paid : 36134";
			Style titleStyle = reportService.getTitleStyle();
			titleStyle.setBorder(Border.THIN());
			frb.setWhenNoData(title, titleStyle);
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(0);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(titleStyle, reportService.getSubTitleStyle(), reportService.getColumnHeaderStyle(),
					reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource("paymentTotal", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getConditions() {
		try {
			FastReportBuilder frb = new FastReportBuilder();
			String conditions = "1. The building shall be used exclusively for MultistoriedResidential Apartment with Community building purpose and the use shall not be changed to any other use without prior approval of this Authority.\\n2. The development shall be undertaken strictly according to plans enclosed with necessary permission endorsement.\\n3. Total Parking space measuring 6798.03Sqm (in Basement/ Ground and Open) as shown in the approved plan shall be left for parking of vehicles and no part of it will be used for any other purpose.\\n4. The land over which construction is proposed is accessible by an approved means of access of 31.096 Mtr.(Thirty one point zero nine six meter) in width.\\n5. The land in question must be in lawful ownership and peaceful possession of the applicant.\\n6. The applicant shall free gift 143.41sqm wide strip of land to Bhubaneswar Development Authority/ULB for further widening of the road to the standard width as per CDP-2010, BDA.\\n7. The permission granted under these regulations shall remain valid upto three years from the date of issue.However the permission shall have to be revalidated before theexpiry of the above period on payment of such fee as may be prescribed under rules and such revalidation shall be valid for one year.\\n8.	(i) Approval of plans and acceptance of any statement or document pertaining to such plan shall not exempt the owner or person or persons under whose supervision the building is constructed from their responsibilities imposed under ODA (Planning & Building Standards) Rules 2020, or under any other law for the time being in force.\\n	(ii) Approval of plan would mean granting of permission to construct under these regulations in force only and shall not mean among other things-\\n		(a) The title over the land or building\\n		(b) Easement rights\\n		(c) Variation in area from recorded area of a plot or a building\\n		(d) Structural stability\\n		(e) Workmanship and soundness of materials used in the construction of the buildings\\n		(f) Quality of building services and amenities in the construction of the building,\\n		(g) the site/area liable to flooding as a result of not taking proper drainage arrangement as per the natural lay of the land, etc and\\n		(h) Other requirements or licenses or clearances required to be obtained for the site /premises or activity under various other laws. APPROVED BY BHUBANESWAR DEVELOPMENT AUTHORITY\\n9. In case of any dispute arising out of land record or in respect of right, title, interest after this permission is granted, the permission so granted shall be treated as automatically cancelled during the period of dispute.\\n10. Neither granting of the permit nor the approval of the drawing and specifications, nor inspections made by the Authority during erection of the building shall in any way relieve the owner of such building from full responsibility for carrying out the work in accordance with the requirements of NBC 2005 and these regulations.\\n11. The owner /applicant shall;\\n		(a) Permit the Authority to enter the building or premises, for which the permission has been granted at any reasonable time for the purpose of enforcing the regulations;\\n		(b) Obtain, wherever applicable, from the competent Authority permissions/clearance required in connection with the proposed work;\\n		(c) Give written notice to the Authority before commencement of work on building site in Form-V,periodic progress report in Form-VIII, notice of completion in Form-VI and notice in case of termination of services of Technical persons engaged by him.\\n		(d) Obtain an Occupancy Certificate from the Authority prior to occupation of building in full or part.\\n12. The applicant shall abide by the provisions of Rule no.15 of ODA (P&BS)Rules, 2020 with regard to third party verification at plinth level, ground level & roof level. Any deviation to the above shall attract penalty as per the provision of the same.\\n13. (a) In case the full plot or part thereof on which permission is accorded is agricultural kisam, the same may be converted to non-agricultural kisam under section8 of OLR Act before commencement of construction.\\n	(b) The owner/applicant shall get the structural plan and design vetted by the institutions identified by the Authority for buildings more than 30 mtr height before commencement of construction.\\n14.Wherever tests of any material are made to ensure conformity of the requirements of the regulations in force,records of the test data shall be kept available for inspection during the construction of building and for such period thereafter as required by the Authority.\\n15.The persons to whom a permit is issued during construction shall keep pasted in a conspicuous place on the property in respect of which the permit was issued;\\n		(a) A copy of the building permit; and\\n		(b) A copy of approved drawings and specifications.\\n16. If the Authority finds at any stage that the construction is not being carried on according to the sanctioned plan or is in violations of any of the provisions of these regulations, it shall notify the owner and no further construction shall beallowed until necessary corrections in the plan are made and the corrected plan is approved.The applicant during the course of construction and till issue of occupancy certificate shall place a display board on his site with details and declaration. APPROVED BY BHUBANESWAR DEVELOPMENT AUTHORITY\\n17. This permission is accorded on deposit /submission of the following;";
			frb.setWhenNoData(conditions, new Style());
			// frb.setTitle(conditions);
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(5);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource("Conditions", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getOtherConditionsDeclaration() {
		try {
			FastReportBuilder frb = new FastReportBuilder();
			String conditions = "If not paid within such time as mentioned above, then interest rate of SBI PLR shall be Imposed and occupancy certificate shall not be issued without realizing the total amount including interest.\\n18. Other conditions to be complied by the applicant are as per the following;";
			frb.setWhenNoData(conditions,new Style());
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(10);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(
					new DJDataSource("OtherConditionsDeclaration", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
	}

	private Subreport getOtherConditions() {
		try {
			FastReportBuilder frb = new FastReportBuilder();
			String conditions = "I. The owner/applicant/Technical Person shall strictly adhere to the terms and conditions imposed in the NOC/Clearances given by Fire Prevention officer/National Airport Authority/SEIAA, Ministry of Forest & Environment/PHED etc wherever applicable.\\nII. Storm water from the premises of roof top shall be conveyed and discharged to the rain water recharging pits as per Rule 47 of ODA (Planning & Building Standards) Rules 2020.\\nIII. The space which is meant for parking shall not be changed to any other use and shall not be partitioned/ closed in any manner.\\nIV. 30% of the parking space in group housing/apartment building shall be exclusively earmarked for ambulance, fire tender, physically handicapped persons andoutside visitors withsignage as per norms under Rule 37 of ODA (Planning & Building Standards) Rules 2020.\\nV. Plantation for one tree per 80 sqm of plot area made by the applicant as per provision under Rule 30 of ODA (Planning & Building Standards) Rules 2020.\\nVI. If the construction / development are not as per the approved plan / deviated beyond permissible norms, the performance security shall be forfeited and action shall be initiated against the applicant/builder / developer as per the provisions of the ODA Act, 1982 Rules and Regulations made there under\\nVII. The Owner/ Applicant/Architect/Structural Engineer are fully and jointly responsible for any structural failure of building due to any structural/construction defects, Authority will be no way be held responsible for the same in what so ever manner.\\nVIII. The concerned Architect / Applicant / Developer are fully responsible for any deviations additions & alternations beyond approved plan/ defective construction etc.shall be liable for action as per the provisions of the Regulation. APPROVED BY BHUBANESWAR DEVELOPMENT AUTHORITY\\nIX. The applicant shall obtain infrastructural specification and subsequent clearance with regard to development of infrastructure from BMC/BDA before commencement of construction.\\nX. All the stipulated conditions of the NOC/Clearances given by CE-CumEngineer Member, BDA& PHED shall be adhered to strictly. All the fire fighting installation etc are to be ensured and maintained by the applicant as per NBC 2016.\\nXI. No storm water/water shall be discharged to the public road/public premises and other adjoining plots.\\nXII. The applicant shall abide by the terms and conditions of the NOC given by CGWA, Airport Authority, SEIAA and Fire Safety Recommendations, EIDP vetting by CE-cum-EM, BDA as well as structural vetting.\\nXIII. Adhere to the provisions of BDA (Planning & Building Standards) Regulation strictly and conditions thereto.\\nXIV. All the passages around the building shall be developed with permeable pavers block for absorption of rain water and seepage in to the ground.\\nXV. Rain water harvesting structure and recharging pits of adequate capacity shall be developed to minimize the storm water runoff to the drain\\nXVI. The applicant shall make own arrangement of solid waste management through micro compost plant within the project premises\\nXVII. The applicant shall register this project before the ORERA as per affidavit submitted before commencement of work.\\nXVIII. The applicant shall install Rooftop P.V. system as per BDA Regulations.\\nXIX. The applicant shall free gift the road affected area to Government/BDA as and when required by the government for development of road.\\nXX. The Authority shall in no way be held responsible for any structural failure and damage due to earthquake/cyclone/any other natural disaster.\\nXXI. The number of dwelling units so approved shall not be changed in any manner.\\nXXII. Lift shall be provided as per the provision of NBCI, 2016 in pursuance with note(ii) of sub-rule (2) of Rule 42 of ODA Rules, 2020. If the same isn’t provided by the applicant, appropriate action shall be taken as per law.";
			frb.setWhenNoData(conditions, reportService.getTitleStyle());
			frb.setHeaderHeight(5);
			frb.setLeftMargin(25);
			frb.setTopMargin(10);
			frb.setBottomMargin(0);
			frb.setDefaultStyles(reportService.getTitleStyle(), reportService.getSubTitleStyle(),
					reportService.getColumnHeaderStyle(), reportService.getDetailStyle());
			frb.setAllowDetailSplit(false);
			frb.setPageSizeAndOrientation(Page.Page_A4_Portrait());
			DynamicReport build = frb.build();
			Subreport sub = new Subreport();
			sub.setDynamicReport(build);
			Style style = new Style();
			style.setStretchWithOverflow(true);
			style.setStreching(RELATIVE_TO_BAND_HEIGHT);
			sub.setStyle(style);
			sub.setDatasource(new DJDataSource("OtherConditions", DJConstants.DATA_SOURCE_ORIGIN_PARAMETER, 0));
			sub.setLayoutManager(new ClassicLayoutManager());
			return sub;
		} catch (ColumnBuilderException e) {
			LOG.error(e.getMessage(), e);
		}
		return null;
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
					//TODO: NA to be replaced by sub occupancy of that block-
					dcrReportBlockDetail.setBlockNo(block.getNumber() + " ("+noOfFloor+" NA)");
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

	private VirtualBuildingReport buildVirtualBuilding(VirtualBuilding virtualBuilding) {
		VirtualBuildingReport virtualBuildingReport = new VirtualBuildingReport();

		if (virtualBuilding != null) {
			if (virtualBuilding.getTotalExistingBuiltUpArea() != null) {
				virtualBuildingReport.setProposedBuitUpArea(
						virtualBuilding.getTotalBuitUpArea().subtract(virtualBuilding.getTotalExistingBuiltUpArea()));
				virtualBuildingReport.setProposedFloorArea(
						virtualBuilding.getTotalFloorArea().subtract(virtualBuilding.getTotalExistingFloorArea()));
				virtualBuildingReport.setProposedCarpetArea(
						virtualBuilding.getTotalCarpetArea().subtract(virtualBuilding.getTotalExistingCarpetArea()));
			}
			virtualBuildingReport.setTotalExistingBuiltUpArea(virtualBuilding.getTotalExistingBuiltUpArea());

			virtualBuildingReport.setTotalExistingFloorArea(virtualBuilding.getTotalExistingFloorArea());
			virtualBuildingReport.setTotalExistingCarpetArea(virtualBuilding.getTotalExistingCarpetArea());

			virtualBuildingReport.setTotalCoverageArea(virtualBuilding.getTotalCoverageArea());

			virtualBuildingReport.setTotalBuitUpArea(virtualBuilding.getTotalBuitUpArea());
			virtualBuildingReport.setTotalFloorArea(virtualBuilding.getTotalFloorArea());
			virtualBuildingReport.setTotalCarpetArea(virtualBuilding.getTotalCarpetArea());

			virtualBuildingReport.setTotalConstructedArea(virtualBuilding.getTotalConstructedArea());
		}
		return virtualBuildingReport;
	}

	private List<OccupancyReport> buildSubOccupanciesReport(Map<String, OccupancyPercentage> occupancyPercentage) {
		List<OccupancyReport> occupanciesReport = new ArrayList<OccupancyReport>();
		if (occupancyPercentage != null) {
			for (String oc : occupancyPercentage.keySet()) {
				OccupancyPercentage ocp = occupancyPercentage.get(oc);
				OccupancyReport ort = new OccupancyReport();
				ort.setOccupancy(ocp.getOccupancy());
				ort.setSubOccupancy(ocp.getSubOccupancy());
				ort.setPercentage(ocp.getPercentage());
				occupanciesReport.add(ort);
			}
		}
		return occupanciesReport;
	}
}
