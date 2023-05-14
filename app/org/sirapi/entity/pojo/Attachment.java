package org.sirapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;

import java.util.ArrayList;
import java.util.List;

public class Attachment extends HADatAcThing implements Comparable<Attachment>  {

    @PropertyField(uri="vstoi:belongsTo")
    private String belongsTo;

    @PropertyField(uri="vstoi:hasDetector")
    private String hasDetector;

    @PropertyField(uri="vstoi:hasPriority")
    private String hasPriority;

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }

    public String getHasDetector() {
        return hasDetector;
    }

    public void setHasDetector(String hasDetector) {
        this.hasDetector = hasDetector;
    }

    public String getHasPriority() {
        return hasPriority;
    }

    public void setHasPriority(String hasPriority) {
        this.hasPriority = hasPriority;
    }

    public Detector getDetector() {
        if (hasDetector == null || hasDetector.isEmpty()) {
            return null;
        }
        return Detector.find(hasDetector);
    }

    public static int getNumberAttachmentsByInstrument(String instrumentUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:Attachment . " +
                " ?uri a ?attModel ." +
                "}";

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

    public static List<Attachment> findByInstrumentWithPages(String instrumentUri, int pageSize, int offset) {
        List<Attachment> attachments = new ArrayList<Attachment>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?attModel rdfs:subClassOf* vstoi:Attachment . " +
                " ?uri a ?attModel . } " +
                " ?uri vstoi:belongs>To <" + instrumentUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                Attachment attachment = Attachment.find(soln.getResource("uri").getURI());
                attachments.add(attachment);
            }
        }
        return attachments;
    }

    public static List<Attachment> findByInstrument(String instrumentUri) {
        System.out.println("findByInstrument: [" + instrumentUri + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?detModel rdfs:subClassOf* vstoi:Detector . " +
                " ?uri a ?detModel ." +
                " ?uri vstoi:belongs>To <" + instrumentUri + ">. " +
                "} ";

        return findByQuery(queryString);
    }

    private static List<Attachment> findByQuery(String queryString) {
        List<Attachment> attachments = new ArrayList<Attachment>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            Attachment attachment = find(soln.getResource("uri").getURI());
            attachments.add(attachment);
        }

        java.util.Collections.sort((List<Attachment>) attachments);
        return attachments;

    }

    public static Attachment find(String uri) {
        Attachment attachment = null;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

        attachment = new Attachment();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                attachment.setLabel(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                attachment.setTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                attachment.setComment(object.asLiteral().getString());
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                attachment.setHascoTypeUri(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
                attachment.setBelongsTo(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_DETECTOR)) {
                attachment.setHasDetector(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PRIORITY)){
                attachment.setHasPriority(object.asLiteral().getString());
            }
        }

        attachment.setUri(uri);

        return attachment;
    }

    static public boolean createAttachment(String instrumentUri, String priority, String hasDetector) {
        if (instrumentUri == null || instrumentUri.isEmpty()) {
            return false;
        }
        if (priority == null || priority.isEmpty()) {
            return false;
        }
        Attachment att = new Attachment();
        att.setLabel("Attachment " + priority);
        att.setTypeUri(VSTOI.ATTACHMENT);
        att.setHascoTypeUri(VSTOI.ATTACHMENT);
        att.setComment("Attachment " + priority + " of instrument with URI " + instrumentUri);
        att.setBelongsTo(instrumentUri);
        att.setHasPriority(priority);
        if (hasDetector != null) {
            att.setHasDetector(hasDetector);
        }
        att.save();
        return true;
    }

    static public boolean updateAttachmentDetector(String attachmentUri, String hasDetector) {
        Attachment oldAttachment = Attachment.find(attachmentUri);
        if (oldAttachment == null) {
            return false;
        }
        Attachment newAttachment = new Attachment();
        newAttachment.setUri(attachmentUri);
        newAttachment.setLabel(oldAttachment.getLabel());
        newAttachment.setTypeUri(oldAttachment.getTypeUri());
        newAttachment.setComment(oldAttachment.getComment());
        newAttachment.setHascoTypeUri(oldAttachment.getHascoTypeUri());
        newAttachment.setBelongsTo(oldAttachment.getBelongsTo());
        newAttachment.setHasPriority(oldAttachment.getHasPriority());
        newAttachment.setHasDetector(hasDetector);
        oldAttachment.delete();
        newAttachment.save();
        return true;
    }

    @Override
    public int compareTo(Attachment another) {
        return this.getHasPriority().compareTo(another.getHasPriority());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

    @Override
    public boolean saveToSolr() {
        return false;
    }

    @Override
    public int deleteFromSolr() {
        return 0;
    }

}
