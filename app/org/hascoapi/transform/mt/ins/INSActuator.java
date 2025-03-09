package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSActuator {

    public static Workbook add(Workbook workbook, Actuator actuator) {

        if (actuator == null) {
            return workbook;
        }

        // Get the "Actuators" sheet
        Sheet actuatorSheet = workbook.getSheet(INSGen.ACTUATORS);

        // Calculate the index for the new row
        int rowIndex = actuatorSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = actuatorSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(actuator.getLabel());  

        // "vstoi:hasActuatorStem"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getHasActuatorStem()));  

        // "vstoi:hasCodebook",
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getHasCodebook()));

        return workbook;

    }

}
