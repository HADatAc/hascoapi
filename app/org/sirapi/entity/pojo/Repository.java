package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.sparql.engine.http.QueryExceptionHTTP;
import org.sirapi.Constants;
import org.sirapi.annotations.PropertyField;
import org.sirapi.annotations.PropertyValueType;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.utils.NameSpaces;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repository extends HADatAcThing {

    private static final Logger log = LoggerFactory.getLogger(Repository.class);

    public static String className = HASCO.REPOSITORY;

    @PropertyField(uri="hasco:hasTitle")
    private String title;

    @PropertyField(uri="hasco:hasBaseOntology")
    private String hasBaseOntology;

    @PropertyField(uri="hasco:hasBaseURL")
    private String hasBaseURL;

    @PropertyField(uri="hasco:hasInstitution", valueType=PropertyValueType.URI)
    private String institutionUri;

    private DateTime startedAt;

    private Agent institution;

    @PropertyField(uri="hasco:hasDefaultNamespaceAbbreviation")
    private String hasDefaultNamespaceAbbreviation;

    @PropertyField(uri="hasco:hasDefaultNamespaceURL", valueType=PropertyValueType.URI)
    private String hasDefaultNamespaceURL;

    @PropertyField(uri="hasco:hasNamespaceAbbreviation")
    private String hasNamespaceAbbreviation;

    @PropertyField(uri="hasco:hasNamespaceURL", valueType=PropertyValueType.URI)
    private String hasNamespaceURL;

    @PropertyField(uri="vstoi:hasVersion")
    private String hasVersion;

    public Repository() {
        this.uri = Constants.DEFAULT_REPOSITORY;
        this.typeUri = HASCO.REPOSITORY;
        this.hascoTypeUri = HASCO.REPOSITORY;
        this.label = "";
        this.title = "";
        this.comment = "";
        this.hasBaseOntology = "";
        this.hasBaseURL = "";
        //this.hasBaseURL =  "http://hadatac.org/kb/" + ConfigProp.getBasePrefix() + "#";
        this.institutionUri = institutionUri;
        this.startedAt = null;
        this.hasDefaultNamespaceAbbreviation = "";
        this.hasDefaultNamespaceURL = "";
        this.hasNamespaceAbbreviation = "";
        this.hasNamespaceURL = "";
        this.hasVersion = Constants.REPOSITORY_VERSION;
    }

    public String getTitle() {
        return title;
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

    public String getHasDefaultNamespaceAbbreviation() {
        if (hasDefaultNamespaceAbbreviation != null && hasDefaultNamespaceAbbreviation.equals("")) {
            return null;
        }
        return hasDefaultNamespaceAbbreviation;
    }

    public String getHasDefaultNamespaceURL() {
        if (hasDefaultNamespaceURL != null && hasDefaultNamespaceURL.equals("")) {
            return null;
        }
        return hasDefaultNamespaceURL;
    }

    public String getHasNamespaceAbbreviation() {
        if (hasNamespaceAbbreviation != null && hasNamespaceAbbreviation.equals("")) {
            return null;
        }
        return hasNamespaceAbbreviation;
    }

    public String getHasNamespaceURL() {
        if (hasNamespaceURL != null && hasNamespaceURL.equals("")) {
            return null;
        }
        return hasNamespaceURL;
    }

    public String getHasVersion() {
        return hasVersion;
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

    public void setTitle(String title) {
        this.title = title;
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

    public void setHasDefaultNamespaceAbbreviation(String hasDefaultNamespaceAbbreviation) {
        this.hasDefaultNamespaceAbbreviation = hasDefaultNamespaceAbbreviation;
    }

    public void setHasDefaultNamespaceURL(String hasDefaultNamespaceURL) {
        this.hasDefaultNamespaceURL = hasDefaultNamespaceURL;
    }

    public void setHasNamespaceAbbreviation(String hasNamespaceAbbreviation) {
        this.hasNamespaceAbbreviation = hasNamespaceAbbreviation;
    }

    public void setHasNamespaceURL(String hasNamespaceURL) {
        this.hasNamespaceURL = hasNamespaceURL;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
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

    public static Repository getRepository() {
        Repository repository = new Repository();

        String uri = "<" + Constants.DEFAULT_REPOSITORY + ">";
        String repositoryQueryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT DISTINCT ?label ?title ?comment ?baseOntology ?baseURL ?institutionUri ?defaultNsAbbreviation ?defaultNsUrl ?nsAbbreviation ?nsUrl ?version " +
                " WHERE {  \n" +
                "      ?type rdfs:subClassOf* hasco:Repository . \n" +
                "      " + uri + " a ?type . \n" +
                "      " + uri + " hasco:hascoType ?hascoType . \n" +
                "      OPTIONAL { " + uri + " rdfs:label ?label } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasTitle ?title } . \n" +
                "      OPTIONAL { " + uri + " rdfs:comment ?comment } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasBaseOntology ?baseOntology } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasBaseURL ?baseURL } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasInstitution ?institutionUri } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasDefaultNamespaceAbbreviation ?defaultNsAbbreviation } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasDefaultNamespaceURL ?defaultNsUrl } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasNamespaceAbbreviation ?nsAbbreviation } . \n" +
                "      OPTIONAL { " + uri + " hasco:hasNamespaceURL ?nsUrl } . \n" +
                "      OPTIONAL { " + uri + " vstoi:hasVersion ?version } . \n" +
                " } \n";

        try {

            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), repositoryQueryString);

            if (!resultsrw.hasNext()) {
                System.out.println("[WARNING] REPOSITORY_URI " + uri + " does not retrieve a repository object");
                return null;
            } else {
                QuerySolution soln = resultsrw.next();
                repository.setUri(Constants.DEFAULT_REPOSITORY);

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
                if (soln.contains("defaultNsAbbreviation")) {
                    repository.setHasDefaultNamespaceAbbreviation(soln.get("defaultNsAbbreviation").toString());
                }
                if (soln.contains("defaultNsUrl")) {
                    repository.setHasDefaultNamespaceURL(soln.get("defaultNsUrl").toString());
                }
                if (soln.contains("nsAbbreviation")) {
                    repository.setHasNamespaceAbbreviation(soln.get("nsAbbreviation").toString());
                }
                if (soln.contains("nsUrl")) {
                    repository.setHasNamespaceURL(soln.get("nsUrl").toString());
                }
                if (soln.contains("version")) {
                    repository.setHasVersion(soln.get("version").toString());
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
    public boolean saveToSolr() {
        return true;
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

}

