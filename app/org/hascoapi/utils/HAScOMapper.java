package org.hascoapi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.VSTOI;



public class HAScOMapper {

    public static final String FULL = "full";

    public static final String ESSENTIAL = "essential";

    /**
     *
     * This method requests a typeResult that is the main HAScO concept to be
     * serialized. According to this
     * main concept, this method will include filter for all the HAScO classes that
     * are currently filtered. In
     * general, a serializeAll filter will be added for the main concept, and a more
     * restricted filter that
     * includes just the core properties of each concept is serialized for the
     * non-main concept.
     *
     * @param typeResult
     * @param mode 'full' 'essential'
     * @return filtered Jackson's ObjectMapper
     */
    public static ObjectMapper getFiltered(String mode, String typeResult) {
        //System.out.println("HAScO.getFiltered() with mode [" + mode + "] and typeResult [" + typeResult +"]");
        ObjectMapper mapper = new ObjectMapper();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();

        // ACTUATOR
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ACTUATOR)) {
            filterProvider.addFilter("actuatorFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("actuatorFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail",  "isAttributeOf", 
                            "hasActuatorStem", "actuatorStem", "hasCodebook", "codebook"));
        }

        // ACTUATOR_INSTANCE
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ACTUATOR_INSTANCE)) {
            filterProvider.addFilter("actuatorInstanceFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("actuatorInstanceFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasStatus", "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasAcquisitionDate", "isDamaged", "hasDamageDate", 
                            "hasOwnerUri", "hasOwner", "hasMaintainerUri", "hasMaintainer", "hasSIRManagerEmail"));
        }

        // ACTUATOR_STEM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ACTUATOR_STEM)) {
            filterProvider.addFilter("actuatorStemFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("actuatorStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail", "activates", "activatesSemanticVariable"));
        }
        // ANNOTATION
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ANNOTATION)) {
            filterProvider.addFilter("annotationFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("annotationFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "belongsTo", "container",
                            "hasAnnotationStem", "annotationStem", "hasPosition", "hasStyle"));
        }

        // ANNOTATION_STEM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ANNOTATION_STEM)) {
            filterProvider.addFilter("annotationStemFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("annotationStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail"));
        }

        // ATTRIBUTE
        if (mode.equals(FULL) && typeResult.equals(SIO.ATTRIBUTE)) {
            filterProvider.addFilter("variableFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("variableFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "variableSpec"));
        }

        // CODEBOOK
        if (mode.equals(FULL) && typeResult.equals(VSTOI.CODEBOOK)) {
            filterProvider.addFilter("codebookFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("codebookFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasSerialNumber", "responseOptions", "hasLanguage",
                            "hasVersion", "wasDerivedFrom", "hasSIRManagerEmail", "hasEditorEmail", "CodebookSlots"));
        }

        // CODEBOOK SLOT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.CODEBOOK_SLOT)) {
            filterProvider.addFilter("CodebookSlotFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("CodebookSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasPriority", "hasResponseOption", "responseOption"));
        }

        // COMPONENT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.COMPONENT)) {
            filterProvider.addFilter("componentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("componentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasMakerUri", "hasMaker", 
                            "hasSIRManagerEmail", "hasEditorEmail",  "isAttributeOf", "hasCodebook", "codebook"));
        }

        // CONTAINER
        if (mode.equals(FULL) && typeResult.equals(VSTOI.CONTAINER)) {
            filterProvider.addFilter("containerFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("containerFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "hasInformant", "comment", "belongsTo", "hasFirst", "hasSerialNumber", "hasLanguage", 
                            "hasVersion", "hasPriority", "hasNext", "hasPrevious", "wasDerivedFrom", "hasMakerUri", "hasMaker",
                            "hasSIRManagerEmail", "hasEditorEmail"));
        }

        // CONTAINER_SLOT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.CONTAINER_SLOT)) {
            filterProvider.addFilter("containerSlotFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("containerSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasNext", "hasPrevious", "hasPriority", "hasComponent", "component", "hasSubcontainer", "detector", 
                            "subcontainer", "belongsTo"));
        }

        // DA
        if (mode.equals(FULL) && typeResult.equals(HASCO.DATA_ACQUISITION)) {
            filterProvider.addFilter("daFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("daFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasVersion",  "isMemberOf", "hasDD", "hasSDD", "comment", 
                            "hasTotalRecordedMessages", "hasNumberDataPoints", 
                            "hasDataFileUri", "hasDataFile", "hasDD", "hasDDUri", "hasSDD", "hasSDDUri"));
        }

        // DATA FILE
        if (mode.equals(FULL) && typeResult.equals(HASCO.DATAFILE)) {
            filterProvider.addFilter("dataFileFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("dataFileFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "id", "label", "typeUri", 
                            "typeLabel", "hascoTypeUri", "hascoTypeLabel",
                            "streamUri", "streamTopicUri", 
                            "comment", "filename", "fileStatus", "lastProcessTime", "file"));
        }

        // DD
        if (mode.equals(FULL) && typeResult.equals(HASCO.DD)) {
            filterProvider.addFilter("ddFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("ddFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasVersion",  "comment", "hasDataFileUri", "hasDataFile"));
        }

        // DEPLOYMENT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DEPLOYMENT)) {
            filterProvider.addFilter("deploymentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("deploymentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasVersion", "platformInstanceUri", "instrumentInstanceUri", "detectorinstanceUri",
                            "designedAt", "startedAt", "endedAt"));
        }

        // DP2
        if (mode.equals(FULL) && typeResult.equals(HASCO.DP2)) {
            filterProvider.addFilter("dp2Filter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("dp2Filter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasVersion",  "comment", "hasDataFileUri", "hasDataFile"));
        }

        // DETECTOR
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DETECTOR)) {
            filterProvider.addFilter("detectorFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("detectorFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasMakerUri", "hasSIRManagerEmail", "hasEditorEmail",  "isAttributeOf", 
                            "hasDetectorStem", "detectorStem", "hasCodebook", "codebook"));
        }

        // DETECTOR_INSTANCE
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DETECTOR_INSTANCE)) {
            filterProvider.addFilter("detectorInstanceFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("detectorInstanceFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasStatus", "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasAcquisitionDate", "isDamaged", "hasDamageDate", 
                            "hasOwnerUri", "hasOwner", "hasMaintainerUri", "hasMaintainer", "hasSIRManagerEmail"));
        }

        // DETECTOR_STEM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DETECTOR_STEM)) {
            filterProvider.addFilter("detectorStemFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("detectorStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail", "detects", "detectsSemanticVariable"));
        }

        // DETECTOR_STEM_TYPE
        /*
        filterProvider.addFilter("detectorStemTypeFilter", 
            SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "className", "superUri", "superLabel", "comment"));
        */

        // DSG
        if (mode.equals(FULL) && typeResult.equals(HASCO.DSG)) {
            filterProvider.addFilter("dsgFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("dsgFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasDataFileUri", "hasDataFile"));
        }

        // ENTITY
        if (mode.equals(FULL) && typeResult.equals(SIO.ENTITY)) {
            filterProvider.addFilter("entityFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("entityFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment"));
        }

        // FUNDING_SCHEME
        if (mode.equals(FULL) && typeResult.equals(SCHEMA.FUNDING_SCHEME)) {
            filterProvider.addFilter("fundingSchemeFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("fundingSchemeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", 
                            "typeLabel", "hascoTypeUri", "hasStatus", "hasShortName", "hasImageUri", 
                            "hasWebDocument", "hascoTypeLabel", "comment", "funderUri", "funder", 
                            "hasVersion", "hasReviewNote", "hasEditorEmail",  
                            "sponsorUri", "sponsor", "startDate", "endDate", "amount", "hasSIRManagerEmail"));
        }

        // HASCO_CLASS
        if (mode.equals(FULL) && typeResult.equals(HASCO.HASCO_CLASS)) {
            filterProvider.addFilter("hascoClassFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("hascoClassFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "nodeId", "comment", "isDomainOf", "isRangeOf", "isDisjointWith", "hasStatus", "subClasses"));
        }

        // INS
        if (mode.equals(FULL) && typeResult.equals(HASCO.INS)) {
            filterProvider.addFilter("insFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("insFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasVersion", "comment", "hasDataFileUri", "hasDataFile"));
        }

        // INSTRUMENT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.INSTRUMENT)) {
            filterProvider.addFilter("instrumentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("instrumentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "hasInformant", "comment", "hasFirst", "hasSerialNumber", "hasLanguage", 
                            "hasVersion", "wasDerivedFrom", "hasSIRManagerEmail", "hasEditorEmail"));
        }

        // INSTRUMENT_INSTANCE
        if (mode.equals(FULL) && typeResult.equals(VSTOI.INSTRUMENT_INSTANCE)) {
            filterProvider.addFilter("instrumentInstanceFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("instrumentInstanceFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasStatus", 
                            "hasSerialNumber", "hasAcquisitionDate", "isDamaged", "hasDamageDate", 
                            "hasOnwerUri", "hasOwner", "hasMaintaineruri", "hasMaintainer", "hasSIRManagerEmail"));
        }

        // KGR
        if (mode.equals(FULL) && typeResult.equals(HASCO.KGR)) {
            filterProvider.addFilter("kgrFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("kgrFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", 
                        "hasStatus", "hascoTypeUri",
                        "hascoTypeLabel", "comment", "hasDataFile", "dataFile"));
        }

        // ORGANIZATION
        if (mode.equals(FULL) && typeResult.equals(SCHEMA.ORGANIZATION)) {
            filterProvider.addFilter("organizationFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("organizationFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", 
                        "hasStatus", "hascoTypeUri",
                        "hasImageUri", "hasWebDocument", 
                        "hascoTypeLabel", "comment", "name", "mbox", "telephone", "url", 
                        "parentOrganizationUri", "childrenOrganizations"));
        }

        // PERSON
        if (mode.equals(FULL) && typeResult.equals(SCHEMA.PERSON)) {
            filterProvider.addFilter("personFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("personFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", 
                        "hasStatus", "hascoTypeUri", "hasImageUri", "hasWebDocument", 
                        "hascoTypeLabel", "comment", "name", "mbox", "telephone", "member", 
                        "givenName", "familyName", 
                        "hasAffiliation", "hasUrl", "jobTitle"));
        }

        // PLACE
        if (mode.equals(FULL) && typeResult.equals(SCHEMA.PLACE)) {
            filterProvider.addFilter("placeFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("placeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", 
                        "hasStatus", "hascoTypeUri", "hasImageUri", "hasWebDocument", 
                        "hascoTypeLabel", "comment", "name", "hasAddress", "containedInPlace", "hasIdentifier", 
                        "hasGeo", "hasLatitude", "hasLongitude", "hasUrl"));
        }
 
        // PLATFORM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.PLATFORM)) {
            filterProvider.addFilter("platformFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("platformFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasVersion"));
        }

        // PLATFORM_INSTANCE
        if (mode.equals(FULL) && typeResult.equals(VSTOI.PLATFORM_INSTANCE)) {
            filterProvider.addFilter("platformInstanceFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("platformInstanceFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasStatus", 
                            "hasSerialNumber", "hasAcquisitionDate", "isDamaged", "hasDamageDate", 
                            "hasOwnerUri", "hasOwner", "hasMaintainerUri", "hasMaintainer", "hasSIRManagerEmail"));
        }

        // POSSIBLE_VALUE
        if (mode.equals(FULL) && typeResult.equals(HASCO.POSSIBLE_VALUE)) {
            filterProvider.addFilter("possibleValueFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("possibleValueFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "partOfSchema", "listPosition", "isPossibleValueOf", 
                            "hasCode", "hasCodeLabel", "hasClass", "hasSIRManagementEmail"));
        }

        // POSTAL_ADDRESS
        if (mode.equals(FULL) && typeResult.equals(SCHEMA.POSTAL_ADDRESS)) {
            filterProvider.addFilter("postalAddressFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("postalAddressFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", 
                        "hasStatus", "hascoTypeUri", "hasImageUri", "hasWebDocument", 
                        "hascoTypeLabel", "comment", "name", "hasStreetAddress", "hasPostalCode", "hasAddressLocalityUri",
                        "hasAddressRegionUri", "hasAddressCountryUri", "hasAddressLocality", "hasAddressRegion", "hasAddressCountry"));
        }
 
        // PROCESS
        if (mode.equals(FULL) && typeResult.equals(VSTOI.PROCESS)) {
            filterProvider.addFilter("processFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("processFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail",
                            "hasTopTask"));
        }
 
        // PROCESS_STEM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.PROCESS_STEM)) {
            filterProvider.addFilter("processStemFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("processStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail"));
        }
         
        // PROJECT
        if (mode.equals(FULL) && typeResult.equals(SCHEMA.PROJECT)) {
            filterProvider.addFilter("projectFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("projectFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", 
                        "typeLabel", "hascoTypeLabel", "comment", "hascoTypeUri", 
                        "hasStatus", "hasShortName", "hasImageUri", "hasWebDocument", 
                        "hasVersion", "hasReviewNote", "hasEditorEmail",  
                        "fundingUri", "funding", "contributorUris", "contributors",
                        "startDate", "endDate", "hasSIRManagerEmail"));
        }

        // REQUIRED_COMPONENT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.REQUIRED_COMPONENT)) {
            filterProvider.addFilter("requiredComponentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("requiredComponentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "usedComponent", "hasContainerSlotUri"));
        }
 
        // REQUIRED_INSTRUMENT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.REQUIRED_INSTRUMENT)) {
            filterProvider.addFilter("requiredInstrumentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("requiredInstrumentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "usedInstrument", "hasRequiredComponent", "instrument", "components"));
        }
 
        // RESPONSE OPTION
        if (mode.equals(FULL) && typeResult.equals(VSTOI.RESPONSE_OPTION)) {
            filterProvider.addFilter("responseOptionFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("responseOptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "hasSIRManagerEmail", "hasEditorEmail"));
        }

        // SDD
        if (mode.equals(FULL) && typeResult.equals(HASCO.SDD)) {
            filterProvider.addFilter("sddFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("sddFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasVersion",  "comment", "hasDataFileUri", "hasDataFile"));
        }

        // SDD_ATTRIBUTE
        if (mode.equals(FULL) && typeResult.equals(HASCO.SDD_ATTRIBUTE)) {
            filterProvider.addFilter("sddAttributeFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("sddAttributeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "partOfSchema", "listPosition", "attribute", "objectUri", 
                            "unit", "eventUri", "inRelationTo", "wasDerivedFrom", "hasSIRManagerEmail"));
        }

        // SDD_OBJECT
        if (mode.equals(FULL) && typeResult.equals(HASCO.SDD_OBJECT)) {
            filterProvider.addFilter("sddObjectFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("sddObjectFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "partOfSchema", "listPosition", "entity", "role", "relation", 
                            "inRelationTo", "wasDerivedFrom", "hasSIRManagementEmail"));
        }

        // SEMANTIC_DATA_DICTIONARY
        if (mode.equals(FULL) && typeResult.equals(HASCO.SEMANTIC_DATA_DICTIONARY)) {
            filterProvider.addFilter("semanticDataDictionaryFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("semanticDataDictionaryFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "hasVersion",  "comment"));
        }

        // SEMANTIC_VARIABLE
        if (mode.equals(FULL) && typeResult.equals(HASCO.SEMANTIC_VARIABLE)) {
            filterProvider.addFilter("semanticVariableFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("semanticVariableFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "entityUri", "attributeUri", "inRelationToUri", "unitUri", "timeUri"));
        }

        // STR
        if (mode.equals(FULL) && typeResult.equals(HASCO.STR)) {
            filterProvider.addFilter("strFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("strFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasVersion", "comment", "hasDataFileUri", "hasDataFile"));
        }

        // STREAM
        if (mode.equals(FULL) && typeResult.equals(HASCO.STREAM)) {
            filterProvider.addFilter("streamFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("streamFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", 
                            "hasStreamStatus", "topics",                             
                            "hasImageUri", "hasWebDocument", 
                            "deploymentUri", "semanticDataDictionaryUri", "studyUri",  
                            "hascoTypeLabel", "hasVersion",  "comment", "permissionUri", 
                            "designedAt", "startedAt", "endedAt", "method", "messageProtocol", "messageIP", "messagePort"));
        }

        // STREAM_TOPIC
        if (mode.equals(FULL) && typeResult.equals(HASCO.STREAM_TOPIC)) {
            filterProvider.addFilter("streamTopicFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("streamTopicFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri", 
                            "streamUri", "hasTopicStatus", "deploymentUri", "semanticDataDictionaryUri",                             
                            "hasImageUri", "hasWebDocument", "hasTotalReceivedMessages", 
                            "hascoTypeLabel", "hasVersion",  "comment", "hasDeployment", "hasSDD"));
        }

        // STUDY
        if (mode.equals(FULL) && typeResult.equals(HASCO.STUDY)) {
            filterProvider.addFilter("studyFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("studyFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment"));
        }

        // STUDY OBJECT
        if (mode.equals(FULL) && typeResult.equals(HASCO.STUDY_OBJECT)) {
            filterProvider.addFilter("studyObjectFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("studyObjectFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "isMemberOf"));
        }

        // STUDY OBJECT COLLECTION
        if (mode.equals(FULL) && typeResult.equals(HASCO.STUDY_OBJECT_COLLECTION)) {
            filterProvider.addFilter("studyObjectCollectionFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("studyObjectCollectionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "isMemberOfUri", "isMemberOf"));
        }

        // STUDY ROLE
        if (mode.equals(FULL) && typeResult.equals(HASCO.STUDY_ROLE)) {
            filterProvider.addFilter("studyRoleFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("studyRoleFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "isMemberOf"));
        }

        // SUBCONTAINER
        if (mode.equals(FULL) && typeResult.equals(VSTOI.SUBCONTAINER)) {
            filterProvider.addFilter("subcontainerFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("subcontainerFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "superUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "hasInformant", "comment", "belongsTo", "hasFirst", "hasNext", "hasPrevious", 
                            "hasPriority", "hasSerialNumber", "hasLanguage", "hasVersion", "hasSIRManagerEmail"));
        }

        // TASK
        if (mode.equals(FULL) && typeResult.equals(VSTOI.TASK)) {
            filterProvider.addFilter("taskFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("taskFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", "hascoTypeLabel", "comment", "hasLanguage", "hasVersion", 
                            "hasTemporalDependency", "temporalDependencyLabel", 
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasEditorEmail", "hasSupertask", "subtask", 
                            "requiredInstrumentation"));
        }
 
        // VALUE
        if (mode.equals(FULL) && typeResult.equals(HASCO.VALUE)) {
            filterProvider.addFilter("valueFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("valueFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "studyObjectUri", "variable"));
        }

        // VIRTUAL COLUMN
        if (mode.equals(FULL) && typeResult.equals(HASCO.VIRTUAL_COLUMN)) {
            filterProvider.addFilter("virtualColumnFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("virtualColumnFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hasStatus", "hascoTypeUri",
                            "hasImageUri", "hasWebDocument", 
                            "hascoTypeLabel", "comment", "socreference", "groundingLabel", "isMemberOf", "isMemberOfUri"));
        }

        mapper.setFilterProvider(filterProvider);

        return mapper;
    }

    public static ObjectMapper getFilteredByClass(String mode, Class clazz) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();

        if (clazz == Actuator.class) {
            return getFiltered(mode, VSTOI.ACTUATOR);
        } else if (clazz == ActuatorInstance.class) {
            return getFiltered(mode, VSTOI.ACTUATOR_INSTANCE);
        } else if (clazz == ActuatorStem.class) {
            return getFiltered(mode, VSTOI.ACTUATOR_STEM);
        } else if (clazz == Annotation.class) {
            return getFiltered(mode, VSTOI.ANNOTATION);
        } if (clazz == AnnotationStem.class) {
            return getFiltered(mode, VSTOI.ANNOTATION_STEM);
        } if (clazz == Attribute.class) {   // CANNOT FIND IT ABOVE
            return getFiltered(mode, SIO.ATTRIBUTE);
        } else if (clazz == Codebook.class) {
            return getFiltered(mode, VSTOI.CODEBOOK);
        } else if (clazz == CodebookSlot.class) {
            return getFiltered(mode, VSTOI.CODEBOOK_SLOT);
        } else if (clazz == Container.class) {
            return getFiltered(mode, VSTOI.CONTAINER);
        } else if (clazz == ContainerSlot.class) {
            return getFiltered(mode, VSTOI.CONTAINER_SLOT);
        } else if (clazz == DA.class) {
            return getFiltered(mode, HASCO.DATA_ACQUISITION);
        } else if (clazz == DataFile.class) {
            return getFiltered(mode, HASCO.DATAFILE);
        } else if (clazz == DD.class) {
            return getFiltered(mode, HASCO.DD);
        } else if (clazz == Detector.class) {
            return getFiltered(mode, VSTOI.DETECTOR);
        } else if (clazz == DetectorInstance.class) {
            return getFiltered(mode, VSTOI.DETECTOR_INSTANCE);
        } else if (clazz == DetectorStem.class) {
            return getFiltered(mode, VSTOI.DETECTOR_STEM);
        } else if (clazz == Deployment.class) {
            return getFiltered(mode, VSTOI.DEPLOYMENT);
        } else if (clazz == DP2.class) {
            return getFiltered(mode, HASCO.DP2);
        } else if (clazz == Entity.class) {
            return getFiltered(mode, SIO.ENTITY);
        } else if (clazz == FundingScheme.class) {
            return getFiltered(mode, SCHEMA.FUNDING_SCHEME);
        } else if (clazz == INS.class) {
            return getFiltered(mode, HASCO.INS);
        } else if (clazz == Instrument.class) {
            return getFiltered(mode, VSTOI.INSTRUMENT);
        } else if (clazz == KGR.class) {
            return getFiltered(mode, HASCO.KGR);
        } else if (clazz == Organization.class) {
            return getFiltered(mode, SCHEMA.ORGANIZATION);
        } else if (clazz == Person.class) {
            return getFiltered(mode, SCHEMA.PERSON);
        } else if (clazz == Place.class) {
            return getFiltered(mode, SCHEMA.PLACE);
        } else if (clazz == Platform.class) {
            return getFiltered(mode, VSTOI.PLATFORM);
        } else if (clazz == PostalAddress.class) {
            return getFiltered(mode, SCHEMA.POSTAL_ADDRESS);
        } else if (clazz == org.hascoapi.entity.pojo.Process.class) {
            return getFiltered(mode, VSTOI.PROCESS);
        } else if (clazz == ProcessStem.class) {
            return getFiltered(mode, VSTOI.PROCESS_STEM);
        } else if (clazz == Project.class) {
            return getFiltered(mode, SCHEMA.PROJECT);
        } else if (clazz == ResponseOption.class) {
            return getFiltered(mode, VSTOI.RESPONSE_OPTION);
        } else if (clazz == SDD.class) {
            return getFiltered(mode, HASCO.SDD);
        } else if (clazz == SemanticVariable.class) {
            return getFiltered(mode, HASCO.SEMANTIC_VARIABLE);
        } else if (clazz == STR.class) {
            return getFiltered(mode, HASCO.STR);
        } else if (clazz == Study.class) {
            return getFiltered(mode, HASCO.STUDY);
        } else if (clazz == StudyObject.class) {
            return getFiltered(mode, HASCO.STUDY_OBJECT);
        } else if (clazz == StudyObjectCollection.class) {
            return getFiltered(mode, HASCO.STUDY_OBJECT_COLLECTION);
        } else if (clazz == StudyRole.class) {
            return getFiltered(mode, HASCO.STUDY_ROLE);
        } else if (clazz == Subcontainer.class) {
            return getFiltered(mode, VSTOI.SUBCONTAINER);
        //} else if (clazz == Value.class) {
        //    return getFiltered(mode, VSTOI.VALUE);
        } else if (clazz == VirtualColumn.class) {
            return getFiltered(mode, HASCO.VIRTUAL_COLUMN);
        } 
        return getFiltered(mode, "NONE");
    }

}
