package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.Project;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGRProject {

    public static KGRGenHelper add(KGRGenHelper helper, Project project) {
        if (project == null) {
            return helper;
        }

        // Get the "Projects" sheet
        Sheet projectSheet = helper.workbook.getSheet(KGRGen.PROJECTS);

        // Calculate the index for the new row
        int rowIndex = projectSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = projectSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(project.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(project.getHascoTypeUri()));
        
        // "schema:alternateName"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(project.getHasShortName() != null ? project.getHasShortName() : "");

        // "rdfs:label"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(project.getLabel());
        
        // "schema:url"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(project.getHasWebDocument() != null ? project.getHasWebDocument() : "");
        
        // "rdfs:comment"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(project.getComment() != null ? project.getComment() : "");
        
        // "schema:funding"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(URIUtils.replaceNameSpaceEx(project.getFundingUri()));
        
        // "schema:startDate"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(project.getStartDate() != null ? project.getStartDate() : "");
        
        // "schema:endDate"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(project.getEndDate() != null ? project.getEndDate() : "");

        return helper;
    }
}

