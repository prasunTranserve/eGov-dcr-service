package org.egov.edcr.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.egov.common.entity.edcr.Block;
import org.egov.common.entity.edcr.DcrReportBlockDetail;
import org.egov.common.entity.edcr.DcrReportFloorDetail;
import org.egov.common.entity.edcr.OdishaParkingHelper;
import org.egov.common.entity.edcr.Plan;
import org.egov.common.entity.edcr.SetBack;
import org.egov.common.entity.edcr.TypicalFloor;
import org.egov.edcr.constants.DxfFileConstants;
import org.egov.edcr.feature.Parking;
import org.egov.edcr.od.OdishaUtill;
import org.egov.infra.exception.ApplicationRuntimeException;
import org.egov.infra.microservice.models.RequestInfo;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.List;
import com.itextpdf.text.ListItem;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PermitOrderServiceBPA1 extends PermitOrderService {
	
	private static final Logger LOG = Logger.getLogger(PermitOrderServiceBPA1.class);

	public static String PARAGRAPH_ONE = "Permission under sub-section (3) of the Section-16 of the Odisha Development Authorities Act, 1982 is hereby granted in favour of;";
	public static String ADDRESS = "LAXMIPRIYA PANDA AND OTHERS,";
	public static String ADDRESS2 = "KALARAHANGA";
	public static String ADDRESS1 = "INJANA,";

	public static String PARAGRAPH_1_1 = "For %s of a ";
//	public static String PARAGRAPH_1_2 = "S+2 ";
	public static String PARAGRAPH_1_3 = "storeyed ";
//	public static String PARAGRAPH_1_4 = "Residential ";
	public static String PARAGRAPH_1_5 = "Building in respect of plot No.";
//	public static String PARAGRAPH_1_6 = "1337, ";
	public static String PARAGRAPH_1_7 = "Khata No.";
//	public static String PARAGRAPH_1_8 = "180 ";
	public static String PARAGRAPH_1_9 = "Village/Mouza.";
//	public static String PARAGRAPH_1_10 = "Injana ";
	public static String PARAGRAPH_1_11 = "of %s within the Development Plan Area subject to following conditions/ restrictions: ";

	public static String PARAGRAPH_2_1 = "The land/ Building shall be used exclusively for %s purpose and the uses shall not be changed to any other use without prior approval of this Authority.\n\n";
	public static String PARAGRAPH_2_2 = "The development shall be undertaken strictly according to plans enclosed with necessary permission endorsement.\n\n";
	public static String PARAGRAPH_2_3_1 = "Parking space measuring ";
//	public static String PARAGRAPH_2_3_2 = "134.51 ";
	public static String PARAGRAPH_2_3_3 = "sq. mtr. as shown in the approved plan shall be exclusively used for parking and no part of it will be used for any other purpose.\n\n";
	public static String PARAGRAPH_2_4 = "The land over which construction is proposed is accessible by an approved means of access of %s mtr. width.\n\n";
	public static String PARAGRAPH_2_5 = "The land in question must be in lawful ownership and peaceful possession of the applicant.\n\n";
	public static String PARAGRAPH_2_6 = "The applicant shall free gift %s sq.mtr. of located in the %s for the widening of the road/construction of new roads and other public amenities prior to completion of the development as indicated in the plan.\n\n";
	public static String PARAGRAPH_2_7_1 = "The permission is valid for period of ";
	public static String PARAGRAPH_2_7_2 = "three years ";
	public static String PARAGRAPH_2_7_3 = "with effect from the date of issue.\n\n";
	public static String PARAGRAPH_2_8 = "Permission accorded under the provision of Section 16 of ODA Act, cannot be construed as an evidence to claim right title interest on the plot on which the permission has been granted.\n\n";
	public static String PARAGRAPH_2_9 = "If any dispute arises with respect to right, title interest on the land on which the permission has been granted, the permission so granted shall be automatically treated as canceled during the period of dispute.\n\n";
	public static String PARAGRAPH_2_10 = "Any construction and development made by the applicant or owner on the disputed land will be at his risk without any legal or financial liability on the Authority.\n\n";
	public static String OTHER_CONDITIONS = "Other conditions to be complied by the applicant are as per the following:\n";
	public static String PARAGRAPH_2_11_1 = "The ";
//	public static String PARAGRAPH_2_11_2 = "S+2 residential building ";
	public static String PARAGRAPH_2_11_3 = "is approved on payment following fees:\n\n";
//	public static String PARAGRAPH_2_11_3_A = "Sanction fee : Rs 6,689.00/-only";
	// public static String PARAGRAPH_2_11_3_B = "Construction Workers Welfare Cess
	// : Rs 83,967.00/-only";
	public static String PARAGRAPH_3_1 = "Total plot area (As per document) : ";
	// public static String PARAGRAPH_3_2 = "607.24 sqmt.";
	public static String PARAGRAPH_4_1 = "Total plot area (As per Possession) : ";
	// public static String PARAGRAPH_4_2 = "563.11 sqmt.";
	public static String PARAGRAPH_5_1 = "Total built up area : ";
	// public static String PARAGRAPH_5_2 = "445.92 sqmt.";
	public static String PARAGRAPH_6_1 = "Total FAR area : ";
	// public static String PARAGRAPH_6_2 = "311.41 sqmt.";
	public static String PARAGRAPH_7_1 = "FAR : ";
	// public static String PARAGRAPH_7_2 = "0.55";

	private Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA, 16, Font.BOLD);
	private Font font1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
	private Font fontBold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
	private Font fontBoldUnderlined = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.UNDERLINE);
	
	@Override
	public InputStream generateReport(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo) {
		try {
			return createPdf(plan, bpaApplication, requestInfo);
		} catch (Exception e) {
			e.printStackTrace();
			throw new ApplicationRuntimeException("Error while generating permit order pdf", e);
		}
	}

	public InputStream createPdf(Plan plan, LinkedHashMap bpaApplication, RequestInfo requestInfo) throws Exception {
		
		String tenantIdActual = getValue(bpaApplication, "tenantId");
		Document document = new Document();
		ByteArrayOutputStream outputBytes;
		outputBytes = new ByteArrayOutputStream();
		PdfWriter.getInstance(document, outputBytes);
		document.open();
		Image logo = getLogo();
		String[] ulbGradeNameAndUlbArray = getUlbNameAndGradeFromMdms(requestInfo, tenantIdActual);
		String ulbGradeNameAndUlb = (ulbGradeNameAndUlbArray[0] + " " + ulbGradeNameAndUlbArray[1]);
		
		Map<String, Object> additionalDetails = getAdditionalDetailsMap(bpaApplication);
		//Cuttack Municipal Corporation
		Paragraph headerTitle = new Paragraph(ulbGradeNameAndUlb, fontHeader);
		headerTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		//Form-II (Order for Grant of Permission)
		Paragraph headerSubTitle = new Paragraph("Form-II (Order for Grant of Permission)", font1);
		headerSubTitle.setAlignment(Paragraph.ALIGN_CENTER);
		
		String tenantId = StringUtils.capitalize(tenantIdActual.split("\\.")[1]);
		@SuppressWarnings("deprecation")
		Date date = new Date(Long.valueOf(getValue(bpaApplication, "approvalDate")));
		DateFormat format = new SimpleDateFormat("dd/MM/yyyy");
		String approvalNo = getValue(bpaApplication, "approvalNo");
		String approvalDate = format.format(date);
		Paragraph headerSubTitle2 = new Paragraph(
				"Letter No. " + approvalNo + ", " + tenantId + ", Dated: " + approvalDate,
				fontBold);
		headerSubTitle2.setAlignment(Paragraph.ALIGN_CENTER);
		
		String applicationNo = getValue(bpaApplication, "applicationNo");
		Paragraph headerSubTitle3 = new Paragraph("Sujog-OBPS APPLICATION NO. " + applicationNo, fontBoldUnderlined);
		headerSubTitle3.setAlignment(Paragraph.ALIGN_CENTER);

		Paragraph paragraph1 = new Paragraph(PARAGRAPH_ONE, font1);

		Paragraph addressed = new Paragraph("Smt. /Shri,", fontBold);

		String ownersNamesCsv = getNameOfOwner(bpaApplication);
		
		Paragraph applicants = new Paragraph(ownersNamesCsv + ",", fontBold);
		applicants.setIndentationLeft(30);

		String localityName = getValue(bpaApplication, "$.landInfo.address.locality.name");
		Paragraph address2 = new Paragraph(localityName + ",", fontBold);
		address2.setIndentationLeft(30);

		// to show correspondenceAddress from owner details--
		String primaryOwnerCorrespondenceAddress = getCorrespondenceAddress(bpaApplication);
		Paragraph address3 = new Paragraph(primaryOwnerCorrespondenceAddress, fontBold);
		address3.setIndentationLeft(30);
		
		Chunk forServiceType = new Chunk(String.format("For %s of a ", getServiceType(plan)), font1);
		String floorInfo = plan.getPlanInformation().getFloorInfo() + " ";
		Chunk floorInform = new Chunk(floorInfo, fontBold);
		Chunk storeyed = new Chunk(PARAGRAPH_1_3, font1);
		String subOccupancy = plan.getPlanInformation().getSubOccupancy() + " ";
		Chunk subOccupanc = new Chunk(subOccupancy, fontBold);
		Chunk buildingInRespectOf = new Chunk(PARAGRAPH_1_5, font1);
		String plotNo = plan.getPlanInformation().getPlotNo() + " ";
		Chunk pltoNumber = new Chunk(plotNo, fontBold);
		Chunk chunk7 = new Chunk(PARAGRAPH_1_7, font1);
		String khataNo = plan.getPlanInformation().getKhataNo() + " ";
		Chunk khataNumber = new Chunk(khataNo, fontBold);
		Chunk village = new Chunk(PARAGRAPH_1_9, font1);
		Chunk locality = new Chunk(localityName + " ", fontBold);
		Chunk withinDevelopmentPlanArea = new Chunk(String.format(PARAGRAPH_1_11, tenantId), font1);

		Phrase paragraph2 = new Phrase();
		paragraph2.add(forServiceType);
		paragraph2.add(floorInform);
		paragraph2.add(storeyed);
		paragraph2.add(subOccupanc);
		paragraph2.add(buildingInRespectOf);
		paragraph2.add(pltoNumber);
		paragraph2.add(chunk7);
		paragraph2.add(khataNumber);
		paragraph2.add(village);
		paragraph2.add(locality);
		paragraph2.add(withinDevelopmentPlanArea);
		Paragraph secondPara = new Paragraph(paragraph2);

		List list1 = new List(List.ORDERED, List.ALPHABETICAL);
		ListItem list1Item1 = new ListItem();
		Phrase landUsePhrase = new Phrase(String.format(PARAGRAPH_2_1, subOccupancy), font1);
		list1Item1.add(landUsePhrase);
		ListItem list1Item2 = new ListItem();
		Phrase developmentUndertakenPhrase = new Phrase(PARAGRAPH_2_2, font1);
		list1Item2.add(developmentUndertakenPhrase);
		ListItem list1Item3 = new ListItem();
		Chunk chunk15 = new Chunk(PARAGRAPH_2_3_1, font1);
		Chunk chunk16 = new Chunk(plan.getPlanInformation().getTotalParking() + " ", fontBold);
		Chunk chunk17 = new Chunk(PARAGRAPH_2_3_3, font1);
		Phrase chunk18 = new Phrase();
		chunk18.add(chunk15);
		chunk18.add(chunk16);
		chunk18.add(chunk17);
		list1Item3.add(chunk18);
		ListItem list1Item4 = new ListItem();
		BigDecimal roadWidth = plan.getPlanInformation().getTotalRoadWidth();
		Phrase chunk19 = new Phrase(String.format(PARAGRAPH_2_4, roadWidth), font1);
		list1Item4.add(chunk19);
		ListItem list1Item5 = new ListItem();
		Phrase chunk20 = new Phrase(PARAGRAPH_2_5, font1);
		list1Item5.add(chunk20);
		ListItem list1Item6 = new ListItem();
		String fContent = String.format(PARAGRAPH_2_6, getGiftedArea(plan).toString(),ulbGradeNameAndUlb);
		Phrase chunk21 = new Phrase(fContent, font1);
		list1Item6.add(chunk21);
		ListItem list1Item7 = new ListItem();
		Chunk chunk22 = new Chunk(PARAGRAPH_2_7_1, font1);
		Chunk chunk23 = new Chunk(PARAGRAPH_2_7_2, fontBold);
		Chunk chunk24 = new Chunk(PARAGRAPH_2_7_3, font1);
		Phrase chunk25 = new Phrase();
		chunk25.add(chunk22);
		chunk25.add(chunk23);
		chunk25.add(chunk24);
		list1Item7.add(chunk25);
		ListItem list1Item8 = new ListItem();
		Phrase chunk26 = new Phrase(PARAGRAPH_2_8, font1);
		list1Item8.add(chunk26);
		ListItem list1Item9 = new ListItem();
		Phrase chunk27 = new Phrase(PARAGRAPH_2_9, font1);
		list1Item9.add(chunk27);
		ListItem list1Item10 = new ListItem();
		Phrase chunk28 = new Phrase(PARAGRAPH_2_10, font1);
		list1Item10.add(chunk28);
		ListItem otherConditionsItem = new ListItem();
		Phrase OtherConditionsPhrase = new Phrase(OTHER_CONDITIONS, font1);
		// dynamic Other conditions from additionalDetails of application--
		if (Objects.nonNull(additionalDetails)
				&& Objects.nonNull(additionalDetails.get("otherConditionsForPermitCertificate"))) {
			Chunk otherConditions = new Chunk(
					String.valueOf(additionalDetails.get("otherConditionsForPermitCertificate"))+"\n\n", font1);
			otherConditionsItem.add(OtherConditionsPhrase);
			otherConditionsItem.add(otherConditions);
		}
		ListItem list1Item11 = new ListItem();
		Chunk chunk29 = new Chunk(PARAGRAPH_2_11_1, font1);
		Chunk chunk30 = new Chunk(
				plan.getPlanInformation().getFloorInfo() + " " + plan.getPlanInformation().getSubOccupancy() + " ",
				fontBold);
		Chunk chunk31 = new Chunk(PARAGRAPH_2_11_3, font1);
		Phrase chunk32 = new Phrase();
		chunk32.add(chunk29);
		chunk32.add(chunk30);
		chunk32.add(chunk31);
		list1Item11.add(chunk32);
		List payments = new List(List.UNORDERED);
		
		// call collection-services to fetch payment details
		java.util.List<Map<String,Object>> permitFeeBillAccountDetails = getPermitFeeBillAccountDetails(requestInfo, applicationNo, tenantIdActual);
		for(Map<String,Object> billAccountDetail:permitFeeBillAccountDetails) {
			String adjustedAmount = String.valueOf(billAccountDetail.get("adjustedAmount"));
			//skip those taxheadcodes for which adjustedAmount is 0-
			if(StringUtils.isNotEmpty(adjustedAmount) && "0.0".equals(adjustedAmount))
				continue;
			String taxHeadCode = String.valueOf(billAccountDetail.get("taxHeadCode"));
			String taxHeadName = getFeeComponentNameFromTaxHeadCode(taxHeadCode);
			
			ListItem individualPaymentSentence = new ListItem();
			Phrase paymentLine = new Phrase();
			Chunk paymentSentencePre = new Chunk(taxHeadName+" : Rs ", font1);
			Chunk paymentSentence = new Chunk(adjustedAmount, font1);
			Chunk paymentSentencePost = new Chunk("/-Only", font1);
			paymentLine.add(paymentSentencePre);
			paymentLine.add(paymentSentence);
			paymentLine.add(paymentSentencePost);
			// add reason for adjustment/other fee-
			if (TAXHEAD_BPA_SANC_ADJUSTMENT_AMOUNT_CODE.equalsIgnoreCase(taxHeadCode)) {
				String modificationReasonSanctionFeeAdjustmentAmount = getValue(bpaApplication,
						"$.additionalDetails.modificationReasonSanctionFeeAdjustmentAmount");
				paymentLine.add(new Chunk(" (" + modificationReasonSanctionFeeAdjustmentAmount + ")", font1));
			}
			individualPaymentSentence.add(paymentLine);
			payments.add(individualPaymentSentence);
		}
		
		list1Item11.add(payments);
		list1.add(list1Item1);
		list1.add(list1Item2);
		list1.add(list1Item3);
		list1.add(list1Item4);
		list1.add(list1Item5);
		list1.add(list1Item6);
		list1.add(list1Item7);
		list1.add(list1Item8);
		list1.add(list1Item9);
		list1.add(list1Item10);
		list1.add(otherConditionsItem);
		list1.add(list1Item11);

		BigDecimal plotAreaAsPerDeclaration = plan.getPlot().getArea();
		Chunk chunk35 = new Chunk(PARAGRAPH_3_1, font1);
		Chunk chunk36 = new Chunk(plotAreaAsPerDeclaration + SQM, fontBold);
		Paragraph chunk37 = new Paragraph();
		chunk37.add(chunk35);
		chunk37.add(chunk36);

		BigDecimal plotAreaAsPerPossession = plotAreaAsPerDeclaration;
		Chunk chunk38 = new Chunk(PARAGRAPH_4_1, font1);
		// TODO: to check which parameter in scrutiny gives occupied plot area
		Chunk chunk39 = new Chunk(plotAreaAsPerPossession + SQM, fontBold);
		Paragraph chunk40 = new Paragraph();
		chunk40.add(chunk38);
		chunk40.add(chunk39);

		Chunk chunk41 = new Chunk(PARAGRAPH_5_1, font1);
		Chunk chunk42 = new Chunk(plan.getVirtualBuilding() != null
				? plan.getVirtualBuilding().getTotalBuitUpArea().setScale(2, BigDecimal.ROUND_UP) + SQM
				: "0", fontBold);
		Paragraph chunk43 = new Paragraph();
		chunk43.add(chunk41);
		chunk43.add(chunk42);

		Chunk chunk44 = new Chunk(PARAGRAPH_6_1, font1);
		BigDecimal providedFar = BigDecimal.valueOf(plan.getFarDetails().getProvidedFar());
		Chunk chunk45 = new Chunk(
				plan.getVirtualBuilding().getTotalFloorArea().setScale(2, BigDecimal.ROUND_UP) + SQM,
				fontBold);
		Paragraph chunk46 = new Paragraph();
		chunk46.add(chunk44);
		chunk46.add(chunk45);

		Chunk chunk47 = new Chunk(PARAGRAPH_7_1, font1);
		Chunk chunk48 = new Chunk(plan.getFarDetails().getProvidedFar() + "", fontBold);
		Paragraph chunk49 = new Paragraph();
		chunk49.add(chunk47);
		chunk49.add(chunk48);

		PdfPTable table1 = new PdfPTable(6);
		table1.setLockedWidth(false);
		table1.setWidthPercentage(100f);
		addTableHeader1(table1);
		addRows1(table1, plan);

		Image qrCode = getQrCode(ownersNamesCsv, getValue(bpaApplication, "approvalNo"), approvalDate, getValue(bpaApplication, "edcrNumber"));

		document.add(qrCode);
		document.add(logo);
		document.add(headerTitle);
		document.add(Chunk.NEWLINE);
		document.add(headerSubTitle);
		document.add(headerSubTitle2);
		document.add(headerSubTitle3);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(paragraph1);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(addressed);
		document.add(applicants);
		document.add(address2);
		document.add(address3);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(secondPara);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(list1);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(chunk37);
		document.add(chunk40);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(chunk43);
		document.add(chunk46);
		document.add(chunk49);
		document.add(Chunk.NEWLINE);
		document.add(Chunk.NEWLINE);
		document.add(table1);
		document.close();
		return new ByteArrayInputStream(outputBytes.toByteArray());
	}

	private void addRows1(PdfPTable table1, Plan plan) {

		java.util.List<DcrReportBlockDetail> blockDetails = buildBlockWiseProposedInfo(plan);

		int rowSpanHelper = 0;
		for (DcrReportBlockDetail block : blockDetails) {

			java.util.List<DcrReportFloorDetail> floorDetails = block.getDcrReportFloorDetails();
			for (DcrReportFloorDetail floor : floorDetails) {
				rowSpanHelper++;
			}
			rowSpanHelper++;
		}

		Font fontPara1 = FontFactory.getFont(FontFactory.HELVETICA, 12);
		PdfPCell cell1 = new PdfPCell();
		Phrase cellValue1 = new Phrase("Stilt (Parking)", fontPara1);
		cell1.addElement(cellValue1);
		// String stiltParkingProvided=plan.getpa
		Parking parking = new Parking();
		OdishaParkingHelper parkingData = parking.prepareParkingData(plan);
		BigDecimal stiltParking = parkingData.stiltParkingProvided;
		PdfPCell cell2 = new PdfPCell();
		Phrase cellValue2 = new Phrase(stiltParking + "", fontPara1);
		cell2.addElement(cellValue2);
		PdfPCell cell3 = new PdfPCell();
		
		Phrase cellValue3 = new Phrase("", fontPara1);
		int rowSpanParking = 2;
		//set rowspanhelper as rowspan has to be only 2 and then for each block-
		rowSpanHelper=0;
		cell3.setRowspan(rowSpanParking + rowSpanHelper);
		cell3.addElement(cellValue3);
		PdfPCell cell4 = new PdfPCell();
		Phrase cellValue4 = new Phrase("", fontPara1);
		cell4.setRowspan(rowSpanParking + rowSpanHelper);
		cell4.addElement(cellValue4);
		PdfPCell cell5 = new PdfPCell();
		Phrase cellValue5 = new Phrase("", fontPara1);
		cell5.setRowspan(rowSpanParking + rowSpanHelper);
		cell5.addElement(cellValue5);
		PdfPCell cell6 = new PdfPCell();
		Phrase cellValue6 = new Phrase("", fontPara1);
		cell6.setRowspan(rowSpanParking + rowSpanHelper);
		cell6.addElement(cellValue6);
		PdfPCell cell7 = new PdfPCell();
		Phrase cellValue7 = new Phrase("Stilt (Services)", fontPara1);
		BigDecimal totalAreaStiltFloor = OdishaUtill.getStiltArea(plan);
		cell7.addElement(cellValue7);
		PdfPCell cell8 = new PdfPCell();
		Phrase cellValue8 = new Phrase(totalAreaStiltFloor + "", fontPara1);
		cell8.addElement(cellValue8);

		table1.addCell(cell1);
		table1.addCell(cell2);
		table1.addCell(cell3);
		table1.addCell(cell4);
		table1.addCell(cell5);
		table1.addCell(cell6);
		table1.addCell(cell7);
		table1.addCell(cell8);

		for (DcrReportBlockDetail block : blockDetails) {
			PdfPCell blockNameCell = new PdfPCell();
			Phrase blockNamephrase = new Phrase("Block " + block.getBlockNo() + " Details", fontPara1);
			blockNameCell.setColspan(2);
			blockNameCell.addElement(blockNamephrase);
			table1.addCell(blockNameCell);
			
			//my code starts-
			Map<String, BigDecimal> setBackData = getSetBackData(plan,plan.getBlockByName(block.getBlockNo()));
			BigDecimal frontSetbackProvided = setBackData.get("frontSetbackProvided");
			BigDecimal rearSetbackProvided = setBackData.get("rearSetbackProvided");
			BigDecimal leftSetbackProvided = setBackData.get("leftSetbackProvided");
			BigDecimal rightSetbackProvided = setBackData.get("rightSetbackProvided");

			int noOfFloors=0;
			java.util.List<DcrReportFloorDetail> floorDetails = block.getDcrReportFloorDetails();
			for (DcrReportFloorDetail floor : floorDetails) {
				noOfFloors++;
			}
			noOfFloors++;
			PdfPCell frontSetBackCell = new PdfPCell();
			Phrase frontSetBackPhrase = new Phrase(frontSetbackProvided + "", fontPara1);
			frontSetBackCell.setRowspan(noOfFloors);
			frontSetBackCell.addElement(frontSetBackPhrase);
			PdfPCell rearSetBackCell = new PdfPCell();
			Phrase rearSetBackPhrase = new Phrase(rearSetbackProvided + "", fontPara1);
			rearSetBackCell.setRowspan(noOfFloors);
			rearSetBackCell.addElement(rearSetBackPhrase);
			PdfPCell leftSetBackCell = new PdfPCell();
			Phrase leftSetBackPhrase = new Phrase(leftSetbackProvided + "", fontPara1);
			leftSetBackCell.setRowspan(noOfFloors);
			leftSetBackCell.addElement(leftSetBackPhrase);
			PdfPCell rightSetBackCell = new PdfPCell();
			Phrase rightSetBackPhrase = new Phrase(rightSetbackProvided + "", fontPara1);
			rightSetBackCell.setRowspan(noOfFloors);
			rightSetBackCell.addElement(rightSetBackPhrase);
			table1.addCell(frontSetBackCell);
			table1.addCell(rearSetBackCell);
			table1.addCell(leftSetBackCell);
			table1.addCell(rightSetBackCell);
			// my code ends
			
			for (DcrReportFloorDetail floor : floorDetails) {
				PdfPCell floorNameCell = new PdfPCell();
				Phrase floorNamephrase = new Phrase(floor.getFloorNo(), fontPara1);
				floorNameCell.addElement(floorNamephrase);
				table1.addCell(floorNameCell);
				PdfPCell floorAreaCell = new PdfPCell();
				Phrase floorAreaphrase = new Phrase(floor.getBuiltUpArea() + "", fontPara1);
				floorAreaCell.addElement(floorAreaphrase);
				table1.addCell(floorAreaCell);
			}
		}

	}

	private void addTableHeader1(PdfPTable table1) {
		Font fontPara1Bold = FontFactory.getFont(FontFactory.HELVETICA, 12, Font.BOLD);
		Stream.of("Category", "Area(sqmt)", "Front set back(mt)", "Rear set back(mt)", "Left setback(mt)",
				"Right setback(mt)").forEach(columnTitle -> {
					PdfPCell header = new PdfPCell();
					header.setBorderWidth(2);
					header.setVerticalAlignment(Element.ALIGN_MIDDLE);
					header.setPhrase(new Phrase(columnTitle, fontPara1Bold));
					table1.addCell(header);

				});
	}

}
