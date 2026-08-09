package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.FieldOfView;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.utils.URIUtils;

public class DP2SensingPerspective {

    public static void setHeaders(Sheet sheet) {
    String[] headers = {
        "hasURI",
        "a",
        "vstoi:perspectiveOf",
        "hasco:hasPerspectiveEntity",
        "hasco:hasPerspectiveCharacteristic",
        "vstoi:hasAccuracyPercentage",
        "vstoi:hasAccuracyR2",
        "vstoi:hasOutputResolution",
        "vstoi:hasMaxResponseTimeValue",
        "hasco:hasResponseTimeUnit",
        "vstoi:hasLowRangeValue",
        "vstoi:hasHighRangeValue"
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

    /*
    public static DP2GenHelper add(DP2GenHelper helper, SensingPerspective sensingPerspective) {

        if (sensingPerspective == null) {
            System.out.println("[WARNING] SensingPerspective is null");
            return helper;
        }

        // Get the "SensingPerspective" sheet
        Sheet sheet = helper.workbook.getSheet(DP2Gen.SENSINGPERSPECTIVE);

        // Calculate the index for the new row
        int rowIndex = sheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = sheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(sensingPerspective.getUri()));

        // "a"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(sensingPerspective.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(sensingPerspective.getLabel());

        // "hasco:hasFieldOfView"
        Cell cell4 = newRow.createCell(3);
        if (sensingPerspective.getHasFieldOfView() != null) {
            cell4.setCellValue(URIUtils.replaceNameSpaceEx(sensingPerspective.getHasFieldOfView()));
            FieldOfView fieldOfView = FieldOfView.find(sensingPerspective.getHasFieldOfView());
            if (fieldOfView != null) {
                helper.fieldofview.put(fieldOfView.getUri(), fieldOfView);
            }
        } else {
            cell4.setCellValue("");
        }

        // "hasco:hasDeployment"
        Cell cell5 = newRow.createCell(4);
        if (sensingPerspective.getHasDeployment() != null) {
            cell5.setCellValue(URIUtils.replaceNameSpaceEx(sensingPerspective.getHasDeployment()));
            Deployment deployment = Deployment.find(sensingPerspective.getHasDeployment());
            if (deployment != null) {
                // Não há mapa para Deployment em DP2GenHelper, apenas adicionamos o URI
            }
        } else {
            cell5.setCellValue("");
        }

        return helper;
    }

     */
}
