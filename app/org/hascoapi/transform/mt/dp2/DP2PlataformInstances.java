package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.URIUtils;

public class DP2PlataformInstances {

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

    public static DP2GenHelper add(DP2GenHelper helper, PlatformInstance platformInstance) {

        if (helper == null) {
            System.out.println("[ERROR] DP2PlatformInstance: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] DP2PlatformInstance: helper's workbook is null");
            return helper;
        }

        if (platformInstance == null) {
            return helper;
        }

        // Get the "Components" sheet
        Sheet platforminstancessheet = helper.workbook.getSheet(DP2Gen.PLATFORMINTANCES);

        // Calculate the index for the new row
        int rowIndex = platforminstancessheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = platforminstancessheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(platformInstance.getUri()));

        // "a"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(platformInstance.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(platformInstance.getLabel());

        // "vstoi:hasSerialNumber"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(platformInstance.getHasSerialNumber());

        // "hasco:hasFirstCoordinate
        Cell cell5 = newRow.createCell(3);
        cell5.setCellValue(platformInstance.getFirstCoordinate());

        // "hasco:hasFirstCoordinateUnit"
        Cell cell6 = newRow.createCell(3);
        cell6.setCellValue(platformInstance.getFirstCoordinateUnit());

        // "hasco:hasFirstCoordinateCharacteristic
        Cell cell7 = newRow.createCell(3);
        cell7.setCellValue(platformInstance.getFirstCoordinateCharacteristic());

        // "hasco:hasSecondCoordinate
        Cell cell8 = newRow.createCell(3);
        cell8.setCellValue(platformInstance.getSecondCoordinate());

        // hasco:hasSecondCoordinateUnit
        Cell cell9 = newRow.createCell(3);
        cell9.setCellValue(platformInstance.getSecondCoordinateUnit());

        // hasco:hasSecondCoordinateCharacteristic
        Cell cell10 = newRow.createCell(3);
        cell10.setCellValue(platformInstance.getSecondCoordinateCharacteristic());

        // hasco:hasThirdCoordinate
        Cell cell11 = newRow.createCell(3);
        cell11.setCellValue(platformInstance.getThirdCoordinate());

        // hasco:hasThirdCoordinateUnit
        Cell cell12 = newRow.createCell(3);
        cell12.setCellValue(platformInstance.getThirdCoordinateUnit());

        // hasco:hasThirdCoordinateCharacteristic
        Cell cell13 = newRow.createCell(3);
        cell13.setCellValue(platformInstance.getThirdCoordinateCharacteristic());

        // "hasco:partOf"
        Cell cell14 = newRow.createCell(4);
        if (platformInstance.getPartOf() != null) {
            cell14.setCellValue(URIUtils.replaceNameSpaceEx(platformInstance.getPartOf()));
        } else {
            cell14.setCellValue("");
        }


        return helper;

    }
}
