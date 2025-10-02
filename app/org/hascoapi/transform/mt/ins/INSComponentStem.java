package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSComponentStem {

    public static INSGenHelper add(INSGenHelper helper, ComponentStem componentStem) {

        if (helper == null) {
            System.out.println("[ERROR] INSComponentStem: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSComponentStem: helper's workbook is null");
            return helper;
        }

        if (componentStem == null) {
            return helper;
        }

        // Get the "ComponentStems" sheet
        Sheet componentStemSheet = helper.workbook.getSheet(INSGen.COMPONENT_STEMS);

        // Calculate the index for the new row
        int rowIndex = componentStemSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = componentStemSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(componentStem.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(componentStem.getHascoTypeUri()));  

        // "rdfs:subClassOf"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(componentStem.getSuperUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(componentStem.getLabel());  

        // "vstoi:hasContent"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(componentStem.getHasContent());  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(componentStem.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(componentStem.getHasVersion());

        // "hasco:hasMaker"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue("");

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(componentStem.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(componentStem.getHasImageUri());

        // "hasco:hasWebDocument"};
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue(componentStem.getHasWebDocument());

        return helper;
    }

}
