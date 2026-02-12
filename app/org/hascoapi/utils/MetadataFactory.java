package org.hascoapi.utils;

import java.net.URISyntaxException;
import java.util.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.*;
import org.eclipse.rdf4j.model.util.RDFCollections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetadataFactory {

    private static final Logger log = LoggerFactory.getLogger(MetadataFactory.class);

    public static Model createModel(List<Map<String, Object>> rows, String namedGraphUri) {
        return createModel(rows, null, namedGraphUri, null);
    }

    public static Model createModel(List<Map<String, Object>> rows, Map<String,List<String>>  property_lists, String namedGraphUri) {
        return createModel(rows, property_lists, namedGraphUri, null);
    }

    public static Model createModel(List<Map<String, Object>> rows, Map<String,List<String>>  property_lists, String namedGraphUri, Model model) {

        if (rows == null) {
            System.out.println("[ERROR] MetadataFactory.createModel() received null ROWS");
        }
        if (namedGraphUri == null || namedGraphUri.isEmpty()) {
            System.out.println("[ERROR] MetadataFactory.createModel() received null namedGraphUri");
        }

        if (model == null) {
            ModelFactory modelFactory = new LinkedHashModelFactory();
            model = modelFactory.createEmptyModel();
        }

        ValueFactory factory = SimpleValueFactory.getInstance();
        IRI namedGraph = null;
        if (!namedGraphUri.isEmpty()) {
            namedGraph = factory.createIRI(namedGraphUri);
        }

        int rowIndex = 0;
        for (Map<String, Object> row : rows) {
            rowIndex++;
            if (row == null || row.get("hasURI") == null) {
                System.out.println("[ERROR] Row " + rowIndex + " - MetadataFactory.createModel() failed because the 'hasURI' is missing");
            } else {
                String subjectURI = (String)row.get("hasURI");


                IRI sub = factory.createIRI(URIUtils.replacePrefixEx(subjectURI));

                int propertyCount = 0;
                for (String key : row.keySet()) {

                    if ("hasURI".equals(key)) continue;

                    propertyCount++;

                    IRI pred = null;
                    if ("a".equals(key)) {
                        pred = factory.createIRI(URIUtils.replacePrefixEx("rdf:type"));
                    } else {
                        pred = factory.createIRI(URIUtils.replacePrefixEx(key));
                    }

                    String cellValue = null;
                    Object raw = row.get(key);
                    if (raw instanceof List) {
                        List<?> list = (List<?>) raw;
                        if (!list.isEmpty()) {
                            cellValue = list.get(0).toString();
                        }
                    } else if (raw != null) {
                        cellValue = raw.toString();
                    }

                    if (URIUtils.isValidURI(cellValue)) {
                        IRI obj = factory.createIRI(URIUtils.replacePrefixEx(cellValue));
                        if (namedGraph == null) {
                            model.add(sub, pred, obj);
                            System.out.println("[WARNING] Triple (" + sub + "," + pred + "," + obj + ") is default named graph.");
                        } else {
                            model.add(sub, pred, obj, (Resource)namedGraph);
                        }
                    } else {
                        if (cellValue == null) {
                            cellValue = "NULL";
                        }
                        Literal obj = factory.createLiteral(
                                cellValue.replace("\n", " ").replace("\r", " ").replace("\"", "''"));
                        if (namedGraph == null) {
                            model.add(sub, pred, obj);
                            System.out.println("[WARNING] Triple (" + sub + "," + pred + "," + obj + ") is default named graph.");
                        } else {
                            model.add(sub, pred, obj, (Resource)namedGraph);
                        }
                    }
                }

                if (property_lists != null && property_lists.size() > 0) {
                    for (Map.Entry<String, List<String>> entry : property_lists.entrySet()) {
                        String predRaw = (String)entry.getKey();
                        IRI pred = factory.createIRI(URIUtils.replacePrefixEx(predRaw));
                        List<String> list = (List<String>)entry.getValue();
                        if (list != null && list.size() > 0) {
                            for (String objRaw : list) {
                                if (URIUtils.isValidURI(objRaw)) {
                                    IRI obj = factory.createIRI(URIUtils.replacePrefixEx(objRaw));
                                    if (namedGraph == null) {
                                        model.add(sub, pred, obj);
                                        System.out.println("[WARNING] Triple (" + sub + "," + pred + "," + obj + ") is default named graph.");
                                    } else {
                                        model.add(sub, pred, obj, (Resource)namedGraph);
                                    }
                                } else {
                                    if (objRaw == null) {
                                        objRaw = "NULL";
                                    }
                                    Literal obj = factory.createLiteral(objRaw.replace("\n", " ").replace("\r", " ").replace("\"", "''"));
                                    if (namedGraph == null) {
                                        model.add(sub, pred, obj);
                                        System.out.println("[WARNING] Triple (" + sub + "," + pred + "," + obj + ") is default named graph.");
                                    } else {
                                        model.add(sub, pred, obj, (Resource)namedGraph);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }


        return model;
    }

    /*
    private static void addAttributeListToModel(Model model, String key, Map<String, Object> row, IRI subj, IRI pred, IRI namedGraph) {

        // debug
        if ( subj.toString().contains("ZBFA")) {
            int x = 1;
        }
        // end of debug

        SimpleValueFactory simpleValueFactory = SimpleValueFactory.getInstance();
        BNode head = simpleValueFactory.createBNode();

        Object objValue = row.get(key);
        List<String> attributes = new ArrayList<>();

        if ( objValue instanceof String ) {
            attributes.add((String)objValue);
        } else if ( objValue instanceof ArrayList ) {
            attributes = (ArrayList<String>)objValue;
            attributes.remove(attributes.size()-1);
        }

        List<IRI> attributeIRIs = new ArrayList<>();
        for ( String attribute : attributes ) {
            attributeIRIs.add(simpleValueFactory.createIRI(URIUtils.replacePrefixEx(attribute)));
        }
        Collections.reverse(attributeIRIs);

        Model tmpModel = RDFCollections.asRDF(attributeIRIs, head, new LinkedHashModel());
        tmpModel.add(subj, pred, head);

        tmpModel.forEach(statement -> {
            model.add(statement.getSubject(), statement.getPredicate(), statement.getObject(), (Resource)namedGraph);
        });

    }
    */

    public static int commitModelToTripleStore(Model model, String endpointUrl) {
        if (model == null) {
            System.out.println("[ERROR] MetadataFactory.commitModelToTripleStore is receiving a NULL model.");
        }
        try {
            GSPClient gspClient = new GSPClient(endpointUrl);
            //System.out.println("Calling GSPClient.");
            gspClient.postModel(model);
            //System.out.println("Completed GSPClient.");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (model == null) {
            return 0;
        }
        return model.size();
    }
}
