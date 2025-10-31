package org.hascoapi.utils;

import java.util.*;

/**
 * Centralizes all metadata types and their corresponding sheet keys
 * for consistent processing across annotators (INS, DP2, SDD, DSG, STR).
 */
public class MetadataSheetsCatalog {

    // Map: MetadataType -> List of sheet keys
    private static final Map<String, List<String>> METADATA_SHEETS = new HashMap<>();

    static {
        METADATA_SHEETS.put("INS", Arrays.asList(
                "hasDependencies",
                "Instruments",
                "SlotElements",
                "ComponentStems",
                "Components",
                "CodeBooks",
                "CodeBookSlots",
                "ResponseOptions",
                "Annotations",
                "AnnotationStems"
        ));

        METADATA_SHEETS.put("DP2", Arrays.asList(
                "hasDependencies",
                "Deployments",
                "PlatformModels",
                "Platforms",
                "FieldsOfView",
                "Instruments",
                "Components",
                "SensingPerspective"
        ));

        METADATA_SHEETS.put("SDD", Arrays.asList(
                "SDD_ID",
                "hasDependencies",
                "Data_Dictionary",
                "Codebook",
                "Code_Mappings",
                "Imports",
                "Timeline",
                "Version"
        ));

        METADATA_SHEETS.put("DSG", Arrays.asList(
                "hasStudyURI",
                "hasStudyKG",
                "hasDependencies",
                "hasStudyDescription",
                "hasEntityDesign",
                "hasVariableDesign",
                "hasVersion"
        ));

        METADATA_SHEETS.put("STR", Arrays.asList(
                "Study_ID",
                "Version",
                "FileStream",
                "MessageStream",
                "MessageTopic"
        ));
    }

    /**
     * Returns the list of sheet keys for a given metadata type (e.g., "INS", "DP2", etc.).
     */
    public static List<String> getSheetsForType(String metadataType) {
        return METADATA_SHEETS.getOrDefault(metadataType.toUpperCase(), Collections.emptyList());
    }

    /**
     * Returns all registered metadata types.
     */
    public static Set<String> getMetadataTypes() {
        return METADATA_SHEETS.keySet();
    }

    /**
     * Prints all metadata types and their sheet keys (for debugging).
     */
    public static void printCatalog() {
        METADATA_SHEETS.forEach((type, sheets) -> {
            System.out.println("Metadata type: " + type);
            sheets.forEach(sheet -> System.out.println("  - " + sheet));
        });
    }
}
