package org.sirapi.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

import org.json.simple.JSONArray;

import com.typesafe.config.ConfigFactory;

public class ConfigProp {

    private static Properties getProperties(String confFileName) {
        Properties prop = new Properties();
        try {
            InputStream is = ConfigProp.class.getClassLoader().getResourceAsStream(confFileName);
            prop.load(is);
            is.close();
        } catch (Exception e) {
            return null;
        }

        return prop;
    }

    public static String getPropertyValue(String confFileName, String field) {
        Properties prop = getProperties(confFileName);
        if (null == prop) {
            return "";
        }
        return prop.getProperty(field);
    }

    public static void setPropertyValue(String confFileName, String field, String value) {
        Properties prop = getProperties(confFileName);
        if (null == prop) {
            return;
        }
        prop.setProperty(field, value);
        URL url = ConfigProp.class.getClassLoader().getResource(confFileName);
        try {
            prop.store(new FileOutputStream(new File(url.toURI())), null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static String getBaseURL() {
        return ConfigFactory.load().getString("sirapi.console.host_deploy");
    }

    public static String getJWTSecret() {
        return ConfigFactory.load().getString("pac4j.jwt.secret");
    }

    /**
    public static String getBasePrefix() {
        return ConfigFactory.load().getString("sirapi.community.ont_prefix");
    }

    public static String getKbPrefix() {
        return ConfigFactory.load().getString("sirapi.community.ont_prefix") + "-kb:";
    }

    public static String getPageTitle() {
        return ConfigFactory.load().getString("sirapi.community.pagetitle");
    }
    public static String getShortName() {
        return ConfigFactory.load().getString("sirapi.community.shortname");
    }
    public static String getFullName() {
        return ConfigFactory.load().getString("sirapi.community.fullname");
    }
    public static String getDescription() {
        return ConfigFactory.load().getString("sirapi.community.description");
    }
    public static String getNSAbbreviation() {
        return ConfigFactory.load().getString("sirapi.namespace.abbreviation");
    }
    public static String getNSValue() {
        return ConfigFactory.load().getString("sirapi.namespace.value");
    }
     */


}
