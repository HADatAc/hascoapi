package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.vocabularies.VSTOI;

import static org.hascoapi.Constants.*;

@JsonFilter("instrumentInstanceFilter")
public class InstrumentInstance extends VSTOIInstance {

	public InstrumentInstance() {
		this.setTypeUri(VSTOI.INSTRUMENT_INSTANCE);
		this.setHascoTypeUri(VSTOI.INSTRUMENT_INSTANCE); 
	}

	public static InstrumentInstance find(String uri) {
		InstrumentInstance instance = new InstrumentInstance();
		return (InstrumentInstance)VSTOIInstance.find(instance,uri);
	} 

	public static List<InstrumentInstance> findByAnatomy(String uberonUri, String organizationUri) {
		List<InstrumentInstance> instances = new ArrayList<InstrumentInstance>();
		String cleanUberonUri = normalizeUri(uberonUri);
		if (cleanUberonUri == null || cleanUberonUri.isEmpty()) {
			return instances;
		}

		String cleanOrganizationUri = normalizeUri(organizationUri);

		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
				+ " SELECT DISTINCT ?uri WHERE { "
				+ "   ?uri hasco:hascoType vstoi:InstrumentInstance . "
				+ "   { ?uri vstoi:hasInstrument ?model . } "
				+ "   UNION { ?uri hasco:hasInstrument ?model . } "
				+ "   UNION { ?uri rdf:type ?model . FILTER(?model != vstoi:InstrumentInstance && ?model != owl:NamedIndividual) } "
				+ "   ?model rdfs:subClassOf* vstoi:Instrument . "
				+ "   ?model vstoi:hasAnatomy ?anatomy . "
				+ "   BIND(STR(?anatomy) AS ?anatomyStr) . "
				+ "   BIND(REPLACE(STR(?anatomy), ' ', '') AS ?anatomyNoSpace) . "
				+ "   BIND(STR(<" + cleanUberonUri + ">) AS ?targetAnatomy) . "
				+ "   FILTER( "
				+ "      ?anatomyStr = ?targetAnatomy "
				+ "      || CONTAINS(CONCAT(';', ?anatomyStr, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "      || CONTAINS(CONCAT(';', ?anatomyNoSpace, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "   ) . ";

		if (cleanOrganizationUri != null && !cleanOrganizationUri.isEmpty()) {
			queryString += "   { ?uri vstoi:hasOwner <" + cleanOrganizationUri + "> . } "
					+ "   UNION { "
					+ "     ?dep hasco:hascoType vstoi:Deployment . "
					+ "     ?dep vstoi:hasInstrumentInstance ?uri . "
					+ "     ?dep vstoi:hasPlatformInstance ?platform . "
					+ "     ?platform hasco:partOf <" + cleanOrganizationUri + "> . "
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
			InstrumentInstance instance = find(soln.getResource("uri").getURI().trim());
			if (instance != null) {
				instances.add(instance);
			}
		}

		return instances;
	}

	public static int findTotalByAnatomy(String uberonUri, String organizationUri) {
		String cleanUberonUri = normalizeUri(uberonUri);
		if (cleanUberonUri == null || cleanUberonUri.isEmpty()) {
			return 0;
		}

		String cleanOrganizationUri = normalizeUri(organizationUri);

		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
				+ " SELECT (COUNT(DISTINCT ?uri) AS ?total) WHERE { "
				+ "   ?uri hasco:hascoType vstoi:InstrumentInstance . "
				+ "   { ?uri vstoi:hasInstrument ?model . } "
				+ "   UNION { ?uri hasco:hasInstrument ?model . } "
				+ "   UNION { ?uri rdf:type ?model . FILTER(?model != vstoi:InstrumentInstance && ?model != owl:NamedIndividual) } "
				+ "   ?model rdfs:subClassOf* vstoi:Instrument . "
				+ "   ?model vstoi:hasAnatomy ?anatomy . "
				+ "   BIND(STR(?anatomy) AS ?anatomyStr) . "
				+ "   BIND(REPLACE(STR(?anatomy), ' ', '') AS ?anatomyNoSpace) . "
				+ "   BIND(STR(<" + cleanUberonUri + ">) AS ?targetAnatomy) . "
				+ "   FILTER( "
				+ "      ?anatomyStr = ?targetAnatomy "
				+ "      || CONTAINS(CONCAT(';', ?anatomyStr, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "      || CONTAINS(CONCAT(';', ?anatomyNoSpace, ';'), CONCAT(';', ?targetAnatomy, ';')) "
				+ "   ) . ";

		if (cleanOrganizationUri != null && !cleanOrganizationUri.isEmpty()) {
			queryString += "   { ?uri vstoi:hasOwner <" + cleanOrganizationUri + "> . } "
					+ "   UNION { "
					+ "     ?dep hasco:hascoType vstoi:Deployment . "
					+ "     ?dep vstoi:hasInstrumentInstance ?uri . "
					+ "     ?dep vstoi:hasPlatformInstance ?platform . "
					+ "     ?platform hasco:partOf <" + cleanOrganizationUri + "> . "
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

}
