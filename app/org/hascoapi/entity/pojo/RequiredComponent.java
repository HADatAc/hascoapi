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
import org.hascoapi.vocabularies.FOAF;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.SCHEMA;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;

@JsonFilter("requiredComponentFilter")
public class RequiredComponent extends HADatAcThing {

	private static final Logger log = LoggerFactory.getLogger(RequiredComponent.class);

	@PropertyField(uri="vstoi:hasContainerSlot")
    protected String hasContainerSlotUri;

	@PropertyField(uri="vstoi:hasComponent")
	private String usesComponent;

	public String getHasContainerSlot() {
		return hasContainerSlotUri;
	}

	public void setHasContainerSlot(String hasContainerSlotUri) {
		this.hasContainerSlotUri = hasContainerSlotUri;
	}

	public ContainerSlot getContainerSlot() {
		if (hasContainerSlotUri == null || hasContainerSlotUri.isEmpty()) {
			return null;
		}
		return ContainerSlot.find(hasContainerSlotUri);
	} 

	public String getUsesComponent() {
		return usesComponent;
	}

	public void setUsesComponent(String usesComponent) {
		this.usesComponent = usesComponent;
	}

	public Component getComponent() {
		if (usesComponent == null || usesComponent.isEmpty()) {
			return null;
		}
		return Component.find(usesComponent);
	} 

	public RequiredComponent() {
		this.setTypeUri(VSTOI.REQUIRED_COMPONENT);
		this.setHascoTypeUri(VSTOI.REQUIRED_COMPONENT);
	}

	public static RequiredComponent find(String uri) {
		//System.out.println("RequiredInstrumentation.find(): uri = [" + uri + "]");
		RequiredComponent requiredComponent;
		String hascoTypeUri = Utils.retrieveHASCOTypeUri(uri);
		if (hascoTypeUri == null) {
			System.out.println("[ERROR] RequiredComponent.java: URI [" + uri + "] has no HASCO TYPE.");
			return null;
		}
		//System.out.println("Place.find(): typeUri = [" + typeUri + "]");
		if (hascoTypeUri.equals(VSTOI.REQUIRED_COMPONENT)) {
			requiredComponent = new RequiredComponent();
		} else {
			return null;
		}

	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} 
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					requiredComponent.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					requiredComponent.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					requiredComponent.setComment(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					requiredComponent.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_CONTAINER_SLOT)) {
					requiredComponent.setHasContainerSlot(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.USES_COMPONENT)) {
					requiredComponent.setUsesComponent(str);
				}
			}
		}

		requiredComponent.setUri(uri);
		
		return requiredComponent;
	}

    @Override public void save() {
		saveToTripleStore();
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}
