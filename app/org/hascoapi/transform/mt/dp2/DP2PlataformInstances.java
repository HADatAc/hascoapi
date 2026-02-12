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
                "hasco:hasMaker",
                "vstoi:hasSerialNumber",
                "vstoi:hasStatus",
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

        // CRITICAL FIX: Use getTypeUri() which returns the Platform class URI (e.g., pmsr:PMSR_Simulation_Platform)
        // NOT the hascoTypeUri which returns vstoi:PlatformInstance
        newRow.createCell(1).setCellValue(platformInstance.getTypeUri() != null ? URIUtils.replaceNameSpaceEx(platformInstance.getTypeUri()) : "");

        newRow.createCell(2).setCellValue(platformInstance.getLabel() != null ? platformInstance.getLabel() : "");

        // Get hasMaker from the Platform type
        String hasMaker = "";
        if (platformInstance.getTypeUri() != null && !platformInstance.getTypeUri().isEmpty()) {
            try {
                Platform platform = Platform.find(platformInstance.getTypeUri());
                if (platform != null && platform.getHasMaker() != null && !platform.getHasMaker().isEmpty()) {
                    hasMaker = URIUtils.replaceNameSpaceEx(platform.getHasMaker());
                }
            } catch (Exception e) {
                System.out.println("[WARN] DP2PlatformInstances: Failed to get hasMaker for platform " + platformInstance.getTypeUri() + ": " + e.getMessage());
            }
        }
        newRow.createCell(3).setCellValue(hasMaker);

        newRow.createCell(4).setCellValue(platformInstance.getHasSerialNumber() != null ? platformInstance.getHasSerialNumber() : "");

        // Use the actual status from the PlatformInstance and ensure it's in prefix format (e.g., vstoi:OPERATIONAL not http://...)
        String status = "";
        if (platformInstance.getHasStatus() != null && !platformInstance.getHasStatus().isEmpty()) {
            String statusRaw = platformInstance.getHasStatus();
            // First try URIUtils conversion
            String statusConverted = URIUtils.replaceNameSpaceEx(statusRaw);

            // If still a full URI (conversion failed), manually extract the local name
            if (statusConverted.startsWith("http://") || statusConverted.startsWith("https://")) {
                if (statusRaw.contains("#")) {
                    String localName = statusRaw.substring(statusRaw.lastIndexOf("#") + 1);
                    statusConverted = "vstoi:" + localName;
                } else if (statusRaw.contains("/")) {
                    String localName = statusRaw.substring(statusRaw.lastIndexOf("/") + 1);
                    statusConverted = "vstoi:" + localName;
                }
            }
            status = statusConverted;
        }
        newRow.createCell(5).setCellValue(status);

        newRow.createCell(6).setCellValue(platformInstance.getFirstCoordinate() != null ? platformInstance.getFirstCoordinate().toString() : "");
        newRow.createCell(7).setCellValue(platformInstance.getFirstCoordinateUnit() != null ? platformInstance.getFirstCoordinateUnit() : "");
        newRow.createCell(8).setCellValue(platformInstance.getFirstCoordinateCharacteristic() != null ? platformInstance.getFirstCoordinateCharacteristic() : "");

        newRow.createCell(9).setCellValue(platformInstance.getSecondCoordinate() != null ? platformInstance.getSecondCoordinate().toString() : "");
        newRow.createCell(10).setCellValue(platformInstance.getSecondCoordinateUnit() != null ? platformInstance.getSecondCoordinateUnit() : "");
        newRow.createCell(11).setCellValue(platformInstance.getSecondCoordinateCharacteristic() != null ? platformInstance.getSecondCoordinateCharacteristic() : "");

        newRow.createCell(12).setCellValue(platformInstance.getThirdCoordinate() != null ? platformInstance.getThirdCoordinate().toString() : "");
        newRow.createCell(13).setCellValue(platformInstance.getThirdCoordinateUnit() != null ? platformInstance.getThirdCoordinateUnit() : "");
        newRow.createCell(14).setCellValue(platformInstance.getThirdCoordinateCharacteristic() != null ? platformInstance.getThirdCoordinateCharacteristic() : "");

        Cell partOfCell = newRow.createCell(15);
        if (platformInstance.getPartOf() != null) {
            partOfCell.setCellValue(URIUtils.replaceNameSpaceEx(platformInstance.getPartOf()));
        } else {
            partOfCell.setCellValue("");
        }

        return helper;

    }
}
