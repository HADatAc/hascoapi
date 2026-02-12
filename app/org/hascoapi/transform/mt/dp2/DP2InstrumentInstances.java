package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.InstrumentInstance;
import org.hascoapi.utils.URIUtils;

public class DP2InstrumentInstances {

    public static void setHeaders(Sheet sheet) {
        // Fix: Remove skos:definition and owl:sameAs
        // Fix: Add vstoi:hasStatus and hasco:hasWebDocument
        String[] headers = { "hasURI", "a", "rdfs:label", "vstoi:hasSerialNumber", "vstoi:hasStatus", "hasco:hasWebDocument" };

        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static DP2GenHelper add(DP2GenHelper helper, InstrumentInstance instrumentInstance) {

        if (helper == null) {
            System.out.println("[ERROR] DP2InstrumentInstance: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] DP2InstrumentInstance: helper's workbook is null");
            return helper;
        }

        if (instrumentInstance == null) {
            return helper;
        }

        // Get the "InstrumentInstances" sheet
        Sheet instrumentinstancessheet = helper.workbook.getSheet(DP2Gen.INSTRUMENTINSTANCES);

        // Calculate the index for the new row
        int rowIndex = instrumentinstancessheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = instrumentinstancessheet.createRow(rowIndex);

        newRow.createCell(0).setCellValue(instrumentInstance.getUri() != null ? URIUtils.replaceNameSpaceEx(instrumentInstance.getUri()) : "");

        // CRITICAL FIX: Use typeUri (the actual Instrument class from INS) instead of hascoTypeUri (generic vstoi:InstrumentInstance)
        newRow.createCell(1).setCellValue(instrumentInstance.getTypeUri() != null ? URIUtils.replaceNameSpaceEx(instrumentInstance.getTypeUri()) : "");

        newRow.createCell(2).setCellValue(instrumentInstance.getLabel() != null ? instrumentInstance.getLabel() : "");
        newRow.createCell(3).setCellValue(instrumentInstance.getHasSerialNumber() != null ? instrumentInstance.getHasSerialNumber() : "");

        // Fix: Add vstoi:hasStatus with default value vstoi:OPERATIONAL
        String status = instrumentInstance.getHasStatus();
        if (status == null || status.isEmpty()) {
            status = "vstoi:OPERATIONAL";
        } else if (!status.contains(":")) {
            // Ensure it has a prefix
            status = "vstoi:" + status;
        }
        newRow.createCell(4).setCellValue(status);

        // Fix: Add hasco:hasWebDocument
        newRow.createCell(5).setCellValue(instrumentInstance.getHasWebDocument() != null ? instrumentInstance.getHasWebDocument() : "");

        return helper;

    }
}
