package org.hascoapi.utils;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Fix InstrumentInstances sheet in a DP2 workbook.
 *
 * Rules implemented:
 * 1) Fill blank serial numbers with XXYYYY where XX in {08,09,10} and YYYY starts at 0001.
 * 2) Set column "a" to a simulator model URI from INS-PMSR Instruments sheet.
 *    Uses DP2 description hints (low/high fidelity) and best-effort label matching.
 * 3) Set rdfs:label to "[model label]. SN: [serial]".
 *
 * Usage:
 * runMain org.hascoapi.utils.FixDp2PiagetInstrumentInstances <dp2.xlsx> <ins.xlsx>
 */
public class FixDp2PiagetInstrumentInstances {

    private static final String SHEET_INSTRUMENTS_INS = "Instruments";
    private static final String SHEET_INSTRUMENT_INSTANCES = "InstrumentInstances";
    private static final String SHEET_PLATFORM_INSTANCES = "PlatformInstances";
    private static final String SHEET_DEPLOYMENTS = "Deployments";

    private static final String ORG_GAIA = "08";
    private static final String ORG_VISEU = "09";
    private static final String ORG_SILVES = "10";

    private static class InstrumentModel {
        String uri;
        String label;
        String comment;

        InstrumentModel(String uri, String label, String comment) {
            this.uri = uri;
            this.label = label;
            this.comment = comment;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: runMain org.hascoapi.utils.FixDp2PiagetInstrumentInstances <dp2.xlsx> <ins.xlsx>");
            System.exit(1);
        }

        File dp2File = new File(args[0]);
        File insFile = new File(args[1]);
        if (!dp2File.exists()) {
            System.err.println("[ERROR] DP2 file not found: " + args[0]);
            System.exit(1);
        }
        if (!insFile.exists()) {
            System.err.println("[ERROR] INS file not found: " + args[1]);
            System.exit(1);
        }

        File backup = new File(dp2File.getAbsolutePath() + ".before-instrument-fix");
        File tmp = new File(dp2File.getAbsolutePath() + ".tmp");

