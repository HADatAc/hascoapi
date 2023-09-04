package test;

import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestClassOrder;
import play.mvc.Result;
import static org.junit.Assert.assertEquals;
import org.sirapi.console.controllers.restapi.CodebookAPI;
import org.sirapi.console.controllers.restapi.DetectorAPI;
import org.sirapi.console.controllers.restapi.DetectorStemAPI;
import org.sirapi.console.controllers.restapi.InstrumentAPI;
import org.sirapi.console.controllers.restapi.ResponseOptionAPI;
import static play.test.Helpers.contentAsString;

import static test.Responses.*;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class IntegrationTest {

    /**
     *   RESET (Level 100)
     */

    @Nested
    @Order(101)
    public class ResetInstrument {
        // Unit test deleteInstrumentForTesting
        @Test
        @DisplayName("Testing 101 reset Instrument")
        public void test101ResetInstrument() {
            final InstrumentAPI controller = new InstrumentAPI();
            Result result;
            result = controller.deleteInstrumentForTesting();
            result = controller.deleteInstrumentForTesting();
            assertEquals(RESPONSE_NOK_RESET_INSTRUMENT,contentAsString(result));
        }
    }    

    @Nested
    @Order(102)
    public class ResetDetectorSlots {
        // Unit test deleteDetectorSlotsForTesting
        @Test
        @DisplayName("Testing 102 reset DetectorSlots")
        public void test102ResetDetectorSlots() {
            final InstrumentAPI controller = new InstrumentAPI();
            Result result;
            result = controller.deleteDetectorSlotsForTesting();
            result = controller.deleteDetectorSlotsForTesting();
            assertEquals(RESPONSE_NOK_RESET_DETECTOR_SLOTS,contentAsString(result));
        }
    }

    @Nested
    @Order(103)
    public class ResetDetectorStems {
        // Unit test deleteDetectorStemsForTesting
        @Test
        @DisplayName("Testing 103 reset DetectorStems")
        public void test103ResetDetestorStems() {
            final DetectorStemAPI controller = new DetectorStemAPI();
            Result result;
            result = controller.deleteDetectorStemsForTesting();
            result = controller.deleteDetectorStemsForTesting();
            assertEquals(RESPONSE_NOK_RESET_DETECTOR_STEMS,contentAsString(result));
        }
    }

    @Nested
    @Order(104)
    public class ResetDetectors {
        // Unit test deleteDetectorsForTesting
        @Test
        @DisplayName("Testing 104 reset Detectors")
        public void test104ResetDetestors() {
            final DetectorAPI controller = new DetectorAPI();
            Result result;
            result = controller.deleteDetectorsForTesting();
            result = controller.deleteDetectorsForTesting();
            assertEquals(RESPONSE_NOK_RESET_DETECTORS,contentAsString(result));
        }
    }

    @Nested
    @Order(105)
    public class ResetCodebook {
        // Unit test deleteCodebookForTesting
        @Test
        @DisplayName("Testing 105 reset Codebook")
        public void test105ResetCodebook() {
            final CodebookAPI controller = new CodebookAPI();
            Result result;
            result = controller.deleteCodebookForTesting();
            result = controller.deleteCodebookForTesting();
            assertEquals(RESPONSE_NOK_RESET_CODEBOOK,contentAsString(result));
        }
    }

    @Nested
    @Order(106)
    public class ResetResponseOptionSlots {
        // Unit test deleteResponseOptionSlotsForTesting
        @Test
        @DisplayName("Testing 106 reset ResponseOptionSlots")
        public void test106ResetResponseOptionSlots() {
            final CodebookAPI controller = new CodebookAPI();
            Result result;
            result = controller.deleteResponseOptionSlotsForTesting();
            result = controller.deleteResponseOptionSlotsForTesting();
            assertEquals(RESPONSE_NOK_RESET_RESPONSE_OPTION_SLOTS,contentAsString(result));
        }
    }

    @Nested
    @Order(107)
    public class ResetResponseOptions {
        // Unit test deleteResponseOptionsForTesting
        @Test
        @DisplayName("Testing 107 reset ResponseOptions")
        public void test107ResetResponseOptions() {
            final ResponseOptionAPI controller = new ResponseOptionAPI();
            Result result;
            result = controller.deleteResponseOptionsForTesting();
            result = controller.deleteResponseOptionsForTesting();
            assertEquals(RESPONSE_NOK_RESET_RESPONSE_OPTIONS,contentAsString(result));
        }
    }

    /**
     *   CREATE (Level 200)
     */

    @Nested
    @Order(201)
    public class CreateInstrument {

        // Unit test createInstrumentForTesting
        @Test
        @DisplayName("Testing 201 create Instrument")
        public void test201CreateInstrument() {
            final InstrumentAPI controller = new InstrumentAPI();
            Result result = controller.createInstrumentForTesting();
            assertEquals(RESPONSE_OK_CREATE_INSTRUMENT,contentAsString(result));
        }

    }

    @Nested
    @Order(202)
    public class CreateDetectorSlots {
        // Unit test createDetectorSlotsForTesting
        @Test
        @DisplayName("Testing 202 create DetectorSlots")
        public void test202CreateDetectorSlots() {
            final InstrumentAPI controller = new InstrumentAPI();
            Result result = controller.createDetectorSlotsForTesting();
            assertEquals(RESPONSE_OK_CREATE_DETECTOr_SLOTS,contentAsString(result));
        }
    }

    @Nested
    @Order(203)
    public class CreateDetectorStems {
        // Unit test createDetectorStemsForTesting
        @Test
        @DisplayName("Testing 203 create DetectorStems")
        public void test203CreateDetestorStems() {
            final DetectorStemAPI controller = new DetectorStemAPI();
            Result result;
            result = controller.createDetectorStemsForTesting();
            assertEquals(RESPONSE_OK_CREATE_DETECTOR_STEMS,contentAsString(result));
        }
    }

    @Nested
    @Order(204)
    public class CreateDetectors {
        // Unit test createDetectorsForTesting
        @Test
        @DisplayName("Testing 204 create Detectors")
        public void test204CreateDetestors() {
            final DetectorAPI controller = new DetectorAPI();
            Result result;
            result = controller.createDetectorsForTesting();
            assertEquals(RESPONSE_OK_CREATE_DETECTORS,contentAsString(result));
        }
    }

    /**
     *   QUERY URI (Level 300)
     */


    /**
     *   QUERY FIND (Level 400)
     */


    /**
     *   QUERY RENDERING (Level 500)
     */


    /**
     *   DELETE (Level 600)
     */

    @Nested
    @Order(604)
    public class DeleteDetectors {
        // Unit test deleteDetectorsForTesting
        @Test
        @DisplayName("Testing 604 delete Detectors")
        public void test204DeleteDetestors() {
            final DetectorAPI controller = new DetectorAPI();
            Result result;
            result = controller.deleteDetectorsForTesting();
            assertEquals(RESPONSE_OK_DELETE_DETECTORS,contentAsString(result));
        }
    }

    @Nested
    @Order(605)
    public class DeleteDetectorStems {
        // Unit test createDetectorStemsForTesting
        @Test
        @DisplayName("Testing 605 delete DetectorStems")
        public void test605CreateDetestorStems() {
            final DetectorStemAPI controller = new DetectorStemAPI();
            Result result;
            result = controller.deleteDetectorStemsForTesting();
            assertEquals(RESPONSE_OK_DELETE_DETECTOR_STEMS,contentAsString(result));
        }
    }

    @Nested
    @Order(606)
    public class DeleteDetectorSlots {
        // Unit test deleteDetectorSlotsForTesting
        @Test
        @DisplayName("Testing 606 delete DetectorSlots")
        public void test606DeleteDetectorSlots() {
            final InstrumentAPI controller = new InstrumentAPI();
            Result result = controller.deleteDetectorSlotsForTesting();
            assertEquals(RESPONSE_OK_DELETE_DETECTOR_SLOTS,contentAsString(result));
        }
    }

    @Nested
    @Order(607)
    public class DeleteInstrument {

        // Unit test createInstrumentForTesting
        @Test
        @DisplayName("Testing 607 delete Instrument")
        public void test607DeleteInstrument() {
            final InstrumentAPI controller = new InstrumentAPI();
            Result result = controller.deleteInstrumentForTesting();
            assertEquals(RESPONSE_OK_DELETE_INSTRUMENT,contentAsString(result));
        }
    }



}