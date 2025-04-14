package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.SKOS;
import org.hascoapi.vocabularies.VSTOI;

public class GenericFindSocial<T> {

    public static <T> List<T> findByKeywordTypeManagerEmailandStatusWithPages(Class clazz, String keyword, String type, String managerEmail, String status, int pageSize, int offset) {
        if (clazz == null) {
            return null;
        }
        String hascoType = GenericFind.classNameWithNamespace(clazz);
        return findElementsByKeywordTypeManagerEmailAndStatusWithPages(clazz, hascoType, keyword, type, managerEmail, status, pageSize, offset);
    }

    public static <T> List<T> findElementsByKeywordTypeManagerEmailAndStatusWithPages(Class clazz, String hascoType, String keyword, String type, String managerEmail, String status, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
                " ?uri hasco:hascoType " + hascoType + " . ";
		if (type != null && !type.isEmpty()) {
			queryString += " ?uri rdfs:type <" + type + "> . ";
		}
        if (managerEmail != null && !managerEmail.isEmpty()) {
			queryString += " ?uri vstoi:hasSIRManagerEmail ?managerEmail . ";
        }
        if (status != null && !status.isEmpty()) {
			queryString += "  ?uri vstoi:hasStatus <" + status + "> . ";
        }
 		queryString += " OPTIONAL { ?uri rdfs:label ?label . } ";
		if (keyword != null && !keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		}
        if (managerEmail != null && !managerEmail.isEmpty()) {
	        queryString += "   FILTER (?managerEmail = \"" + managerEmail + "\") ";
        }
        queryString += "} " +
                " ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return GenericFind.findByQuery(clazz, queryString);
	}

	public static int findTotalByKeywordAndLanguage(Class clazz, String keyword, String type, String managerEmail, String status) {
        String hascoType = GenericFind.classNameWithNamespace(clazz);
        if (hascoType == null) {
            return -1;
        }
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
            " ?uri hasco:hascoType " + hascoType + " . ";
		if (type != null && !type.isEmpty()) {
			queryString += " ?uri rdfs:type <" + type + "> . ";
		}
        if (managerEmail != null && !managerEmail.isEmpty()) {
			queryString += " ?uri vstoi:hasSIRManagerEmail ?managerEmail . ";
        }
        if (status != null && !status.isEmpty()) {
			queryString += "  ?uri vstoi:hasStatus <" + status + "> . ";
        }
 		queryString += " OPTIONAL { ?uri rdfs:label ?label . } ";
		if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		} else if (!managerEmail.isEmpty()) {
	        queryString += "   FILTER (?managerEmail = \"" + managerEmail + "\") ";
        }
        queryString += "}";
        return GenericFind.findTotalByQuery(queryString);
	}


}

