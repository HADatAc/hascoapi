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

public class GenericFindWithStatus<T> {

    /**
     *     FIND ELEMENTS BY STATUS (AND THEIR TOTALS)
     */

     public List<T> findByStatusWithPages(Class clazz, String hasStatus, int pageSize, int offset) {
        //System.out.println("findByStatusWithPages: Clazz=[" + clazz + "]");
        String hascoTypeStr = GenericFind.classNameWithNamespace(clazz);
        if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
            hascoTypeStr = GenericFind.superclassNameWithNamespace(clazz);
        }
        if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
            return null;
        }
        //System.out.println("findByStatusWithPages: hascoTypeStr=[" + hascoTypeStr + "]");
        if (clazz == Detector.class) {
            return findDetectorInstancesByStatusWithPages(clazz, hascoTypeStr, hasStatus, pageSize, offset);
        } else if (GenericFind.isSIR(clazz)) {
            return findSIRInstancesByStatusWithPages(clazz, hascoTypeStr, hasStatus, pageSize, offset);
        } else if (GenericFind.isMT(clazz)) {
            return findMTInstancesByStatusWithPages(clazz, hascoTypeStr, hasStatus, pageSize, offset);
        } else {
            return findElementsByStatusWithPages(clazz, hascoTypeStr, hasStatus, pageSize, offset);
        }
    }

	public List<T> findDetectorInstancesByStatusWithPages(Class clazz, String hascoTypeStr, String hasStatus, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
                " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " OPTIONAL { ?stem vstoi:hasContent ?content . } " +
				" ?uri vstoi:hasStatus <" + hasStatus + "> . " +
				"}" +
				" ORDER BY ASC(?content) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return GenericFind.findByQuery(clazz, queryString);
	}

	public List<T> findSIRInstancesByStatusWithPages(Class clazz, String hascoTypeStr, String hasStatus, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?uri hasco:hascoType " + hascoTypeStr + " . " +
                " OPTIONAL { ?uri vstoi:hasContent ?content . } " +
				" ?uri vstoi:hasStatus <" + hasStatus + "> . " +
				"}" +
				" ORDER BY ASC(?content) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return GenericFind.findByQuery(clazz, queryString);
	}

	public List<T> findMTInstancesByStatusWithPages(Class clazz, String hascoTypeStr, String hasStatus, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?uri hasco:hascoType " + hascoTypeStr + " . " +
				" OPTIONAL { ?uri rdfs:label ?label . } " +
                " ?uri hasco:hasDataFile ?dataFile . " +   // a MT concept requires an associated DataFile
				" ?uri vstoi:hasStatus <" + hasStatus + "> . " +
				"}" +
				" ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return GenericFind.findByQuery(clazz, queryString);
	}

	public List<T> findElementsByStatusWithPages(Class clazz, String hascoTypeStr, String hasStatus, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?uri hasco:hascoType " + hascoTypeStr + " . " +
				" OPTIONAL { ?uri rdfs:label ?label . } " +
				" ?uri vstoi:hasStatus <" + hasStatus + "> . " +
				"}" +
				" ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
        //System.out.println(queryString);
		return GenericFind.findByQuery(clazz, queryString);
	}

	public static int findTotalByStatus(Class clazz, String hasStatus) {
        if (GenericFind.isMT(clazz)) {
            return findTotalMTByStatus(clazz,hasStatus);
        } else {
            String hascoTypeStr = GenericFind.classNameWithNamespace(clazz);
            if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
                hascoTypeStr = GenericFind.superclassNameWithNamespace(clazz);
            }
            if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
                return -1;
            }
            return findTotalElementsByStatus(hascoTypeStr,hasStatus);
        }
    }

	public static int findTotalElementsByStatus(String hascoTypeStr, String hasStatus) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
				" ?uri hasco:hascoType " + hascoTypeStr + " . " +
				" ?uri vstoi:hasStatus <" + hasStatus + "> . " +
				"}";
        return GenericFind.findTotalByQuery(queryString);
	}

	public static int findTotalMTByStatus(Class clazz, String hasStatus) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
				" ?uri hasco:hascoType " + GenericFind.classNameWithNamespace(clazz) + " . " +
                " ?uri hasco:hasDataFile ?dataFile . " +   // a MT concept requires an associated DataFile
				" ?uri vstoi:hasStatus <" + hasStatus + "> . " +
				"}";
        return GenericFind.findTotalByQuery(queryString);
	}

    /**
     *     FIND ELEMENTS BY STATUS AND MANAGER (AND THEIR TOTALS)
     */

    /* 
     *   When withCurrent=TRUE it means retrieving all the elements of the requested status PLUS the current ones.
     *   When withCurrent=FALSE it means retrieving just the elements of the requested status.
     */ 
	public List<T> findByStatusManagerEmailWithPages(Class clazz, String hasStatus, String managerEmail, boolean withCurrent, int pageSize, int offset) {
        //System.out.println("findByStatusManagerEmailWithPages: Clazz=[" + clazz + "]");
        String hascoTypeStr = GenericFind.classNameWithNamespace(clazz);
        if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
            hascoTypeStr = GenericFind.superclassNameWithNamespace(clazz);
        }
        if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
            return null;
        }
        //System.out.println("findByStatusManagerEmailWithPages: hascoTypeStr=[" + hascoTypeStr + "]");
        if (clazz == Detector.class) {
            return findDetectorInstancesByStatusManagerEmailWithPages(clazz, hascoTypeStr, hasStatus, managerEmail, withCurrent, pageSize, offset);
        } else if (GenericFind.isSIR(clazz)) {
            return findSIRInstancesByStatusManagerEmailWithPages(clazz, hascoTypeStr, hasStatus, managerEmail, withCurrent, pageSize, offset);
        } else if (GenericFind.isMT(clazz)) {
            return findMTInstancesByStatusManagerEmailWithPages(clazz, hascoTypeStr, hasStatus, managerEmail, withCurrent, pageSize, offset);
        } else {
            return findElementsByStatusManagerEmailWithPages(clazz, hascoTypeStr, hasStatus, managerEmail, withCurrent, pageSize, offset);
        }
    }

	public List<T> findDetectorInstancesByStatusManagerEmailWithPages(Class clazz, String hascoTypeStr, String hasStatus, String managerEmail, boolean withCurrent, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (withCurrent) {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " ?uri vstoi:hasDetectorStem ?stem . " +
                    " OPTIONAL { ?stem vstoi:hasContent ?content . } " +
                    " { " +
                    "   ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    "   ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "     FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    " } UNION { " +
                    "   ?uri vstoi:hasStatus <" + VSTOI.CURRENT + "> . " +
                    " } " +
                    "}" +
                    " ORDER BY ASC(?content) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        } else {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " ?uri vstoi:hasDetectorStem ?stem . " +
                    " OPTIONAL { ?stem vstoi:hasContent ?content . } " +
                    " ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    "}" +
                    " ORDER BY ASC(?content) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        }
		return GenericFind.findByQuery(clazz, queryString);
	}

	public List<T> findSIRInstancesByStatusManagerEmailWithPages(Class clazz, String hascoTypeStr, String hasStatus, String managerEmail,  boolean withCurrent, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (withCurrent) {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " OPTIONAL { ?uri vstoi:hasContent ?content . } " +
                    " { " +
                    "   ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    "   ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "     FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    " } UNION { " +
                    "   ?uri vstoi:hasStatus <" + VSTOI.CURRENT + "> . " +
                    " } " +
                    "}" +
                    " ORDER BY ASC(?content) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        } else {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " OPTIONAL { ?uri vstoi:hasContent ?content . } " +
                    " ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    "}" +
                    " ORDER BY ASC(?content) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        }
		return GenericFind.findByQuery(clazz, queryString);
	}

	public List<T> findMTInstancesByStatusManagerEmailWithPages(Class clazz, String hascoTypeStr, String hasStatus, String managerEmail, boolean withCurrent, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (withCurrent) {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " OPTIONAL { ?uri rdfs:label ?label . } " +
                    " ?uri hasco:hasDataFile ?dataFile . " +   // a MT concept requires an associated DataFile
                    " { " +
                    "   ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    "   ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "     FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    " } UNION { " +
                    "   ?uri vstoi:hasStatus <" + VSTOI.CURRENT + "> . " +
                    " } " +
                    "}" +
                    " ORDER BY ASC(?label) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        } else {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " OPTIONAL { ?uri rdfs:label ?label . } " +
                    " ?uri hasco:hasDataFile ?dataFile . " +   // a MT concept requires an associated DataFile
                    " ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    "}" +
                    " ORDER BY ASC(?label) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        }
		return GenericFind.findByQuery(clazz, queryString);
	}

	public List<T> findElementsByStatusManagerEmailWithPages(Class clazz, String hascoTypeStr, String hasStatus, String managerEmail, boolean withCurrent, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (withCurrent) {
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " OPTIONAL { ?uri rdfs:label ?label . } " +
                    " { " +
                    "   ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    "   ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "     FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    " } UNION { " +
                    "   ?uri vstoi:hasStatus <" + VSTOI.CURRENT + "> . " +
                    " } " +
                    "}" +
                    " ORDER BY ASC(?label) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        } else {            
            queryString += " SELECT ?uri WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " OPTIONAL { ?uri rdfs:label ?label . } " +
                    " ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    "}" +
                    " ORDER BY ASC(?label) " +
                    " LIMIT " + pageSize +
                    " OFFSET " + offset;
        }
        //System.out.println(queryString);
		return GenericFind.findByQuery(clazz, queryString);
	}

	public static int findTotalByStatusManagerEmail(Class clazz, String hasStatus, String managerEmail, boolean withCurrent) {
        if (GenericFind.isMT(clazz)) {
            return findTotalMTByStatusManagerEmail(clazz,hasStatus,managerEmail, withCurrent);
        } else {
            String hascoTypeStr = GenericFind.classNameWithNamespace(clazz);
            if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
                hascoTypeStr = GenericFind.superclassNameWithNamespace(clazz);
            }
            if (hascoTypeStr == null || hascoTypeStr.isEmpty()) {
                return -1;
            }
            return findTotalElementsByStatusManagerEmail(hascoTypeStr,hasStatus,managerEmail, withCurrent);
        }
    }

	public static int findTotalElementsByStatusManagerEmail(String hascoTypeStr, String hasStatus, String managerEmail, boolean withCurrent) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (withCurrent) {
            queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " { " +
                    "   ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    "   ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "     FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    " } UNION { " +
                    "   ?uri vstoi:hasStatus <" + VSTOI.CURRENT + "> . " +
                    " } " +
                    "}";
        } else {
            queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                    " ?uri hasco:hascoType " + hascoTypeStr + " . " +
                    " ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    "}";
        }
        return GenericFind.findTotalByQuery(queryString);
	}

	public static int findTotalMTByStatusManagerEmail(Class clazz, String hasStatus, String managerEmail, boolean withCurrent) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
        if (withCurrent) {
            queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                    " ?uri hasco:hascoType " + GenericFind.classNameWithNamespace(clazz) + " . " +
                    " ?uri hasco:hasDataFile ?dataFile . " +   // a MT concept requires an associated DataFile
                    " { " +
                    "   ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    "   ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "     FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    " } UNION { " +
                    "   ?uri vstoi:hasStatus <" + VSTOI.CURRENT + "> . " +
                    " } " +
                    "}";
        } else {
            queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
                    " ?uri hasco:hascoType " + GenericFind.classNameWithNamespace(clazz) + " . " +
                    " ?uri hasco:hasDataFile ?dataFile . " +   // a MT concept requires an associated DataFile
                    " ?uri vstoi:hasStatus <" + hasStatus + "> . " +
                    " ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
                    "   FILTER (?managerEmail = \"" + managerEmail + "\") " +
                    "}";
        }
        return GenericFind.findTotalByQuery(queryString);
	}

}

