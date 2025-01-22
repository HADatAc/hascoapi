package org.hascoapi.vocabularies;

import org.hascoapi.annotations.PropertyField;

import java.util.HashMap;
import java.util.Map;

public class VSTOI {

    public static final String VSTOI = "http://hadatac.org/ont/vstoi#";

    /*
     * CLASSES
     */

    public static final String CONTAINER                    = VSTOI + "Container";
    public static final String DEPLOYMENT                   = VSTOI + "Deployment";
    public static final String DETECTOR                     = VSTOI + "Detector";
    public static final String INSTRUMENT                   = VSTOI + "Instrument";
    public static final String PHYSICAL_INSTRUMENT          = VSTOI + "PhysicalInstrument";
    public static final String PLATFORM                     = VSTOI + "Platform";
    public static final String QUESTIONNAIRE                = VSTOI + "Questionnaire";
    public static final String SIMULATION_MODEL             = VSTOI + "SimulationModel";
    public static final String SUBCONTAINER                 = VSTOI + "Subcontainer";

    /*
     * PROPERTIES
     */

    public static final String IS_INSTRUMENT_ATTACHMENT     = VSTOI + "isInstrumentContainerSlot";
    public static final String HAS_PLATFORM_INSTANCE        = VSTOI + "hasPlatformInstance";
    public static final String HAS_SERIAL_NUMBER            = VSTOI + "hasSerialNumber";
    public static final String HAS_WEB_DOCUMENTATION        = VSTOI + "hasWebDocumentation";

    /**************************************************************************************************
     * NEW ADDITIONS TO VSTOI IN SUPPORT OF QUESTIONNAIRES
     **************************************************************************************************/

    /*
     * CLASSES
     */

    public static final String ANNOTATION_STEM              = VSTOI + "AnnotationStem";
    public static final String ANNOTATION                   = VSTOI + "Annotation";
    public static final String CODEBOOK                     = VSTOI + "Codebook";
    public static final String CODEBOOK_SLOT                = VSTOI + "CodebookSlot";
    public static final String CONTAINER_SLOT               = VSTOI + "ContainerSlot";
    public static final String DETECTOR_STEM                = VSTOI + "DetectorStem";
    public static final String DETECTOR_INSTANCE            = VSTOI + "DetectorInstance";
    public static final String FIELD_OF_VIEW                = VSTOI + "FieldOfView";
    public static final String INFORMANT                    = VSTOI + "Informant";
    public static final String ITEM                         = VSTOI + "Item";
    public static final String INSTRUMENT_INSTANCE          = VSTOI + "InstrumentInstance";
    public static final String PLATFORM_INSTANCE            = VSTOI + "PlatformInstance";
    public static final String PROCESS                      = VSTOI + "Process";
    public static final String PROCESS_STEM                 = VSTOI + "ProcessStem";
    public static final String PSYCHOMETRIC_QUESTIONNAIRE   = VSTOI + "PsychometricQuestionnaire";
    public static final String RESPONSE_OPTION              = VSTOI + "ResponseOption";
    public static final String TABLE                        = VSTOI + "Table";
    public static final String VSTOI_INSTANCE               = VSTOI + "VSTOIInstance";

    /*
     * PROPERTIES
     */

    public static final String BELONGS_TO                   = VSTOI + "belongsTo";
    public static final String DESIGNED_AT_TIME             = VSTOI + "designedAtTime";
    public static final String HAS_ACQUISITION_DATE         = VSTOI + "hasAcquisitionDate";    
    public static final String HAS_ANNOTATION_STEM          = VSTOI + "hasAnnotationStem";
    public static final String HAS_CODEBOOK                 = VSTOI + "hasCodebook";
    public static final String HAS_CONTENT                  = VSTOI + "hasContent";
    public static final String HAS_CONTENT_WITH_STYLE       = VSTOI + "hasContentWithStyle";
    public static final String HAS_DAMAGE_DATE              = VSTOI + "hasDamageDate";
    public static final String HAS_DETECTOR                 = VSTOI + "hasDetector";
    public static final String HAS_DETECTOR_INSTANCE        = VSTOI + "hasDetectorInstance";
    public static final String HAS_DETECTOR_STEM            = VSTOI + "hasDetectorStem";
    public static final String HAS_EDITOR_EMAIL             = VSTOI + "hasEditorEmail";
    public static final String HAS_FIRST                    = VSTOI + "hasFirst";
    public static final String HAS_INFORMANT                = VSTOI + "hasInformant";
    public static final String HAS_INSTRUMENT_INSTANCE      = VSTOI + "hasInstrumentInstance";
    public static final String HAS_LANGUAGE                 = VSTOI + "hasLanguage";
    public static final String HAS_NEXT                     = VSTOI + "hasNext";
    public static final String HAS_PREVIOUS                 = VSTOI + "hasPrevious";
    public static final String HAS_POSITION                 = VSTOI + "hasPosition";
    public static final String HAS_PRIORITY                 = VSTOI + "hasPriority";
    public static final String HAS_RESPONSE_OPTION          = VSTOI + "hasResponseOption";
    public static final String HAS_REVIEW_NOTE              = VSTOI + "hasReviewNote";
    public static final String HAS_SHORT_NAME               = VSTOI + "hasShortName";
    public static final String HAS_STATUS                   = VSTOI + "hasStatus";
    public static final String HAS_SIR_MANAGER_EMAIL        = VSTOI + "hasSIRManagerEmail";
    public static final String HAS_SUBCONTAINER             = VSTOI + "hasSubcontainer";
    public static final String HAS_VERSION                  = VSTOI + "hasVersion";
    public static final String HOST_TYPE                    = VSTOI + "hostType";
    public static final String IS_ATTRIBUTE_OF              = VSTOI + "isAttributeOf";
    public static final String IS_DAMAGED                   = VSTOI + "isDamaged";
    public static final String IS_FIELD_OF_VIEW_OF          = VSTOI + "isFieldOfViewOf";
    public static final String OF_CODEBOOK                  = VSTOI + "ofCodebook";

