package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Platform;

import org.hascoapi.utils.URIUtils;

public class DP2Plataforms {

    public static void setHeaders(Sheet sheet) {
        String[] headers = { "hasURI", "rdfs:subClassOf", "rdfs:label", "hasco:hasMaker", "rdfs:comment", "hasco:hasImage", "vstoi:hasWebDocumentation" };

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

        Sheet platformSheet = helper.workbook.getSheet(DP2Gen.PLATFORMS);
        int rowIndex = platformSheet.getLastRowNum() + 1;
        Row newRow = platformSheet.createRow(rowIndex);

        newRow.createCell(0).setCellValue(URIUtils.replaceNameSpaceEx(platform.getUri()));
        newRow.createCell(1).setCellValue(URIUtils.replaceNameSpaceEx(platform.getSuperUri()));
        newRow.createCell(2).setCellValue(platform.getLabel());

        // Maker isn't wired on Platform in this project, keep blank for now
        newRow.createCell(3).setCellValue("");

        newRow.createCell(4).setCellValue(platform.getComment());
        newRow.createCell(5).setCellValue(platform.getHasImageUri());
        newRow.createCell(6).setCellValue(platform.getHasWebDocument());

        return helper;
    }
}
