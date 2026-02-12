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

        Sheet deploymentSheet = helper.workbook.getSheet(DP2Gen.DEPLOYMENTS);
        int rowIndex = deploymentSheet.getLastRowNum() + 1;
        Row newRow = deploymentSheet.createRow(rowIndex);

        newRow.createCell(0).setCellValue(deploy.getUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getUri()) : "");
        newRow.createCell(1).setCellValue(deploy.getHascoTypeUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getHascoTypeUri()) : "");
        newRow.createCell(2).setCellValue(deploy.getLabel() != null ? URIUtils.replaceNameSpaceEx(deploy.getLabel()) : "");

        // Use CURIE style for object references
        newRow.createCell(3).setCellValue(deploy.getPlatformInstanceUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getPlatformInstanceUri()) : "");
        newRow.createCell(4).setCellValue(deploy.getInstrumentInstanceUri() != null ? URIUtils.replaceNameSpaceEx(deploy.getInstrumentInstanceUri()) : "");

        java.util.List<String> comps = deploy.getComponentInstanceUri();
        if (comps == null || comps.isEmpty()) {
            newRow.createCell(5).setCellValue("");
        } else if (comps.size() == 1) {
            newRow.createCell(5).setCellValue(URIUtils.replaceNameSpaceEx(comps.get(0)));
        } else {
            String joined = comps.stream()
                    .filter(s -> s != null && !s.isEmpty())
                    .map(URIUtils::replaceNameSpaceEx)
                    .reduce((a, b) -> a + ";" + b)
                    .orElse("");
            newRow.createCell(5).setCellValue(joined);
        }

        newRow.createCell(6).setCellValue(deploy.getDesignedAt() != null ? deploy.getDesignedAt() : "");
        newRow.createCell(7).setCellValue(deploy.getStartedAt() != null ? deploy.getStartedAt() : "");
        newRow.createCell(8).setCellValue(deploy.getEndedAt() != null ? deploy.getEndedAt() : "");

        return helper;
    }
}
