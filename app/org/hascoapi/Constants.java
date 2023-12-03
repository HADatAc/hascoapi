package org.hascoapi;

public class Constants {

    public static final String REPOSITORY_VERSION = "0.6";

    public static final String FLASH_MESSAGE_KEY = "message";
    public static final String FLASH_ERROR_KEY = "error";

    public static final String CONTAINER_SLOT_PREFIX = "CTS";
    public static final String CODEBOOK_SLOT_PREFIX = "CBS";
    public static final String TEST_KB = "http://hadatac.org/kb/test/";

    public static final String TEST_INSTRUMENT_URI = TEST_KB + "TestInstrument";
    public static final int TEST_INSTRUMENT_TOT_CONTAINER_SLOTS = 2;
    public static final String TEST_SUBCONTAINER_URI = TEST_KB + "TestSubcontainer";
    public static final int TEST_SUBCONTAINER_TOT_CONTAINER_SLOTS = 2;
    public static final String TEST_CONTAINER_SLOT1_URI = TEST_INSTRUMENT_URI + "/" + CONTAINER_SLOT_PREFIX + "/0001";
    public static final String TEST_CONTAINER_SLOT2_URI = TEST_INSTRUMENT_URI + "/" + CONTAINER_SLOT_PREFIX + "/0002";
    public static final String TEST_CONTAINER_SLOT3_URI = TEST_SUBCONTAINER_URI + "/" + CONTAINER_SLOT_PREFIX + "/0001";
    public static final String TEST_CONTAINER_SLOT4_URI = TEST_SUBCONTAINER_URI + "/" + CONTAINER_SLOT_PREFIX + "/0002";
    public static final String TEST_DETECTOR_STEM1_URI = TEST_KB + "TestDetectorStem1";
    public static final String TEST_DETECTOR_STEM2_URI = TEST_KB + "TestDetectorStem2";
    public static final String TEST_DETECTOR1_URI = TEST_KB + "TestDetector1";  // For Instrument with Stem 1
    public static final String TEST_DETECTOR2_URI = TEST_KB + "TestDetector2";  // For Instrument with Stem 2
    public static final String TEST_DETECTOR3_URI = TEST_KB + "TestDetector3";  // For Subcontainer with Stem 1
    public static final String TEST_DETECTOR4_URI = TEST_KB + "TestDetector4";  // For Subcontainer with Stem 2
    public static final String TEST_CODEBOOK_URI = TEST_KB + "TestCodebook";
    public static final String TEST_CODEBOOK_TOT_CODEBOOK_SLOTS = "2";
    public static final String TEST_CODEBOOK_SLOT1_URI = TEST_CODEBOOK_URI + "/" + CODEBOOK_SLOT_PREFIX + "/0001";
    public static final String TEST_CODEBOOK_SLOT2_URI = TEST_CODEBOOK_URI + "/" + CODEBOOK_SLOT_PREFIX + "/0002";
    public static final String TEST_RESPONSE_OPTION1_URI = TEST_KB + "TestResponseOption1";
    public static final String TEST_RESPONSE_OPTION2_URI = TEST_KB + "TestResponseOption2";
    public static final String TEST_ANNOTATION_STEM1_URI = TEST_KB + "TestAnnotationStem1";
    public static final String TEST_ANNOTATION_STEM2_URI = TEST_KB + "TestAnnotationStem2";
    public static final String TEST_ANNOTATION_STEM_INSTRUCTION_URI = TEST_KB + "TestAnnotationStemInstruction";
    public static final String TEST_ANNOTATION_STEM_PAGE_URI = TEST_KB + "TestAnnotationStemPage";
    public static final String TEST_ANNOTATION_STEM_DATEFIELD_URI = TEST_KB + "TestAnnotationStemDateField";
    public static final String TEST_ANNOTATION_STEM_COPYRIGHT_URI = TEST_KB + "TestAnnotationStemCopyright";
    public static final String TEST_ANNOTATION1_URI = TEST_KB + "TestAnnotation1";
    public static final String TEST_ANNOTATION2_URI = TEST_KB + "TestAnnotation2";
    public static final String TEST_ANNOTATION_INSTRUCTION_URI = TEST_KB + "TestAnnotationInstruction";
    public static final String TEST_ANNOTATION_PAGE_URI = TEST_KB + "TestAnnotationPage";
    public static final String TEST_ANNOTATION_DATEFIELD_URI = TEST_KB + "TestAnnotationDateField";
    public static final String TEST_ANNOTATION_COPYRIGHT_URI = TEST_KB + "TestAnnotationCopyright";
    public static final String TEST_SEMANTIC_VARIABLE1_URI = TEST_KB + "TestSemanticVariable1";
    public static final String TEST_SEMANTIC_VARIABLE2_URI = TEST_KB + "TestSemanticVariable2";
    public static final String TEST_ENTITY_URI = TEST_KB + "TestEntity";
    public static final String TEST_ATTRIBUTE1_URI = TEST_KB + "TestAttribute1";
    public static final String TEST_ATTRIBUTE2_URI = TEST_KB + "TestAttribute2";
    public static final String TEST_UNIT_URI = TEST_KB + "TestUnit";

    public static final String DEFAULT_KB = "http://hadatac.org/kb/default/";
    public static final String DEFAULT_REPOSITORY = DEFAULT_KB + "repository";


}
