package org.sirapi.vocabularies;

import org.sirapi.annotations.PropertyField;

import java.util.HashMap;
import java.util.Map;

public class VSTOI {

    public static final String VSTOI = "http://hadatac.org/ont/vstoi#";

    /*
     * CLASSES
     */

    public static final String DETECTOR = "http://hadatac.org/ont/vstoi#Detector";
    public static final String INSTRUMENT = "http://hadatac.org/ont/vstoi#Instrument";
    public static final String QUESTIONNAIRE = "http://hadatac.org/ont/vstoi#Questionnaire";

    /*
     * PROPERTIES
     */

    public static final String IS_INSTRUMENT_ATTACHMENT = "http://hadatac.org/ont/vstoi#isInstrumentDetectorSlot";
    public static final String HAS_PLATFORM = "http://hadatac.org/ont/vstoi#hasPlatform";
    public static final String HAS_SERIAL_NUMBER = "http://hadatac.org/ont/vstoi#hasSerialNumber";
    public static final String HAS_WEB_DOCUMENTATION = "http://hadatac.org/ont/vstoi#hasWebDocumentation";

    /**************************************************************************************************
     * NEW ADDITIONS TO VSTOI IN SUPPORT OF QUESTIONNAIRES
     **************************************************************************************************/

    /*
     * CLASSES
     */

    public static final String ANNOTATION_STEM = "http://hadatac.org/ont/vstoi#AnnotationStem";
    public static final String ANNOTATION = "http://hadatac.org/ont/vstoi#Annotation";
    public static final String CODEBOOK = "http://hadatac.org/ont/vstoi#Codebook";
    public static final String DETECTOR_STEM = "http://hadatac.org/ont/vstoi#DetectorStem";
    public static final String DETECTOR_SLOT = "http://hadatac.org/ont/vstoi#DetectorSlot";
    public static final String INFORMANT = "http://hadatac.org/ont/vstoi#Informant";
    public static final String ITEM = "http://hadatac.org/ont/vstoi#Item";
    public static final String PSYCHOMETRIC_QUESTIONNAIRE = "http://hadatac.org/ont/vstoi#PsychometricQuestionnaire";
    public static final String RESPONSE_OPTION = "http://hadatac.org/ont/vstoi#ResponseOption";
    public static final String RESPONSE_OPTION_SLOT = "http://hadatac.org/ont/vstoi#ResponseOptionSlot";
    public static final String TABLE = "http://hadatac.org/ont/vstoi#Table";

    /*
     * PROPERTIES
     */

    public static final String BELONGS_TO = "http://hadatac.org/ont/vstoi#belongsTo";
    public static final String HAS_ANNOTATION_STEM = "http://hadatac.org/ont/vstoi#hasAnnotationStem";
    public static final String HAS_CONTENT = "http://hadatac.org/ont/vstoi#hasContent";
    public static final String HAS_COPYRIGHT_NOTICE = "http://hadatac.org/ont/vstoi#hasCopyrightNotice";
    public static final String HAS_DATE_FIELD = "http://hadatac.org/ont/vstoi#hasDateField";
    public static final String HAS_DETECTOR = "http://hadatac.org/ont/vstoi#hasDetector";
    public static final String HAS_DETECTOR_STEM = "http://hadatac.org/ont/vstoi#hasDetectorStem";
    public static final String HAS_CODEBOOK = "http://hadatac.org/ont/vstoi#hasCodebook";
    public static final String HAS_INFORMANT = "http://hadatac.org/ont/vstoi#hasInformant";
    public static final String HAS_INSTRUCTION = "http://hadatac.org/ont/vstoi#hasInstruction";
    public static final String HAS_LANGUAGE = "http://hadatac.org/ont/vstoi#hasLanguage";
    public static final String HAS_PAGE_NUMBER = "http://hadatac.org/ont/vstoi#hasPageNumber";
    public static final String HAS_POSITION = "http://hadatac.org/ont/vstoi#hasPosition";
    public static final String HAS_PRIORITY = "http://hadatac.org/ont/vstoi#hasPriority";
    public static final String HAS_RESPONSE_OPTION = "http://hadatac.org/ont/vstoi#hasResponseOption";
    public static final String HAS_SHORT_NAME = "http://hadatac.org/ont/vstoi#hasShortName";
    public static final String HAS_STATUS = "http://hadatac.org/ont/vstoi#hasStatus";
    public static final String HAS_STYLE = "http://hadatac.org/ont/vstoi#hasStyle";
    public static final String HAS_SIR_MAINTAINER_EMAIL = "http://hadatac.org/ont/vstoi#hasSIRManagerEmail";
    public static final String HAS_SUBJECT_ID_FIELD = "http://hadatac.org/ont/vstoi#hasSubjectIDField";
    public static final String HAS_SUBJECT_RELATIONSHIP_FIELD = "http://hadatac.org/ont/vstoi#hasSubjectRelationshipField";
    public static final String HAS_VERSION = "http://hadatac.org/ont/vstoi#hasVersion";
    public static final String OF_CODEBOOK = "http://hadatac.org/ont/vstoi#ofCodebook";

