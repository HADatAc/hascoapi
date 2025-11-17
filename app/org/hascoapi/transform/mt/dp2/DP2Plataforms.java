package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Platform;
import org.hascoapi.transform.mt.ins.INSGen;
import org.hascoapi.transform.mt.ins.INSGenHelper;
import org.hascoapi.utils.URIUtils;

public class DP2Plataforms {

    public static DP2GenHelper add(DP2GenHelper helper, Platform platform) {

        if (helper == null) {
            System.out.println("[ERROR] INSComponent: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSComponent: helper's workbook is null");
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

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(platform.getHascoTypeUri()));

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(platform.getTypeUri()));

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(platform.getLabel());

        // "vstoi:hasComponentStem"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(platform.getHasComponentStem()));
        ComponentStem componentStem = null;
        if (platform.getHasComponentStem() != null && !platform.getHasComponentStem().isEmpty()) {
            componentStem = ComponentStem.find(platform.getHasComponentStem());
            if (componentStem != null) {
                helper.componentStems.put(componentStem.getUri(),componentStem);
            }
        }

        // "vstoi:hasCodebook",
        Cell cell6 = newRow.createCell(5);
        if (platform != null && platform.getHasCodebook() != null) {
            cell6.setCellValue(URIUtils.replaceNameSpaceEx(platform.getHasCodebook()));
            Codebook codebook = Codebook.find(platform.getHasCodebook());
            if (codebook != null) {
                helper.codebooks.put(codebook.getUri(),codebook);
            }
        } else {
            cell6.setCellValue("");
        }

        // "vstoi:isAttributeOf"
        Cell cell7 = newRow.createCell(6);
        if (platform != null && platform.getIsAttributeOf() != null) {
            cell7.setCellValue(URIUtils.replaceNameSpaceEx(platform.getIsAttributeOf()));
        } else {
            cell7.setCellValue("");
        }

        return helper;

    }
}
