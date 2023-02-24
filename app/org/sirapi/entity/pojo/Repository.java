package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.sirapi.annotations.PropertyField;
import org.sirapi.annotations.PropertyValueType;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.URIUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.ConfigProp;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.utils.NameSpaces;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Repository extends HADatAcThing {

    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    public static String className = HASCO.REPOSITORY;

    private String uri;

    private String label;

    @PropertyField(uri="hasco:hasTitle")
    private String title;

    @PropertyField(uri="rdfs:comment")
    private String comment;

    @PropertyField(uri="hasco:hasBaseOntology")
    private String hasBaseOntology;

    @PropertyField(uri="hasco:hasBaseURL")
    private String hasBaseURL;

    @PropertyField(uri="hasco:hasInstitution", valueType=PropertyValueType.URI)
    private String institutionUri;

    private DateTime startedAt;

    private Agent institution;

    public Repository() {
        this.uri = ConfigProp.getBaseURL();
        this.typeUri = HASCO.REPOSITORY;
        this.hascoTypeUri = HASCO.REPOSITORY;
        this.label = ConfigProp.getShortName();
        this.title = ConfigProp.getFullName();
        this.comment = ConfigProp.getDescription();
        this.hasBaseOntology = ConfigProp.getBasePrefix();
        this.hasBaseURL =  "http://hadatac.org/kb/" + ConfigProp.getBasePrefix() + "#";
        this.institutionUri = institutionUri;
        this.startedAt = null;
    }

    public String getUri() {
        return uri;
    }

    public String getLabel() {
        return label;
    }

    public String getTitle() {
        return title;
    }

    public String getComment() {
        return comment;
    }

    public String getBaseOntology() {
        return hasBaseOntology;
    }

    public String getBaseURL() {
        return hasBaseURL;
    }

    @JsonIgnore
    public String getInstitutionUri() {
        return institutionUri;
    }

    public Agent getInstitution() {
        if (institutionUri == null || institutionUri.equals("")) {
            return null;
        }
        if (institution != null && institution.getUri().equals(institutionUri)) {
            return institution;
        }
        return Agent.find(institutionUri);
    }

    // get Start Time Methods
    public String getStartedAt() {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        return formatter.withZone(DateTimeZone.UTC).print(startedAt);
    }
    public String getStartedAtXsd() {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
        return formatter.withZone(DateTimeZone.UTC).print(startedAt);
    }

    // set Methods

    public void setUri(String uri) {
        this.uri = uri;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setBaseOntology(String hasBaseOntology) {
        this.hasBaseOntology = hasBaseOntology;
    }

    public void setBaseURL(String hasBaseURL) {
        this.hasBaseURL = hasBaseURL;
    }

    public void setInstitutionUri(String institutionUri) {
        if (institutionUri != null && !institutionUri.equals("")) {
            if (institutionUri.indexOf("http") > -1) {
                this.institutionUri = institutionUri;
            }
        }
    }

    // set Start Time Methods
    public void setStartedAt(String startedAt) {
        if (startedAt == null || startedAt.equals("")) {
            this.startedAt = null;
        } else {
            DateTimeFormatter formatter = DateTimeFormat.forPattern("EEE MMM dd HH:mm:ss zzz yyyy");
            this.startedAt = formatter.parseDateTime(startedAt);
        }
    }

    public void setStartedAtXsd(String startedAt) {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTimeNoMillis();
        this.startedAt = formatter.parseDateTime(startedAt);
    }

    public void setStartedAtXsdWithMillis(String startedAt) {
        DateTimeFormatter formatter = ISODateTimeFormat.dateTime();
        this.startedAt = formatter.parseDateTime(startedAt);
    }

    public Repository find() {
        return new Repository();
    }

    public static Repository find(String uri) {
        if (uri == null || uri.equals("")) {
            System.out.println("[ERROR] No valid REPOSITORY_URI provided to retrieve Repository object: " + uri);
            return null;
        }
        Repository repository = new Repository();

        // THIS NEEDS TO BE IMPLEMENTED
        String prefixedUri = URIUtils.replacePrefixEx(uri);
        String adjustedUri = prefixedUri;
        if (adjustedUri.startsWith("http")) {
            adjustedUri = "<" + adjustedUri + ">";
        }
        String repositoryQueryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT DISTINCT ?label ?title ?comment ?baseOntology ?baseURL ?institutionUri " +
                " WHERE {  \n" +
                "      ?type rdfs:subClassOf* hasco:Repository . \n" +
                "      " + adjustedUri + " a ?type . \n" +
                "      " + adjustedUri + " hasco:hascoType ?hascoType . \n" +
                "      OPTIONAL { " + adjustedUri + " rdfs:label ?label } . \n" +
                "      OPTIONAL { " + adjustedUri + " hasco:hasTitle ?title } . \n" +
                "      OPTIONAL { " + adjustedUri + " rdfs:comment ?comment } . \n" +
                "      OPTIONAL { " + adjustedUri + " hasco:hasBaseOntology ?baseOntology } . \n" +
                "      OPTIONAL { " + adjustedUri + " hasco:hasBaseURL ?baseURL } . \n" +
                "      OPTIONAL { " + adjustedUri + " hasco:hasInstitution ?institutionUri } . \n" +
                " } \n";

        try {
            // System.out.println("Study's find() query: " + studyQueryString);

            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), repositoryQueryString);

            if (!resultsrw.hasNext()) {
                System.out.println("[ERROR] STUDY_URI " + adjustedUri + " does not retrieve a study object");
                return null;
            } else {
                QuerySolution soln = resultsrw.next();
                repository.setUri(prefixedUri);

                repository.setTypeUri(HASCO.REPOSITORY);
                repository.setHascoTypeUri(HASCO.REPOSITORY);

                if (soln.contains("label")) {
                    repository.setLabel(soln.get("label").toString());
                }
                if (soln.contains("title")) {
                    repository.setTitle(soln.get("title").toString());
                }
                if (soln.contains("comment")) {
                    repository.setComment(soln.get("comment").toString());
                }
                if (soln.contains("baseOntology")) {
                    repository.setComment(soln.get("baseOntology").toString());
                }
                if (soln.contains("baseURL")) {
                    repository.setComment(soln.get("baseURL").toString());
                }
                if (soln.contains("institutionUri")) {
                    repository.setInstitutionUri(soln.get("institutionUri").toString());
                }
            }
        } catch (QueryExceptionHTTP e) {
            e.printStackTrace();
        }
        return repository;
    }

    @Override
    public void save() {
        saveToTripleStore();
    }

    @Override
    public boolean saveToTripleStore() {
        return true;
    }

    @Override
    public boolean saveToSolr() {
        return true;
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

    @Override
    public void deleteFromTripleStore() {
        super.deleteFromTripleStore();
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

}

