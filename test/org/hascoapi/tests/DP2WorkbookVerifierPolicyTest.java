package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.ingestion.DP2WorkbookVerifier;
import org.hascoapi.ingestion.SpreadsheetRecordFile;
import org.hascoapi.utils.IngestionLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class DP2WorkbookVerifierPolicyTest {

    private static class CapturingLogger extends IngestionLogger {
        private final List<String> errors = new ArrayList<>();

        CapturingLogger() {
            super((DataFile) null);
        }

        @Override
        public void printException(String message) {
            errors.add(message);
        }

        @Override
        public void printWarning(String message) {
            // no-op for this test
        }

        List<String> getErrors() {
            return errors;
        }
    }

    @TempDir
    Path tempDir;

    @Test
    public void verify_rejectsInstrumentModelThatIsNotInsLike() throws Exception {
        File workbook = createDp2Workbook(tempDir.resolve("dp2-policy-nonins.xlsx").toFile(), "vstoi:InstrumentInstance");

        DataFile dataFile = new DataFile("DFL-TEST", workbook.getName());
        CapturingLogger logger = new CapturingLogger();
        dataFile.setLogger(logger);
        dataFile.setRecordFile(new SpreadsheetRecordFile(workbook, "InfoSheet"));

        DP2WorkbookVerifier verifier = new DP2WorkbookVerifier(dataFile, buildCatalog());
        boolean ok = verifier.verify();

        assertFalse(ok, "Verifier should fail when InstrumentInstances.a is not INS-like.");
        assertTrue(
                logger.getErrors().stream().anyMatch(msg -> msg.contains("II_MODEL_POLICY_VIOLATION")),
                "Verifier should report II_MODEL_POLICY_VIOLATION"
        );
    }

    @Test
    public void verify_rejectsInstrumentModelThatCannotBeResolved() throws Exception {
        File workbook = createDp2Workbook(tempDir.resolve("dp2-policy-ins-unresolvable.xlsx").toFile(), "https://pmsr.net/ont/INS-FAKE/MODEL001");

        DataFile dataFile = new DataFile("DFL-TEST", workbook.getName());
        CapturingLogger logger = new CapturingLogger();
        dataFile.setLogger(logger);
        dataFile.setRecordFile(new SpreadsheetRecordFile(workbook, "InfoSheet"));

        DP2WorkbookVerifier verifier = new DP2WorkbookVerifier(dataFile, buildCatalog());
        boolean ok = verifier.verify();

        assertFalse(ok, "Verifier should fail when InstrumentInstances.a does not resolve in repository.");
        assertTrue(
                logger.getErrors().stream().anyMatch(msg -> msg.contains("II_MODEL_NOT_FOUND")),
                "Verifier should report II_MODEL_NOT_FOUND"
        );
    }

    private static Map<String, String> buildCatalog() {
        Map<String, String> map = new HashMap<>();
        map.put("hasDependencies", "#Namespace");
        map.put("Deployments", "#Deployments");
        map.put("ComponentDeployments", "#ComponentDeployments");
        map.put("Platforms", "#Platforms");
        map.put("PlatformInstances", "#PlatformInstances");
        map.put("FieldsOfView", "#FieldsOfView");
        map.put("InstrumentInstances", "#InstrumentInstances");
        map.put("ComponentInstances", "#ComponentInstances");
        map.put("SensingPerspective", "#SensingPerspective");
        return map;
    }

    private static File createDp2Workbook(File target, String modelUri) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet info = wb.createSheet("InfoSheet");
            Row h = info.createRow(0);
            h.createCell(0).setCellValue("Attribute");
            h.createCell(1).setCellValue("Value");
            String[][] keys = new String[][] {
                    {"hasDependencies", "#Namespace"},
                    {"Deployments", "#Deployments"},
                    {"ComponentDeployments", "#ComponentDeployments"},
                    {"Platforms", "#Platforms"},
                    {"PlatformInstances", "#PlatformInstances"},
                    {"FieldsOfView", "#FieldsOfView"},
                    {"InstrumentInstances", "#InstrumentInstances"},
                    {"ComponentInstances", "#ComponentInstances"},
                    {"SensingPerspective", "#SensingPerspective"},
            };
            for (int i = 0; i < keys.length; i++) {
                Row r = info.createRow(i + 1);
                r.createCell(0).setCellValue(keys[i][0]);
                r.createCell(1).setCellValue(keys[i][1]);
            }

            Sheet namespace = wb.createSheet("Namespace");
            Row nsH = namespace.createRow(0);
            nsH.createCell(0).setCellValue("hasPrefix");
            nsH.createCell(1).setCellValue("hasNameSpace");
            Row nsR = namespace.createRow(1);
            nsR.createCell(0).setCellValue("pmsr");
            nsR.createCell(1).setCellValue("https://pmsr.net/ont/");

            Sheet deployments = wb.createSheet("Deployments");
            Row dH = deployments.createRow(0);
            dH.createCell(0).setCellValue("hasURI");
            dH.createCell(1).setCellValue("a");
            dH.createCell(2).setCellValue("rdfs:label");
            dH.createCell(3).setCellValue("vstoi:hasPlatformInstance");
            dH.createCell(4).setCellValue("vstoi:hasInstrumentInstance");
            dH.createCell(5).setCellValue("vstoi:designedAtTime");
            dH.createCell(6).setCellValue("prov:startedAtTime");
            Row dR = deployments.createRow(1);
            dR.createCell(0).setCellValue("https://pmsr.net/ont/DPL-TEST-001");
            dR.createCell(1).setCellValue("vstoi:Deployment");
            dR.createCell(2).setCellValue("Deployment Test");
            dR.createCell(3).setCellValue("https://pmsr.net/ont/PLI-TEST-001");
            dR.createCell(4).setCellValue("https://pmsr.net/ont/INSI-TEST-001");
            dR.createCell(5).setCellValue("2026-08-07T00:00:00.000");
            dR.createCell(6).setCellValue("2026-08-07T00:00:00.000");

            Sheet componentDeployments = wb.createSheet("ComponentDeployments");
            Row cdH = componentDeployments.createRow(0);
            cdH.createCell(0).setCellValue("Deployment URI");
            cdH.createCell(1).setCellValue("Instrument Slot URI");
            cdH.createCell(2).setCellValue("Component Instance URI");

            Sheet platforms = wb.createSheet("Platforms");
            Row pH = platforms.createRow(0);
            pH.createCell(0).setCellValue("hasURI");

            Sheet platformInstances = wb.createSheet("PlatformInstances");
            Row piH = platformInstances.createRow(0);
            piH.createCell(0).setCellValue("hasURI");
            piH.createCell(1).setCellValue("a");
            piH.createCell(2).setCellValue("rdfs:label");
            Row piR = platformInstances.createRow(1);
            piR.createCell(0).setCellValue("https://pmsr.net/ont/PLI-TEST-001");
            piR.createCell(1).setCellValue("vstoi:PlatformInstance");
            piR.createCell(2).setCellValue("Platform Instance Test");

            Sheet fov = wb.createSheet("FieldsOfView");
            Row fH = fov.createRow(0);
            fH.createCell(0).setCellValue("hasURI");

            Sheet instrumentInstances = wb.createSheet("InstrumentInstances");
            Row iiH = instrumentInstances.createRow(0);
            iiH.createCell(0).setCellValue("hasURI");
            iiH.createCell(1).setCellValue("a");
            iiH.createCell(2).setCellValue("rdfs:label");
            iiH.createCell(3).setCellValue("vstoi:hasSerialNumber");
            Row iiR = instrumentInstances.createRow(1);
            iiR.createCell(0).setCellValue("https://pmsr.net/ont/INSI-TEST-001");
            iiR.createCell(1).setCellValue(modelUri);
            iiR.createCell(2).setCellValue("Instrument Instance Test");
            iiR.createCell(3).setCellValue("SN-001");

            Sheet componentInstances = wb.createSheet("ComponentInstances");
            Row ciH = componentInstances.createRow(0);
            ciH.createCell(0).setCellValue("hasURI");
            ciH.createCell(1).setCellValue("a");
            ciH.createCell(2).setCellValue("rdfs:label");
            Row ciR = componentInstances.createRow(1);
            ciR.createCell(0).setCellValue("https://pmsr.net/ont/COMPINS-TEST-001");
            ciR.createCell(1).setCellValue("vstoi:ComponentInstance");
            ciR.createCell(2).setCellValue("Component Instance Test");

            Sheet sensing = wb.createSheet("SensingPerspective");
            Row sH = sensing.createRow(0);
            sH.createCell(0).setCellValue("hasURI");

            try (FileOutputStream fos = new FileOutputStream(target)) {
                wb.write(fos);
            }
        }
        return target;
    }
}
