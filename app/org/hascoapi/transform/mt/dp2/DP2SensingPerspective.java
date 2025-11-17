package org.hascoapi.transform.mt.dp2;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.ComponentInstance;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.utils.URIUtils;

public class DP2SensingPerspective {
    /*
     Verificar implementação pois não existe Sensing
    */
    /*
    public static DP2GenHelper add(DP2GenHelper helper, Perspective Perspective) {

        if (helper == null) {
            System.out.println("[ERROR] INSComponent: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSComponent: helper's workbook is null");
            return helper;
        }

        if (componentInstance == null) {
            return helper;
        }

        // Get the "Components" sheet
        Sheet componentSheet = helper.workbook.getSheet(DP2Gen.COMPONENTINSTANCES);

        // Calculate the index for the new row
        int rowIndex = componentSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = componentSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getHascoTypeUri()));

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getTypeUri()));

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(componentInstance.getLabel());

        // "vstoi:hasComponentStem"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getHasComponentStem()));
        ComponentStem componentStem = null;
        if (componentInstance.getHasComponentStem() != null && !componentInstance.getHasComponentStem().isEmpty()) {
            componentStem = ComponentStem.find(componentInstance.getHasComponentStem());
            if (componentStem != null) {
                helper.componentStems.put(componentStem.getUri(),componentStem);
            }
        }

        // "vstoi:hasCodebook",
        Cell cell6 = newRow.createCell(5);
        if (componentInstance != null && componentInstance.getHasCodebook() != null) {
            cell6.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getHasCodebook()));
            Codebook codebook = Codebook.find(componentInstance.getHasCodebook());
            if (codebook != null) {
                helper.codebooks.put(codebook.getUri(),codebook);
            }
        } else {
            cell6.setCellValue("");
        }

        // "vstoi:isAttributeOf"
        Cell cell7 = newRow.createCell(6);
        if (componentInstance != null && componentInstance.getIsAttributeOf() != null) {
            cell7.setCellValue(URIUtils.replaceNameSpaceEx(componentInstance.getIsAttributeOf()));
        } else {
            cell7.setCellValue("");
        }

        return helper;

    }

     */
}
