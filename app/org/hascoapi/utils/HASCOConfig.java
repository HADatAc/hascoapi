package org.hascoapi.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class HASCOConfig extends INIConfiguration {

    public HASCOConfig(String filePath) {
        try {
            // First try classpath (works when the file is packaged as a resource in /conf)
            InputStream is = HASCOConfig.class.getClassLoader().getResourceAsStream(filePath);

            // Fallback to filesystem path (works when running from repo root, e.g. "conf/template.generic.conf")
            if (is == null) {
                File f = new File(filePath);
                if (!f.exists()) {
                    // Common case: callers pass "conf/..." but classpath expects just "template..." or vice-versa.
                    // Try the path as-is relative to the working directory; if still missing, fail with a clear message.
                    throw new FileNotFoundException("Could not find config file on classpath or filesystem: " + filePath);
                }
                is = new FileInputStream(f);
            }

            try (InputStreamReader reader = new InputStreamReader(is)) {
                read(reader);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (ConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
