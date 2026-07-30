package org.hascoapi.utils;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.typesafe.config.ConfigFactory;

public class CollectionUtil {

    // private variables
    private static CollectionUtil single_instance = null;
    private static Map<String, String> configCache = null;

    private CollectionUtil() {
        configCache = new HashMap<String,String>();
        initConfigCache();
    }

    public void initConfigCache() {
        configCache = new HashMap<String, String>();
        String triplestoreUrl = ConfigFactory.load().getString("hascoapi.repository.triplestore");
        System.out.println("[CollectionUtil] Triplestore URL from config: " + triplestoreUrl);
        configCache.put("hascoapi.repository.triplestore", triplestoreUrl);
    }

    // static method to create instance of Singleton class
    public static CollectionUtil getInstance() {
        if (single_instance == null) {
            single_instance = new CollectionUtil();
        }
        return single_instance;
    }

    public Map<String, String> getInstanceConfigCache() {
        return configCache;
    }

    public static Map<String, String> getConfigCache() {
        return CollectionUtil.getInstance().getInstanceConfigCache();
    }

    public enum Collection {

        // triplestore
        SPARQL_QUERY ("/store/query"),
        SPARQL_UPDATE ("/store/update"),
        SPARQL_GRAPH ("/store/data");

        private final String collectionString;

        private Collection(String collectionString) {
            this.collectionString = collectionString;
        }

        public String get() {
            return collectionString;
        }
    }

    public static String getCollectionName(String collection) {
        if (Arrays.asList(
                Collection.SPARQL_QUERY.get(),
                Collection.SPARQL_UPDATE.get(),
                Collection.SPARQL_GRAPH.get()).contains(collection)) {
            return collection;
        }

        return collection;
    }

    public static String getCollectionPath(Collection collection) {
        String triplestoreBaseUrl = resolveTriplestoreBaseUrl();
        String collectionName = null;
        switch (collection) {
            case SPARQL_QUERY:
            case SPARQL_UPDATE:
            case SPARQL_GRAPH :
                collectionName = triplestoreBaseUrl + getCollectionName(collection.get());
                break;
        }

        return collectionName;
    }

    /**
     * Resolve triplestore base URL with runtime fallback.
     *
     * Strategy:
     * 1) Try configured URL first (typically docker network host, e.g., fuseki)
     * 2) If unreachable and host is not localhost, retry with localhost
     */
    private static String resolveTriplestoreBaseUrl() {
        String configuredUrl = getConfigCache().get("hascoapi.repository.triplestore");
        if (configuredUrl == null || configuredUrl.trim().isEmpty()) {
            return configuredUrl;
        }

        configuredUrl = configuredUrl.trim();

        if (isEndpointReachable(configuredUrl)) {
            return configuredUrl;
        }

        String localhostUrl = replaceHostWithLocalhost(configuredUrl);
        if (localhostUrl != null && !localhostUrl.equals(configuredUrl) && isEndpointReachable(localhostUrl)) {
            System.out.println("[CollectionUtil] Triplestore endpoint fallback activated: "
                    + configuredUrl + " -> " + localhostUrl);
            getConfigCache().put("hascoapi.repository.triplestore", localhostUrl);
            return localhostUrl;
        }

        return configuredUrl;
    }

    private static boolean isEndpointReachable(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            int port = uri.getPort();

            if (host == null || port <= 0) {
                return false;
            }

            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress(host, port), 1000);
                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String replaceHostWithLocalhost(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl);
            String host = uri.getHost();
            if (host == null || "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host)) {
                return baseUrl;
            }

            URI updated = new URI(
                    uri.getScheme(),
                    uri.getUserInfo(),
                    "localhost",
                    uri.getPort(),
                    uri.getPath(),
                    uri.getQuery(),
                    uri.getFragment());
            return updated.toString();
        } catch (Exception e) {
            return null;
        }
    }
}

