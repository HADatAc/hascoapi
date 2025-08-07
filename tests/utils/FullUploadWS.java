package utils;

import DSG.DSGUploadTest;
import INS.INSUploadTest;
import SDD.SDDUploadTest;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

public class FullUploadWS {
    private final Launcher launcher = LauncherFactory.create();

    @Test
    public void runFullUpload() throws InterruptedException {
        /*
         System.setProperty("insType", "WS");
         runTestClass(INSUploadTest.class);
         Thread.sleep(2000);
         */
        /*
          Por enquanto só temos os ficheiros dsg e sdd
         */
         System.setProperty("dsgType", "WS");
         runTestClass(DSGUploadTest.class);
         Thread.sleep(2000);
         System.setProperty("sddType", "WS");
         runTestClass(SDDUploadTest.class);
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
