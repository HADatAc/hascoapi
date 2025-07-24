package DSG;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import base.BaseDelete;

public class DSGDeleteTest extends BaseDelete {

    @Test
    @DisplayName("Delete DSG file by name")
    void shouldDeleteDP2ByName() throws InterruptedException {
        // Uningest the file first if it exists
        deleteFile("dsg", "testeDSG");
        quit();
    }
}
