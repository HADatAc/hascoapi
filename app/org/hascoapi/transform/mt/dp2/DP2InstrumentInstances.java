package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.InstrumentInstance;
import org.hascoapi.utils.URIUtils;

public class DP2InstrumentInstances {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "a", "rdfs:label", "vstoi:hasSerialNumber", "skos:definition", "owl:sameAs" };

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

        // Get the "Components" sheet
        Sheet instrumentinstancessheet = helper.workbook.getSheet(DP2Gen.INSTRUMENTINSTANCES);

        // Calculate the index for the new row
        int rowIndex = instrumentinstancessheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = instrumentinstancessheet.createRow(rowIndex);

        newRow.createCell(0).setCellValue(instrumentInstance.getUri() != null ? URIUtils.replaceNameSpaceEx(instrumentInstance.getUri()) : "");
        newRow.createCell(1).setCellValue(instrumentInstance.getHascoTypeUri() != null ? URIUtils.replaceNameSpaceEx(instrumentInstance.getHascoTypeUri()) : "");
        newRow.createCell(2).setCellValue(instrumentInstance.getLabel() != null ? instrumentInstance.getLabel() : "");
        newRow.createCell(3).setCellValue(instrumentInstance.getHasSerialNumber() != null ? instrumentInstance.getHasSerialNumber() : "");
        newRow.createCell(4).setCellValue(instrumentInstance.getDescription() != null ? instrumentInstance.getDescription() : "");
        newRow.createCell(5).setCellValue(""); // owl:sameAs not currently available

        return helper;

    }
}
