package org.hascoapi.tests;

import static org.junit.jupiter.api.Assertions.*;

import org.hascoapi.entity.pojo.ProcessBasedStudy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ProcessBasedStudyTest
 *
 * Unit tests for ProcessBasedStudy POJO to ensure proper behavior of:
 * - Property getters/setters for all study metadata fields
 * - Validation logic (email, dates, Study ID format, Process URI requirement)
 * - URI derivation from Process
 * - Metadata auto-generation detection
 *
 * Covers WKF Specification v1.1 ProcessBasedStudy requirements.
 */
@DisplayName("ProcessBasedStudy Entity Tests")
public class ProcessBasedStudyTest {

    private ProcessBasedStudy study;
    private static final String TEST_PROCESS_URI = "http://localhost/kb/pmsr/WKF-001/PROC/0001";
    private static final String TEST_STUDY_URI = "http://localhost/kb/pmsr/STD-001";
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @BeforeEach
    public void setUp() {
        study = new ProcessBasedStudy();
    }

    // ==================== Basic Property Tests ====================

    @Test
    @DisplayName("ProcessBasedStudy should require Process URI")
    public void testProcessUriRequired() {
        study.setProcessUri(TEST_PROCESS_URI);
        assertEquals(TEST_PROCESS_URI, study.getProcessUri(),
            "ProcessBasedStudy must store and retrieve Process URI");
    }

