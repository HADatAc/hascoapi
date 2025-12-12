package org.hascoapi.transform.mt.dsg;

import org.hascoapi.entity.pojo.Study;
import org.apache.poi.ss.usermodel.*;
import org.hascoapi.utils.URIUtils;

public class DSGSTD {

    public static DSGGenHelper add(DSGGenHelper helper, Study study) {
        Sheet sheet = helper.workbook.getSheet(DSGGen.STD);
        if (sheet == null) {
            sheet = helper.workbook.createSheet(DSGGen.STD);
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Study ID");
            headerRow.createCell(1).setCellValue("Title");
            headerRow.createCell(2).setCellValue("Specific Aims");
            headerRow.createCell(3).setCellValue("Significance");
            headerRow.createCell(4).setCellValue("Institution");
            headerRow.createCell(5).setCellValue("Principal Investigator");
            headerRow.createCell(6).setCellValue("PI Address");
            headerRow.createCell(7).setCellValue("PI City");
            headerRow.createCell(8).setCellValue("PI State");
            headerRow.createCell(9).setCellValue("PI Zip Code");
            headerRow.createCell(10).setCellValue("Email");
            headerRow.createCell(11).setCellValue("PI Phone");
            headerRow.createCell(12).setCellValue("Co-PI 1 First Name");
            headerRow.createCell(13).setCellValue("Co-PI 1 Last Name");
            headerRow.createCell(14).setCellValue("Co-PI 1 Email");
            headerRow.createCell(15).setCellValue("Co-PI 2 First Name");
            headerRow.createCell(16).setCellValue("Co-PI 2 Last Name");
            headerRow.createCell(17).setCellValue("Co-PI 2 Email");
            headerRow.createCell(18).setCellValue("Contact First Name");
            headerRow.createCell(19).setCellValue("Contact Last Name");
            headerRow.createCell(20).setCellValue("Contact Email");
            headerRow.createCell(21).setCellValue("Project Created Date");
            headerRow.createCell(22).setCellValue("Project Last Updated Date");
            headerRow.createCell(23).setCellValue("DC Access?");
        }
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);
        // Map available Study fields; unknowns left blank
        String studyUriAbbrev = URIUtils.replaceNameSpaceEx(safe(study.getUri()));
        row.createCell(0).setCellValue(studyUriAbbrev);
        // Prefer Title if available, otherwise label
        String title = study.getTitle() != null ? study.getTitle() : study.getLabel();
        row.createCell(1).setCellValue(safe(title));
        row.createCell(2).setCellValue(safe(study.getComment()));
        row.createCell(3).setCellValue("");
        row.createCell(4).setCellValue(URIUtils.replaceNameSpaceEx(safe(study.getInstitutionUri())));
        row.createCell(5).setCellValue(URIUtils.replaceNameSpaceEx(safe(study.getPiUri())));
        row.createCell(6).setCellValue("");
        row.createCell(7).setCellValue("");
        row.createCell(8).setCellValue("");
        row.createCell(9).setCellValue("");
        row.createCell(10).setCellValue(safe(study.getHasSIRManagerEmail()));
        row.createCell(11).setCellValue("");
        row.createCell(12).setCellValue("");
        row.createCell(13).setCellValue("");
        row.createCell(14).setCellValue("");
        row.createCell(15).setCellValue("");
        row.createCell(16).setCellValue("");
        row.createCell(17).setCellValue("");
        row.createCell(18).setCellValue("");
        row.createCell(19).setCellValue("");
        row.createCell(20).setCellValue("");
        row.createCell(21).setCellValue(safe(study.getStartedAt()));
        row.createCell(22).setCellValue(safe(study.getEndedAt()));
        row.createCell(23).setCellValue("");

        // Update InfoSheet hasStudyURI with the current study (abreviado)
        Sheet infoSheet = helper.workbook.getSheet(DSGGen.INFOSHEET);
        if (infoSheet != null) {
            Row studyUriRow = infoSheet.getRow(2);
            if (studyUriRow == null) studyUriRow = infoSheet.createRow(2);
            Cell valueCell = studyUriRow.getCell(1);
            if (valueCell == null) valueCell = studyUriRow.createCell(1);
            valueCell.setCellValue(studyUriAbbrev);
        }
        return helper;
    }

    private static String safe(String val) {
        return val == null ? "" : val;
    }

    public static DSGGenHelper addByStatus(DSGGenHelper helper, String status) {
        return helper;
    }
}
