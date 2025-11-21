package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.Instrument;

import org.hascoapi.utils.URIUtils;

public class DP2Deployments {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "a", "rdfs:label", "vstoi:hasPlatformInstance", "vstoi:hasInstrumentInstance", 
                             "vstoi:hasComponentInstance", "vstoi:designedAtTime", "prov:startedAtTime", "prov:endedAtTime" };
        
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }
    public static DP2GenHelper add(DP2GenHelper helper, Deployment deploy) {

        if (deploy == null) {
            System.out.println("[WARNING] Deployment is null");
            return helper;
        }

        // Get the "Deployments" sheet
        Sheet deploymentSheet = helper.workbook.getSheet(DP2Gen.DEPLOYMENTS);

        // Calculate the index for the new row
        int rowIndex = deploymentSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = deploymentSheet.createRow(rowIndex);

        // Add data to the new row

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(deploy.getUri()));

        // "a"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(deploy.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(deploy.getLabel()));

        // "vstoi:hasPlatformInstance"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(deploy.getPlatformInstanceUri());

        // "vstoi:hasInstrumentInstance"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(deploy.getInstrumentInstanceUri());

        // "vstoi:hasComponentInstance",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(deploy.getComponentInstanceUri().toString());

        // "vstoi:designedAtTime"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(deploy.getDesignedAt());

        // "prov:startedAtTime"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(deploy.getStartedAt());

        // "prov:endedAtTime"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(deploy.getEndedAt());



        return helper;
    }
}
