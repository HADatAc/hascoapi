package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.InstrumentInstance;
import org.hascoapi.utils.URIUtils;

public class DP2InstrumentInstances {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "a", "rdfs:label", "vstoi:hasSerialNumber", "hasco:partOf" };
        
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

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(instrumentInstance.getUri()));

        // "a"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(instrumentInstance.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(instrumentInstance.getLabel());

        // "vstoi:hasSerialNumber"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(instrumentInstance.getHasSerialNumber());

        // "skos:definition"
        Cell cell5 = newRow.createCell(3);
        cell5.setCellValue(instrumentInstance.getDescription());

        // "owl:sameAs"
        Cell cell6 = newRow.createCell(3);
        cell6.setCellValue("");

        return helper;

    }
}
