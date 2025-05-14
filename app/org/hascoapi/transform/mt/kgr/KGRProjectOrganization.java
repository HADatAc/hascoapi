package org.hascoapi.transform.mt.kgr;

import org.hascoapi.entity.pojo.Project;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class KGRProjectOrganization {

    public static KGRGenHelper add(KGRGenHelper helper, Project project) {
        if (project == null) {
            return helper;
        }

        // Get the "ProjectOrganizations" sheet
        Sheet projectOrganizationSheet = helper.workbook.getSheet(KGRGen.PROJECT_ORGANIZATIONS);

        if (project.getContributorUris().size() > 0) {

            // Calculate the index for the first new row
            int rowIndex = projectOrganizationSheet.getLastRowNum() + 1;

            for (int pos = 0; pos < project.getContributorUris().size(); pos++) {

                // Create the new row
                Row newRow = projectOrganizationSheet.createRow(pos + rowIndex);

                // "hasURI"
                Cell cell1 = newRow.createCell(0);
                cell1.setCellValue(URIUtils.replaceNameSpaceEx(project.getUri()));

                // "schema:contributor"
                Cell cell2 = newRow.createCell(1);
                cell2.setCellValue(URIUtils.replaceNameSpaceEx(project.getContributorUris().get(pos)));
                
                String orgUri = URIUtils.replaceNameSpaceEx(project.getContributorUris().get(pos));
                if (orgUri != null && !orgUri.isEmpty()) {
                    Organization organization = Organization.find(orgUri);
                    if (organization != null) {
                        helper = KGRProjectOrganization.addOrganization(helper, organization);
                    }
                }
            }

        } 

        return helper;
    }

    private static KGRGenHelper addOrganization(KGRGenHelper helper, Organization organization) {

        helper.addOrganization(organization);
        if (organization.getHasAddress() != null) {
            PostalAddress pa = organization.getHasAddress();
            helper.addPostalAddress(pa);
            if (pa.getHasAddressLocality() != null) {
                helper.addPlace(pa.getHasAddressLocality());
            }
            if (pa.getHasAddressRegion() != null) {
                helper.addPlace(pa.getHasAddressRegion());
            }
            if (pa.getHasAddressCountry() != null) {
                helper.addPlace(pa.getHasAddressCountry());
            }
        }
        return helper;
    }

}

