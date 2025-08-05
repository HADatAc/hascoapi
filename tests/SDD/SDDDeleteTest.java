package SDD;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import base.BaseDelete;

public class SDDDeleteTest extends BaseDelete {

    @Test
    @DisplayName("Delete SDD file by name")
    void shouldDeleteSDDByName() throws InterruptedException {
        // Uningest the file first if it exists
        deleteAllFiles("sdd");
        quit();
    }
}
