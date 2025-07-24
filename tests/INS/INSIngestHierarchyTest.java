package INS;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import base.BaseIngest;

public class INSIngestHierarchyTest extends BaseIngest {

    @Test
    @DisplayName("Ingest INS file: testeINSHIERARCHY")
    void shouldIngestTesteHierarchy() throws InterruptedException {
        ingestSpecificINS("testeINSHIERARCHY");
    }
}
