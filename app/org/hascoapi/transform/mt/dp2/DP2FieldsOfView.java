package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.FieldOfView;
import org.hascoapi.utils.URIUtils;

public class DP2FieldsOfView {

    public static void setHeaders(Sheet sheet) {
        String[] headers = {
                "hasURI",
                "a",
                "hasco:hasGeometry",
                "rdfs:label",
                "hasco:isFieldOfViewOf",
                "hasco:hasFirstParameter",
                "hasco:hasFirstParameterUnit",
                "hasco:hasFirstParameterCharacteristic",
                "hasco:hasSecondParameter",
                "hasco:hasSecondParameterUnit",
                "hasco:hasSecondParameterCharacteristic",
                "hasco:hasThirdParameter",
                "hasco:hasThirdParameterUnit",
                "hasco:hasThirdParameterCharacteristic"
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

    public static DP2GenHelper add(DP2GenHelper helper, FieldOfView fieldOfView) {

        if (fieldOfView == null) {
            System.out.println("[WARNING] FieldOfView is null");
            return helper;
        }

        // Get the "FieldsOfView" sheet
        Sheet sheet = helper.workbook.getSheet(DP2Gen.FIELDSOFVIEW);

        // Calculate the index for the new row
        int rowIndex = sheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = sheet.createRow(rowIndex);

        newRow.createCell(0).setCellValue(fieldOfView.getUri() != null ? URIUtils.replaceNameSpaceEx(fieldOfView.getUri()) : "");
        newRow.createCell(1).setCellValue(fieldOfView.getHascoTypeUri() != null ? URIUtils.replaceNameSpaceEx(fieldOfView.getHascoTypeUri()) : "");

        // geometry not currently wired on POJO in this code path
        newRow.createCell(2).setCellValue(fieldOfView.getGeometry() != null ? fieldOfView.getGeometry() : "");

        newRow.createCell(3).setCellValue(fieldOfView.getLabel() != null ? fieldOfView.getLabel() : "");
        newRow.createCell(4).setCellValue(fieldOfView.getField() != null ? fieldOfView.getField() : "");

        newRow.createCell(5).setCellValue(fieldOfView.getFirstParameter() != null ? fieldOfView.getFirstParameter().toString() : "");
        newRow.createCell(6).setCellValue(fieldOfView.getFirstParameterUnit() != null ? fieldOfView.getFirstParameterUnit() : "");
        newRow.createCell(7).setCellValue(fieldOfView.getFirstParameterCharacteristic() != null ? fieldOfView.getFirstParameterCharacteristic() : "");

        newRow.createCell(8).setCellValue(fieldOfView.getSecondParameter() != null ? fieldOfView.getSecondParameter().toString() : "");
        newRow.createCell(9).setCellValue(fieldOfView.getSecondParameterUnit() != null ? fieldOfView.getSecondParameterUnit() : "");
        newRow.createCell(10).setCellValue(fieldOfView.getSecondParameterCharacteristic() != null ? fieldOfView.getSecondParameterCharacteristic() : "");

        newRow.createCell(11).setCellValue(fieldOfView.getThirdParameter() != null ? fieldOfView.getThirdParameter().toString() : "");
        newRow.createCell(12).setCellValue(fieldOfView.getThirdParameterUnit() != null ? fieldOfView.getThirdParameterUnit() : "");
        newRow.createCell(13).setCellValue(fieldOfView.getThirdParameterCharacteristic() != null ? fieldOfView.getThirdParameterCharacteristic() : "");

        return helper;
    }
}
