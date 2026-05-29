package org.hascoapi.transform.mt.dsg;

import java.util.List;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.VirtualColumn;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

/**
 * DSGVD generates Variable Design (VD) sheet for studies.
 * 
 * The VD sheet contains information about:
 * - VirtualColumns: Variables defined for Study Object Collections in the SSD
 * - SDDAttributes: Data dictionary attributes that define measurements and observations
 *
 * This reverse-engineers the Variable Design from stored VirtualColumns and SDDAttributes.
 */
public class DSGVD {

    /**
     * Add VirtualColumns and SDDAttributes for a given study to the VD sheet.
     * 
     * @param helper DSGGenHelper containing workbook
     * @param study Study to generate VD for
     * @return Updated DSGGenHelper
     */
    public static DSGGenHelper addByStudy(DSGGenHelper helper, Study study) {
        if (study == null || study.getUri() == null) {
            System.out.println("[DSGVD] Study is null or has no URI, skipping");
            return helper;
        }

        System.out.println("[DSGVD] Generating VD for study: " + study.getUri());

        Sheet vdSheet = helper.workbook.getSheet(DSGGen.VD);
        if (vdSheet == null) {
            System.err.println("[DSGVD] VD sheet not found in workbook");
            return helper;
        }

        try {
            // Retrieve VirtualColumns for this study
            List<VirtualColumn> virtualColumns = VirtualColumn.findVCsByStudy(study.getUri());
            System.out.println("[DSGVD] Found " + (virtualColumns != null ? virtualColumns.size() : 0) + " VirtualColumns for study");
            
            if (virtualColumns != null && !virtualColumns.isEmpty()) {
                for (VirtualColumn vc : virtualColumns) {
                    addVirtualColumnToVD(vdSheet, vc, study);
                }
            }
            
            // TODO: Add SDDAttributes associated with study's SDDs
            // For now, VirtualColumns are the primary focus
            
            System.out.println("[DSGVD] Completed VD generation for study: " + study.getUri());
            
        } catch (Exception e) {
            System.err.println("[DSGVD] ERROR adding VD for study " + study.getUri() + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        return helper;
    }
    
    /**
     * Add a VirtualColumn entry to the VD sheet.
     * 
     * @param vdSheet Sheet to add to
     * @param vc VirtualColumn to add
     * @param study Parent study
     */
    private static void addVirtualColumnToVD(Sheet vdSheet, VirtualColumn vc, Study study) {
        if (vc == null) {
            return;
        }
        
        try {
            int rowNum = vdSheet.getLastRowNum() + 1;
            Row row = vdSheet.createRow(rowNum);
            
            // Column 0: sheet - reference to SOC from SSD
            String socRef = vc.getSOCReference();
            if (socRef != null && !socRef.isEmpty()) {
                row.createCell(0).setCellValue("#" + socRef);
            } else {
                row.createCell(0).setCellValue("");
            }
            
            // Column 1: hasURI - the VirtualColumn URI
            String vcUri = vc.getUri();
            if (vcUri != null) {
                String abbreviatedUri = URIUtils.replaceNameSpaceEx(vcUri);
                row.createCell(1).setCellValue(abbreviatedUri);
            } else {
                row.createCell(1).setCellValue("");
            }
            
            // Column 2: variableLabel - grounding label
            String groundingLabel = vc.getGroundingLabel();
            row.createCell(2).setCellValue(groundingLabel != null ? groundingLabel : "");
            
            // Column 3: variableDefinition - description/comment
            String comment = vc.getComment();
            row.createCell(3).setCellValue(comment != null ? comment : "");
            
            // Column 4: hasUnit - typically empty for VirtualColumns
            row.createCell(4).setCellValue("");
            
            // Column 5: hasCodebook - typically empty
            row.createCell(5).setCellValue("");
            
            // Column 6: hasAttribute - typically empty for basic VCs
            row.createCell(6).setCellValue("");
            
            // Column 7: hasAttributeOf - typically empty
            row.createCell(7).setCellValue("");
            
            // Column 8: hasScale - typically empty
            row.createCell(8).setCellValue("");
            
            // Column 9: isAbout - the type/class this VC is about
            String typeUri = vc.getTypeUri();
            if (typeUri != null && !typeUri.isEmpty()) {
                String abbreviatedType = URIUtils.replaceNameSpaceEx(typeUri);
                row.createCell(9).setCellValue(abbreviatedType);
            } else {
                row.createCell(9).setCellValue("");
            }
            
            System.out.println("[DSGVD]   Added VC: " + socRef + " -> " + groundingLabel);
            
        } catch (Exception e) {
            System.err.println("[DSGVD] ERROR adding VirtualColumn to VD sheet: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
