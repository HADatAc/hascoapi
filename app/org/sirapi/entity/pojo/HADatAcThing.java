package org.sirapi.entity.pojo;

import java.io.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.NotImplementedException;
import org.sirapi.utils.MetadataFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.model.Model;
import org.sirapi.RepositoryInstance;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.GSPClient;
import org.sirapi.utils.MetadataFactory;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.URIUtils;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.Constants;

import org.sirapi.annotations.PropertyField;
import org.sirapi.annotations.ReversedPropertyField;
import org.sirapi.annotations.Subject;

/*
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.sirapi.utils.*;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.eclipse.rdf4j.model.Model;

import org.sirapi.annotations.PropertyField;
import org.sirapi.annotations.ReversedPropertyField;
import org.sirapi.annotations.Subject;
*/

public abstract class HADatAcThing {

    public static String OWL_THING = "http://www.w3.org/2002/07/owl#Thing";

    @Subject
    String uri = "";

    @PropertyField(uri="rdf:type")
    String typeUri = "";

    @PropertyField(uri="hasco:hascoType")
    String hascoTypeUri = "";

    @PropertyField(uri="rdfs:label")
    String label = "";

    @PropertyField(uri="rdfs:comment")
    String comment = "";

    String field = "";
    String query = "";
    int count = 0;

    String namedGraph = "";

    // delete the object or not when deleting the data file it was generated from
    boolean deletable = true;

    public static String stringify(List<String> preValues) {
        List<String> finalValues = new ArrayList<String>();
        preValues.forEach((value) -> {
            if (value.startsWith("http")) {
                if (value.contains("; ")) {
                    finalValues.addAll(Arrays.asList(value.split("; ")).stream()
                            .map(s -> "<" + s + ">")
                            .collect(Collectors.toList()));
                } else {
                    finalValues.add("<" + value + ">");
                }
            } else {
                finalValues.add("\"" + value + "\"");
            }
        });

        return String.join(" ", finalValues);
    }

    public String getUri() {
        return uri.replace("<","").replace(">","");
    }

    public String getUriNamespace() {
        if(uri == "" || uri == null || uri.equals("")){
            return "";
        } else{
            return URIUtils.replaceNameSpaceEx(uri.replace("<","").replace(">",""));
        }
    }

    public void setUri(String uri) {
        if (uri == null || uri.equals("")) {
            this.uri = "";
            return;
        }
        this.uri = URIUtils.replacePrefixEx(uri);
    }

    public String getTypeUri() {
        return typeUri;
    }

    public void setTypeUri(String typeUri) {
        this.typeUri = typeUri;
    }

    public String getTypeLabel() {
        if (typeUri == null) {
            return "";
        }
        Entity ent = Entity.find(typeUri);
        if (ent == null || ent.getLabel() == null) {
            return "";
        }
        return ent.getLabel();
    }

    public String getHascoTypeUri() { return hascoTypeUri; }

    public void setHascoTypeUri(String hascoTypeUri) {
        this.hascoTypeUri = hascoTypeUri;
    }

    public String getHascoTypeLabel() {
        if (hascoTypeUri == null) {
            return "";
        }
        Entity ent = Entity.find(hascoTypeUri);
        if (ent == null || ent.getLabel() == null) {
            return "";
        }
        return ent.getLabel();
    }

