package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.sirapi.annotations.PropertyField;
import org.sirapi.annotations.PropertyValueType;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.URIUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.ConfigProp;
import org.sirapi.utils.FirstLabel;
import org.sirapi.utils.NameSpaces;
import org.sirapi.vocabularies.HASCO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

@JsonFilter("SemanticVariableFilter")
public class SemanticVariable extends HADatAcThing {

	private static final String className = "hasco:SemanticVariable";

	private static final Logger log = LoggerFactory.getLogger(SemanticVariable.class);
	public static final String EMPTY_CONTENT = "n/a";

	// Mandatory properties for SemanticVariable

	@PropertyField(uri="hasco:hasEntity", valueType=PropertyValueType.URI)
	private String entUri;
	private String entLabel;

	@PropertyField(uri="hasco:hasAttribute", valueType=PropertyValueType.URI)
	private String attrUri;
	private String attrLabel;

	// Optional properties for SemanticVariables

	@PropertyField(uri="hasco:hasRole")
	private String role;

	@PropertyField(uri="hasco:hasInRelationTo", valueType=PropertyValueType.URI)
    private String inRelationToUri;
	private String inRelationToLabel;

    private String relation;

	@PropertyField(uri="hasco:hasUnit", valueType=PropertyValueType.URI)
	private String unitUri;
	private String unitLabel;

	@PropertyField(uri="hasco:hasEvent", valueType=PropertyValueType.URI)
    private String timeAttrUri;
	private String timeAttrLabel;

	@PropertyField(uri="hasco:isCategorical")
	private boolean isCategorical;

	private Map<String, String> relations = new HashMap<String, String>();

	private static Map<String, SemanticVariable> semVarCache;

	private static Map<String, SemanticVariable> getCache() {
		if (semVarCache == null) {
			semVarCache = new HashMap<String, SemanticVariable>();
		}
		return semVarCache;
	}
	public static void resetCache() {
		semVarCache = null;
	}

	public SemanticVariable() {
    	this.typeUri = HASCO.SEMANTIC_VARIABLE;
    	this.hascoTypeUri = HASCO.SEMANTIC_VARIABLE;
	}

    public SemanticVariable(String label, AlignmentEntityRole entRole, AttributeInRelationTo attrInRel) {
    	this(label, entRole, attrInRel, null, null);
    }

    public SemanticVariable(String label, AlignmentEntityRole entRole, AttributeInRelationTo attrInRel, Unit unit) {
    	this(label, entRole, attrInRel, unit, null);
    }

    public SemanticVariable(String label, AlignmentEntityRole entRole, AttributeInRelationTo attrInRel, Unit unit, Attribute timeAttr) {
		this(HASCO.SEMANTIC_VARIABLE, HASCO.SEMANTIC_VARIABLE, label, entRole.getEntity(), entRole.getRole(), attrInRel.getAttribute(),
				attrInRel.getInRelationTo(), unit, timeAttr, false);
    }

	public SemanticVariable(String typeUri, String hascoTypeUri , String label, Entity ent, String role, Attribute attr, Entity inRelationTo, Unit unit, Attribute timeAttr, boolean isCategorical) {
		this.typeUri = typeUri;
		this.hascoTypeUri = hascoTypeUri;
		this.label = label;
		if (ent != null) {
			this.entUri = ent.getUri();
		}
		this.role = role;
		if (attr != null) {
			this.attrUri = attr.getUri();
		}
		if (inRelationTo != null) {
			this.inRelationToUri = inRelationTo.getUri();
		}
		if (unit != null) {
			this.unitUri = unit.getUri();
		}
		if (timeAttr != null) {
			this.timeAttrUri = timeAttr.getUri();
		}
		this.isCategorical = false;
	}

	public String getKey() {
    	return getRole() + getEntityStr() + getAttributeStr() + getInRelationToStr() + getUnitStr() + getTimeStr();
    }

