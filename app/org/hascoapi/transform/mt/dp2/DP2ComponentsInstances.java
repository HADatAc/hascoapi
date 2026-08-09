package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.Deployment;

import org.hascoapi.utils.URIUtils;

public class DP2ComponentsInstances {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "a", "hasco:hascoType", "rdfs:label", "vstoi:hasSerialNumber", "vstoi:isInstrumentAttachment" };

        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static DP2GenHelper add(DP2GenHelper helper, ComponentInstance componentInstance) {

        if (componentInstance == null) {
            System.out.println("[WARNING] ComponentInstance is null");
            return helper;
        }

        // Get the "ComponentInstances" sheet
        Sheet componentinstanceSheet = helper.workbook.getSheet(DP2Gen.COMPONENTINSTANCES);

        // Calculate the index for the new row
        int rowIndex = componentinstanceSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = componentinstanceSheet.createRow(rowIndex);

        // Add data to the new row

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(componentInstance.getUri() != null ? URIUtils.replaceNameSpaceEx(componentInstance.getUri()) : "");

        // "a" - CRITICAL FIX: Use typeUri (the actual Component class from INS) instead of hascoTypeUri (generic vstoi:ComponentInstance)
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(componentInstance.getTypeUri() != null ? URIUtils.replaceNameSpaceEx(componentInstance.getTypeUri()) : "");

        // "hasco:hascoType" fixed for component instances in DP2 v3
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue("hasco:ComponentInstance");

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(componentInstance.getLabel() != null ? URIUtils.replaceNameSpaceEx(componentInstance.getLabel()) : "");

        // "vstoi:hasSerialNumber"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(componentInstance.getHasSerialNumber() != null ? componentInstance.getHasSerialNumber() : "");

        // "vstoi:isInstrumentAttachment" (not currently mapped in POJO)
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue("");


        return helper;
    }
}
