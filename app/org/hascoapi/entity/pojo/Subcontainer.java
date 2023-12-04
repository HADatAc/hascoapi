package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
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

@JsonFilter("subContainerFilter")
public class Subcontainer extends Container implements SlotListElement {

	private static final Logger log = LoggerFactory.getLogger(Subcontainer.class);

 	@PropertyField(uri="vstoi:belongsTo")
	private String belongsTo;

 	@PropertyField(uri="vstoi:hasNext")
	private String hasNext;

 	@PropertyField(uri="vstoi:hasPrevious")
	private String hasPrevious;

	public String getBelongsTo() {
		return belongsTo;
	}

	public void setBelongsTo(String belongsTo) {
		this.belongsTo = belongsTo;
	}
   
	public String getHasNext() {
		return hasNext;
	}

	public void setHasNext(String hasNext) {
		this.hasNext = hasNext;
	}
   
	public String getHasPrevious() {
		return hasPrevious;
	}

	public void setHasPrevious(String hasPrevious) {
		this.hasPrevious = hasPrevious;
	}
   
	@Override
	public boolean equals(Object o) {
		if((o instanceof Subcontainer) && (((Subcontainer)o).getUri().equals(this.getUri()))) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return getUri().hashCode();
	}

	public static Subcontainer find(String uri) {
	    Subcontainer subContainer = null;
	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} else {
			subContainer = new Subcontainer();
		}
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					subContainer.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					subContainer.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					subContainer.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
					subContainer.setHasStatus(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
					subContainer.setBelongsTo(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_FIRST)) {
					subContainer.setHasFirst(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_NEXT)) {
					subContainer.setHasNext(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PREVIOUS)) {
					subContainer.setHasPrevious(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
					subContainer.setSerialNumber(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INFORMANT)) {
					subContainer.setHasInformant(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					subContainer.setImage(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					subContainer.setComment(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SHORT_NAME)) {
					subContainer.setHasShortName(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
					subContainer.setHasLanguage(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
					subContainer.setHasVersion(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					subContainer.setHasSIRManagerEmail(str);
				}
			}
		}

		subContainer.setUri(uri);
		
		return subContainer;
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
