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
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SKOS;
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.VSTOI;

public class GenericFind<T> {

    public static Class getElementClass(String elementType) {
        
        if (elementType.equals("instrument")) {
            return Instrument.class;
        } else if (elementType.equals("detectorstem")) {
            return DetectorStem.class;
        } else if (elementType.equals("detector")) {
            return Detector.class;
        } else if (elementType.equals("containerslot")) {
            return ContainerSlot.class;
        } else if (elementType.equals("codebook")) {
            return Codebook.class;
        } else if (elementType.equals("responseoption")) {
            return ResponseOption.class;
        } else if (elementType.equals("codebookslot")) {
            return CodebookSlot.class;
        } else if (elementType.equals("annotationstem")) {
            return AnnotationStem.class;
        } else if (elementType.equals("annotation")) {
            return Annotation.class;
        } else if (elementType.equals("semanticvariable")) {
            return SemanticVariable.class;
        } else if (elementType.equals("instrumenttype")) {
            return InstrumentType.class;
        } else if (elementType.equals("detectorstemtype")) {
            return DetectorStemType.class;
        } else if (elementType.equals("entity")) {
            return Entity.class;
        } else if (elementType.equals("attribute")) {
            return Attribute.class;
        } else if (elementType.equals("unit")) {
            return Unit.class;
        } else if (elementType.equals("agent")) {
            return Agent.class;
        } 
        return null;
    }


