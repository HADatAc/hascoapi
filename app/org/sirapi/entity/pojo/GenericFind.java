package org.sirapi.entity.pojo;

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
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.URIUtils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.SKOS;
import org.sirapi.vocabularies.SIO;
import org.sirapi.vocabularies.VSTOI;

public class GenericFind<T> {

    private static String classNameWithNamespace (Class clazz) {
        if (clazz == Instrument.class) {
            return URIUtils.replaceNameSpace(VSTOI.INSTRUMENT);
        } else if (clazz == DetectorSlot.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR_SLOT);
        } else if (clazz == DetectorStem.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR_STEM);
        } else if (clazz == Detector.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR);
        } else if (clazz == Codebook.class) {
            return URIUtils.replaceNameSpace(VSTOI.CODEBOOK);
        } else if (clazz == ResponseOptionSlot.class) {
            return URIUtils.replaceNameSpace(VSTOI.RESPONSE_OPTION_SLOT);
        } else if (clazz == ResponseOption.class) {
            return URIUtils.replaceNameSpace(VSTOI.RESPONSE_OPTION);
        } else if (clazz == SemanticVariable.class) {
            return URIUtils.replaceNameSpace(HASCO.SEMANTIC_VARIABLE);
        } else if (clazz == Agent.class) {
            return URIUtils.replaceNameSpace(HASCO.AGENT);
        }
        return null;
    }

    private static String superclassNameWithNamespace (Class clazz) {
        if (clazz == InstrumentType.class) {
            return URIUtils.replaceNameSpace(VSTOI.INSTRUMENT);
        } else if (clazz == DetectorStemType.class) {
            return URIUtils.replaceNameSpace(VSTOI.DETECTOR_STEM);
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
     *     QUERY SPECIFICATION
     */

    private static <T> List<T> findInstancesWithPages(Class clazz, String className, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + className + " . " +
                " ?uri a ?type ." +
                " ?uri rdfs:label ?label . " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(clazz, queryString);
    }
    
    private static <T> List<T> findSubclassesWithPages(Class clazz, String subClassName, int pageSize, int offset) {
        //System.out.println("subClassName: " + subClassName);
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* " + subClassName + " . " +
                " ?uri rdfs:label ?label . " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findByQuery(clazz, queryString);
    }
    
    public static <T> List<T> findWithPages(Class clazz, int pageSize, int offset) {
        if (clazz == null) {
            return null;
        }
        String className = classNameWithNamespace(clazz);
        if (className != null) {
            return findInstancesWithPages(clazz, className, pageSize, offset);
        }
        String subClassName = superclassNameWithNamespace(clazz);
        if (subClassName != null) {
            return findSubclassesWithPages(clazz, subClassName, pageSize, offset);
        }
        return null;
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

    public List<T> findByKeywordWithPages(Class clazz, String keyword, int pageSize, int offset) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* " + superclassNameWithNamespace(clazz) + " . " +
                " ?uri a ?type ." +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        return findByQuery(clazz, queryString);
    }

    public static int findTotalByKeyword(Class clazz, String keyword) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT (count(?uri) as ?tot) WHERE { " +
                " ?type rdfs:subClassOf* " + classNameWithNamespace(clazz) + " . " +
                " ?uri a ?type ." +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} ";

        return findTotalByQuery(queryString);
    }

	public List<T> findByKeywordAndLanguageWithPages(Class clazz, String keyword, String language, int pageSize, int offset) {
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList();
		queryString += " SELECT ?uri WHERE { " +
				" ?type rdfs:subClassOf* " + classNameWithNamespace(clazz) + " . " +
				" ?uri a ?type .";
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
		queryString += "} " +
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
        } else if (clazz == DetectorSlot.class) {
            return (T)DetectorSlot.find(uri);
        } else if (clazz == DetectorStem.class) {
            return (T)DetectorStem.find(uri);
        } else if (clazz == Detector.class) {
            return (T)Detector.find(uri);
        } else if (clazz == Codebook.class) {
            return (T)Codebook.find(uri);
        } else if (clazz == ResponseOptionSlot.class) {
            return (T)ResponseOptionSlot.find(uri);
        } else if (clazz == ResponseOption.class) {
            return (T)ResponseOption.find(uri);
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
				int i = Integer.parseInt(soln.getLiteral("tot").getString());
                //System.out.println("FindByQuery: query = [" + queryString + "]");
                //System.out.println("FindByQuery: total = [" + i + "]");
                return i;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
    }

}

