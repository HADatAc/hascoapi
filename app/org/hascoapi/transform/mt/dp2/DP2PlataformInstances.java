package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.utils.URIUtils;

public class DP2PlataformInstances {

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

        // "hasco:partOf"
        // Veriricar part of
        Cell cell14 = newRow.createCell(4);
        // cell14.setCellValue(platformInstance.getHasSerialNumber());


        return helper;

    }
}
