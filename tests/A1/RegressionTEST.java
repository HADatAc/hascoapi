package A1;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import config.AdminAuto;
import config.AttachPDFINST;
import DA.DADeleteTest;
import DP2.DP2DeleteTest;
import DSG.DSGDeleteTest;
import INS.INSDeleteTest;
import utils.FullIngestTestDRAFT;
import utils.FullUploadTestALL;

public class RegressionTEST {
    private final Launcher launcher = LauncherFactory.create();

    @Test
    void runsetupanddeletetests() throws InterruptedException {

        /*// Setup of rep configuration
        runTestClass(RepositoryFormAutomationTest.class);
        Thread.sleep(5000);

         */

        //Admin Status and Data conf permission
        runTestClass(AdminAuto.class);
        Thread.sleep(5000);

        // All data upload
        runTestClass(FullUploadTestALL.class);
        Thread.sleep(5000);

        // All data ingest
        runTestClass(FullIngestTestDRAFT.class);
        Thread.sleep(5000);

        // All data Regression Test
        runTestClass(FullRegressionTest.class);
        Thread.sleep(5000);

        //AttachPDFINST
        runTestClass(AttachPDFINST.class);
        Thread.sleep(5000);

        // INS
        runTestClass(INSDeleteTest.class);
        Thread.sleep(2000);

        // DP2
        runTestClass(DP2DeleteTest.class);
        Thread.sleep(2000);

        // DA
        runTestClass(DADeleteTest.class);
        Thread.sleep(2000);

        // DSG
        runTestClass(DSGDeleteTest.class);
        Thread.sleep(2000);


        // SDD
        // runTestClass(SDDDeleteTest.class);
        // Thread.sleep(2000);

        // STR
        //runTestClass(STRDeleteTest.class);
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
