package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.URIUtils;

public class DP2PlataformInstances {

    public static void setHeaders(Sheet sheet) {
        String[] headers = {
                "hasURI",
                "a",
                "rdfs:label",
                "vstoi:hasSerialNumber",
                "hasco:hasFirstCoordinate",
                "hasco:hasFirstCoordinateUnit",
                "hasco:hasFirstCoordinateCharacteristic",
                "hasco:hasSecondCoordinate",
                "hasco:hasSecondCoordinateUnit",
                "hasco:hasSecondCoordinateCharacteristic",
                "hasco:hasThirdCoordinate",
                "hasco:hasThirdCoordinateUnit",
                "hasco:hasThirdCoordinateCharacteristic",
                "hasco:partOf"
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

        newRow.createCell(0).setCellValue(platformInstance.getUri() != null ? URIUtils.replaceNameSpaceEx(platformInstance.getUri()) : "");
        newRow.createCell(1).setCellValue(platformInstance.getHascoTypeUri() != null ? URIUtils.replaceNameSpaceEx(platformInstance.getHascoTypeUri()) : "");
        newRow.createCell(2).setCellValue(platformInstance.getLabel() != null ? platformInstance.getLabel() : "");
        newRow.createCell(3).setCellValue(platformInstance.getHasSerialNumber() != null ? platformInstance.getHasSerialNumber() : "");

        newRow.createCell(4).setCellValue(platformInstance.getFirstCoordinate() != null ? platformInstance.getFirstCoordinate().toString() : "");
        newRow.createCell(5).setCellValue(platformInstance.getFirstCoordinateUnit() != null ? platformInstance.getFirstCoordinateUnit() : "");
        newRow.createCell(6).setCellValue(platformInstance.getFirstCoordinateCharacteristic() != null ? platformInstance.getFirstCoordinateCharacteristic() : "");

        newRow.createCell(7).setCellValue(platformInstance.getSecondCoordinate() != null ? platformInstance.getSecondCoordinate().toString() : "");
        newRow.createCell(8).setCellValue(platformInstance.getSecondCoordinateUnit() != null ? platformInstance.getSecondCoordinateUnit() : "");
        newRow.createCell(9).setCellValue(platformInstance.getSecondCoordinateCharacteristic() != null ? platformInstance.getSecondCoordinateCharacteristic() : "");

        newRow.createCell(10).setCellValue(platformInstance.getThirdCoordinate() != null ? platformInstance.getThirdCoordinate().toString() : "");
        newRow.createCell(11).setCellValue(platformInstance.getThirdCoordinateUnit() != null ? platformInstance.getThirdCoordinateUnit() : "");
        newRow.createCell(12).setCellValue(platformInstance.getThirdCoordinateCharacteristic() != null ? platformInstance.getThirdCoordinateCharacteristic() : "");

        Cell partOfCell = newRow.createCell(13);
        if (platformInstance.getPartOf() != null) {
            partOfCell.setCellValue(URIUtils.replaceNameSpaceEx(platformInstance.getPartOf()));
        } else {
            partOfCell.setCellValue("");
        }

        return helper;

    }
}
