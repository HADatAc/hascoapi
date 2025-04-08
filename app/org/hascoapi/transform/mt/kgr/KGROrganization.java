package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGROrganization {

    public static KGRGenHelper add(KGRGenHelper helper, Organization organization) {

        if (organization == null) {
            return helper;
        }

        // Get the "Organizations" sheet
        Sheet organizationSheet = helper.workbook.getSheet(KGRGen.ORGANIZATIONS);

        // Calculate the index for the new row
        int rowIndex = organizationSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = organizationSheet.createRow(rowIndex);

        // "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(organization.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(organization.getHascoTypeUri()));
        
        // "rdfs:label"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(organization.getLabel());
        
        // "schema:alternateName"
        Cell cell4 = newRow.createCell(3);
        cell4.setCellValue(organization.getHasShortName() != null ? organization.getHasShortName() : "");
        
        // "rdf:type"
        Cell cell5 = newRow.createCell(4);
        cell5.setCellValue(URIUtils.replaceNameSpaceEx(organization.getTypeUri()));
        
        // "foaf:name"
        Cell cell6 = newRow.createCell(5);
        cell6.setCellValue(organization.getName() != null ? organization.getName() : "");
        
        // "foaf:mbox"
        Cell cell7 = newRow.createCell(6);
        cell7.setCellValue(organization.getMbox() != null ? organization.getMbox() : "");
        
        // "schema:telephone"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(organization.getTelephone() != null ? organization.getTelephone() : "");
        
        // "schema:url"
        Cell cell9 = newRow.createCell(8);
        cell9.setCellValue(organization.getHasWebDocument() != null ? organization.getHasWebDocument() : "");
        
        // "schema:parentOrganization"
        Cell cell10 = newRow.createCell(9);
        cell10.setCellValue(organization.getParentOrganizationUri() != null ? URIUtils.replaceNameSpaceEx(organization.getParentOrganizationUri()) : "");

        return helper;
    }
}