    /*
     * STATUS
     */

     public static final String DRAFT                       = VSTOI + "Draft";
     public static final String UNDER_REVIEW                = VSTOI + "UnderReview";
     public static final String CURRENT                     = VSTOI + "Current";
     public static final String DEPRECATED                  = VSTOI + "Deprecated";

     /*
     * POSITIONS
     */

    public static final String NOT_VISIBLE                  = VSTOI + "NotVisible";
    public static final String TOP_LEFT                     = VSTOI + "TopLeft";
    public static final String TOP_CENTER                   = VSTOI + "TopCenter";
    public static final String TOP_RIGHT                    = VSTOI + "TopRight";
    public static final String LINE_BELOW_TOP               = VSTOI + "LineBelowTop";
    public static final String BOTTOM_LEFT                  = VSTOI + "BotomLeft";
    public static final String BOTTOM_CENTER                = VSTOI + "BottomCenter";
    public static final String BOTTOM_RIGHT                 = VSTOI + "BottomRight";
    public static final String LINE_ABOVE_BOTTOM            = VSTOI + "LineAboveBottom";
    public static final String PAGE_TOP_LEFT                = VSTOI + "PageTopLeft";
    public static final String PAGE_TOP_CENTER              = VSTOI + "PageTopCenter";
    public static final String PAGE_TOP_RIGHT               = VSTOI + "PageTopRight";
    public static final String PAGE_LINE_BELOW_TOP          = VSTOI + "PageLineBelowTop";
    public static final String PAGE_BOTTOM_LEFT             = VSTOI + "PageBottomLeft";
    public static final String PAGE_BOTTOM_CENTER           = VSTOI + "PageBottomCenter";
    public static final String PAGE_BOTTOM_RIGHT            = VSTOI + "PageBottomRight";
    public static final String PAGE_LINE_ABOVE_BOTTOM       = VSTOI + "PageLineAboveBottom";

    /**
     * INSTANCES
     */

    public static final String DEFAULT_LANGUAGE = "en";

    public static final String DEFAULT_WAS_GENERATED_BY = VSTOI + "Original";
    public static Map<String, String> wasGeneratedBy;
    static {
        wasGeneratedBy = new HashMap<>();
        wasGeneratedBy.put(DEFAULT_WAS_GENERATED_BY, "Original");
        wasGeneratedBy.put(VSTOI + "Translation", "Translation");
        wasGeneratedBy.put(VSTOI + "Generalization", "Generalization");
        wasGeneratedBy.put(VSTOI + "Specialization", "Specialization");
    }

    public static final String DEFAULT_INFORMANT = VSTOI + "Self";
    public static Map<String, String> informant;
    static {
        informant = new HashMap<>();
        informant.put(DEFAULT_INFORMANT, "Self");
        informant.put(VSTOI + "Youth", "Youth");
        informant.put(VSTOI + "Caregiver", "Caregiver");
        informant.put(VSTOI + "HouseholdReferencePerson", "HouseholdReferencePerson");
        informant.put(VSTOI + "PhysicalWorld", "PhysicalWorld");
    }

    public static final String DEFAULT_CONTAINER_POSITION = TOP_CENTER;
    public static Map<String, String> containerPosition;
    static {
        containerPosition = new HashMap<>();
        containerPosition.put(TOP_CENTER, "TopCenter");
        containerPosition.put(TOP_LEFT, "TopLeft");
        containerPosition.put(TOP_RIGHT, "TopRight");
        containerPosition.put(LINE_BELOW_TOP, "LineBelowTop");
        containerPosition.put(BOTTOM_CENTER, "BottomCenter");
        containerPosition.put(BOTTOM_LEFT, "BottomLeft");
        containerPosition.put(BOTTOM_RIGHT, "BottomRight");
        containerPosition.put(LINE_ABOVE_BOTTOM, "LineAboveBottom");
        containerPosition.put(NOT_VISIBLE, "NotVisible");
    }

    public static final String DEFAULT_INSTRUMENT_TYPE = INSTRUMENT;
    public static Map<String, String> instrumentType;
    static {
        instrumentType = new HashMap<>();
        instrumentType.put(QUESTIONNAIRE, "Questionnaire");
        instrumentType.put(PHYSICAL_INSTRUMENT, "PhysicalInstrument");
        instrumentType.put(SIMULATION_MODEL, "SimulationModel");
    }

    public static final String DEFAULT_PAGE_POSITION = VSTOI + "PageTopCenter";
    public static Map<String, String> pagePosition;
    static {
        pagePosition = new HashMap<>();
        pagePosition.put(PAGE_TOP_CENTER, "PageTopCenter");
        pagePosition.put(PAGE_TOP_LEFT, "PageTopLeft");
        pagePosition.put(PAGE_TOP_RIGHT, "PageTopRight");
        pagePosition.put(PAGE_LINE_BELOW_TOP, "PageLineBelowTop");
        pagePosition.put(PAGE_BOTTOM_CENTER, "PageBottomCenter");
        pagePosition.put(PAGE_BOTTOM_LEFT, "PageBottomLeft");
        pagePosition.put(PAGE_BOTTOM_RIGHT, "PageBottomRight");
        pagePosition.put(PAGE_LINE_ABOVE_BOTTOM, "PageLineAboveBottom");
        pagePosition.put(NOT_VISIBLE, "NotVisible");
    }

}
