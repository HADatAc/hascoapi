package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.FieldOfView;
import org.hascoapi.utils.URIUtils;

public class DP2FieldsOfView {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "a", "rdfs:label", "hasco:hasFirstParameter", "hasco:hasSecondParameter", "hasco:hasThirdParameter", "hasco:hasUnit" };
        
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

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(fieldOfView.getUri()));

        // "a"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(fieldOfView.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(fieldOfView.getLabel());

        // "hasco:isFieldOfViewOf"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(fieldOfView.getField());

        // "hasco:hasFirstParameter
        Cell cell5 = newRow.createCell(3);
        cell5.setCellValue(fieldOfView.getFirstParameter());

        // "hasco:hasFirstParameterUnit"
        Cell cell6 = newRow.createCell(3);
        cell6.setCellValue(fieldOfView.getFirstParameterUnit());

        // "hasco:hasFirstParameterCharacteristic
        Cell cell7 = newRow.createCell(3);
        cell7.setCellValue(fieldOfView.getFirstParameterCharacteristic());

        // "hasco:hasSecondParameter
        Cell cell8 = newRow.createCell(3);
        cell8.setCellValue(fieldOfView.getSecondParameter());

        // hasco:hasSecondParameterUnit
        Cell cell9 = newRow.createCell(3);
        cell9.setCellValue(fieldOfView.getSecondParameterUnit());

        // hasco:hasSecondParameterCharacteristic
        Cell cell10 = newRow.createCell(3);
        cell10.setCellValue(fieldOfView.getSecondParameterCharacteristic());

        // hasco:hasThirdParameter
        Cell cell11 = newRow.createCell(3);
        cell11.setCellValue(fieldOfView.getThirdParameter());

        // hasco:hasThirdParameterUnit
        Cell cell12 = newRow.createCell(3);
        cell12.setCellValue(fieldOfView.getThirdParameterUnit());

        // hasco:hasThirdParameterCharacteristic
        Cell cell13 = newRow.createCell(3);
        cell13.setCellValue(fieldOfView.getThirdParameterCharacteristic());

        return helper;
    }
}
