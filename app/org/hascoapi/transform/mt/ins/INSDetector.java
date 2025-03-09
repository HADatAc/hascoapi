package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSDetector {

    public static Workbook add(Workbook workbook, Detector detector) {

        if (detector == null) {
            return workbook;
        }

        // Get the "Detectors" sheet
        Sheet detectorSheet = workbook.getSheet(INSGen.DETECTORS);

        // Calculate the index for the new row
        int rowIndex = detectorSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = detectorSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(detector.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(detector.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(detector.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(detector.getLabel());  

        // "vstoi:hasDetectorStem"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(detector.getHasDetectorStem()));  

        // "vstoi:hasCodebook",
        Cell cell6 = newRow.createCell(5);
        if (detector != null && detector.getHasCodebook() != null) {
            cell6.setCellValue(URIUtils.replaceNameSpaceEx(detector.getHasCodebook()));
        } else {
            cell6.setCellValue("");
        }

        return workbook;
    }

}
