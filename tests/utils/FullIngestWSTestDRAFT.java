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

public class FullIngestWSTestDRAFT {

    private final Launcher launcher = LauncherFactory.create();

    @BeforeAll
    static void setMode() {
        BaseIngest.ingestMode = "draft";
    }

    @Test
    void runOnlyIngestsForCurrentMode() throws InterruptedException {


        // DSG
        runTestClass(DSGIngestTest.class);
        Thread.sleep(2000);

        // SDD
        runTestClass(SDDIngestDPQTest.class);
        Thread.sleep(2000);

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
