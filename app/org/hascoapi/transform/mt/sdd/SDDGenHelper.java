package org.hascoapi.transform.mt.sdd;

import org.apache.poi.ss.usermodel.Workbook;
import org.hascoapi.entity.pojo.NameSpace;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for SDD generation.
 * Manages the workbook and namespace collection during generation.
 */
public class SDDGenHelper {

    /**
     * The workbook being generated
     */
    public Workbook workbook;

    /**
     * Map of namespaces collected during generation
     * Key: namespace URI, Value: NameSpace object
     */
    public Map<String, NameSpace> namespaces;

    /**
     * Constructor
     */
    public SDDGenHelper() {
        this.namespaces = new HashMap<>();
    }

    /**
     * Add a namespace to the collection
     */
    public void addNamespace(NameSpace ns) {
        if (ns != null && ns.getUri() != null) {
            this.namespaces.put(ns.getUri(), ns);
        }
    }

    /**
     * Get or create a namespace from a prefixed URI (e.g., "ncit:C46110")
     */
    public void registerPrefixFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return;
        }

        // Extract prefix if URI contains ':'
        if (uri.contains(":") && !uri.startsWith("http")) {
            String prefix = uri.substring(0, uri.indexOf(":"));

            org.hascoapi.utils.NameSpaces nsInstance = org.hascoapi.utils.NameSpaces.getInstance();
            String nsUri = nsInstance.getNameByAbbreviation(prefix);

            // getNameByAbbreviation returns "owl" when missing; ignore that.
            if (nsUri != null && !nsUri.isEmpty() && !"owl".equals(nsUri)) {
                NameSpace ns = new NameSpace();
                ns.setLabel(prefix);
                ns.setUri(nsUri);
                addNamespace(ns);
            }
        }
    }

    /**
     * Detect and register namespace from a full URI (e.g., "http://purl.obolibrary.org/obo/NCIT_C46110")
     * Automatically creates a prefix and adds to namespaces collection.
     * Returns the prefixed form (e.g., "ncit:C46110")
     */
    public String registerAndConvertFullUri(String fullUri) {
        if (fullUri == null || fullUri.isEmpty()) {
            return fullUri;
        }

        // If it's already prefixed (contains : but doesn't start with http), just register it
        if (fullUri.contains(":") && !fullUri.startsWith("http")) {
            registerPrefixFromUri(fullUri);
            return fullUri;
        }

        // If it doesn't start with http, it's not a full URI
        if (!fullUri.startsWith("http://") && !fullUri.startsWith("https://")) {
            return fullUri;
        }

        // Check if this URI is already registered in the global NameSpaces
        org.hascoapi.utils.NameSpaces nsInstance = org.hascoapi.utils.NameSpaces.getInstance();
        String prefixed = org.hascoapi.utils.URIUtils.replaceNameSpaceEx(fullUri);

        // If replaceNameSpaceEx successfully converted it, we need to add it to our local collection
        if (!prefixed.equals(fullUri) && prefixed.contains(":")) {
            // Extract the prefix and namespace URI from global registry
            String prefix = prefixed.substring(0, prefixed.indexOf(":"));
            String nsUri = nsInstance.getNameByAbbreviation(prefix);

            // Add to local collection if not already there
            if (nsUri != null && !nsUri.isEmpty() && !namespaces.containsKey(nsUri)) {
                NameSpace ns = new NameSpace();
                ns.setLabel(prefix);
                ns.setUri(nsUri);
                addNamespace(ns);
                System.out.println("[SDDGenHelper] Registered namespace from global registry: " + prefix + " → " + nsUri);
            }

            return prefixed;
        }

        // URI not registered - need to auto-detect and create namespace
        // Common patterns:
        // http://purl.obolibrary.org/obo/NCIT_C46110 → ncit:C46110 (base: http://purl.obolibrary.org/obo/NCIT_)
        // http://www.w3.org/2006/time#Interval → time:Interval (base: http://www.w3.org/2006/time#)

        String namespaceUri = "";
        String localName = "";
        String prefix = "";

        // Try # separator first
        if (fullUri.contains("#")) {
            int hashIndex = fullUri.lastIndexOf("#");
            namespaceUri = fullUri.substring(0, hashIndex + 1);
            localName = fullUri.substring(hashIndex + 1);

            // Try to extract meaningful prefix from URI
            // http://www.w3.org/2006/time# → time
            String[] parts = namespaceUri.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i].replace("#", "").toLowerCase();
                if (!part.isEmpty() && part.matches("[a-z][a-z0-9]*")) {
                    prefix = part;
                    break;
                }
            }
        }
        // Try / separator with underscore pattern (e.g., NCIT_C46110)
        else if (fullUri.contains("_")) {
            int underscoreIndex = fullUri.lastIndexOf("_");
            // Look backwards to find the start of the prefix part
            int slashIndex = fullUri.lastIndexOf("/", underscoreIndex);
            if (slashIndex > 0) {
                String prefixPart = fullUri.substring(slashIndex + 1, underscoreIndex);
                namespaceUri = fullUri.substring(0, underscoreIndex + 1);
                localName = fullUri.substring(underscoreIndex + 1);
                prefix = prefixPart.toLowerCase();
            }
        }
        // Try last / separator as fallback
        else if (fullUri.contains("/")) {
            int lastSlash = fullUri.lastIndexOf("/");
            namespaceUri = fullUri.substring(0, lastSlash + 1);
            localName = fullUri.substring(lastSlash + 1);

            // Extract prefix from path
            String[] parts = namespaceUri.split("/");
            for (int i = parts.length - 1; i >= 0; i--) {
                String part = parts[i].toLowerCase();
                if (!part.isEmpty() && !part.equals("obo") && part.matches("[a-z][a-z0-9]*")) {
                    prefix = part;
                    break;
                }
            }
        }

        // If we successfully extracted namespace components, create and register it
        if (!namespaceUri.isEmpty() && !localName.isEmpty() && !prefix.isEmpty()) {
            // Check if this namespace URI is already in our collection
            if (!namespaces.containsKey(namespaceUri)) {
                NameSpace ns = new NameSpace();
                ns.setLabel(prefix);
                ns.setUri(namespaceUri);
                addNamespace(ns);
                System.out.println("[SDDGenHelper] Auto-registered namespace: " + prefix + " → " + namespaceUri);
            }
            return prefix + ":" + localName;
        }

        // Couldn't parse, return as-is
        return fullUri;
    }
}
