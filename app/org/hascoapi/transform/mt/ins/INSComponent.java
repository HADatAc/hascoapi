package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSComponent {

    public static INSGenHelper add(INSGenHelper helper, Component component) {

        if (helper == null) {
            System.out.println("[ERROR] INSComponent: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSComponent: helper's workbook is null");
            return helper;
        }

        if (component == null) {
            return helper;
        }

        // Get the "Components" sheet
        Sheet componentSheet = helper.workbook.getSheet(INSGen.COMPONENTS);

        // Calculate the index for the new row
        int rowIndex = componentSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = componentSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(component.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(component.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(component.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(component.getLabel());  

        // "vstoi:hasComponentStem"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(component.getHasComponentStem()));
        ComponentStem componentStem = null;
        if (component.getHasComponentStem() != null && !component.getHasComponentStem().isEmpty()) {
            componentStem = ComponentStem.find(component.getHasComponentStem());
            if (componentStem != null) {
                helper.componentStems.put(componentStem.getUri(),componentStem);
            } 
        }

        // "vstoi:hasCodebook",
        Cell cell6 = newRow.createCell(5);
        if (component != null && component.getHasCodebook() != null) {
            cell6.setCellValue(URIUtils.replaceNameSpaceEx(component.getHasCodebook()));
            Codebook codebook = Codebook.find(component.getHasCodebook());
            if (codebook != null) {
                helper.codebooks.put(codebook.getUri(),codebook);
            } 
        } else {
            cell6.setCellValue("");
        }

        // "vstoi:isAttributeOf"
        Cell cell7 = newRow.createCell(6);
        if (component != null && component.getIsAttributeOf() != null) {
            cell7.setCellValue(URIUtils.replaceNameSpaceEx(component.getIsAttributeOf()));
        } else {
            cell7.setCellValue("");
        }

        return helper;

    }

}
