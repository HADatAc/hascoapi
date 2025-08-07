package A1;

import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import config.AdminAuto;
import config.AttachPDFINST;
import utils.FullIngestNHANESTestDRAFT;
import utils.FullUploadNHANESTestALL;
import repository.RepositoryFormAutomationTest;

public class FullSetupNHANES {
    private final Launcher launcher = LauncherFactory.create();

    @Test
    void runOnlyIngestsForCurrentMode() throws InterruptedException {
        // Setup of rep configuration
        runTestClass(RepositoryFormAutomationTest.class);
        Thread.sleep(5000);

        //Admin Status and Data conf permission
        runTestClass(AdminAuto.class);
         Thread.sleep(5000);

        // All data upload
        runTestClass(FullUploadNHANESTestALL.class);
        Thread.sleep(5000);

        // All data ingest
        runTestClass(FullIngestNHANESTestDRAFT.class);
        Thread.sleep(5000);

        // All data Regression Test
        runTestClass(FullRegressionTest.class);
        Thread.sleep(5000);

        //AttachPDFINST
        runTestClass(AttachPDFINST.class);
        Thread.sleep(5000);
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
