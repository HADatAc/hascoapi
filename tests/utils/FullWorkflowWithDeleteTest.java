package utils;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import DP2.DP2DeleteTest;
import DP2.DP2IngestTest;
import DP2.DP2RegressionTest;
import DP2.DP2UploadTest;
import SDD.SDDDeleteTest;
import DSG.DSGIngestTest;
import DSG.DSGRegressionTest;
import STR.STRUploadTest;
import INS.INSDeleteTest;
import INS.INSNHANESIngestTest;
import INS.INSRegressionTest;
import INS.INSUploadTest;
import SDD.SDDIngestDPQTest;
import SDD.SDDRegressionTest;
import SDD.SDDUploadTest;
import STR.STRDeleteTest;
import STR.STRIngestTest;
import STR.STRRegressionTest;

public class FullWorkflowWithDeleteTest {

    private final Launcher launcher = LauncherFactory.create();
    /*
    1º DSG
    2ª SDD
    3º DP2
    4º STR
        */
    @Test
    void runCompleteWorkflowForAllTypes() throws InterruptedException {
        // INS
        runFullCycle(
                INSUploadTest.class,
                INSNHANESIngestTest.class,
                INSRegressionTest.class,
                INSDeleteTest.class
        );

        // DP2
        runFullCycle(
                DP2UploadTest.class,
                DP2IngestTest.class,
                DP2RegressionTest.class,
                DP2DeleteTest.class
        );

        // DSG
        runFullCycle(
                STRUploadTest.class,
                DSGIngestTest.class,
                DSGRegressionTest.class,
                SDDDeleteTest.class
        );

        // SDD
        runFullCycle(
                SDDUploadTest.class,
                SDDIngestDPQTest.class,
                SDDRegressionTest.class,
                SDDDeleteTest.class
        );

        // STR
        runFullCycle(
                STRUploadTest.class,
                STRIngestTest.class,
                STRRegressionTest.class,
                STRDeleteTest.class
        );
    }
//Uses wildcard imports to avoid cluttering the code with too many import statements.
    private void runFullCycle(Class<?> upload, Class<?> ingest, Class<?> regression, Class<?> delete) throws InterruptedException {
        runTestClass(upload);
        Thread.sleep(2000);

        runTestClass(ingest);
        Thread.sleep(3000);

        runTestClass(regression);
        Thread.sleep(3000);

        runTestClass(delete);
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