	@Override
	public String getLabel() {
		if (this.label == null || this.label.isEmpty()) {
			return this.toString();
		}
		return this.label;
	}

	public Entity getEntity() {
		Entity ent = Entity.find(this.entUri);
    	return ent;
    }

    public String getEntityLabel() {
		if (this.entLabel != null) {
			return this.entLabel;
		}
		Entity ent = getEntity();
		if (ent == null) {
			this.entLabel = "";
			return this.entLabel;
		}
		return ent.getLabel();
	}

	public String getEntityUri() {
		return this.entUri;
	}

	public String getEntityStr() {
        if (entUri == null || entUri.isEmpty()) {
        	return "";
        }
    	return entUri;
    }

    public void setEntityUri(String entUri) {
    	this.entUri = entUri;
	}

    public String getRole() {
    	if (role == null) {
    		return "";
    	}
    	return role;
    }

	public void setRole(String role) {
		this.role = role;
	}

    public Attribute getAttribute() {
		if (this.attrUri.isEmpty()) {
			return null;
		}
		return Attribute.find(this.attrUri);
    }

	public void setAttributeUri(String attrUri) {
    	this.attrUri = attrUri;
	}

	public String getAttributeUri() {
		return this.attrUri;
	}

	public String getAttributeStr() {
        if (attrUri.isEmpty()) {
            return "";
        }
    	return attrUri;
    }

	public Entity getInRelationTo() {
		if (this.inRelationToUri == null || this.inRelationToUri.isEmpty()) {
			return null;
		}
		return Entity.find(this.inRelationToUri);
    }

	public String getInRelationToUri() {
		return inRelationToUri;
	}

	public void setInRelationToUri(String inRelationToUri) {
    	this.inRelationToUri = inRelationToUri;
	}

	public String getInRelationToStr() {
        if (inRelationToUri == null || inRelationToUri.isEmpty()) {
            return "";
        }
    	return inRelationToUri;
    }

	public String getInRelationToLabel() {
		if (this.inRelationToLabel != null) {
			return this.inRelationToLabel;
		}
		Entity inRelationTo = getInRelationTo();
		if (inRelationTo == null) {
			this.inRelationToLabel = "";
			return this.inRelationToLabel;
		}
		return inRelationTo.getLabel();
	}

	public List<String> getRelationsList() {
		return new ArrayList(relations.values());
	}

	public void addRelation(String key, String relation) {
		relations.put(key, relation);
	}

	public Unit getUnit() {
		if (unitUri == null || unitUri.isEmpty()) {
			return null;
		}
    	return Unit.find(unitUri);
    }

	public void setUnitUri(String unitUri) {
    	this.unitUri = unitUri;
	}

	public String getUnitUri() {
		return unitUri;
	}

	public String getUnitStr() {
        if (unitUri == null || unitUri.isEmpty()) {
            return "";
        }
    	return unitUri;
    }

	public String getUnitLabel() {
		if (this.unitLabel != null) {
			return this.unitLabel;
		}
		Unit unit = getUnit();
		if (unit == null) {
			this.unitLabel = "";
			return this.unitLabel;
		}
		return unit.getLabel();
	}

	public Attribute getTime() {
  		if (timeAttrUri == null || timeAttrUri.isEmpty()) {
  			return null;
		}
    	return Attribute.find(timeAttrUri);
    }

	public void setTimeUri(String timeAttrUri) {
		this.timeAttrUri = timeAttrUri;
	}

	public String getTimeUri() {
		return timeAttrUri;
	}

	public String getTimeStr() {
        if (timeAttrUri == null || timeAttrUri.isEmpty()) {
            return "";
        }
    	return timeAttrUri;
    }

	public String getTimeLabel() {
		if (this.timeAttrLabel != null) {
			return this.timeAttrLabel;
		}
		Attribute timeAttr = getTime();
		if (timeAttr == null) {
			this.timeAttrLabel = "";
			return this.timeAttrLabel;
		}
		return timeAttr.getLabel();
	}