    /**
     * INSTANCES
     */

    public static final String DEFAULT_LANGUAGE = "en";

    public static final String DEFAULT_WAS_GENERATED_BY = "http://hadatac.org/ont/vstoi#Original";
    public static Map<String, String> wasGeneratedBy;
    static {
        wasGeneratedBy = new HashMap<>();
        wasGeneratedBy.put(DEFAULT_WAS_GENERATED_BY, "Original");
        wasGeneratedBy.put("http://hadatac.org/ont/vstoi#Translation", "Translation");
        wasGeneratedBy.put("http://hadatac.org/ont/vstoi#Generalization", "Generalization");
        wasGeneratedBy.put("http://hadatac.org/ont/vstoi#Specialization", "Specialization");
    }

    public static final String DEFAULT_INFORMANT = "http://hadatac.org/ont/vstoi#Self";
    public static Map<String, String> informant;
    static {
        informant = new HashMap<>();
        informant.put(DEFAULT_INFORMANT, "Self");
        informant.put("http://hadatac.org/ont/vstoi#Youth", "Youth");
        informant.put("http://hadatac.org/ont/vstoi#Caregiver", "Caregiver");
        informant.put("http://hadatac.org/ont/vstoi#HouseholdReferencePerson", "HouseholdReferencePerson");
    }

    public static final String DEFAULT_CONTAINER_POSITION = "http://hadatac.org/ont/vstoi#TopCenter";
    public static Map<String, String> containerPosition;
    static {
        containerPosition = new HashMap<>();
        containerPosition.put(DEFAULT_CONTAINER_POSITION, "TopCenter");
        containerPosition.put("http://hadatac.org/ont/vstoi#TopLeft", "TopLeft");
        containerPosition.put("http://hadatac.org/ont/vstoi#TopRight", "TopRight");
        containerPosition.put("http://hadatac.org/ont/vstoi#BelowTopFullLine", "BelowTopFullLine");
        containerPosition.put("http://hadatac.org/ont/vstoi#BottomCenter", "BottomCenter");
        containerPosition.put("http://hadatac.org/ont/vstoi#BottomLeft", "BottomLeft");
        containerPosition.put("http://hadatac.org/ont/vstoi#BottomRight", "BottomRight");
        containerPosition.put("http://hadatac.org/ont/vstoi#AboveBottomFullLine", "AboveBottomFullLine");
        containerPosition.put("http://hadatac.org/ont/vstoi#NotVisible", "NotVisible");
    }

    public static final String DEFAULT_PAGE_POSITION = "http://hadatac.org/ont/vstoi#PageTopCenter";
    public static Map<String, String> pagePosition;
    static {
        pagePosition = new HashMap<>();
        pagePosition.put(DEFAULT_INFORMANT, "PageTopCenter");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageTopLeft", "PageTopLeft");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageTopRight", "PageTopRight");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageBelowTopFullLine", "PageBelowTopFullLine");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageBottomCenter", "PageBottomCenter");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageBottomLeft", "PageBottomLeft");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageBottomRight", "PageBottomRight");
        pagePosition.put("http://hadatac.org/ont/vstoi#PageAboveBottomFullLine", "PageAboveBottomFullLine");
        pagePosition.put("http://hadatac.org/ont/vstoi#NotVisible", "NotVisible");
    }

}