    public String getTypeNamespace() {
        if (uri == "" || uri == null || uri.equals("")){
            return "";
        } else {
            return URIUtils.replaceNameSpaceEx(uri.replace("<","").replace(">",""));
        }
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getNamedGraph() {
        return namedGraph;
    }

    public void setNamedGraph(String namedGraph) {
        this.namedGraph = namedGraph;
    };

    public boolean getDeletable() {
        return deletable;
    }

    public void setDeletable(boolean deletable) {
        this.deletable = deletable;
    }

    public static List<String> getLabels(String uri) {
        List<String> results = new ArrayList<String>();
        if (uri == null || uri.equals("")) {
            return results;
        }
        if (uri.startsWith("http")) {
            uri = "<" + uri.trim() + ">";
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?label WHERE { \n" +
                "  " + uri + " rdfs:label ?label . \n" +
                "}";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln.get("label") != null && !soln.get("label").toString().isEmpty()) {
                results.add(soln.get("label").toString().replace("@en", ""));
            }
        }

        return results;
    }

    public static String getLabel(String uri) {
        List<String> labels = getLabels(uri);
        if (labels.size() > 0) {
            return labels.get(0);
        }

        return "";
    }

    public static String getShortestLabel(String uri) {
        List<String> labels = getLabels(uri);
        if (labels.size() > 0) {
            labels.sort(new Comparator<String>() {
                @Override
                public int compare(String o1, String o2) {
                    return Integer.compare(o1.length(), o2.length());
                }
            });

            return labels.get(0);
        }

        return "";
    }

    public static int getNumberInstances() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "select (COUNT(?categ) as ?tot) where " +
                " { SELECT ?i (COUNT(?i) as ?categ) " +
                "     WHERE {" +
                "             ?i a ?c . " +
                "     } " +
                " GROUP BY ?i " +
                " }";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
          e.printStackTrace();
        }