	public boolean getIsCategorical() {
		return isCategorical;
	}

	public boolean isCategorical() {
		return isCategorical;
	}

	public void setIsCategorical(boolean isCategorical) {
    	this.isCategorical = isCategorical;
	}

	public static String upperCase(String orig) {
    	String[] words = orig.split(" ");
    	StringBuffer sb = new StringBuffer();

    	for (int i = 0; i < words.length; i++) {
    		sb.append(Character.toUpperCase(words[i].charAt(0)))
    		.append(words[i].substring(1)).append(" ");
    	}          
    	return sb.toString().trim();
    }      

    public static String prep(String orig) {
    	String aux = upperCase(orig);
    	return aux.replaceAll(" ","-").replaceAll("[()]","");
    }

	public static SemanticVariable find(String sv_uri) {
		try {
			if (SemanticVariable.getCache().get(sv_uri) != null) {
				return SemanticVariable.getCache().get(sv_uri);
			}
			SemanticVariable semVar = null;
			//System.out.println("Looking for semantic variable with URI <" + sv_uri + ">");

			String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
					"SELECT ?hasEntity ?hasAttribute ?hascoTypeUri " +
					" ?hasUnit ?hasDASO ?hasDASE ?relation ?inRelationTo ?label WHERE { \n" +
					"    <" + sv_uri + "> a hasco:SemanticVariable . \n" +
					"    <" + sv_uri + "> hasco:hascoType hasco:SemanticVariable . \n" +
					"    OPTIONAL { <" + sv_uri + "> hasco:hasEntity ?hasEntity } . \n" +
					"    OPTIONAL { <" + sv_uri + "> hasco:hasAttribute ?hasAttribute } . \n" +
					"    OPTIONAL { <" + sv_uri + "> hasco:hasUnit ?hasUnit } . \n" +
					"    OPTIONAL { <" + sv_uri + "> hasco:hasEvent ?hasDASE } . \n" +
					"    OPTIONAL { <" + sv_uri + "> hasco:isAttributeOf ?hasDASO } . \n" +
					"    OPTIONAL { <" + sv_uri + "> hasco:Relation ?relation . \n" +
					"               <" + sv_uri + "> ?relation ?inRelationTo . } . \n" +
					"    OPTIONAL { <" + sv_uri + "> rdfs:label ?label } . \n" +
					"}";

			//System.out.println("SemanticVariable find() queryString: \n" + queryString);

			ResultSetRewindable resultsrw = SPARQLUtils.select(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

			if (!resultsrw.hasNext()) {
				System.out.println("[WARNING] SemanticVariable. Could not find Semantic Variable with URI: <" + sv_uri + ">");
				return semVar;
			}

			String localNameStr = "";
			String labelStr = "";
			String entityStr = "";
			String attributeStr = "";
			String unitStr = "";
			String dasoUriStr = "";
			String daseUriStr = "";
			String inRelationToUri = "";
			String relationUri = "";

			Map<String, String> relationMap = new HashMap<>();
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();

				/*
				 *  The label should be the exact value in the SDD, e.g., cannot be altered be something like
				 *  FirstLabel.getPrettyLabel(sv_uri) since that would prevent the matching of the label with
				 *  the column header of the data acquisition file/message
				 */
				labelStr = soln.get("label").toString();

				if (soln.get("hasEntity") != null) {
					entityStr = soln.get("hasEntity").toString();
				}
				if (soln.get("hasAttribute") != null) {
					attributeStr = soln.get("hasAttribute").toString();
				}
				if (soln.get("hasUnit") != null) {
					unitStr = soln.get("hasUnit").toString();
				}
				if (soln.get("hasDASO") != null) {
					dasoUriStr = soln.get("hasDASO").toString();
				}
				if (soln.get("hasDASE") != null) {
					daseUriStr = soln.get("hasDASE").toString();
				}
				if (soln.get("inRelationTo") != null) {
					inRelationToUri = soln.get("inRelationTo").toString();
				}
				if (soln.get("relation") != null) {
					relationUri = soln.get("relation").toString();
				}

				if (relationUri != null && relationUri.length() > 0 && inRelationToUri != null && inRelationToUri.length() > 0) {
					relationMap.put(relationUri, inRelationToUri);
					relationUri = "";
					inRelationToUri = "";
				}

			}

			//System.out.println("Semantic Variable [" + sv_uri + "]. Entity Str is [" + entityStr + "]");

			Entity entity = Entity.find(entityStr);
			Entity inRelationTo = Entity.find(inRelationToUri);
			Attribute attr = Attribute.find(attributeStr);
			Unit unit = Unit.find(unitStr);
			Attribute timeAttr = Attribute.find(daseUriStr);

			semVar = new SemanticVariable(HASCO.SEMANTIC_VARIABLE, HASCO.SEMANTIC_VARIABLE, labelStr, entity, "", attr, inRelationTo, unit, timeAttr, false);

			semVar.setUri(sv_uri);

			for (Map.Entry<String, String> entry : relationMap.entrySet()) {
				semVar.addRelation(entry.getKey(), entry.getValue());
			}

			SemanticVariable.getCache().put(sv_uri, semVar);

			return semVar;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static List<SemanticVariable> findWithPages(int pageSize, int offset) {
		List<SemanticVariable> semVars = new ArrayList<SemanticVariable>();
		String queryString = "";
		queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
				"SELECT ?uri WHERE { " +
				"   ?subUri rdfs:subClassOf* hasco:SemanticVariable . " +
				"   ?uri a ?subUri . " +
				"   ?uri rdfs:label ?label . " +
				" }" +
				" ORDER BY ASC(?label)" +
				" LIMIT " + pageSize +
				" OFFSET " + offset;

		try {
			ResultSetRewindable resultsrw = SPARQLUtils.select(
					CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

			SemanticVariable semVar = null;
			while (resultsrw.hasNext()) {
				QuerySolution soln = resultsrw.next();
				if (soln != null && soln.getResource("uri").getURI()!= null) {
					String uri = soln.get("uri").toString();
					if (uri != null && !uri.isEmpty()) {
						semVar = SemanticVariable.find(uri);
					}
				}
				semVars.add(semVar);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return semVars;

	}

	public static int getNumberSemanticVariables() {
		String query = "";
		query += NameSpaces.getInstance().printSparqlNameSpaceList();
		query += "select distinct (COUNT(?x) AS ?tot) where {" +
				" ?x a <" + HASCO.SEMANTIC_VARIABLE + "> } ";

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

	public static String toString(String role, Entity ent, Attribute attr, Entity inRelationTo, Unit unit, Attribute timeAttr) {
		//System.out.println("[" + attr.getLabel() + "]");
		String str = "";
		if (role != null && !role.isEmpty()) {
			str += prep(role) + "-";
		}
		if (ent != null && ent.getLabel() != null && !ent.getLabel().isEmpty()) {
			str += prep(ent.getLabel());
		}
		if (attr != null && attr.getLabel() != null && !attr.getLabel().isEmpty()) {
			str += "-" + prep(attr.getLabel());
		}
		if (inRelationTo != null && !inRelationTo.getLabel().isEmpty()) {
			str += "-" + prep(inRelationTo.getLabel());
		}
		if (unit != null && unit.getLabel() != null && !unit.getLabel().isEmpty()) {
			str += "-" + prep(unit.getLabel());
		}
		if (timeAttr != null && timeAttr.getLabel() != null && !timeAttr.getLabel().isEmpty()) {
			str += "-" + prep(timeAttr.getLabel());
		}
		return str;
	}

	@Override
	public void save() {
		this.saveToTripleStore();
	}

	@Override
	public boolean saveToSolr() {
		return true;
	}

	@Override
	public int deleteFromSolr() {
		return 0;
	}

}
