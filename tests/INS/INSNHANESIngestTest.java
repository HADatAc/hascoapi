package INS;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import base.BaseIngest;

public class INSNHANESIngestTest extends BaseIngest {

    @Test
    @DisplayName("Ingest INS file: testeINS")
    void shouldIngestTesteINS() throws InterruptedException {
        ingestSpecificINS("testeINS");
    }
}
