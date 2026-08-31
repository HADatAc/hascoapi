package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.Utils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;

@JsonFilter("organizationFilter")
public class Organization extends Agent {

    @PropertyField(uri="schema:parentOrganization")
    protected String parentOrganizationUri;

    @PropertyField(uri="hasco:hasCurator")
    protected String hasCuratorUri;

    public String getParentOrganizationUri() {
        return parentOrganizationUri;
    }
    public void setParentOrganizationUri(String parentOrganizationUri) {
        this.parentOrganizationUri = parentOrganizationUri;
    }

    public String getHasCuratorUri() {
        return hasCuratorUri;
    }

    public void setHasCuratorUri(String hasCuratorUri) {
        this.hasCuratorUri = hasCuratorUri;
    }

    public Person getHasCurator() {
        if (hasCuratorUri == null || hasCuratorUri.trim().isEmpty()) {
            return null;
        }
        return Person.find(hasCuratorUri.trim());
    }

    public static List<Organization> find() {
        String query =
            " SELECT ?uri WHERE { " +
            " ?uri a schema:Organization ." +
            "} ";
        return findManyByQuery(query);
    }

    public static int findTotalSubOrganizations(String uri) {
        if (uri == null || uri.isEmpty()) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE { " +   
                "    ?uri schema:parentOrganization <" + uri + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        

    public static List<Organization> findSubOrganizations(String uri, int pageSize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return new ArrayList<Organization>();
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?uri schema:parentOrganization <" + uri + ">.  " +
				"          ?uri rdfs:label ?label . " +
                " } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findManyByQuery(query);
    }        

    public static int findTotalAffiliations(String uri) {
        if (uri == null || uri.isEmpty()) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE { " +   
                "    ?uri foaf:member <" + uri + "> .  " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        

    public static List<Person> findAffiliations(String uri, int pageSize, int offset) {
        if (uri == null || uri.isEmpty()) {
            return new ArrayList<Person>();
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?uri foaf:member <" + uri + ">.  " +
				"          ?uri rdfs:label ?label . " +
                " } " +
                " ORDER BY ASC(?label) " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return Person.findManyByQuery(query);
    }        

    public static Person findCurator(String organizationUri) {
        if (organizationUri == null || organizationUri.trim().isEmpty()) {
            return null;
        }

        String normalizedOrgUri = organizationUri.trim();
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT DISTINCT ?curator WHERE { "
                + "  <" + normalizedOrgUri + "> hasco:hasCurator ?curator . "
                + "} LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        if (results != null && results.hasNext()) {
            QuerySolution soln = results.next();
            if (soln != null && soln.get("curator") != null && soln.get("curator").isResource()) {
                return Person.find(soln.getResource("curator").getURI());
            }
        }
        return null;
    }

    public static Person ensureCurator(String organizationUri) {
        if (organizationUri == null || organizationUri.trim().isEmpty()) {
            return null;
        }

        String normalizedOrgUri = organizationUri.trim();
        Organization organization = Organization.find(normalizedOrgUri);
        if (organization == null) {
            return null;
        }

        Person existing = findCurator(normalizedOrgUri);
        if (existing != null) {
            return existing;
        }

        String shortName = preferredShortName(organization);
        String curatorLabel = "Curator at " + shortName;
        Person fallbackCurator = findPersonByExactLabelAndAffiliation(curatorLabel, normalizedOrgUri);
        if (fallbackCurator != null) {
            setCuratorLink(normalizedOrgUri, fallbackCurator.getUri());
            return fallbackCurator;
        }

        String emailDomain = preferredEmailDomain(organization);
        String curatorEmail = "curator@" + emailDomain;

        Person curatorByEmail = Person.findByEmail(curatorEmail);
        if (curatorByEmail != null) {
            setCuratorLink(normalizedOrgUri, curatorByEmail.getUri());
            return curatorByEmail;
        }

        Person curator = new Person();
        curator.setUri(generateCuratorPersonUri(normalizedOrgUri));
        curator.setTypeUri(SCHEMA.PERSON);
        curator.setHascoTypeUri(SCHEMA.PERSON);
        curator.setLabel(curatorLabel);
        curator.setName(curatorLabel);
        curator.setGivenName("Curator");
        curator.setFamilyName(shortName);
        curator.setHasAffiliationUri(normalizedOrgUri);
        curator.setMbox(curatorEmail);
        curator.setUserEmail(curatorEmail);
        curator.setUserName("curator_" + safeToken(shortName));
        curator.setComment("Auto-generated curator account for organization-level artifact ownership.");
        curator.setHasStatus(VSTOI.DRAFT);
        curator.setHasSIRManagerEmail(curatorEmail);
        if (organization.getNamedGraph() != null && !organization.getNamedGraph().trim().isEmpty()) {
            curator.setNamedGraph(organization.getNamedGraph().trim());
        }

        curator.save();

        // Re-read by email to guarantee a persisted subject URI in the returned payload.
        Person persistedCurator = Person.findByEmail(curatorEmail);
        if (persistedCurator != null && isHttpUri(persistedCurator.getUri())) {
            curator = persistedCurator;
        }

        setCuratorLink(normalizedOrgUri, curator.getUri());

        return curator;
    }

    private static String generateCuratorPersonUri(String organizationUri) {
        String generated = Utils.uriGen("person");
        if (isHttpUri(generated)) {
            return generated;
        }

        String base = deriveNamespaceBaseFromUri(organizationUri);
        String prefix = Utils.shortPrefix("person");
        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = "PER";
        }

        return URIUtils.canonicalizePmsrUri(Utils.uriGen(base, prefix));
    }

    private static String deriveNamespaceBaseFromUri(String uri) {
        if (uri != null) {
            String normalized = uri.trim();
            int idx = normalized.lastIndexOf('/');
            if (idx > 0) {
                String base = normalized.substring(0, idx + 1);
                if (isHttpUri(base)) {
                    return URIUtils.normalizeNamespaceBase(base);
                }
            }
        }
        return URIUtils.normalizeNamespaceBase("https://pmsr.net/ont/");
    }

    private static List<Organization> findManyByQuery(String requestedQuery) {
        List<Organization> organizations = new ArrayList<Organization>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            String uri = soln.getResource("uri").getURI();
            Organization organization = Organization.find(uri);
            organizations.add(organization);
        }

        java.util.Collections.sort((List<Organization>) organizations);
        return organizations;
    }

    public static Organization findByOriginalID(String originalID) {
        if (originalID == null || originalID.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* schema:Organization . " +
                "          ?uri a ?subUri . " +
                "          ?uri hasco:hasOriginalId ?id .  " +
                "        FILTER (?id=\"" + originalID + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static Organization findByEmail(String email) {
        if (email == null || email.isEmpty()) {
            return null;
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?subUri rdfs:subClassOf* schema:Organization . " +
                "          ?uri a ?subUri . " +
                "          ?uri foaf:mbox ?email .  " +
                "        FILTER (?email=\"" + email + "\"^^xsd:string)  . " +
                " }";
        return findOneByQuery(query);
    }        

    public static Organization findByName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String query = 
            " SELECT ?uri " +
            "   WHERE {  ?subUri rdfs:subClassOf* schema:Organization . " +
            "          ?uri a ?subUri . " +
            "          ?uri foaf:name ?name .  " +
            "        FILTER (?name=\"" + name + "\"^^xsd:string)  . " +
            " }";
        return Organization.findOneByQuery(query);
    }        

    private static Organization findOneByQuery(String requestedQuery) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + requestedQuery;
        //System.out.println("Organization.findOneByQuery() with query=[" + queryString + "]");
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        String uri = null;
        Organization organization = null;
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.get("uri") != null) {
                uri = soln.get("uri").toString();
                organization = Organization.find(uri);
            }
        }

        return organization;
    }

    public static Organization find(String uri) {
        Organization organization = null;

		// Conobjectuct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		org.apache.jena.query.ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			organization = new Organization();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				organization.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);
            
                if (predicate.equals(RDFS.LABEL)) {
                    organization.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    organization.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    organization.setComment(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    organization.setHascoTypeUri(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    organization.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    organization.setHasWebDocument(object);
                } else if (predicate.equals(VSTOI.HAS_STATUS)) {
                    organization.setHasStatus(object);
                } else if (predicate.equals(HASCO.ORIGINAL_ID)) {
                    organization.setOriginalID(object);
                } else if (predicate.equals(SCHEMA.ALTERNATE_NAME)) {
                    organization.setHasShortName(object);
                } else if (predicate.equals(FOAF.NAME)) {
                    organization.setName(object);
                } else if (predicate.equals(FOAF.MBOX)) {
                    organization.setMbox(object);
                } else if (predicate.equals(SCHEMA.TELEPHONE)) {
                    organization.setTelephone(object);
                } else if (predicate.equals(SCHEMA.PARENT_ORGANIZATION)) {
                    organization.setParentOrganizationUri(object);
                } else if (predicate.equals(HASCO.HAS_CURATOR)) {
                    organization.setHasCuratorUri(object);
                } else if (predicate.equals(SCHEMA.ADDRESS)) {
                    organization.setHasAddressUri(object);
                } else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    organization.setHasSIRManagerEmail(object);
                }
            }
        }

        organization.setUri(uri);

        return organization;
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

    private static Person findPersonByExactLabelAndAffiliation(String label, String organizationUri) {
        if (label == null || label.trim().isEmpty() || organizationUri == null || organizationUri.trim().isEmpty()) {
            return null;
        }

        String escapedLabel = label.trim().replace("\\", "\\\\").replace("\"", "\\\"");
        String query = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "SELECT DISTINCT ?uri WHERE { "
                + "  ?subUri rdfs:subClassOf* schema:Person . "
                + "  ?uri a ?subUri . "
                + "  ?uri rdfs:label ?label . "
                + "  ?uri foaf:member <" + organizationUri.trim() + "> . "
                + "  FILTER(LCASE(STR(?label)) = LCASE(\"" + escapedLabel + "\")) "
                + "} LIMIT 1";

        ResultSetRewindable results = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        if (results != null && results.hasNext()) {
            QuerySolution soln = results.next();
            if (soln != null && soln.get("uri") != null && soln.get("uri").isResource()) {
                return Person.find(soln.getResource("uri").getURI());
            }
        }
        return null;
    }

    private static boolean setCuratorLink(String organizationUri, String curatorUri) {
        if (!isHttpUri(organizationUri) || !isHttpUri(curatorUri)) {
            return false;
        }

        String deleteQuery = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "DELETE WHERE { <" + organizationUri + "> hasco:hasCurator ?anyCurator . }";
        if (!executeUpdate(deleteQuery)) {
            return false;
        }

        String insertQuery = NameSpaces.getInstance().printSparqlNameSpaceList()
                + "INSERT DATA { <" + organizationUri + "> hasco:hasCurator <" + curatorUri + "> . }";
        return executeUpdate(insertQuery);
    }

    private static boolean executeUpdate(String query) {
        try {
            UpdateRequest request = UpdateFactory.create(query);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request,
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)
            );
            processor.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static String preferredShortName(Organization organization) {
        if (organization.getHasShortName() != null && !organization.getHasShortName().trim().isEmpty()) {
            return organization.getHasShortName().trim();
        }
        if (organization.getName() != null && !organization.getName().trim().isEmpty()) {
            return organization.getName().trim();
        }
        if (organization.getLabel() != null && !organization.getLabel().trim().isEmpty()) {
            return organization.getLabel().trim();
        }
        return "ORG";
    }

    private static String preferredEmailDomain(Organization organization) {
        String mbox = organization.getMbox();
        if (mbox != null && !mbox.trim().isEmpty()) {
            String normalized = mbox.trim().toLowerCase(Locale.ROOT);
            if (normalized.startsWith("mailto:")) {
                normalized = normalized.substring("mailto:".length()).trim();
            }
            int at = normalized.lastIndexOf('@');
            if (at > 0 && at + 1 < normalized.length()) {
                String domain = normalized.substring(at + 1).trim();
                if (isValidDomain(domain)) {
                    return domain;
                }
            }
        }

        String web = organization.getHasWebDocument();
        if (web != null && !web.trim().isEmpty()) {
            String candidate = web.trim();
            if (!candidate.startsWith("http://") && !candidate.startsWith("https://")) {
                candidate = "https://" + candidate;
            }
            try {
                java.net.URI uri = java.net.URI.create(candidate);
                String host = uri.getHost();
                if (host != null) {
                    host = host.toLowerCase(Locale.ROOT);
                    if (host.startsWith("www.")) {
                        host = host.substring(4);
                    }
                    if (isValidDomain(host)) {
                        return host;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        String token = safeToken(preferredShortName(organization));
        if (token.isEmpty()) {
            token = "org";
        }
        return token + ".org";
    }

    private static boolean isValidDomain(String value) {
        if (value == null) {
            return false;
        }
        String domain = value.trim().toLowerCase(Locale.ROOT);
        return domain.matches("^[a-z0-9.-]+\\.[a-z]{2,}$");
    }

    private static String safeToken(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
        normalized = normalized.replaceAll("^_+", "").replaceAll("_+$", "");
        return normalized;
    }

    private static boolean isHttpUri(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        return normalized.startsWith("http://") || normalized.startsWith("https://");
    }

}
