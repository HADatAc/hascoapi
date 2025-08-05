package utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import  DA.DAIngestTest;
import DP2.DP2IngestTest;
import DSG.DSGIngestTest;
import INS.INSFullIngest;
import SDD.SDDIngestDPQTest;
import STR.STRIngestTest;
import base.BaseIngest;

public class FullIngestTestCURRENT {

    private final Launcher launcher = LauncherFactory.create();

    @BeforeAll
    static void setMode() {
        BaseIngest.ingestMode = "current";
    }

    @Test
    void runOnlyIngestsForCurrentMode() throws InterruptedException {
        // INS
        runTestClass(INSFullIngest.class);
        Thread.sleep(2000);

        // DSG
        runTestClass(DSGIngestTest.class);
        Thread.sleep(2000);

        // DA
        runTestClass(DAIngestTest.class);
        Thread.sleep(2000);

        // SDD
        runTestClass(SDDIngestDPQTest.class);
        Thread.sleep(2000);

        // DP2
        runTestClass(DP2IngestTest.class);
        Thread.sleep(2000);

        // STR
        runTestClass(STRIngestTest.class);
    }

    private void runTestClass(Class<?> testClass) {
        System.out.println("===> Running: " + testClass.getSimpleName());

        launcher.execute(
                LauncherDiscoveryRequestBuilder.request()
                        .selectors(DiscoverySelectors.selectClass(testClass))
                        .build()
        );
    }
}
