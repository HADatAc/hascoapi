package org.sirapi;

public class Constants {

    public static final String REPOSITORY_VERSION = "0.6";

    public static final String FLASH_MESSAGE_KEY = "message";
    public static final String FLASH_ERROR_KEY = "error";

    public static final String DETECTOR_SLOT_PREFIX = "DTS";
    public static final String RESPONSE_OPTION_SLOT_PREFIX = "ROS";
    public static final String SIR_KB = "http://hadatac.org/kb/test/";

    public static final String TEST_INSTRUMENT_URI = SIR_KB + "TestInstrument";
    public static final String TEST_INSTRUMENT_TOT_DETECTOR_SLOTS = "2";
    public static final String TEST_DETECTOR_SLOT1_URI = TEST_INSTRUMENT_URI + "/" + DETECTOR_SLOT_PREFIX + "/1";
    public static final String TEST_DETECTOR_SLOT2_URI = TEST_INSTRUMENT_URI + "/" + DETECTOR_SLOT_PREFIX + "/2";
    public static final String TEST_DETECTOR_STEM1_URI = SIR_KB + "TestDetectorStem1";
    public static final String TEST_DETECTOR_STEM2_URI = SIR_KB + "TestDetectorStem2";
    public static final String TEST_DETECTOR1_URI = SIR_KB + "TestDetector1";
    public static final String TEST_DETECTOR2_URI = SIR_KB + "TestDetector2";
    public static final String TEST_CODEBOOK_URI = SIR_KB + "TestCodebook";
    public static final String TEST_CODEBOOK_TOT_RESPONSE_OPTION_SLOTS = "2";
    public static final String TEST_RESPONSE_OPTION_SLOT1_URI = TEST_CODEBOOK_URI + "/" + RESPONSE_OPTION_SLOT_PREFIX + "/1";
    public static final String TEST_RESPONSE_OPTION_SLOT2_URI = TEST_CODEBOOK_URI + "/" + RESPONSE_OPTION_SLOT_PREFIX + "/2";
    public static final String TEST_RESPONSE_OPTION1_URI = SIR_KB + "TestResponseOption1";
    public static final String TEST_RESPONSE_OPTION2_URI = SIR_KB + "TestResponseOption2";
    public static final String TEST_ANNOTATION_STEM1_URI = SIR_KB + "TestAnnotationStem1";
    public static final String TEST_ANNOTATION_STEM2_URI = SIR_KB + "TestAnnotationStem2";
    public static final String TEST_ANNOTATION1_URI = SIR_KB + "TestAnnotation1";
    public static final String TEST_ANNOTATION2_URI = SIR_KB + "TestAnnotation2";
    public static final String TEST_SEMANTIC_VARIABLE1_URI = SIR_KB + "TestSemanticVariable1";
    public static final String TEST_SEMANTIC_VARIABLE2_URI = SIR_KB + "TestSemanticVariable2";
    public static final String TEST_ENTITY_URI = SIR_KB + "TestEntity";
    public static final String TEST_ATTRIBUTE1_URI = SIR_KB + "TestAttribute1";
    public static final String TEST_ATTRIBUTE2_URI = SIR_KB + "TestAttribute2";
    public static final String TEST_UNIT_URI = SIR_KB + "TestUnit";

    public static final String DEFAULT_KB = "http://hadatac.org/kb/default/";
    public static final String DEFAULT_REPOSITORY = DEFAULT_KB + "repository";


}
