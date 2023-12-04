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

@JsonFilter("subcontainerFilter")
public class Subcontainer extends Container implements SlotElement {

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
	    Subcontainer subcontainer = null;
	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} else {
			subcontainer = new Subcontainer();
		}
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					subcontainer.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					subcontainer.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					subcontainer.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
					subcontainer.setHasStatus(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.BELONGS_TO)) {
					subcontainer.setBelongsTo(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_FIRST)) {
					subcontainer.setHasFirst(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_NEXT)) {
					subcontainer.setHasNext(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_PREVIOUS)) {
					subcontainer.setHasPrevious(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
					subcontainer.setSerialNumber(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INFORMANT)) {
					subcontainer.setHasInformant(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					subcontainer.setImage(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					subcontainer.setComment(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SHORT_NAME)) {
					subcontainer.setHasShortName(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
					subcontainer.setHasLanguage(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
					subcontainer.setHasVersion(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					subcontainer.setHasSIRManagerEmail(str);
				}
			}
		}
		subcontainer.setUri(uri);
		if (subcontainer.getHascoTypeUri().equals(VSTOI.SUBCONTAINER)) {
            return subcontainer;
        } 
        return null;
	}

	public boolean saveAndAttach() {
		//System.out.println("Subcontainer's named graph: " + this.getNamedGraph());
		System.out.println("Subcontainer's getBelongsTo: " + this.getBelongsTo());
		// Subcontair always added to the top of the list. This means that the parent container needs to be updated 
		Container parent = Container.find(getBelongsTo());
		if (parent == null) {
			System.out.println("[ERROR] Subcontainer.saveAndAttach(): could not find parent container");
			return false;
		}
		parent.setNamedGraph(this.getNamedGraph());
		if (parent.getHasFirst() == null) {
			// Subcointainer is the only element in the list
			parent.setHasFirst(this.getUri());
			parent.save();
			this.setHasNext(null); 
			this.setHasPrevious(null);
		} else {
			SlotElement next = SlotOperations.findSlotElement(parent.getHasFirst());
			next.setNamedGraph(this.getNamedGraph());
			parent.setHasFirst(this.getUri());
			parent.save();
			this.setHasNext(next.getUri());
			this.setHasPrevious(null);
			next.setHasPrevious(this.getUri());
			next.save();
		}

		// SAVE SUBCONTAINET
		this.save();
		return true;
	}

	public boolean deleteAndDetach() {

		// DETACH FIRST
		if (getHasPrevious() == null) {
			Container parent = Container.find(getBelongsTo());
			parent.setNamedGraph(this.getNamedGraph());
			if (parent == null) {
				System.out.println("[ERROR] Subcontainer.deleteAndDetach(): could not find parent container");
				return false;
			}
			if (getHasNext() == null) {
				// List is now empty. Parent's hasFirst is reset to null
				parent.setHasFirst(null);
				parent.save();
			} else {
				// Next becomes the first, and its hasPrevious is null. Parent's hasFirst points to next
				SlotElement next = SlotOperations.findSlotElement(this.getHasNext());
		        next.setNamedGraph(this.getNamedGraph());
				next.setHasPrevious(null);
				next.save();
				parent.setHasFirst(next.getUri());
				parent.save();
			}
		} else {
			// Adjust next and previous
			SlotElement previous = SlotOperations.findSlotElement(this.getHasPrevious());
			if (previous != null) {
				System.out.println("[ERROR] Subcontainer.deleteAndDetach(): could not find PREVIOUS element");
				return false;
			}
		    previous.setNamedGraph(this.getNamedGraph());
			SlotElement next = SlotOperations.findSlotElement(this.getHasNext());
			if (next == null) {
				previous.setHasNext(null);
				previous.save();
			} else {
				previous.setHasNext(next.getUri());
				previous.save();
			    next.setNamedGraph(this.getNamedGraph());
				next.setHasPrevious(previous.getUri());
				next.save();
			}
		}
		// DELETE SECOND
		this.delete();
		return true;
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