package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.InstrumentInstance;
import org.hascoapi.utils.URIUtils;

public class DP2InstrumentInstances {

    public static void setHeaders(Sheet sheet) {
        // Updated to match DP2-PMSR.xlsx structure with vstoi:hasOwner
        String[] headers = { "hasURI", "a", "rdfs:label", "vstoi:hasSerialNumber", "skos:definition", "owl:sameAs", "vstoi:hasOwner" };

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

        // Column A: hasURI
        newRow.createCell(0).setCellValue(instrumentInstance.getUri() != null ? URIUtils.replaceNameSpaceEx(instrumentInstance.getUri()) : "");

        // Column B: a (rdf:type) - Use typeUri (the actual Instrument class from INS) instead of hascoTypeUri
        newRow.createCell(1).setCellValue(instrumentInstance.getTypeUri() != null ? URIUtils.replaceNameSpaceEx(instrumentInstance.getTypeUri()) : "");

        // Column C: rdfs:label
        newRow.createCell(2).setCellValue(instrumentInstance.getLabel() != null ? instrumentInstance.getLabel() : "");
        
        // Column D: vstoi:hasSerialNumber
        newRow.createCell(3).setCellValue(instrumentInstance.getHasSerialNumber() != null ? instrumentInstance.getHasSerialNumber() : "");

        // Column E: skos:definition (not currently mapped in Java class)
        newRow.createCell(4).setCellValue("");

        // Column F: owl:sameAs (not currently mapped in Java class)
        newRow.createCell(5).setCellValue("");

        // Column G: vstoi:hasOwner
        String ownerUri = instrumentInstance.getHasOwnerUri();
        newRow.createCell(6).setCellValue(ownerUri != null && !ownerUri.isEmpty() ? URIUtils.replaceNameSpaceEx(ownerUri) : "");

        return helper;

    }
}
