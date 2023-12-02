package org.sirapi.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.sirapi.entity.pojo.*;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.SIO;
import org.sirapi.vocabularies.VSTOI;

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

        // STUDY
        if (mode.equals(FULL) && typeResult.equals(HASCO.STUDY)) {
            filterProvider.addFilter("studyFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("studyFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // STUDY OBJECT
        if (mode.equals(FULL) && typeResult.equals(HASCO.OBJECT_COLLECTION)) {
            filterProvider.addFilter("studyObjectFilter", SimpleBeanPropertyFilter.serializeAllExcept("measurements"));
        } else if (typeResult.equals(HASCO.STUDY_OBJECT)) {
            filterProvider.addFilter("studyObjectFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("studyObjectFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // VALUE
        if (mode.equals(FULL) && typeResult.equals(HASCO.VALUE)) {
            filterProvider.addFilter("valueFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("valueFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "studyObjectUri", "variable"));
        }

        // DEPLOYMENT
        if (mode.equals(FULL) && typeResult.equals(HASCO.DEPLOYMENT)) {
            filterProvider.addFilter("deploymentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("deploymentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // STR
        if (mode.equals(FULL) && typeResult.equals(HASCO.DATA_ACQUISITION)) {
            filterProvider.addFilter("strFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("strFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // DATA FILE
        if (mode.equals(FULL) && typeResult.equals(HASCO.DATA_FILE)) {
            filterProvider.addFilter("dataFileFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("dataFileFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // DA_SCHEMA
        if (mode.equals(FULL) && typeResult.equals(HASCO.DA_SCHEMA)) {
            filterProvider.addFilter("sddFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("sddFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // DA_SCHEMA_ATTRIBUTE
        if (mode.equals(FULL) && typeResult.equals(HASCO.DA_SCHEMA_ATTRIBUTE)) {
            filterProvider.addFilter("variableFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("variableFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "variableSpec"));
        }

        // SEMANTIC_VARIABLE
        if (mode.equals(FULL) && typeResult.equals(HASCO.SEMANTIC_VARIABLE)) {
            filterProvider.addFilter("semanticVariableFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("semanticVariableFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "entityUri", "attributeUri", "inRelationToUri", "unitUri", "timeUri"));
        }

        // DA_SCHEMA_OBJECT
        if (mode.equals(FULL) && typeResult.equals(HASCO.DA_SCHEMA_OBJECT)) {
            filterProvider.addFilter("sddObjectFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("sddObjectFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // ENTITY
        if (mode.equals(FULL) && typeResult.equals(SIO.ENTITY)) {
            filterProvider.addFilter("entityFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("entityFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment"));
        }

        // INSTRUMENT_TYPE
        filterProvider.addFilter("instrumentTypeFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "className", "superUri", "superLabel", "comment"));

        // INSTRUMENT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.INSTRUMENT)) {
            filterProvider.addFilter("instrumentFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("instrumentFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasInformant", "comment", "hasFirst", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "hasSIRManagerEmail"));
        }

        // SUBCONTAINER
        if (mode.equals(FULL) && typeResult.equals(VSTOI.SUBCONTAINER)) {
            filterProvider.addFilter("subContainerFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("subContainerFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "hasInformant", "comment", "hasFirst", "hasNext", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "hasSIRManagerEmail"));
        }

        // DETECTOR_SLOT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DETECTOR_SLOT)) {
            filterProvider.addFilter("detectorSlotFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("detectorSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasNext", "hasPriority", "hasDetector", "detector", "belongsTo"));
        }

        // DETECTOR_STEM_TYPE
        filterProvider.addFilter("detectorStemTypeFilter", 
            SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "className", "superUri", "superLabel", "comment"));

        // DETECTOR_STEM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DETECTOR_STEM)) {
            filterProvider.addFilter("detectorStemFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("detectorStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "detects", "detectsSemanticVariable"));
        }

        // DETECTOR
        if (mode.equals(FULL) && typeResult.equals(VSTOI.DETECTOR)) {
            filterProvider.addFilter("detectorFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("detectorFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasSerialNumber", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail", "hasDetectorStem", "detectorStem", "hasCodebook", "codebook"));
        }

        // CODEBOOK
        if (mode.equals(FULL) && typeResult.equals(VSTOI.CODEBOOK)) {
            filterProvider.addFilter("codebookFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("codebookFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasSerialNumber", "responseOptions", "hasLanguage",
                            "hasVersion", "hasSIRManagerEmail", "ResponseOptionSlots"));
        }

        // RESPONSEOPTION SLOT
        if (mode.equals(FULL) && typeResult.equals(VSTOI.RESPONSE_OPTION_SLOT)) {
            filterProvider.addFilter("ResponseOptionSlotFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("ResponseOptionSlotFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasPriority", "hasResponseOption", "responseOption"));
        }

        // RESPONSE OPTION
        if (mode.equals(FULL) && typeResult.equals(VSTOI.RESPONSE_OPTION)) {
            filterProvider.addFilter("responseOptionFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("responseOptionFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasSerialNumber", "hasLanguage", "hasVersion",
                            "hasSIRManagerEmail"));
        }

        // ANNOTATION_STEM
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ANNOTATION_STEM)) {
            filterProvider.addFilter("annotationStemFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("annotationStemFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "hasContent", "hasLanguage", "hasVersion",
                            "wasDerivedFrom", "wasGeneratedBy", "hasSIRManagerEmail"));
        }

        // ANNOTATION
        if (mode.equals(FULL) && typeResult.equals(VSTOI.ANNOTATION)) {
            filterProvider.addFilter("annotationFilter", SimpleBeanPropertyFilter.serializeAll());
        } else {
            filterProvider.addFilter("annotationFilter",
                    SimpleBeanPropertyFilter.filterOutAllExcept("uri", "label", "typeUri", "typeLabel", "hascoTypeUri",
                            "hascoTypeLabel", "comment", "belongsTo", "container",
                            "hasAnnotationStem", "annotationStem", "hasPosition", "hasStyle"));
        }

        mapper.setFilterProvider(filterProvider);

        return mapper;
    }

    public static ObjectMapper getFilteredByClass(String mode, Class clazz) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();

        // STUDY
        //if (clazz == Study.class) {
        //    return getFiltered(HASCO.STUDY);
        //} else if (clazz == ObjectCollection.class) {
        //    return getFiltered(HASCO.OBJECT_COLLECTION);
        //} else if (clazz == Value.class) {
        //    return getFiltered(HASCO.VALUE);
        //} else if (clazz == Deployment.class) {
        //    return getFiltered(HASCO.DEPLOYMENT);
        //} else if (clazz == ObjectCollection.class) {
        //    return getFiltered(HASCO.OBJECT_COLLECTION);
        //} else if (clazz == DataAcquisition.class) {
        //    return getFiltered(HASCO.DATA_ACQUISITION);
        //} else if (clazz == Datafile.class) {
        //    return getFiltered(HASCO.DATA_FILE);
        //} else if (clazz == DataAcquisitionSchema.class) {
        //    return getFiltered(HASCO.DA_SCHEMA);
        //} else if (clazz == DataAcquisitionSchemaAttribute.class) {
        //    return getFiltered(HASCO.DA_SCHEMA_ATTRIBUTE);
        //} else if (clazz == DataAcquisitionSchemaObject.class) {
        //    return getFiltered(HASCO.DA_SCHEMA_OBJECT);
        //} else 
        if (clazz == SemanticVariable.class) {
            return getFiltered(mode, HASCO.SEMANTIC_VARIABLE);
        } else if (clazz == Entity.class) {
            return getFiltered(mode, SIO.ENTITY);
        } else if (clazz == Attribute.class) {
            return getFiltered(mode, SIO.ATTRIBUTE);
        } else if (clazz == Instrument.class) {
            return getFiltered(mode, VSTOI.INSTRUMENT);
        } else if (clazz == DetectorSlot.class) {
            return getFiltered(mode, VSTOI.DETECTOR_SLOT);
        } else if (clazz == DetectorStem.class) {
            return getFiltered(mode, VSTOI.DETECTOR_STEM);
        } else if (clazz == Detector.class) {
            return getFiltered(mode, VSTOI.DETECTOR);
        } else if (clazz == Codebook.class) {
            return getFiltered(mode, VSTOI.CODEBOOK);
        } else if (clazz == ResponseOption.class) {
            return getFiltered(mode, VSTOI.RESPONSE_OPTION);
        } else if (clazz == ResponseOptionSlot.class) {
            return getFiltered(mode, VSTOI.RESPONSE_OPTION_SLOT);
        } 
        return getFiltered(mode, "NONE");
    }

}
