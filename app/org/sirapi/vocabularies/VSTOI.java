package org.sirapi.vocabularies;

public class VSTOI {

    public static final String VSTOI                                  = "http://hadatac.org/ont/vstoi#";

    /*
     *    CLASSES
     */

    public static final String DETECTOR                               = "http://hadatac.org/ont/vstoi#Detector";
    public static final String INSTRUMENT                             = "http://hadatac.org/ont/vstoi#Instrument";
    public static final String QUESTIONNAIRE                          = "http://hadatac.org/ont/vstoi#Questionnaire";

    /*
     *    PROPERTIES
     */

    public static final String IS_INSTRUMENT_ATTACHMENT               = "http://hadatac.org/ont/vstoi#isInstrumentAttachment";
    public static final String HAS_PLATFORM                           = "http://hadatac.org/ont/vstoi#hasPlatform";
    public static final String HAS_SERIAL_NUMBER                      = "http://hadatac.org/ont/vstoi#hasSerialNumber";
    public static final String HAS_WEB_DOCUMENTATION                  = "http://hadatac.org/ont/vstoi#hasWebDocumentation";

    /**************************************************************************************************
     *  NEW ADDITIONS TO VSTOI IN SUPPORT OF QUESTIONNAIRES
     **************************************************************************************************/

    /*
     *    CLASSES
     */

    public static final String PSYCHOMETRIC_QUESTIONNAIRE              = "http://hadatac.org/ont/vstoi#PsychometricQuestionnaire";
    public static final String ITEM                                    = "http://hadatac.org/ont/vstoi#Item";

    /*
     *    PROPERTIES
     */

    public static final String SIR_OWNER_EMAIL                          = "http://hadatac.org/ont/vstoi#SIROwnerEmail";
    public static final String HAS_SHORT_NAME                           = "http://hadatac.org/ont/vstoi#hasShortName";
    public static final String HAS_LANGUAGE                             = "http://hadatac.org/ont/vstoi#hasLanguage";
    public static final String HAS_INSTRUCTION                          = "http://hadatac.org/ont/vstoi#hasInstruction";
    public static final String HAS_CONTENT                              = "http://hadatac.org/ont/vstoi#hasContent";
    public static final String HAS_PRIORITY                             = "http://hadatac.org/ont/vstoi#hasPriority";

}
