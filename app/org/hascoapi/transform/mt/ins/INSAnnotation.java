package org.hascoapi.transform.mt.ins;

import org.hascoapi.entity.pojo.Annotation;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSAnnotation {

    public static INSGenHelper add(INSGenHelper helper, Annotation annotation) {

        if (helper == null) {
            System.out.println("[ERROR] INSAnnotation: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSAnnotation: helper's workbook is null");
            return helper;
        }

        if (annotation == null) {
            return helper;
        }

        // Get the "Annotations" sheet
        Sheet annotationSheet = helper.workbook.getSheet(INSGen.ANNOTATIONS);

        // Calculate the index for the new row
        int rowIndex = annotationSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = annotationSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(annotation.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(annotation.getHascoTypeUri()));  

        // "rdf:type"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(annotation.getTypeUri()));  

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(annotation.getLabel());  

        // "vstoi:belongsTo"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(annotation.getBelongsTo());  

        // "vstoi:hasAnnotationStem"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(URIUtils.replaceNameSpaceEx(annotation.getHasAnnotationStem()));  

        // "vstoi:hasPosition", 
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(annotation.getHasPosition());  

        // "vstoi:hasContentWithStyle"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(annotation.getHasContentWithStyle());  

        // "rdfs:comment"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(annotation.getComment());

        // "hasco:hasImage"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(annotation.getHasImageUri());

        // "hasco:hasWebDocument"};
        Cell cell11 = newRow.createCell(10);
        cell11.setCellValue(annotation.getHasWebDocument());

        return helper;

    }

}
