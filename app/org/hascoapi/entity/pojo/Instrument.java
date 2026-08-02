package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.query.ResultSet;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.query.QuerySolution;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;

@JsonFilter("instrumentFilter")
public class Instrument extends Container {

	private static final Logger log = LoggerFactory.getLogger(Instrument.class);

	@PropertyField(uri="vstoi:hasFidelity")
	private String hasFidelity;

	@PropertyField(uri="vstoi:hasAnatomy")
	private String hasAnatomy;

	@PropertyField(uri="vstoi:hasAnatomy")
	private List<String> hasAnatomyUris = new ArrayList<>();

	public Instrument() {
		super();
    }
    
	public Instrument(String className) {
		super(className);
    }
    
	@Override
	public boolean equals(Object o) {
		if((o instanceof Instrument) && (((Instrument)o).getUri().equals(this.getUri()))) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getUri().hashCode();
	}

	public String getHasFidelity() {
		return hasFidelity;
	}

	public void setHasFidelity(String hasFidelity) {
		this.hasFidelity = hasFidelity;
	}

	public String getHasAnatomy() {
		if (hasAnatomy != null && !hasAnatomy.trim().isEmpty()) {
			return hasAnatomy;
		}
		if (hasAnatomyUris == null || hasAnatomyUris.isEmpty()) {
			return "";
		}
		return String.join("; ", hasAnatomyUris);
	}

	public void setHasAnatomy(String hasAnatomy) {
		if (hasAnatomy == null) {
			this.hasAnatomy = "";
			this.hasAnatomyUris = new ArrayList<>();
			return;
		}

		List<String> parsedUris = parseAnatomyUris(hasAnatomy);
		if (!parsedUris.isEmpty()) {
			// Prefer URI list serialization when we can confidently parse URIs.
			this.hasAnatomy = "";
			this.hasAnatomyUris = parsedUris;
		} else {
			// Preserve legacy literal when content is not URI-list shaped.
			this.hasAnatomy = hasAnatomy;
			this.hasAnatomyUris = new ArrayList<>();
		}
	}

	@JsonSetter("hasAnatomy")
	public void setHasAnatomyFromJson(JsonNode hasAnatomyNode) {
		if (hasAnatomyNode == null || hasAnatomyNode.isNull()) {
			setHasAnatomy("");
			return;
		}

		if (hasAnatomyNode.isArray()) {
			Set<String> uniqueUris = new LinkedHashSet<>();
			for (JsonNode item : hasAnatomyNode) {
				if (item == null || item.isNull()) {
					continue;
				}
				String value = item.asText(null);
				if (value == null) {
					continue;
				}
				String normalized = normalizeAnatomyToken(value);
				if (isValidAnatomyUri(normalized)) {
					uniqueUris.add(normalized);
				}
			}

			if (!uniqueUris.isEmpty()) {
				this.hasAnatomy = "";
				this.hasAnatomyUris = new ArrayList<>(uniqueUris);
				return;
			}
		}

		setHasAnatomy(hasAnatomyNode.asText(""));
	}

