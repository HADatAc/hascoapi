package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.ActuatorStem;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSActuatorStem {

    public static Workbook add(Workbook workbook, ActuatorStem actuatorStem) {

        if (actuatorStem == null) {
            return workbook;
        }

        // Get the "ActuatorStems" sheet
        Sheet actuatorStemSheet = workbook.getSheet(INSGen.ACTUATOR_STEMS);

        // Calculate the index for the new row
        int rowIndex = actuatorStemSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = actuatorStemSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(actuatorStem.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(actuatorStem.getHascoTypeUri()));  

        // "rdfs:subClassOf"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(actuatorStem.getSuperUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(actuatorStem.getLabel());  

        // "vstoi:hasContent"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(actuatorStem.getHasContent());  

        // "vstoi:hasLanguage",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(actuatorStem.getHasLanguage());

        // "vstoi:hasVersion"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(actuatorStem.getHasVersion());

        // "hasco:hasMaker"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue("");

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(actuatorStem.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(actuatorStem.getHasImageUri());

        // "vstoi:hasWebDocumentation"};
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue(actuatorStem.getHasWebDocument());

        return workbook;
    }

}
