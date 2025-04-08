package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.Person;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGRPerson {

    public static KGRGenHelper add(KGRGenHelper helper, Person person) {

        if (person == null) {
            return helper;
        }

        // Get the "Persons" sheet
        Sheet personSheet = helper.workbook.getSheet(KGRGen.PERSONS);

        // Calculate the index for the new row
        int rowIndex = personSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = personSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(person.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(person.getHascoTypeUri()));
        
        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(person.getLabel());
        
        // "schema:alternateName"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(person.getHasShortName() != null ? person.getHasShortName() : "");
        
        // "rdf:type"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(person.getTypeUri()));
        
        // "foaf:givenName"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(person.getGivenName() != null ? person.getGivenName() : "");
        
        // "foaf:familyName"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(person.getFamilyName() != null ? person.getFamilyName() : "");
        
        // "foaf:mbox"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(person.getMbox() != null ? person.getMbox() : "");
        
        // "foaf:member"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(person.getHasAffiliationUri() != null ? URIUtils.replaceNameSpaceEx(person.getHasAffiliationUri()) : "");

        return helper;
    }
}
