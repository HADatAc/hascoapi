package org.hascoapi.utils;

import org.hascoapi.Constants;

import java.util.*;

/**
 * Centralizes all metadata types and their corresponding sheet keys
 * for consistent processing across annotators (INS, DP2, SDD, DSG, STR).
 */
public class MTSheet {

    // Map: MetadataType -> List of sheet keys
    private static final Map<String, List<String>> METADATA_SHEETS = new HashMap<>();

    static {
        METADATA_SHEETS.put(Constants.MT_INS, Arrays.asList(
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

        METADATA_SHEETS.put(Constants.MT_DP2, Arrays.asList(
                "hasDependencies",
                "Deployments",
                "Platforms",
                "PlatformInstances",
                "FieldsOfView",
                "InstrumentInstances",
                "ComponentInstances",
                "SensingPerspective"
        ));

        METADATA_SHEETS.put(Constants.MT_KGR, Arrays.asList(
                "hasDependencies",
                "hasMediaFolder",
                "verifyUri",
                "FundingSchemes",
                "Projects",
                "ProjectOrganizations",
                "Organizations",
                "Persons",
                "Places",
                "PostalAddresses"
        ));

        METADATA_SHEETS.put(Constants.MT_STD, Arrays.asList(
                "hasStudyURI",
                "hasStudyKG",
                "hasDependencies",
                "hasStudyDescription",
                "hasEntityDesign",
                "hasVariableDesign",
                "hasVersion"
        ));

        METADATA_SHEETS.put(Constants.MT_SSD, Arrays.asList(
                "hasStudyURI",
                "hasStudyKG",
                "hasDependencies",
                "hasStudyDescription",
                "hasEntityDesign",
                "hasVariableDesign",
                "hasVersion"
        ));



        METADATA_SHEETS.put(Constants.MT_DSG, Arrays.asList(
                "hasStudyURI",
                "hasStudyKG",
                "hasDependencies",
                "hasStudyDescription",
                "hasEntityDesign",
                "hasVariableDesign",
                "hasVersion"
        ));

        METADATA_SHEETS.put(Constants.MT_STR, Arrays.asList(
                "Study_ID",
                "Version",
                "FileStream",
                "MessageStream",
                "MessageTopic"
        ));
        METADATA_SHEETS.put(Constants.MT_SDD, Arrays.asList(
                "SDD_ID",
                "hasDependencies",
                "Data_Dictionary",
                "Codebook",
                "Code_Mappings",
                "Imports",
                "Timeline",
                "Version"
        ));

        METADATA_SHEETS.put(Constants.MT_WKF, Arrays.asList(
                "hasDependencies",
                "ProcessStems",
                "Processes",
                "Tasks",
                "RequiredInstruments",
                "hasVersion"
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
