package org.hascoapi.utils;

import org.hascoapi.transform.mt.dp2.DP2Gen;
import org.hascoapi.vocabularies.VSTOI;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Regenerates the DP2 V2 workbook baseline using DP2Gen and copies it to test/resources/generated.
 */
public class RegenerateDp2V2Baseline {

    public static void main(String[] args) throws Exception {
        final String regeneratedFilename = "DP2-PMSR-V2-regenerated.xlsx";
        final String status = VSTOI.DRAFT;

        String result = DP2Gen.genByStatus(status, regeneratedFilename, null, null);
        if (result == null || result.trim().isEmpty()) {
            throw new RuntimeException("DP2Gen returned empty result for " + regeneratedFilename);
        }

        File generatedInIngestion = new File(ConfigProp.getPathIngestion(), regeneratedFilename);
        if (!generatedInIngestion.exists() || generatedInIngestion.length() == 0) {
            throw new RuntimeException("Generated file missing or empty: " + generatedInIngestion.getAbsolutePath());
        }

        File target = new File("test/resources/generated/" + regeneratedFilename);
        File parent = target.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        Files.copy(generatedInIngestion.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("GEN_RESULT=" + result);
        System.out.println("INGESTION_FILE=" + generatedInIngestion.getAbsolutePath());
        System.out.println("BASELINE_FILE=" + target.getAbsolutePath());
        System.out.println("BASELINE_SIZE=" + target.length());
    }
}