        try {
            copyFile(dp2File, backup);

            try (Workbook dp2 = new XSSFWorkbook(new FileInputStream(dp2File));
                 Workbook ins = new XSSFWorkbook(new FileInputStream(insFile))) {

                List<InstrumentModel> models = loadInstrumentModels(ins.getSheet(SHEET_INSTRUMENTS_INS));
                if (models.isEmpty()) {
                    throw new RuntimeException("No models loaded from INS sheet Instruments.");
                }

                InstrumentModel lowFidelityModel = findBestModel(models, "low-fidelity mannequin", "low fidelity mannequin");
                InstrumentModel highFidelityModel = findBestModel(models, "laerdal nursing anne mannequin", "laerdal nursing anne");

                // Optional preference if a "Nursing Anne Basic" instrument class exists in INS.
                InstrumentModel anneBasic = findBestModel(models, "nursing anne basic", "");
                if (anneBasic != null) {
                    lowFidelityModel = anneBasic;
                }

                if (lowFidelityModel == null) {
                    lowFidelityModel = findBestModel(models, "nursing anne basic", "");
                }
                if (highFidelityModel == null) {
                    highFidelityModel = findBestModel(models, "nursing anne", "");
                }
                if (lowFidelityModel == null) {
                    lowFidelityModel = findBestModel(models, "mannequin", "simulator");
                }
                if (highFidelityModel == null) {
                    highFidelityModel = findBestModel(models, "mannequin", "simulator");
                }
                if (lowFidelityModel == null || highFidelityModel == null) {
                    // Last-resort fallback to first INS model so sheet can still be normalized.
                    InstrumentModel fallback = models.get(0);
                    if (lowFidelityModel == null) {
                        lowFidelityModel = fallback;
                    }
                    if (highFidelityModel == null) {
                        highFidelityModel = fallback;
                    }
                }

                Sheet platformSheet = dp2.getSheet(SHEET_PLATFORM_INSTANCES);
                Sheet deploymentSheet = dp2.getSheet(SHEET_DEPLOYMENTS);
                Sheet iiSheet = dp2.getSheet(SHEET_INSTRUMENT_INSTANCES);
                if (iiSheet == null) {
                    throw new RuntimeException("Missing sheet InstrumentInstances in DP2 file.");
                }

                Map<String, String> platformUriToOrgCode = buildPlatformOrgMap(platformSheet);
                Map<String, String> instrumentUriToOrgCode = buildInstrumentOrgFromDeployments(deploymentSheet, platformUriToOrgCode);

                // Column G (vstoi:hasOwner) is authoritative for org assignment.
                Map<String, String> ownerUriToOrgCode = buildOwnerOrgCodeMap(iiSheet);

                Map<String, Integer> seqByOrg = new HashMap<>();
                seqByOrg.put(ORG_GAIA, 1);
                seqByOrg.put(ORG_VISEU, 1);
                seqByOrg.put(ORG_SILVES, 1);

                int fixedSerials = 0;
                int fixedTypes = 0;
                int fixedLabels = 0;
                int rowsTouched = 0;

                for (int r = 1; r <= iiSheet.getLastRowNum(); r++) {
                    Row row = iiSheet.getRow(r);
                    if (row == null) {
                        continue;
                    }

                    String instUri = getCellString(row, 0);
                    if (isBlank(instUri)) {
                        continue;
                    }

                    String description = getCellString(row, 4);
                    String oldLabel = getCellString(row, 2);
                    String ownerUri = getCellString(row, 6);
                    String orgCode = ownerUriToOrgCode.get(ownerUri);
                    if (orgCode == null) {
                        orgCode = instrumentUriToOrgCode.get(instUri);
                    }
                    if (orgCode == null) {
                        orgCode = inferOrgFromText(oldLabel + " " + description);
                    }
                    if (orgCode == null) {
                        orgCode = ORG_GAIA;
                    }

                    String serial = getCellString(row, 3);
                    int next = seqByOrg.getOrDefault(orgCode, 1);
                    String expectedSerial = orgCode + String.format("%04d", next);
                    seqByOrg.put(orgCode, next + 1);

                    if (!expectedSerial.equals(serial)) {
                        serial = expectedSerial;
                        setCellString(row, 3, serial);
                        fixedSerials++;
                    }

                    InstrumentModel chosen = chooseModel(models, lowFidelityModel, highFidelityModel, oldLabel, description);
                    if (chosen != null) {
                        String existingType = getCellString(row, 1);
                        if (!chosen.uri.equals(existingType)) {
                            setCellString(row, 1, chosen.uri);
                            fixedTypes++;
                        }

                        String newLabel = chosen.label + ". SN: " + serial;
                        String existingLabel = getCellString(row, 2);
                        if (!newLabel.equals(existingLabel)) {
                            setCellString(row, 2, newLabel);
                            fixedLabels++;
                        }
                    }

                    rowsTouched++;
                }

                try (FileOutputStream fos = new FileOutputStream(tmp)) {
                    dp2.write(fos);
                }

                System.out.println("[OK] Rows processed: " + rowsTouched);
                System.out.println("[OK] Serials generated: " + fixedSerials);
                System.out.println("[OK] Type URIs updated: " + fixedTypes);
                System.out.println("[OK] Labels updated: " + fixedLabels);
                System.out.println("[INFO] low-fidelity model URI: " + lowFidelityModel.uri + " | label: " + lowFidelityModel.label);
                System.out.println("[INFO] high-fidelity model URI: " + highFidelityModel.uri + " | label: " + highFidelityModel.label);
            }

            if (!dp2File.delete()) {
                throw new RuntimeException("Could not delete original DP2 file before replacement.");
            }
            if (!tmp.renameTo(dp2File)) {
                throw new RuntimeException("Could not move temporary file into place.");
            }

            System.out.println("[OK] Updated file: " + dp2File.getAbsolutePath());
            System.out.println("[OK] Backup file: " + backup.getAbsolutePath());

        } catch (Exception e) {
            if (tmp.exists()) {
                tmp.delete();
            }
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<InstrumentModel> loadInstrumentModels(Sheet instrumentsSheet) {
        List<InstrumentModel> models = new ArrayList<>();
        if (instrumentsSheet == null) {
            return models;
        }

        for (int r = 1; r <= instrumentsSheet.getLastRowNum(); r++) {
            Row row = instrumentsSheet.getRow(r);
            if (row == null) {
                continue;
            }

            String uri = getCellString(row, 0);
            String label = getCellString(row, 3);
            String comment = getCellString(row, 8);
            if (isBlank(uri) || isBlank(label)) {
                continue;
            }
            models.add(new InstrumentModel(uri.trim(), label.trim(), comment == null ? "" : comment.trim()));
        }

        return models;
    }

    private static InstrumentModel findBestModel(List<InstrumentModel> models, String requiredPhrase, String backupPhrase) {
        String required = normalize(requiredPhrase);
        String backup = normalize(backupPhrase);

        for (InstrumentModel m : models) {
            String hay = normalize(m.label + " " + m.comment);
            if (!required.isEmpty() && hay.contains(required)) {
                return m;
            }
        }

        if (!backup.isEmpty()) {
            for (InstrumentModel m : models) {
                String hay = normalize(m.label + " " + m.comment);
                if (hay.contains(backup)) {
                    return m;
                }
            }
        }

        return null;
    }

    private static InstrumentModel chooseModel(List<InstrumentModel> models,
                                               InstrumentModel lowFidelity,
                                               InstrumentModel highFidelity,
                                               String label,
                                               String description) {
        String text = normalize(label + " " + description);

        if (text.contains("low fidelity")) {
            return lowFidelity;
        }
        if (text.contains("high fidelity")) {
            return highFidelity;
        }

        // If the row already references a known model by name in text, use that model.
        for (InstrumentModel m : models) {
            String mLabel = normalize(m.label);
            if (!mLabel.isEmpty() && text.contains(mLabel)) {
                return m;
            }
        }

        // Default to low-fidelity for mannequins when no explicit clue exists.
        if (text.contains("mannequin") || text.contains("manequin") || text.contains("nursing anne")) {
            return lowFidelity;
        }

        return lowFidelity;
    }

    private static Map<String, String> buildPlatformOrgMap(Sheet platformSheet) {
        Map<String, String> map = new HashMap<>();
        if (platformSheet == null) {
            return map;
        }

        for (int r = 1; r <= platformSheet.getLastRowNum(); r++) {
            Row row = platformSheet.getRow(r);
            if (row == null) {
                continue;
            }
            String platformUri = getCellString(row, 0);
            String platformLabel = getCellString(row, 2);
            String orgCode = inferOrgFromText(platformLabel);
            if (!isBlank(platformUri) && orgCode != null) {
                map.put(platformUri, orgCode);
            }
        }

        return map;
    }

    private static Map<String, String> buildInstrumentOrgFromDeployments(Sheet deploymentSheet,
                                                                          Map<String, String> platformToOrg) {
        Map<String, String> map = new HashMap<>();
        if (deploymentSheet == null) {
            return map;
        }

        for (int r = 1; r <= deploymentSheet.getLastRowNum(); r++) {
            Row row = deploymentSheet.getRow(r);
            if (row == null) {
                continue;
            }
            String platformUri = getCellString(row, 3);
            String instrumentUri = getCellString(row, 4);
            if (isBlank(platformUri) || isBlank(instrumentUri)) {
                continue;
            }
            String orgCode = platformToOrg.get(platformUri);
            if (orgCode != null) {
                map.put(instrumentUri, orgCode);
            }
        }

        return map;
    }

    private static Map<String, String> buildOwnerOrgCodeMap(Sheet iiSheet) {
        Map<String, String> ownerToOrg = new LinkedHashMap<>();
        List<String> fallbackCodes = Arrays.asList(ORG_GAIA, ORG_VISEU, ORG_SILVES);
        int fallbackIndex = 0;

        if (iiSheet == null) {
            return ownerToOrg;
        }

        for (int r = 1; r <= iiSheet.getLastRowNum(); r++) {
            Row row = iiSheet.getRow(r);
            if (row == null) {
                continue;
            }

            String ownerUri = getCellString(row, 6);
            if (isBlank(ownerUri) || ownerToOrg.containsKey(ownerUri)) {
                continue;
            }

            String inferred = inferOrgFromText(ownerUri);
            if (inferred != null) {
                ownerToOrg.put(ownerUri, inferred);
                continue;
            }

            // Deterministic fallback: assign in encounter order to 08, 09, 10.
            if (fallbackIndex < fallbackCodes.size()) {
                ownerToOrg.put(ownerUri, fallbackCodes.get(fallbackIndex));
                fallbackIndex++;
            } else {
                ownerToOrg.put(ownerUri, ORG_GAIA);
            }
        }

        return ownerToOrg;
    }

    private static String inferOrgFromText(String text) {
        String t = normalize(text);
        if (t.contains("gaia")) {
            return ORG_GAIA;
        }
        if (t.contains("viseu") || t.contains("viseu")) {
            return ORG_VISEU;
        }
        if (t.contains("silves")) {
            return ORG_SILVES;
        }
        return null;
    }

    private static String getCellString(Row row, int col) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(col);
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double n = cell.getNumericCellValue();
            long ln = (long) n;
            if (Math.abs(n - ln) < 0.0000001d) {
                return String.valueOf(ln);
            }
            return String.valueOf(n);
        }
        if (cell.getCellType() == CellType.BOOLEAN) {
            return String.valueOf(cell.getBooleanCellValue());
        }
        if (cell.getCellType() == CellType.FORMULA) {
            return cell.getCellFormula();
        }
        return null;
    }

    private static void setCellString(Row row, int col, String value) {
        Cell cell = row.getCell(col);
        if (cell == null) {
            cell = row.createCell(col);
        }
        cell.setCellValue(value == null ? "" : value);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.toLowerCase(Locale.ROOT)
                .replace('á', 'a')
                .replace('à', 'a')
                .replace('â', 'a')
                .replace('ã', 'a')
                .replace('é', 'e')
                .replace('ê', 'e')
                .replace('í', 'i')
                .replace('ó', 'o')
                .replace('ô', 'o')
                .replace('õ', 'o')
                .replace('ú', 'u')
                .replace('ç', 'c');
    }

    private static void copyFile(File source, File target) throws Exception {
        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, read);
            }
        }
    }
}