    private static String classNameWithNamespace (Class clazz) {
        if (clazz == Instrument.class) {
            return URIUtils.replaceNameSpace(VSTOI.INSTRUMENT);
        } else if (clazz == ContainerSlot.class) {
            return URIUtils.replaceNameSpace(VSTOI.CONTAINER_SLOT);
        } else if (clazz == DetectorStem.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR_STEM);
        } else if (clazz == Detector.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR);
        } else if (clazz == Codebook.class) {
            return URIUtils.replaceNameSpace(VSTOI.CODEBOOK);
        } else if (clazz == CodebookSlot.class) {
            return URIUtils.replaceNameSpace(VSTOI.CODEBOOK_SLOT);
        } else if (clazz == ResponseOption.class) {
            return URIUtils.replaceNameSpace(VSTOI.RESPONSE_OPTION);
        } else if (clazz == AnnotationStem.class) {
            return URIUtils.replaceNameSpace(VSTOI.ANNOTATION_STEM);
        } else if (clazz == Annotation.class) {
            return URIUtils.replaceNameSpace(VSTOI.ANNOTATION);
        } else if (clazz == SemanticVariable.class) {
            return URIUtils.replaceNameSpace(HASCO.SEMANTIC_VARIABLE);
        } else if (clazz == Agent.class) {
            return URIUtils.replaceNameSpace(HASCO.AGENT);
        }
        return null;
    }

    private static boolean isSIR (Class clazz) {
        // Instrument/Container is not SIR Element
        if (clazz == DetectorStem.class ||
            clazz == Detector.class ||
            clazz == ResponseOption.class ||
            clazz == AnnotationStem.class) {
            return true;
        }
        return false;
    }

    private static String superclassNameWithNamespace (Class clazz) {
        if (clazz == InstrumentType.class) {
            return URIUtils.replaceNameSpace(VSTOI.INSTRUMENT);
        } else if (clazz == DetectorStemType.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR);
        } else if (clazz == Entity.class) {
            return URIUtils.replaceNameSpace(SIO.ENTITY);
        } else if (clazz == Attribute.class) {
            return URIUtils.replaceNameSpace(SIO.ATTRIBUTE);
        } else if (clazz == Unit.class) {
            return URIUtils.replaceNameSpace(SIO.UNIT);
        } 
        return null;
    }

    /**
     *     FIND ELEMENTS (AND THEIR TOTALS) WITH PAGES
     */

    public static <T> List<T> findWithPages(Class clazz, int pageSize, int offset) {
        if (clazz == null) {
            return null;
        }
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            if (clazz == Detector.class) {
              return findDetectorInstancesWithPages(clazz, className, pageSize, offset);
            } else if (isSIR(clazz)) {
              return findSIRInstancesWithPages(clazz, className, pageSize, offset);
            } else {
              return findInstancesWithPages(clazz, className, pageSize, offset);
            }
        }
        String superClassName = superclassNameWithNamespace(clazz);
        if (superClassName != null) {
            return findSubclassesWithPages(clazz, superClassName, pageSize, offset);
        }
        return null;
    }

    private static <T> List<T> findDetectorInstancesWithPages(Class clazz, String className, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(clazz, queryString);
    }
    
    private static <T> List<T> findSIRInstancesWithPages(Class clazz, String className, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " ?uri vstoi:hasContent ?content . " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(clazz, queryString);
    }
    
    private static <T> List<T> findInstancesWithPages(Class clazz, String className, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " OPTIONAL { ?uri rdfs:label ?label . } " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(clazz, queryString);
    }
    
    private static <T> List<T> findSubclassesWithPages(Class clazz, String superClassName, int pageSize, int offset) {
        //System.out.println("subClassName: " + subClassName);
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* " + superClassName + " . " +
                " ?uri rdfs:label ?label . " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(clazz, queryString);
    }
    
    public static int getNumberElements(String elementType) {
        if (elementType == null || elementType.isEmpty()) {
            return -1   ;
        }
        Class clazz = getElementClass(elementType);
        if (clazz == null) {        
            return -1;
        }
        return GenericFind.findTotal(clazz);
    }

    public static int findTotal(Class clazz) {
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            return findTotalInstances(className);
        }
        String superClassName = superclassNameWithNamespace(clazz);
        if (superClassName != null) {
            return findTotalSubclasses(superClassName);
        }
        return -1;
    }

    private static int findTotalInstances(String className) {
        String queryString = "";
        queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " select (count(?uri) as ?tot) where { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                "}";
        return findTotalByQuery(queryString);
    }

	private static int findTotalSubclasses(String superClassName) {
        String queryString = "";
        queryString += NameSpaces.getInstance().printSparqlNameSpaceList();
        queryString += " select (count(?type) as ?tot) where { " +
                " ?type rdfs:subClassOf* " + superClassName + " . " +
                "}";
        return findTotalByQuery(queryString);
	}

    /**
     *     FIND ELEMENTS BY KEYWORD (AND THEIR TOTALS) WITH PAGES
     */

    public static <T> List<T> findByKeywordWithPages(Class clazz, String keyword, int pageSize, int offset) {
        if (clazz == null) {
            return null;
        }
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            if (clazz == Detector.class) {
              return findDetectorInstancesByKeywordWithPages(clazz, className, keyword, pageSize, offset);
            } else if (isSIR(clazz)) {
              return findSIRInstancesByKeywordWithPages(clazz, className, keyword, pageSize, offset);
            } else {
              return findInstancesByKeywordWithPages(clazz, className, keyword, pageSize, offset);
            }
        }
        String subClassName = superclassNameWithNamespace(clazz);
        if (subClassName != null) {
            return findSubclassesByKeywordWithPages(clazz, subClassName, keyword, pageSize, offset);
        }
        return null;
    }

    public static <T> List<T> findDetectorInstancesByKeywordWithPages(Class clazz, String className, String keyword, int pageSize, int offset) {
        //System.out.println("GenericFind.findDetectorInstancesByKeywordWithPages: " + className + "  " + pageSize + "  " + offset);
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " ?stem vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        //System.out.println("GenericFind.findSIRInstancesByKeywordWithPages: [" + queryString + "]");
        return findByQuery(clazz, queryString);
    }

    public static <T> List<T> findSIRInstancesByKeywordWithPages(Class clazz, String className, String keyword, int pageSize, int offset) {
        //System.out.println("GenericFind.findSIRInstancesByKeywordWithPages: " + className + "  " + pageSize + "  " + offset);
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " ?uri vstoi:hasContent ?content . " +
                "   FILTER regex(?content, \"" + keyword + "\", \"i\") " +
                "} " +
                " ORDER BY ASC(?content) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        //System.out.println("GenericFind.findSIRInstancesByKeywordWithPages: [" + queryString + "]");
        return findByQuery(clazz, queryString);
    }

    public static <T> List<T> findInstancesByKeywordWithPages(Class clazz, String className, String keyword, int pageSize, int offset) {
        //System.out.println("GenericFind.findInstancesByKeywordWithPages: " + className + "  " + pageSize + "  " + offset);
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        //System.out.println("GenericFind.findInstancesByKeywordWithPages: [" + queryString + "]");
        return findByQuery(clazz, queryString);
    }

    public static <T> List<T> findSubclassesByKeywordWithPages(Class clazz, String superClassName, String keyword, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* " + superClassName + " . " +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findByQuery(clazz, queryString);
    }

    public static int findTotalByKeyword(Class clazz, String keyword) {
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            return findTotalInstancesByKeyword(className, keyword);
        }
        String superClassName = superclassNameWithNamespace(clazz);
        if (superClassName != null) {
            return findTotalSubclassesByKeyword(superClassName, keyword);
        }
        return -1;
    }

    public static int findTotalInstancesByKeyword(String className, String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type . " +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} ";

        return findTotalByQuery(queryString);
    }

    public static int findTotalSubclassesByKeyword(String superClassName, String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?uri rdfs:subClassOf* " + superClassName + " . " +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} ";

        return findTotalByQuery(queryString);
    }

    /**
     *     FIND ELEMENTS BY KEYWORD AND LANGUAGE (AND THEIR TOTALS) WITH PAHES
     */

    public static <T> List<T> findByKeywordAndLanguageWithPages(Class clazz, String keyword, String language, int pageSize, int offset) {
        if (clazz == null) {
            return null;
        }
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            if (clazz == Detector.class) {
              return findDetectorInstancesByKeywordAndLanguageWithPages(clazz, className, keyword, language, pageSize, offset);
            } else if (isSIR(clazz)) {
              return findSIRInstancesByKeywordAndLanguageWithPages(clazz, className, keyword, language, pageSize, offset);
            } else {
              return findInstancesByKeywordAndLanguageWithPages(clazz, className, keyword, language, pageSize, offset);
            }
        }
        String subClassName = superclassNameWithNamespace(clazz);
        if (subClassName != null) {
            return findSubClassesByKeywordAndLanguageWithPages(clazz, subClassName, keyword, language, pageSize, offset);
        }
        return null;
    }

	public static <T> List<T> findDetectorInstancesByKeywordAndLanguageWithPages(Class clazz, String className, String keyword, String language, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?type rdfs:subClassOf* " + className + " . " +
				" ?uri a ?type . " +
                " ?uri vstoi:hasDetectorStem ?stem . ";
		if (!language.isEmpty()) {
			queryString += " ?stem vstoi:hasLanguage ?language . ";
		}
		queryString += " OPTIONAL { ?stem vstoi:hasContent ?content . } ";
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "} " +
                " ORDER BY ASC(?content) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public static <T> List<T> findSIRInstancesByKeywordAndLanguageWithPages(Class clazz, String className, String keyword, String language, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?type rdfs:subClassOf* " + className + " . " +
				" ?uri a ?type .";
		if (!language.isEmpty()) {
			queryString += " ?uri vstoi:hasLanguage ?language . ";
		}
		queryString += " OPTIONAL { ?uri vstoi:hasContent ?content . } ";
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?content, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "} " +
                " ORDER BY ASC(?content) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
        //System.out.println("GenericFind: KeywordAndLangauge - " + className + " query [" + queryString + "]");
		return findByQuery(clazz, queryString);
	}

	public static <T> List<T> findInstancesByKeywordAndLanguageWithPages(Class clazz, String className, String keyword, String language, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?type rdfs:subClassOf* " + className + " . " +
				" ?uri a ?type .";
		if (!language.isEmpty()) {
			queryString += " ?uri vstoi:hasLanguage ?language . ";
		}
		queryString += " OPTIONAL { ?uri rdfs:label ?label . } ";
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "} " +
                " ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public static <T> List<T> findSubClassesByKeywordAndLanguageWithPages(Class clazz, String subClassName, String keyword, String language, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?uri rdfs:subClassOf* " + subClassName + " . ";
		if (!language.isEmpty()) {
			queryString += " ?uri vstoi:hasLanguage ?language . ";
		}
		queryString += " OPTIONAL { ?uri rdfs:label ?label . } ";
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "} " +
                " ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public static int findTotalByKeywordAndLanguage(Class clazz, String keyword, String language) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
				" ?instModel rdfs:subClassOf* " + classNameWithNamespace(clazz) + " . " +
				" ?uri a ?instModel .";
		if (!language.isEmpty()) {
			queryString += " ?uri vstoi:hasLanguage ?language . ";
		}
		if (!keyword.isEmpty()) {
			queryString += " ?uri rdfs:label ?label . ";
		}
		if (!keyword.isEmpty() && !language.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\") && (?language = \"" + language + "\")) ";
		} else if (!keyword.isEmpty()) {
			queryString += "   FILTER (regex(?label, \"" + keyword + "\", \"i\")) ";
		} else if (!language.isEmpty()) {
			queryString += "   FILTER ((?language = \"" + language + "\")) ";
		}
		queryString += "}";
        return findTotalByQuery(queryString);
	}

    /**
     *     FIND ELEMENTS BY MANAGER (AND THEIR TOTALS)
     */

	public List<T> findByManagerEmailWithPages(Class clazz, String managerEmail, int pageSize, int offset) {
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            if (clazz == Detector.class) {
              return findDetectorInstancesByManagerEmailWithPages(clazz, className, managerEmail, pageSize, offset);
            } else if (isSIR(clazz)) {
              return findSIRInstancesByManagerEmailWithPages(clazz, className, managerEmail, pageSize, offset);
            } else {
              return findInstancesByManagerEmailWithPages(clazz, className, managerEmail, pageSize, offset);
            }
        }
        String subClassName = superclassNameWithNamespace(clazz);
        if (subClassName != null) {
            return findSubclassesByManagerEmailWithPages(clazz, subClassName, managerEmail, pageSize, offset);
        }
        return null;
    }

	public List<T> findDetectorInstancesByManagerEmailWithPages(Class clazz, String className, String managerEmail, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?model rdfs:subClassOf* " + className + " . " +
				" ?uri a ?model ." +
                " ?uri vstoi:hasDetectorStem ?stem . " +
                " OPTIONAL { ?stem vstoi:hasContent ?content . } " +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}" +
				" ORDER BY ASC(?content) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public List<T> findSIRInstancesByManagerEmailWithPages(Class clazz, String className, String managerEmail, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?model rdfs:subClassOf* " + className + " . " +
				" ?uri a ?model ." +
                " OPTIONAL { ?uri vstoi:hasContent ?content . } " +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}" +
				" ORDER BY ASC(?content) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public List<T> findInstancesByManagerEmailWithPages(Class clazz, String className, String managerEmail, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?model rdfs:subClassOf* " + className + " . " +
				" ?uri a ?model ." +
				" OPTIONAL { ?uri rdfs:label ?label . } " +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}" +
				" ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public List<T> findSubclassesByManagerEmailWithPages(Class clazz, String subClassName, String managerEmail, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?uri rdfs:subClassOf* " + subClassName + " . " +
				" OPTIONAL { ?uri rdfs:label ?label . } " +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}" +
				" ORDER BY ASC(?label) " +
				" LIMIT " + pageSize +
				" OFFSET " + offset;
		return findByQuery(clazz, queryString);
	}

	public static int findTotalByManagerEmail(Class clazz, String managerEmail) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT (count(?uri) as ?tot) WHERE { " +
				" ?model rdfs:subClassOf* " + classNameWithNamespace(clazz) + " . " +
				" ?uri a ?model ." +
				" ?uri vstoi:hasSIRManagerEmail ?managerEmail . " +
				"   FILTER (?managerEmail = \"" + managerEmail + "\") " +
				"}";
        //System.out.println("GenericFind.findTotalByManagerWithPages: " + clazz.getName() + "  query [" + queryString + "]");
        return findTotalByQuery(queryString);
	}


    /** 
     *    QUERY EXECUTION
     */

    private static <T> List<T> findByQuery(Class clazz,String queryString) {
        //System.out.println("FindByQuery: query = [" + queryString + "]");
        List<T> list = new ArrayList<T>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null) {
                if (soln.getResource("uri") != null && soln.getResource("uri").getURI() != null) {
                    T element = findElement(clazz, soln.getResource("uri").getURI());
                    if (element != null) {                        
                      list.add(element);
                    }
                } else {
                    System.out.println("Failed to retrieve URI for objects selected in a query.");
                }
            }
        }
        return list;
    }

    private static <T> T findElement(Class clazz, String uri) {

        // List of subclasses 
        if (clazz == InstrumentType.class) {
            return (T)InstrumentType.find(uri);
        } else if (clazz == DetectorStemType.class) {
            return (T)DetectorStemType.find(uri);
        } else if (clazz == Entity.class) {
            return (T)Entity.find(uri);
        } else if (clazz == Attribute.class) {
            return (T)Attribute.find(uri);
        } else if (clazz == Unit.class) {
            return (T)Unit.find(uri);

        // List of elements
        } else if (clazz == Instrument.class) {
            return (T)Instrument.find(uri);
        } else if (clazz == ContainerSlot.class) {
            return (T)ContainerSlot.find(uri);
        } else if (clazz == DetectorStem.class) {
            return (T)DetectorStem.find(uri);
        } else if (clazz == Detector.class) {
            return (T)Detector.findDetector(uri);
        } else if (clazz == Codebook.class) {
            return (T)Codebook.find(uri);
        } else if (clazz == CodebookSlot.class) {
            return (T)CodebookSlot.find(uri);
        } else if (clazz == ResponseOption.class) {
            return (T)ResponseOption.find(uri);
        } else if (clazz == AnnotationStem.class) {
            return (T)AnnotationStem.find(uri);
        } else if (clazz == Annotation.class) {
            return (T)Annotation.find(uri);
        } else if (clazz == SemanticVariable.class) {
            return (T)SemanticVariable.find(uri);
        } else if (clazz == Agent.class) {
            return (T)Agent.find(uri);
        }
        return null;
    
    }


    private static int findTotalByQuery(String queryString) {
        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }


}

