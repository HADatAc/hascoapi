package org.hascoapi.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.Collections;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import org.hascoapi.RepositoryInstance;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.vocabularies.HASCO;

public class NameSpaces {

    private ConcurrentHashMap<String, NameSpace> table = new ConcurrentHashMap<String, NameSpace>();
    private NameSpace localNamespace = null;
    private String turtleNameSpaceList = "";
    private String sparqlNameSpaceList = "";

    private static NameSpaces instance = null;

    public static NameSpaces getInstance() {
        if (instance == null) {
            instance = new NameSpaces();
        }
        return instance;
    }

    private List<NameSpace> InitiateNameSpaces() {

        List<NameSpace> namespaces = new ArrayList<NameSpace>();

        // RDF
        NameSpace RDF_NAMESPACE = new NameSpace();
        RDF_NAMESPACE.setAbbreviation("rdf");
        RDF_NAMESPACE.setName("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
        RDF_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        RDF_NAMESPACE.setMimeType("text/turtle");
        RDF_NAMESPACE.setURL("https://www.w3.org/1999/02/22-rdf-syntax-ns#");
        RDF_NAMESPACE.setComment("W3C's Resource Description Framework");
        RDF_NAMESPACE.setVersion("1.0");
        RDF_NAMESPACE.updateNumberOfLoadedTriples();
        RDF_NAMESPACE.setPriority(1);
        namespaces.add(RDF_NAMESPACE);

        // RDFS
        NameSpace RDFS_NAMESPACE = new NameSpace();
        RDFS_NAMESPACE.setAbbreviation("rdfs");
        RDFS_NAMESPACE.setName("http://www.w3.org/2000/01/rdf-schema#");
        RDFS_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        RDFS_NAMESPACE.setMimeType("text/turtle");
        RDFS_NAMESPACE.setURL("https://www.w3.org/2000/01/rdf-schema#");
        RDFS_NAMESPACE.setComment("W3C's RDF Schema");
        RDFS_NAMESPACE.setVersion("1.0");
        RDFS_NAMESPACE.updateNumberOfLoadedTriples();https://hadatac.org/ont/hasco/
        RDFS_NAMESPACE.setPriority(2);
        namespaces.add(RDFS_NAMESPACE);

        // XMLS
        NameSpace XSD_NAMESPACE = new NameSpace();
        XSD_NAMESPACE.setAbbreviation("xsd");
        XSD_NAMESPACE.setName("http://www.w3.org/2001/XMLSchema#");
        XSD_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        XSD_NAMESPACE.setMimeType("application/rdf+xml");
        XSD_NAMESPACE.setURL("http://www.w3.org/2001/XMLSchema#");
        XSD_NAMESPACE.setComment("W3C's XML Schema");
        XSD_NAMESPACE.setVersion("1.0");
        XSD_NAMESPACE.updateNumberOfLoadedTriples();
        XSD_NAMESPACE.setPriority(3);
        namespaces.add(XSD_NAMESPACE);

        // OWL
        NameSpace OWL_NAMESPACE = new NameSpace();
        OWL_NAMESPACE.setAbbreviation("owl");
        OWL_NAMESPACE.setName("http://www.w3.org/2002/07/owl#");
        OWL_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        OWL_NAMESPACE.setMimeType("text/turtle");
        OWL_NAMESPACE.setURL("https://www.w3.org/2002/07/owl#");
        OWL_NAMESPACE.setComment("W3C's Ontology Web Language");
        OWL_NAMESPACE.setVersion("1.0");
        OWL_NAMESPACE.updateNumberOfLoadedTriples();
        OWL_NAMESPACE.setPriority(4);
        namespaces.add(OWL_NAMESPACE);


        // SKOS
        NameSpace SKOS_NAMESPACE = new NameSpace();
        SKOS_NAMESPACE.setAbbreviation("skos");
        SKOS_NAMESPACE.setName("http://www.w3.org/2004/02/skos/core#");
        SKOS_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        SKOS_NAMESPACE.setComment("Simple Knowledge Organization System Namespace Document");
        SKOS_NAMESPACE.setVersion("18-August-2009");
        SKOS_NAMESPACE.updateNumberOfLoadedTriples();
        SKOS_NAMESPACE.setPriority(5);
        namespaces.add(SKOS_NAMESPACE);

        // DCTERMS
        // dcterms=http://purl.org/dc/terms/,,

        // PROV
        NameSpace PROV_NAMESPACE = new NameSpace();
        PROV_NAMESPACE.setAbbreviation("prov");
        PROV_NAMESPACE.setName("http://www.w3.org/ns/prov#");
        PROV_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        PROV_NAMESPACE.setMimeType("text/turtle");
        PROV_NAMESPACE.setURL("https://hadatac.org/ont/prov/");
        PROV_NAMESPACE.setComment("The W3C Provenance Ontology");
        PROV_NAMESPACE.setVersion("30-April-2013");
        PROV_NAMESPACE.updateNumberOfLoadedTriples();
        PROV_NAMESPACE.setPriority(6);
        namespaces.add(PROV_NAMESPACE);


        // SIO
        NameSpace SIO_NAMESPACE = new NameSpace();
        SIO_NAMESPACE.setAbbreviation("sio");
        SIO_NAMESPACE.setName("http://semanticscience.org/resource/");
        SIO_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        SIO_NAMESPACE.setMimeType("application/rdf+xml");
        SIO_NAMESPACE.setURL("https://raw.githubusercontent.com/MaastrichtU-IDS/semanticscience/master/ontology/sio.owl");
        SIO_NAMESPACE.setComment("Semanticscience Integrated Ontology");
        SIO_NAMESPACE.setVersion("1.59");
        SIO_NAMESPACE.updateNumberOfLoadedTriples();
        SIO_NAMESPACE.setPriority(7);
        namespaces.add(SIO_NAMESPACE);


        // UO
        // uo=http://purl.obolibrary.org/obo/UO_,application/rdf+xml,https://raw.githubusercontent.com/bio-ontology-research-group/unit-ontology/master/uo.owl

        // HAScO
        NameSpace HASCO_NAMESPACE = new NameSpace();
        HASCO_NAMESPACE.setAbbreviation("hasco");
        HASCO_NAMESPACE.setName("http://hadatac.org/ont/hasco/");
        HASCO_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        HASCO_NAMESPACE.setMimeType("text/turtle");
        HASCO_NAMESPACE.setURL("https://hadatac.org/ont/hasco/");
        HASCO_NAMESPACE.setComment("Human-Aware Science Ontology");
        HASCO_NAMESPACE.setVersion("1.0");
        HASCO_NAMESPACE.updateNumberOfLoadedTriples();
        HASCO_NAMESPACE.setPriority(8);
        namespaces.add(HASCO_NAMESPACE);

        // VSTOI
        NameSpace VSTOI_NAMESPACE = new NameSpace();
        VSTOI_NAMESPACE.setAbbreviation("vstoi");
        VSTOI_NAMESPACE.setName("http://hadatac.org/ont/vstoi#");
        VSTOI_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        VSTOI_NAMESPACE.setMimeType("text/turtle");
        VSTOI_NAMESPACE.setURL("https://hadatac.org/ont/vstoi#");
        VSTOI_NAMESPACE.setComment("Virtual Terrestrial Solar Observatory - Instruments");
        VSTOI_NAMESPACE.setVersion("1.0");
        VSTOI_NAMESPACE.updateNumberOfLoadedTriples();
        VSTOI_NAMESPACE.setPriority(9);
        namespaces.add(VSTOI_NAMESPACE);

        // Languages
        NameSpace lcc_639_1_NAMESPACE = new NameSpace();
        lcc_639_1_NAMESPACE.setAbbreviation("lcc-639-1");
        lcc_639_1_NAMESPACE.setName("https://www.omg.org/spec/LCC/Languages/ISO639-1-LanguageCodes/");
        lcc_639_1_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        lcc_639_1_NAMESPACE.setMimeType("text/turtle");
        lcc_639_1_NAMESPACE.setURL("https://www.omg.org/spec/LCC/20211101/Languages/ISO639-1-LanguageCodes.ttl");
        lcc_639_1_NAMESPACE.setComment("Language codes from ISO 639-1, as expressed in https://www.w3schools.com/tags/ref_language_codes.asp");
        lcc_639_1_NAMESPACE.setVersion("1.0");
        lcc_639_1_NAMESPACE.updateNumberOfLoadedTriples();
        lcc_639_1_NAMESPACE.setPriority(10);
        namespaces.add(lcc_639_1_NAMESPACE);

        // FHIR
        NameSpace FHIR_NAMESPACE = new NameSpace();
        FHIR_NAMESPACE.setAbbreviation("fhir");
        FHIR_NAMESPACE.setName("http://hl7.org/fhir/");
        FHIR_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        FHIR_NAMESPACE.setComment("FHIR is a standard for health care data exchange, published by HL7.");
        FHIR_NAMESPACE.setVersion("R5");
        FHIR_NAMESPACE.updateNumberOfLoadedTriples();
        FHIR_NAMESPACE.setPriority(11);
        namespaces.add(FHIR_NAMESPACE);

        // FOAF
        NameSpace FOAF_NAMESPACE = new NameSpace();
        FOAF_NAMESPACE.setAbbreviation("foaf");
        FOAF_NAMESPACE.setName("http://xmlns.com/foaf/0.1/");
        FOAF_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        FOAF_NAMESPACE.setMimeType("application/rdf+xml");
        FOAF_NAMESPACE.setURL("http://xmlns.com/foaf/spec/index.rdf");
        FOAF_NAMESPACE.setComment("Friend of a Friend (FOAF) vocabulary");
        FOAF_NAMESPACE.setVersion("0.1");
        FOAF_NAMESPACE.updateNumberOfLoadedTriples();
        FOAF_NAMESPACE.setPriority(12);
        namespaces.add(FOAF_NAMESPACE);

        // DEFAULT
        NameSpace DEFAULT_NAMESPACE = new NameSpace();
        DEFAULT_NAMESPACE.setAbbreviation("default");
        DEFAULT_NAMESPACE.setName("http://hadatac.org/kb/default/");
        DEFAULT_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        DEFAULT_NAMESPACE.setComment("Default Namespace");
        DEFAULT_NAMESPACE.setVersion("0.1");
        DEFAULT_NAMESPACE.updateNumberOfLoadedTriples();
        DEFAULT_NAMESPACE.setPriority(13);
        namespaces.add(DEFAULT_NAMESPACE);

        // TEST
        NameSpace TEST_NAMESPACE = new NameSpace();
        TEST_NAMESPACE.setAbbreviation("test");
        TEST_NAMESPACE.setName(Constants.TEST_KB);
        TEST_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
        TEST_NAMESPACE.setComment("Test Namespace");
        TEST_NAMESPACE.setVersion("1.0");
        TEST_NAMESPACE.updateNumberOfLoadedTriples();
        TEST_NAMESPACE.setPriority(20);
        namespaces.add(TEST_NAMESPACE);

        return namespaces;

    }

    public void deleteLocalNamespace() {
        if (localNamespace == null) {
            return;
        }
        table.remove(localNamespace.getAbbreviation());
        localNamespace = null;
    }

    public void updateLocalNamespace() {
        // Local namespace
        String abbrev = RepositoryInstance.getInstance().getHasDefaultNamespaceAbbreviation();
        String url = RepositoryInstance.getInstance().getHasDefaultNamespaceURL();
        if (RepositoryInstance.getInstance() != null &&
            abbrev != null && !abbrev.equals("") &&
            url != null && !url.equals("")) {
            NameSpace LOCAL_NAMESPACE = new NameSpace();
            LOCAL_NAMESPACE.setAbbreviation(abbrev);
            LOCAL_NAMESPACE.setName(url);
            LOCAL_NAMESPACE.setTypeUri(HASCO.ONTOLOGY);
            LOCAL_NAMESPACE.updateNumberOfLoadedTriples();
            LOCAL_NAMESPACE.setPriority(30);
            localNamespace = LOCAL_NAMESPACE;

            table.put(abbrev, localNamespace);
        }
    }

    private NameSpaces() {

        System.out.println("Instantiating NameSpaces");

        List<NameSpace> namespaces = InitiateNameSpaces();

        // ADD NAMESPACES INTO CACHE
        for (NameSpace ns : namespaces) {
            table.put(ns.getAbbreviation(), ns);
        }

        System.out.println("  - Generating ordered list of ontologies");
        // CREATE AN ORDERED LIST OF NAMESPACES (ONTOLOGIES)
        //ontologyList = getOrderedNamespacesAsList();

    }

    public String getNameByAbbreviation(String abbr) {
        NameSpace ns = table.get(abbr);
        if (ns == null) {
            return "owl";
        } else {
            return ns.getName();
        }
    }

    public Map<String, Integer> getLoadedOntologies() {
        Map<String, Integer> loadedOntologies = new HashMap<String, Integer>();
        List<NameSpace> list = new ArrayList<NameSpace>(table.values());
        for (NameSpace ns : list) {
            if (ns.getNumberOfLoadedTriples() > 0) {
                loadedOntologies.put(ns.getAbbreviation(), ns.getNumberOfLoadedTriples());
            }
        }
        return loadedOntologies;
    }

    /*
    public List<NameSpace> getOntologyList() {
        return ontologyList;
    }
     */

    /*
    public void reload() {
        table.clear();
        List<NameSpace> namespaces = NameSpace.findAll();
        for (NameSpace ns : namespaces) {
            ns.updateNumberOfLoadedTriples();
            table.put(ns.getAbbreviation(), ns);
        }

        //ontologyList = getOrderedNamespacesAsList();
        sparqlNameSpaceList = getSparqlNameSpaceList();
        turtleNameSpaceList = getTurtleNameSpaceList();
    }
     */

    public static List<NameSpace> loadFromFile(InputStream inputStream) {
        List<NameSpace> namespaces = new ArrayList<NameSpace>();

        try {
            Properties prop = new Properties();
            prop.load(inputStream);
            for (Map.Entry<Object, Object> nsEntry : prop.entrySet()) {
                String nsAbbrev = ((String) nsEntry.getKey());

                //System.out.println("NameSpaces.loadFromFile() nsAbbrev = " + nsAbbrev);

                if (nsAbbrev != null) {
                    String[] tmpList = prop.getProperty(nsAbbrev).split(",");
                    NameSpace tmpNS = null;
                    if (tmpList.length >= 1 && tmpList[0] != null && !tmpList[0].equals("")) {
                        tmpNS = new NameSpace();
                        tmpNS.setAbbreviation(nsAbbrev);
                        tmpNS.setName(tmpList[0]);
                        if (tmpList.length >= 2 && tmpList[1] != null && !tmpList[1].equals("")) {
                            tmpNS.setMimeType(tmpList[1]);
                        }
                        if (tmpList.length >= 3 && tmpList[2] != null && !tmpList[2].equals("")) {
                            tmpNS.setURL(tmpList[2]);
                        }
                        if (tmpList.length >= 4 && tmpList[3] != null && !tmpList[3].equals("")) {
                            try {
                                int priority = Integer.parseInt(tmpList[3]);
                                tmpNS.setPriority(priority);
                            } catch (NumberFormatException e) {
                                System.err.println("Bad priority value for " + nsAbbrev + ". Expected an integer and got " + tmpList[3]);
                            }
                        }
                    }
                    if (tmpNS != null) {
                        namespaces.add(tmpNS);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("[NameSpaces.java ERROR]: could not read file namespaces.properties");
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return namespaces;
    }

    public ConcurrentHashMap<String, NameSpace> getNamespaces() {
        return table;
    }

    public List<NameSpace> getOrderedNamespacesAsList() {
        List<NameSpace> orderedList = new ArrayList<NameSpace>(table.values());
        orderedList.sort(new Comparator<NameSpace>() {
            @Override
            public int compare(NameSpace o1, NameSpace o2) {
                return o1.getAbbreviation().toLowerCase().compareTo(
                        o2.getAbbreviation().toLowerCase());
            }
        });
        return orderedList;
    }

    private String getTurtleNameSpaceList() {
        String namespaces = "";
        for (Map.Entry<String, NameSpace> entry : table.entrySet()) {
            String abbrev = entry.getKey().toString();
            ;
            NameSpace ns = entry.getValue();
            namespaces = namespaces + "@prefix " + abbrev + ": <" + ns.getName() + "> . \n";
        }

        return namespaces;
    }

    private String getSparqlNameSpaceList() {
        String namespaces = "";
        for (Map.Entry<String, NameSpace> entry : table.entrySet()) {
            String abbrev = entry.getKey().toString();
            ;
            NameSpace ns = entry.getValue();
            namespaces = namespaces + "PREFIX " + abbrev + ": <" + ns.getName() + "> \n";
        }

        return namespaces;
    }

    public String printTurtleNameSpaceList() {
        if (!"".equals(turtleNameSpaceList)) {
            return turtleNameSpaceList;
        }

        return getTurtleNameSpaceList();
    }

    public String printSparqlNameSpaceList() {
        if (!"".equals(sparqlNameSpaceList)) {
            return sparqlNameSpaceList;
        }

        return getSparqlNameSpaceList();
    }

    public int getNumOfNameSpaces() {
        if (table == null) {
            return 0;
        }

        return table.size();
    }

    public String jsonLoadedOntologies() {
        String json = "";
        boolean first = true;
        List<Map.Entry<String, Integer>> entries =
                new ArrayList<Map.Entry<String, Integer>>(getLoadedOntologies().entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return Integer.compare(b.getValue(), a.getValue());
            }
        });
        for (Map.Entry<String, Integer> entry : entries) {
            if (first) {
                first = false;
            } else {
                json = json + ",";
            }
            String abbrev = entry.getKey().toString();
            ;
            int triples = entry.getValue();
            json = json + " [\"" + abbrev + "\"," + triples + "]";
        }

        return json;
    }

    public List<String> listLoadedOntologies() {
        List<String> loadedList = new ArrayList<String>();
        for (Map.Entry<String, NameSpace> entry : table.entrySet()) {
            if (entry.getValue().getNumberOfLoadedTriples() >= 1)
                loadedList.add(entry.getKey());
        }
        return loadedList;
    }

    public List<String> getOrderedPriorityLoadedOntologyKeyList() {
        // generate a list of all loaded ontolgies
        List<NameSpace> namespaceList = new ArrayList<NameSpace>();
        for (Map.Entry<String, NameSpace> entry : table.entrySet()) {
            if (entry.getValue().getNumberOfLoadedTriples() >= 1)
                namespaceList.add(entry.getValue());
        }

        // sort by priority
        namespaceList.sort(new Comparator<NameSpace>() {
            @Override
            public int compare(NameSpace o1, NameSpace o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });

        // Get URIs
        List<String> loadedList = new ArrayList<String>();

        for (NameSpace n : namespaceList) {
            loadedList.add(n.getAbbreviation().toString());


        }
        return loadedList;
    }

    public List<String> getOrderedPriorityLoadedOntologyList() {
        // generate a list of all loaded ontolgies
        List<NameSpace> namespaceList = new ArrayList<NameSpace>();
        for (Map.Entry<String, NameSpace> entry : table.entrySet()) {
            if (entry.getValue().getNumberOfLoadedTriples() >= 1)
                namespaceList.add(entry.getValue());
        }

        // sort by priority
        namespaceList.sort(new Comparator<NameSpace>() {
            @Override
            public int compare(NameSpace o1, NameSpace o2) {
                return o1.getPriority() - o2.getPriority();
            }
        });

        // Get URIs
        List<String> loadedList = new ArrayList<String>();
        for (NameSpace n : namespaceList) {

            loadedList.addAll(n.getOntologyURIs());

        }

        return loadedList;
    }

}
