package DP2;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import base.BaseDelete;

public class DP2DeleteTest extends BaseDelete {

    @Test
    @DisplayName("Delete DP2 file by name")
    void shouldDeleteDP2ByName() throws InterruptedException {

        deleteFile("dp2", "testeDP2");
        quit();
    }
}
