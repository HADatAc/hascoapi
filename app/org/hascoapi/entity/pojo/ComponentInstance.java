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
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;

import static org.hascoapi.Constants.*;

@JsonFilter("componentInstanceFilter")
public class ComponentInstance extends VSTOIInstance {

	private static final String DEFAULT_COMPONENT_INSTANCE_TYPE_FILTER =
			" { ?uri hasco:hascoType hasco:ComponentInstance . } "
					+ " UNION { ?uri hasco:hascoType vstoi:ComponentInstance . } ";

	public ComponentInstance() {
		this.setTypeUri(VSTOI.COMPONENT_INSTANCE);
		this.setHascoTypeUri(HASCO.COMPONENT_INSTANCE); 
	}

	public static ComponentInstance find(String uri) {
		ComponentInstance instance = new ComponentInstance();
		return (ComponentInstance)VSTOIInstance.find(instance,uri);
	} 

	public static List<ComponentInstance> findWithPageByOwner(String organizationUri, int pageSize, int offset) {
		return findWithPageByOwnerAndHascoType(organizationUri, pageSize, offset, null);
	}

	public static List<ComponentInstance> findWithPageByOwnerAndHascoType(String organizationUri, int pageSize, int offset, String hascoTypeUri) {
		String cleanOrganizationUri = normalizeUri(organizationUri);

		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
				+ " SELECT DISTINCT ?uri WHERE { "
				+ buildTypeFilter(hascoTypeUri);

		if (cleanOrganizationUri != null && !cleanOrganizationUri.isEmpty()) {
			queryString += buildOrganizationComponentTraversal(cleanOrganizationUri);
		}

		queryString += " } ORDER BY ?uri "
				+ " LIMIT " + pageSize
				+ " OFFSET " + offset;

		List<ComponentInstance> instances = new ArrayList<ComponentInstance>();
		ResultSetRewindable resultsrw = SPARQLUtils.select(
				CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			if (soln.getResource("uri") == null) {
				continue;
			}
			ComponentInstance instance = find(soln.getResource("uri").getURI().trim());
			if (instance != null) {
				instances.add(instance);
			}
		}

		return instances;
	}

	public static int findTotalByOwner(String organizationUri) {
		return findTotalByOwnerAndHascoType(organizationUri, null);
	}

	public static int findTotalByOwnerAndHascoType(String organizationUri, String hascoTypeUri) {
		String cleanOrganizationUri = normalizeUri(organizationUri);

		String queryString = NameSpaces.getInstance().printSparqlNameSpaceList()
				+ " SELECT (COUNT(DISTINCT ?uri) AS ?total) WHERE { "
				+ buildTypeFilter(hascoTypeUri);

		if (cleanOrganizationUri != null && !cleanOrganizationUri.isEmpty()) {
			queryString += buildOrganizationComponentTraversal(cleanOrganizationUri);
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

	private static String buildTypeFilter(String hascoTypeUri) {
		String clean = normalizeUri(hascoTypeUri);
		if (clean != null && !clean.isEmpty() && !isCanonicalComponentInstanceType(clean)) {
			return " ?uri hasco:hascoType <" + clean + "> . ";
		}
		return DEFAULT_COMPONENT_INSTANCE_TYPE_FILTER;
	}

	private static boolean isCanonicalComponentInstanceType(String typeUri) {
		if (typeUri == null) {
			return false;
		}

		String trimmed = typeUri.trim();
		if (trimmed.isEmpty()) {
			return false;
		}

		return "vstoi:ComponentInstance".equals(trimmed)
				|| "hasco:ComponentInstance".equals(trimmed)
				|| trimmed.endsWith("#ComponentInstance")
				|| trimmed.endsWith("/ComponentInstance");
	}

	/**
	 * Organization-scoped traversal for component instances:
	 * instrument instances -> deployments -> component deployments -> component instances.
	 */
	private static String buildOrganizationComponentTraversal(String organizationUri) {
		return "   ?ii vstoi:hasOwner <" + organizationUri + "> . "
				+ "   ?dpl vstoi:hasInstrumentInstance ?ii . "
				+ "   ?cd (hasco:hascoDeployment|hasco:hasDeployment) ?dpl . "
				+ "   ?cd (hasco:hasComponentInstance|vstoi:hasComponentInstance) ?uri . ";
	}

}
