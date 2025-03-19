package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.entity.pojo.ActuatorStem;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSActuator {

    public static INSGenHelper add(INSGenHelper helper, Actuator actuator) {

        if (helper == null) {
            System.out.println("[ERROR] INSActuator: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSActuator: helper's workbook is null");
            return helper;
        }

        if (actuator == null) {
            return helper;
        }

        // Get the "Actuators" sheet
        Sheet actuatorSheet = helper.workbook.getSheet(INSGen.ACTUATORS);

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
        ActuatorStem actuatorStem = null;
        if (actuator.getHasActuatorStem() != null && !actuator.getHasActuatorStem().isEmpty()) {
            actuatorStem = ActuatorStem.find(actuator.getHasActuatorStem());
            if (actuatorStem != null) {
                helper.actStems.put(actuatorStem.getUri(),actuatorStem);
            } 
        }

        // "vstoi:hasCodebook",
        Cell cell6 = newRow.createCell(5);
        if (actuator != null && actuator.getHasCodebook() != null) {
            cell6.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getHasCodebook()));
            Codebook codebook = Codebook.find(actuator.getHasCodebook());
            if (codebook != null) {
                helper.codebooks.put(codebook.getUri(),codebook);
            } 
        } else {
            cell6.setCellValue("");
        }

        // "vstoi:isAttributeOf"
        Cell cell7 = newRow.createCell(6);
        if (actuator != null && actuator.getIsAttributeOf() != null) {
            cell7.setCellValue(URIUtils.replaceNameSpaceEx(actuator.getIsAttributeOf()));
        } else {
            cell7.setCellValue("");
        }

        return helper;

    }

}