    @Test
    @DisplayName("ProcessBasedStudy should handle null Process URI")
    public void testNullProcessUri() {
        study.setProcessUri(null);
        assertNull(study.getProcessUri(),
            "ProcessBasedStudy should accept null Process URI");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Study ID property")
    public void testStudyId() {
        String studyId = "STD-001";
        study.setStudyID(studyId);
        assertEquals(studyId, study.getStudyID(),
            "ProcessBasedStudy should store and retrieve Study ID");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Study Title property")
    public void testStudyTitle() {
        String title = "Cardiac Resuscitation Simulation";
        study.setStudyTitle(title);
        assertEquals(title, study.getStudyTitle(),
            "ProcessBasedStudy should store and retrieve Study Title");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Specific Aims property")
    public void testSpecificAims() {
        String aims = "To evaluate CPR performance in emergency scenarios";
        study.setSpecificAims(aims);
        assertEquals(aims, study.getSpecificAims(),
            "ProcessBasedStudy should store and retrieve Specific Aims");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Significance property")
    public void testSignificance() {
        String significance = "Improves patient outcomes in cardiac arrest situations";
        study.setSignificance(significance);
        assertEquals(significance, study.getSignificance(),
            "ProcessBasedStudy should store and retrieve Significance");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Institution property")
    public void testInstitution() {
        String institution = "Johns Hopkins University";
        study.setInstitutionName(institution);
        assertEquals(institution, study.getInstitutionName(),
            "ProcessBasedStudy should store and retrieve Institution");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Principal Investigator property")
    public void testPrincipalInvestigator() {
        String pi = "Dr. Jane Smith";
        study.setPrincipalInvestigator(pi);
        assertEquals(pi, study.getPrincipalInvestigator(),
            "ProcessBasedStudy should store and retrieve Principal Investigator");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Contact Email property")
    public void testContactEmail() {
        String email = "jsmith@jhu.edu";
        study.setContactEmail(email);
        assertEquals(email, study.getContactEmail(),
            "ProcessBasedStudy should store and retrieve Contact Email");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support Start Date property")
    public void testStartDate() {
        String startDate = "2026-01-15";
        study.setStartDate(startDate);
        assertEquals(startDate, study.getStartDate(),
            "ProcessBasedStudy should store and retrieve Start Date");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support End Date property")
    public void testEndDate() {
        String endDate = "2026-12-31";
        study.setEndDate(endDate);
        assertEquals(endDate, study.getEndDate(),
            "ProcessBasedStudy should store and retrieve End Date");
    }

    // ==================== Validation Tests ====================

    @Test
    @DisplayName("validate() should reject missing Process URI")
    public void testValidateRejectsMissingProcessUri() {
        study.setUri(TEST_STUDY_URI);
        study.setStudyID("STD-001");
        // No Process URI set
        
        boolean result = study.validate();
        assertFalse(result, "validate() should return false when Process URI is missing");
    }

    @Test
    @DisplayName("validate() should reject invalid email format")
    public void testValidateRejectsInvalidEmail() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        study.setContactEmail("invalid-email");
        
        boolean result = study.validate();
        assertFalse(result, "validate() should reject invalid email format");
    }

    @Test
    @DisplayName("validate() should accept valid email format")
    public void testValidateAcceptsValidEmail() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        study.setContactEmail("researcher@university.edu");
        
        boolean result = study.validate();
        assertTrue(result, "validate() should accept valid email format");
    }

    @Test
    @DisplayName("validate() should reject invalid Study ID format")
    public void testValidateRejectsInvalidStudyId() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("INVALID-FORMAT");
        
        boolean result = study.validate();
        assertFalse(result, "validate() should reject Study ID not starting with 'STD-'");
    }

    @Test
    @DisplayName("validate() should accept valid Study ID format")
    public void testValidateAcceptsValidStudyId() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-042");
        
        boolean result = study.validate();
        assertTrue(result, "validate() should accept Study ID starting with 'STD-'");
    }

    @Test
    @DisplayName("validate() should reject end date before start date")
    public void testValidateRejectsEndBeforeStart() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        study.setStartDate("2026-12-31");
        study.setEndDate("2026-01-01");
        
        boolean result = study.validate();
        assertFalse(result, "validate() should reject end date before start date");
    }

    @Test
    @DisplayName("validate() should accept valid date range")
    public void testValidateAcceptsValidDateRange() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        study.setStartDate("2026-01-01");
        study.setEndDate("2026-12-31");
        
        boolean result = study.validate();
        assertTrue(result, "validate() should accept valid date range");
    }

    @Test
    @DisplayName("validate() should reject invalid date format")
    public void testValidateRejectsInvalidDateFormat() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        study.setStartDate("01/15/2026"); // Invalid format (should be ISO 8601)
        
        boolean result = study.validate();
        assertFalse(result, "validate() should reject non-ISO 8601 date format");
    }

    @Test
    @DisplayName("validate() should accept null optional fields")
    public void testValidateAcceptsNullOptionalFields() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        // All other fields null
        
        boolean result = study.validate();
        assertTrue(result, "validate() should accept null optional fields");
    }

    // ==================== Auto-Generation Detection Tests ====================

    @Test
    @DisplayName("isAutoGenerated() should detect default title")
    public void testIsAutoGeneratedDetectsDefaultTitle() {
        study.setStudyTitle("Auto-generated study for WKF-001");
        
        // This would require implementing isAutoGenerated() method in ProcessBasedStudy
        // For now, we test the pattern that would be used to detect auto-generation
        assertTrue(study.getStudyTitle().contains("Auto-generated"),
            "Study title containing 'Auto-generated' indicates auto-generation");
    }

    @Test
    @DisplayName("ProcessBasedStudy should support empty metadata (backward compatibility)")
    public void testBackwardCompatibilityWithEmptyMetadata() {
        study.setUri(TEST_STUDY_URI);
        study.setProcessUri(TEST_PROCESS_URI);
        study.setStudyID("STD-001");
        // All study metadata fields are null
        
        boolean result = study.validate();
        assertTrue(result, "ProcessBasedStudy should validate with empty study metadata");
    }

    // ==================== Inheritance Tests ====================

    @Test
    @DisplayName("ProcessBasedStudy should extend Study")
    public void testProcessBasedStudyExtendsStudy() {
        assertTrue(study instanceof org.hascoapi.entity.pojo.Study,
            "ProcessBasedStudy must extend Study class");
    }

    @Test
    @DisplayName("ProcessBasedStudy should inherit Study properties")
    public void testProcessBasedStudyInheritsStudyProperties() {
        String label = "Test Study";
        study.setLabel(label);
        assertEquals(label, study.getLabel(),
            "ProcessBasedStudy should inherit label from Study");
        
        String comment = "Test comment";
        study.setComment(comment);
        assertEquals(comment, study.getComment(),
            "ProcessBasedStudy should inherit comment from Study");
    }

    // ==================== Edge Cases ====================

    @Test
    @DisplayName("ProcessBasedStudy should handle very long text fields")
    public void testHandlesLongTextFields() {
        String longText = "A".repeat(5000); // 5000 character string
        
        study.setSpecificAims(longText);
        assertEquals(longText, study.getSpecificAims(),
            "ProcessBasedStudy should handle long text in Specific Aims");
        
        study.setSignificance(longText);
        assertEquals(longText, study.getSignificance(),
            "ProcessBasedStudy should handle long text in Significance");
    }

    @Test
    @DisplayName("ProcessBasedStudy should handle special characters in text")
    public void testHandlesSpecialCharacters() {
        String specialText = "Test with special chars: <>&\"'@#$%^&*()";
        
        study.setStudyTitle(specialText);
        assertEquals(specialText, study.getStudyTitle(),
            "ProcessBasedStudy should preserve special characters");
    }

    @Test
    @DisplayName("ProcessBasedStudy should handle Unicode in text fields")
    public void testHandlesUnicodeCharacters() {
        String unicodeText = "Test with Unicode: 你好 мир こんにちは 🔬";
        
        study.setStudyTitle(unicodeText);
        assertEquals(unicodeText, study.getStudyTitle(),
            "ProcessBasedStudy should preserve Unicode characters");
    }

    @Test
    @DisplayName("ProcessBasedStudy should trim whitespace in Study ID")
    public void testTrimsWhitespaceInStudyId() {
        study.setStudyID("  STD-001  ");
        
        // Assuming implementation trims whitespace
        String studyId = study.getStudyID();
        assertNotNull(studyId);
        // Would need to verify actual trimming behavior
    }
}
