package org.hascoapi.transform.mt.soc;

import org.apache.poi.ss.usermodel.Workbook;

/**
 * SOCGenHelper - Helper class for SOCGen
 *
 * Holds the workbook instance during generation process.
 * Follows the same pattern as DSGGenHelper, WKFGenHelper, etc.
 */
public class SOCGenHelper {
    public Workbook workbook;

    public SOCGenHelper() {
        this.workbook = null;
    }
}

