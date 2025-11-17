package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.transform.mt.ins.INSGen;
import org.hascoapi.transform.mt.ins.INSGenHelper;
import org.hascoapi.utils.URIUtils;

public class DP2Deployments {
    public static DP2GenHelper add(DP2GenHelper helper, Deployment deploy) {
        /*
        Alterar para campos do Deployment
         */

        if (deploy == null) {
            return helper;
        }

        // Get the "Instruments" sheet
        Sheet instrumentSheet = helper.workbook.getSheet(INSGen.INSTRUMENTS);

        // Calculate the index for the new row
        int rowIndex = instrumentSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = instrumentSheet.createRow(rowIndex);

        // Add data to the new row

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(deploy.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(deploy.getHascoTypeUri()));

        // "rdfs:subClassOf"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(deploy.getSuperUri()));

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(deploy.getLabel());

        // "vstoi:hasShortName"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(deploy.getHasShortName());

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(deploy.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(deploy.getHasVersion());

        // "hasco:hasMaker"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue("");

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(deploy.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(deploy.getHasImageUri());

        // "vstoi:maxLoggedMeasurements"
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue("");

        // "vstoi:minOperatingTemperature",
        Cell cell12 = newRow.createCell(11);
        cell12.setCellValue("");

        // "vstoi:maxOperatingTemperature"
        Cell cell13 = newRow.createCell(12);
        cell13.setCellValue("");

        // "hasco:hasOperatingTemperatureUnit"
        Cell cell14 = newRow.createCell(13);
        cell14.setCellValue("");

        // "hasco:hasWebDocument"};
        Cell cell15 = newRow.createCell(14);
        cell15.setCellValue(deploy.getHasWebDocument());

        // "vstoi:hasFirst"
        Cell cell16 = newRow.createCell(15);
        String hasFirst = "";
        if (deploy != null && deploy.getHasFirst() != null) {
            hasFirst = URIUtils.replaceNameSpaceEx(deploy.getHasFirst());
        }
        cell16.setCellValue(hasFirst);

        return helper;
    }
}
