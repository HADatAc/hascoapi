package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.hascoapi.Constants;
import org.hascoapi.transform.mt.dp2.DP2Gen;
import org.hascoapi.utils.MTSheet;
import org.junit.jupiter.api.Test;

public class DP2ComponentDeploymentsStructureTest {

    @Test
    public void mtSheet_dp2IncludesComponentDeployments_afterDeployments() {
        List<String> sheets = MTSheet.getSheetsForType(Constants.MT_DP2);
        int deployIdx = sheets.indexOf("Deployments");
        int compDepIdx = sheets.indexOf("ComponentDeployments");

        assertTrue(deployIdx >= 0, "Deployments key should exist in DP2 catalog");
        assertTrue(compDepIdx >= 0, "ComponentDeployments key should exist in DP2 catalog");
        assertEquals(deployIdx + 1, compDepIdx, "ComponentDeployments should follow Deployments in DP2 catalog order");
    }

    @Test
    public void dp2Gen_createIncludesComponentDeploymentsSheet_withExpectedHeaders() throws Exception {
        Workbook wb = DP2Gen.create("/tmp/ignore.xlsx");
        try {
            Sheet sheet = wb.getSheet("ComponentDeployments");
            assertNotNull(sheet, "DP2 workbook should include ComponentDeployments sheet");

            Row header = sheet.getRow(0);
            assertNotNull(header, "ComponentDeployments header row should exist");
            assertEquals("Deployment URI", header.getCell(0).getStringCellValue());
            assertEquals("Instrument Slot URI", header.getCell(1).getStringCellValue());
            assertEquals("Component Instance URI", header.getCell(2).getStringCellValue());

            Sheet info = wb.getSheet("InfoSheet");
            assertNotNull(info, "InfoSheet should exist");

            boolean found = false;
            for (int r = 1; r <= info.getLastRowNum(); r++) {
                Row row = info.getRow(r);
                if (row == null || row.getCell(0) == null || row.getCell(1) == null) {
                    continue;
                }
                if ("ComponentDeployments".equals(row.getCell(0).getStringCellValue())
                        && "#ComponentDeployments".equals(row.getCell(1).getStringCellValue())) {
                    found = true;
                    break;
                }
            }
            assertTrue(found, "InfoSheet should declare ComponentDeployments mapping");
        } finally {
            wb.close();
        }
    }
}