        return -1;
    }

    public void save() { throw new NotImplementedException("Used unimplemented HADatAcThing.save() method"); }
    public void delete() { throw new NotImplementedException("Used unimplemented HADatAcThing.delete() method"); }

    private Model generateRDFModel() {
        Map<String, Object> row = new HashMap<String, Object>();
        List<Map<String, Object>> reversed_rows = new ArrayList<Map<String, Object>>();

        try {
            Class<?> currentClass = getClass();
            while(currentClass != null) {
                //System.out.println("inside HADatAcThing.generateRDFModel: currentClass: " + currentClass.getName());
                //System.out.println("inside HADatAcThing.generateRDFModel(): hasURI: [" + uri + "]");

                for (Field field: currentClass.getDeclaredFields()) {
                    //System.out.println("inside HADatAcThing.saveToTripleStore(): field [" + field.getName() + "] or type [" + field.getType() + "]");
                    field.setAccessible(true);
                    if (field.isAnnotationPresent(Subject.class)) {
                        String uri = (String)field.get(this);
                        //System.out.println("inside HADatAcThing.saveToTripleStore(): has Subject.class annotation present. hasUri=[" + uri + "]");
                        if (URIUtils.isValidURI(uri)) {
                            row.put("hasURI", uri);
                            //System.out.println("inside HADatAcThing.saveToTripleStore(): hasUri=[" + uri + "]");
                        } else {
                            System.out.println("[ERROR] URI [" + uri + "] IS NOT VALID");
                            return null;
                        }
                    }

                    if (field.isAnnotationPresent(ReversedPropertyField.class)) {
                        ReversedPropertyField reversedPropertyField = field.getAnnotation(ReversedPropertyField.class);
                        String propertyUri = reversedPropertyField.uri();

                        if (field.getType().equals(String.class)) {
                            String value = (String)field.get(this);
                            if (!value.isEmpty()) {
                                Map<String, Object> rvs_row = new HashMap<String, Object>();
                                rvs_row.put(propertyUri, value);
                                reversed_rows.add(rvs_row);
                            }
                        }
                    }

                    if (field.isAnnotationPresent(PropertyField.class)) {
                        PropertyField propertyField = field.getAnnotation(PropertyField.class);
                        String propertyUri = propertyField.uri();
                        //System.out.println("inside HADatAcThing.saveToTripleStore(): propertyUri=" + propertyField.uri());

                        if (field.getType().equals(String.class)) {
                            String value = (String)field.get(this);
                            if (value != null && !value.isEmpty()) {
                                //System.out.println("in String assigned [" + value + "] to [" + propertyUri + "]");
                                row.put(propertyUri, value);
                            }
                        }

                        //System.out.println("inside HADatAcThing.saveToTripleStore() (1) ");

                        if (field.getType().equals(List.class)) {
                            List<?> list = (List<?>)field.get(this);
                            if (!list.isEmpty() && list.get(0) instanceof String) {
                                for (String element : (List<String>)list) {
                                    if (element != null && !element.isEmpty()) {
                                        //System.out.println("in List assigned [" + element + "] to [" + propertyUri + "]");
                                        row.put(propertyUri, element);
                                    }
                                }
                            }
                        }

                        //System.out.println("inside HADatAcThing.saveToTripleStore() (2) ");

                        if (field.getType().equals(Integer.class)) {
                            row.put(propertyUri, ((Integer)field.get(this)).toString());
                        }

                        if (field.getType().equals(Double.class)) {
                            row.put(propertyUri, ((Double)field.get(this)).toString());
                        }

                        if (field.getType().equals(Long.class)) {
                            row.put(propertyUri, ((Long)field.get(this)).toString());
                        }

                        //System.out.println("inside HADatAcThing.saveToTripleStore() (3) ");
                    }
                }

                currentClass = currentClass.getSuperclass();
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //System.out.println("inside HADatAcThing.saveToTripleStore() (4) ");

        if (!row.containsKey("hasURI")) {
            return null;
        }

        //System.out.println("inside HADatAcThing.saveToTripleStore() (5) ");


        String objUri = (String)row.get("hasURI");
        for (Map<String, Object> rvs_row : reversed_rows) {
            for (String key : rvs_row.keySet()) {
                String value = (String)row.get(key);
                if (URIUtils.isValidURI(value)) {
                    rvs_row.put("hasURI", value);
                    rvs_row.remove(key);
                    rvs_row.put(key, objUri);
                } else {
                    continue;
                }
            }
        }
        reversed_rows.add(row);
        if (getNamedGraph() == null || getNamedGraph().isEmpty()) {
            //System.out.println("Default URL: [" + RepositoryInstance.getInstance().getHasDefaultNamespaceURL() + "]");
            //System.out.println("Default Abbrev: [" + RepositoryInstance.getInstance().getHasDefaultNamespaceAbbreviation() + "]");
            if (RepositoryInstance.getInstance() != null && RepositoryInstance.getInstance().getHasDefaultNamespaceURL() != null) {
                return MetadataFactory.createModel(reversed_rows,RepositoryInstance.getInstance().getHasDefaultNamespaceURL());
            } else {
                return MetadataFactory.createModel(reversed_rows,Constants.DEFAULT_REPOSITORY);
            }
        }
        return MetadataFactory.createModel(reversed_rows, getNamedGraph());
    }

    public String printRDF() {
/*
        Model model = generateRDFModel();
        ByteArrayOutputStream out = null;
        out = new ByteArrayOutputStream();
        RDFWriter writer = Rio.createWriter(RDFFormat.RDFRepositoryInstance.getInstance().getHasDefaultNamespaceURL()XML, out);
        try {
            writer.startRDF();
            for (org.eclipse.rdf4j.model.Statement st: model) {
                writer.handleStatement(st);
            }
            writer.endRDF();
        }
        catch (RDFHandlerException e) {
            // oh no, do something!
        }
        finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out.toString();

 */
        return null;
    }

    @SuppressWarnings("unchecked")
    public boolean saveToTripleStore() {
        //System.out.println("inside HADatAcThing.saveToTripleStore(): calling deleteFromTripleStore().");
        deleteFromTripleStore();

        //Model model = MetadataFactory.createModel(reversed_rows, getNamedGraph());
        Model model = generateRDFModel();
        if (model == null) {
            System.out.println("[ERROR] inside HADatAcThing.saveToTripleStore(): MetadataFactory.commitModelToTripleStore() received EMPTY model");
        }
        int numCommitted = MetadataFactory.commitModelToTripleStore(
                model, CollectionUtil.getCollectionPath(
                        CollectionUtil.Collection.SPARQL_GRAPH));

        return numCommitted >= 0;
    }

    public void fromStatement(Statement statement) {
        String predicate = statement.getPredicate().getURI();
        String object = statement.getObject().toString();

        fromPredicateObject(predicate, object);
    }

    public void fromQuerySolution(QuerySolution solnFromDescribe) {
        // build object from results of DESCRIBE query
        String predicate = solnFromDescribe.get("predicate").toString();
        String object = solnFromDescribe.get("object").toString();

        fromPredicateObject(predicate, object);
    }

    @SuppressWarnings("unchecked")
    public void fromPredicateObject(String predicate, String object) {
        try {
            Class<?> currentClass = getClass();
            while(currentClass != null) {
                for (Field field: currentClass.getDeclaredFields()) {
                    field.setAccessible(true);

                    if (field.isAnnotationPresent(PropertyField.class)) {
                        PropertyField propertyField = field.getAnnotation(PropertyField.class);
                        String propertyUri = URIUtils.replacePrefixEx(propertyField.uri());

                        if (predicate.equals(propertyUri)) {
                            if (field.getType().equals(String.class)) {
                                field.set(this, object);
                            }

                            if (field.getType().equals(List.class)) {
                                List<String> list = (List<String>)field.get(this);
                                if (!list.contains(object)) {
                                    list.add(object);
                                }
                            }

                            if (field.getType().equals(Integer.class)) {
                                field.set(this, Integer.parseInt(object));
                            }

                            if (field.getType().equals(Double.class)) {
                                field.set(this, Double.parseDouble(object));
                            }

                            if (field.getType().equals(Long.class)) {
                                field.set(this, Long.parseLong(object));
                            }
                        }
                    }
                }

                currentClass = currentClass.getSuperclass();
            }
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void deleteFromTripleStore() {
        String query = "";
        if (getUri() == null || getUri().equals("")) {
            return;
        }

        //System.out.println("Deleting <" + getUri() + "> from triple store");

        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        //System.out.println("Deleting query namespaces [" + query + "]");
        if ( this.getNamedGraph() != null && this.getNamedGraph().length() > 0 ) {
            query += "DELETE WHERE { \n" + " " +
                    "    GRAPH <" + this.getNamedGraph() + "> { \n";
            if (getUri().startsWith("http")) {
                query += "<" + this.getUri() + ">";
            } else {
                query += this.getUri();
            }
            query += " ?p ?o . } \n";
            query += " } ";
            UpdateRequest request = UpdateFactory.create(query);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();

        } else {
            // if ( getUri().contains("3539947") ) System.out.println("find 3539947!!!! delete without namespace!!!");

            String query1 = query+ " DELETE WHERE { \n " +
                   "    GRAPH <" + GSPClient.defaultGraphUri + "> { \n";
            if (getUri().startsWith("http")) {
                query1 += "<" + this.getUri() + ">";
            } else {
                query1 += this.getUri();
            }
            query1 += " ?p ?o . } \n";
            query1 += " } ";
            UpdateRequest request = UpdateFactory.create(query1);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            try {
                processor.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }

            /*
            // Added for deleting Virtual Columns
            String query2 = query +" DELETE WHERE { \n";
            query2 += " ?o ?p ";
            if (getUri().startsWith("http")) {
                query2 += "<" + this.getUri() + ">";
            } else {
                query2 += this.getUri();
            }
            query2 += " } ";
            request = UpdateFactory.create(query2);
             processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();
            System.out.println("Deleting query 2 [" + query2 + "]");

            // Added for deleting triples on empty graph
            String query3 = query+ " DELETE WHERE { \n";
            if (getUri().startsWith("http")) {
                query3 += "<" + this.getUri() + ">";
            } else {
                query3 += this.getUri();
            }
            query3 += " ?p ?o \n";
            query3 += " } ";
            request = UpdateFactory.create(query3);
            processor = UpdateExecutionFactory.createRemote(
                    request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
            processor.execute();
            System.out.println("Deleting query 3 [" + query3 + "]");
             */

        }

        //System.out.println("Deleted <" + getUri() + "> from triple store");
    }

    public abstract boolean saveToSolr();
    public abstract int deleteFromSolr();

}