	public static Instrument find(String uri) {
		Instrument instrument = null;

		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			instrument = new Instrument(VSTOI.INSTRUMENT);
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				instrument.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					instrument.setLabel(object);
				} else if (predicate.equals(RDFS.SUBCLASS_OF)) {
					instrument.setSuperUri(object);
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					instrument.setHascoTypeUri(object);
				} else if (predicate.equals(VSTOI.HAS_STATUS)) {
					instrument.setHasStatus(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					instrument.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					instrument.setHasWebDocument(object);
				} else if (predicate.equals(VSTOI.HAS_FIRST)) {
					instrument.setHasFirst(object);
				} else if (predicate.equals(VSTOI.HAS_INFORMANT)) {
					instrument.setHasInformant(object);
				} else if (predicate.equals(RDFS.COMMENT)) {
					instrument.setComment(object);
				} else if (predicate.equals(VSTOI.HAS_SHORT_NAME)) {
					instrument.setHasShortName(object);
				} else if (predicate.equals(VSTOI.HAS_MAKER)) {
					instrument.setHasMakerUri(object);
				} else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
					instrument.setHasLanguage(object);
				} else if (predicate.equals(VSTOI.HAS_VERSION)) {
					instrument.setHasVersion(object);
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					instrument.setHasReviewNote(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					instrument.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
					instrument.setHasEditorEmail(object);
				} else if (predicate.equals(VSTOI.HAS_FIDELITY)) {
					instrument.setHasFidelity(object);
				} else if (predicate.equals(VSTOI.HAS_ANATOMY)) {
					instrument.consumeHasAnatomyValue(object);
				}
			}
		}

		instrument.setUri(uri);
		return instrument;
	}

	private void consumeHasAnatomyValue(String value) {
		if (value == null || value.trim().isEmpty()) {
			return;
		}

		List<String> parsedUris = parseAnatomyUris(value);
		if (!parsedUris.isEmpty()) {
			if (this.hasAnatomyUris == null) {
				this.hasAnatomyUris = new ArrayList<>();
			}
			for (String uri : parsedUris) {
				if (!this.hasAnatomyUris.contains(uri)) {
					this.hasAnatomyUris.add(uri);
				}
			}
			return;
		}

		if (this.hasAnatomy == null || this.hasAnatomy.trim().isEmpty()) {
			this.hasAnatomy = value;
		}
	}

	private static List<String> parseAnatomyUris(String rawValue) {
		List<String> uris = new ArrayList<>();
		if (rawValue == null || rawValue.trim().isEmpty()) {
			return uris;
		}

		String[] parts = rawValue.split(";");
		for (String part : parts) {
			String normalized = normalizeAnatomyToken(part);
			if (normalized.isEmpty()) {
				continue;
			}
			if (!isValidAnatomyUri(normalized)) {
				return new ArrayList<>();
			}
			if (!uris.contains(normalized)) {
				uris.add(normalized);
			}
		}

		if (uris.isEmpty()) {
			String normalized = normalizeAnatomyToken(rawValue);
			if (isValidAnatomyUri(normalized)) {
				uris.add(normalized);
			}
		}

		return uris;
	}

	private static String normalizeAnatomyToken(String token) {
		if (token == null) {
			return "";
		}
		String normalized = token.trim();
		if (normalized.startsWith("<") && normalized.endsWith(">") && normalized.length() > 2) {
			normalized = normalized.substring(1, normalized.length() - 1).trim();
		}
		return normalized;
	}

	private static boolean isValidAnatomyUri(String value) {
		if (value == null || value.isEmpty()) {
			return false;
		}
		if (value.contains(" ")) {
			return false;
		}
		return URIUtils.isValidURI(value);
	}


	public static List<Instrument> findAvailable() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   { ?instModel rdfs:subClassOf* vstoi:Instrument . " +
		    "     ?uri a ?instModel ." + 
		    "   } MINUS { " + 
		    "     ?dep_uri a vstoi:Deployment . " + 
		    "     ?dep_uri hasco:hasInstrument ?uri .  " +
		    "     FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " + 
		    "    } " + 
		    "} " + 
		    "ORDER BY DESC(?datetime) ";
		
		ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI().trim());
			instruments.add(instrument);
		}			
		
		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}
	
	public static List<Instrument> findDeployed() {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
		    " SELECT ?uri WHERE { " +
		    "   ?instModel rdfs:subClassOf* vstoi:Instrument . " +
		    "   ?uri a ?instModel ." + 
		    "   ?dep_uri a vstoi:Deployment . " + 
		    "   ?dep_uri hasco:hasInstrument ?uri .  " +
		    "   FILTER NOT EXISTS { ?dep_uri prov:endedAtTime ?enddatetime . } " + 
		    "} " + 
		    "ORDER BY DESC(?datetime) ";
		
		ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		while (resultsrw.hasNext()) {
		    QuerySolution soln = resultsrw.next();
		    Instrument instrument = find(soln.getResource("uri").getURI().trim());
		    instruments.add(instrument);
		}			

		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}

	public static List<Instrument> findByAnatomy(String uberonUri, String organizationUri) {
		List<Instrument> instruments = new ArrayList<Instrument>();
		String cleanUberonUri = normalizeUri(uberonUri);
		if (cleanUberonUri == null || cleanUberonUri.isEmpty()) {
			return instruments;
		}

		String cleanOrganizationUri = normalizeUri(organizationUri);

		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
				+ " SELECT DISTINCT ?uri WHERE { "
				+ "   { "
				+ "     ?instModel rdfs:subClassOf* vstoi:Instrument . "
				+ "     ?uri a ?instModel . "
				+ "   } UNION { "
				+ "     ?instModel rdfs:subClassOf* vstoi:Instrument . "
				+ "     ?uri hasco:hascoType ?instModel . "
				+ "   } UNION { "
				+ "     ?uri hasco:hascoType vstoi:Instrument . "
				+ "   } "
				+ "   ?uri vstoi:hasAnatomy ?anatomy . "
				+ "   BIND(STR(?anatomy) AS ?anatomyStr) . "
				+ "   BIND(REPLACE(STR(?anatomy), ' ', '') AS ?anatomyNoSpace) . "
				+ "   BIND(STR(<" + cleanUberonUri + ">) AS ?targetAnatomy) . "
				+ "   FILTER( "
				+ "      ?anatomyStr = ?targetAnatomy "
				+ "      || CONTAINS(CONCAT(';', ?anatomyStr, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "      || CONTAINS(CONCAT(';', ?anatomyNoSpace, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "   ) . ";

		if (cleanOrganizationUri != null && !cleanOrganizationUri.isEmpty()) {
			queryString += "   { "
					+ "     ?ii hasco:hascoType vstoi:InstrumentInstance . "
					+ "     { ?ii vstoi:hasInstrument ?uri . } "
					+ "     UNION { ?ii hasco:hasInstrument ?uri . } "
					+ "     UNION { ?ii rdf:type ?uri . FILTER(?uri != vstoi:InstrumentInstance && ?uri != owl:NamedIndividual) } "
					+ "     { ?ii vstoi:hasOwner <" + cleanOrganizationUri + "> . } "
					+ "     UNION { "
					+ "       ?dep hasco:hascoType vstoi:Deployment . "
					+ "       ?dep vstoi:hasInstrumentInstance ?ii . "
					+ "       ?dep vstoi:hasPlatformInstance ?platform . "
					+ "       ?platform hasco:partOf <" + cleanOrganizationUri + "> . "
					+ "     } "
					+ "   } ";
		}

		queryString += " } ORDER BY ?uri ";

		ResultSetRewindable resultsrw = SPARQLUtils.select(
				CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			if (soln.getResource("uri") == null) {
				continue;
			}
			Instrument instrument = find(soln.getResource("uri").getURI().trim());
			if (instrument != null) {
				instruments.add(instrument);
			}
		}

		java.util.Collections.sort((List<Instrument>) instruments);
		return instruments;
	}

	public static int findTotalByAnatomy(String uberonUri, String organizationUri) {
		String cleanUberonUri = normalizeUri(uberonUri);
		if (cleanUberonUri == null || cleanUberonUri.isEmpty()) {
			return 0;
		}

		String cleanOrganizationUri = normalizeUri(organizationUri);

		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
				+ " SELECT (COUNT(DISTINCT ?uri) AS ?total) WHERE { "
				+ "   { "
				+ "     ?instModel rdfs:subClassOf* vstoi:Instrument . "
				+ "     ?uri a ?instModel . "
				+ "   } UNION { "
				+ "     ?instModel rdfs:subClassOf* vstoi:Instrument . "
				+ "     ?uri hasco:hascoType ?instModel . "
				+ "   } UNION { "
				+ "     ?uri hasco:hascoType vstoi:Instrument . "
				+ "   } "
				+ "   ?uri vstoi:hasAnatomy ?anatomy . "
				+ "   BIND(STR(?anatomy) AS ?anatomyStr) . "
				+ "   BIND(REPLACE(STR(?anatomy), ' ', '') AS ?anatomyNoSpace) . "
				+ "   BIND(STR(<" + cleanUberonUri + ">) AS ?targetAnatomy) . "
				+ "   FILTER( "
				+ "      ?anatomyStr = ?targetAnatomy "
				+ "      || CONTAINS(CONCAT(';', ?anatomyStr, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "      || CONTAINS(CONCAT(';', ?anatomyNoSpace, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "   ) . ";

		if (cleanOrganizationUri != null && !cleanOrganizationUri.isEmpty()) {
			queryString += "   { "
					+ "     ?ii hasco:hascoType vstoi:InstrumentInstance . "
					+ "     { ?ii vstoi:hasInstrument ?uri . } "
					+ "     UNION { ?ii hasco:hasInstrument ?uri . } "
					+ "     UNION { ?ii rdf:type ?uri . FILTER(?uri != vstoi:InstrumentInstance && ?uri != owl:NamedIndividual) } "
					+ "     { ?ii vstoi:hasOwner <" + cleanOrganizationUri + "> . } "
					+ "     UNION { "
					+ "       ?dep hasco:hascoType vstoi:Deployment . "
					+ "       ?dep vstoi:hasInstrumentInstance ?ii . "
					+ "       ?dep vstoi:hasPlatformInstance ?platform . "
					+ "       ?platform hasco:partOf <" + cleanOrganizationUri + "> . "
					+ "     } "
					+ "   } ";
		}

		queryString += " } ";

		ResultSetRewindable resultsrw = SPARQLUtils.select(
				CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			if (soln != null && soln.getLiteral("total") != null) {
				return soln.getLiteral("total").getInt();
			}
		}

		return 0;
	}

	private static String normalizeUri(String uri) {
		if (uri == null) {
			return "";
		}
		String trimmed = uri.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		if (trimmed.startsWith("<") && trimmed.endsWith(">") && trimmed.length() > 2) {
			trimmed = trimmed.substring(1, trimmed.length() - 1);
		}
		if (trimmed.contains("<") || trimmed.contains(">") || trimmed.contains("\"")) {
			return "";
		}
		return trimmed;
	}
	
    @Override public void save() {
		//System.out.println("Instrument.java: Saving " + getUri() + " into triple store.");
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}
