package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.transform.mt.ins.INSGen;
import org.hascoapi.utils.URIUtils;

public class DP2ComponentsInstances {
    public static DP2GenHelper add(DP2GenHelper helper, ComponentInstance componentInstance) {

        if (componentInstance == null) {
            System.out.println("[WARNING] Deployment is null");
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
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getUri()));

        // "a"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getLabel()));

        // "vstoi:hasSerialNumber"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(componentInstance.getHasSerialNumber());
/*
        // "vstoi:isInstrumentAttachment"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(componentInstance.);

 */


        return helper;
    }
}
