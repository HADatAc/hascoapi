package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Platform;

import org.hascoapi.utils.URIUtils;

public class DP2Plataforms {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "a", "rdfs:subClassOf", "rdfs:label", "vstoi:hasComponentStem", "vstoi:hasCodebook", "vstoi:isAttributeOf" };
        
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    public static DP2GenHelper add(DP2GenHelper helper, Platform platform) {

        if (helper == null) {
            System.out.println("[ERROR] DP2Platforms: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] DP2Platforms: helper's workbook is null");
            return helper;
        }

        if (platform == null) {
            return helper;
        }

        // Get the "Components" sheet
        Sheet platformSheet = helper.workbook.getSheet(DP2Gen.PLATFORMS);

        // Calculate the index for the new row
        int rowIndex = platformSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = platformSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(platform.getUri()));

        // "rdfs:subClassOf"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(platform.getHascoTypeUri()));

        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(platform.getLabel());

        // "hasco:hasMaker"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(""); // (platform.getHasMaker());

        // "rdfs:comment",
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(platform.getComment());

        // "hasco:hasImage"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(platform.getHasImageUri());

        // "vstoi:hasWebDocumentation
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(platform.getHasWebDocument());

        return helper;

    }
}
