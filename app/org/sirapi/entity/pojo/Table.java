package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.*;

public class Table implements Comparable<Table> {

    static String className = "vstoi:Table";

    private String code;

    private String value;

    private String url;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getURL() {
        return url;
    }

    public void setURL(String url) {
        this.url = url;
    }

    public static List<Table> findLanguage() {
        List<Table> tables = new ArrayList<Table>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri ?label ?definition WHERE { " +
                " ?uri a skos:Concept . " +
                " ?uri skos:prefLabel ?label . " +
                " ?uri skos:definition ?definition . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Table newTable = new Table();
            newTable.setURL(soln.getResource("uri").getURI());
            newTable.setCode(soln.getLiteral("label").getString());
            newTable.setValue(soln.getLiteral("definition").getString());
            tables.add(newTable);
        }

        java.util.Collections.sort((List<Table>) tables);
        return tables;

    }

    public static List<Table> findGenerationActivity() {
        List<Table> tables = new ArrayList<Table>();
        Iterator<Map.Entry<String, String>> iterator = VSTOI.wasGeneratedBy.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            Table newTable = new Table();
            newTable.setURL(entry.getKey());
            newTable.setValue(entry.getValue());
            tables.add(newTable);
        }

        java.util.Collections.sort((List<Table>) tables);
        return tables;

    }

    public static List<Table> findInformant() {
        List<Table> tables = new ArrayList<Table>();
        Iterator<Map.Entry<String, String>> iterator = VSTOI.informant.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            Table newTable = new Table();
            newTable.setURL(entry.getKey());
            newTable.setValue(entry.getValue());
            tables.add(newTable);
        }

        java.util.Collections.sort((List<Table>) tables);
        return tables;

    }

    @Override
    public int compareTo(Table another) {
        if (this.getValue() != null && another.getValue() != null) {
            return this.getValue().compareTo(another.getValue());
        }
        return this.getURL().compareTo(another.getURL());
    }

}
