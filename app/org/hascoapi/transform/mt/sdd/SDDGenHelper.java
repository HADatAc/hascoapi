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
     * Get or create a namespace from a prefixed URI
     */
    public void registerPrefixFromUri(String uri) {
        if (uri == null || uri.isEmpty()) {
            return;
        }

        // Extract prefix if URI contains ':'
        if (uri.contains(":") && !uri.startsWith("http")) {
            String prefix = uri.substring(0, uri.indexOf(":"));

            // Try to resolve from in-memory namespaces
            org.hascoapi.utils.NameSpaces nsInstance = org.hascoapi.utils.NameSpaces.getInstance();
            String nsUri = nsInstance.getNameSpaceFromPrefix(prefix);

            if (nsUri != null && !nsUri.isEmpty()) {
                NameSpace ns = new NameSpace();
                ns.setLabel(prefix);
                ns.setUri(nsUri);
                addNamespace(ns);
            }
        }
    }
}

