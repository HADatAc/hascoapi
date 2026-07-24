package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.Instrument;

import org.hascoapi.utils.URIUtils;

public class DP2Deployments {

    public static void setHeaders(Sheet sheet) {
        // Must match DP2-PMSR.xlsx golden standard exactly
        String[] headers = { 
            "hasURI", 
            "a", 
            "rdfs:label", 
            "vstoi:hasPlatformInstance", 
            "vstoi:hasInstrumentInstance",
            "vstoi:hasDetectorInstance", 
            "vstoi:designedAtTime", 
            "prov:startedAtTime", 
            "prov:endedAtTime" 
        };

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

        Sheet deploymentSheet = helper.workbook.getSheet(DP2Gen.DEPLOYMENTS);
        int rowIndex = deploymentSheet.getLastRowNum() + 1;
        Row newRow = deploymentSheet.createRow(rowIndex);

        newRow.createCell(0).setCellValue(deploy.getUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getUri()) : "");
        newRow.createCell(1).setCellValue(deploy.getHascoTypeUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getHascoTypeUri()) : "");

        // Fix: Label format should be "Deployment of Instância de [Instrument Name]"
        String label = deploy.getLabel() != null ? deploy.getLabel() : "";
        if (!label.isEmpty() && !label.startsWith("Deployment of ")) {
            label = "Deployment of " + label;
        }
        newRow.createCell(2).setCellValue(label);

        // vstoi:hasPlatformInstance - URI reference to PlatformInstances.hasURI
        newRow.createCell(3).setCellValue(deploy.getPlatformInstanceUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getPlatformInstanceUri()) : "");
        
        // vstoi:hasInstrumentInstance - URI reference to InstrumentInstances.hasURI
        newRow.createCell(4).setCellValue(deploy.getInstrumentInstanceUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getInstrumentInstanceUri()) : "");

        // Optional fields in the golden template are currently not available in Deployment POJO.
        // Keep columns present and leave values blank.
        newRow.createCell(5).setCellValue("");
        newRow.createCell(6).setCellValue("");
        newRow.createCell(7).setCellValue("");
        newRow.createCell(8).setCellValue("");

        return helper;
    }
}
