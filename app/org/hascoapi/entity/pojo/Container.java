package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.jena.query.*;
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
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;

@JsonFilter("containerFilter")
public abstract class Container extends HADatAcClass implements SIRElement, Comparable<Container> {

	private static final Logger log = LoggerFactory.getLogger(Container.class);

	@PropertyField(uri="vstoi:hasStatus")
	private String hasStatus;

	@PropertyField(uri="vstoi:hasFirst")
	private String hasFirst;

	@PropertyField(uri="vstoi:hasInformant")
	private String hasInformant;

	@PropertyField(uri="vstoi:hasShortName")
	private String hasShortName;

	@PropertyField(uri="vstoi:hasLanguage")
	private String hasLanguage;

	@PropertyField(uri="vstoi:hasVersion")
	private String hasVersion;

 	@PropertyField(uri="vstoi:belongsTo")
	private String belongsTo;

 	@PropertyField(uri="vstoi:hasNext")
	private String hasNext;

 	@PropertyField(uri="vstoi:hasPrevious")
	private String hasPrevious;

 	@PropertyField(uri="vstoi:hasPriority")
	private String hasPriority;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

	public String getHasStatus() {
		return hasStatus;
	}

	public void setHasStatus(String hasStatus) {
		this.hasStatus = hasStatus;
	}

	public String getHasFirst() {
		return hasFirst;
	}

	public void setHasFirst(String hasFirst) {
		this.hasFirst = hasFirst;
	}

	public String getHasInformant() {
		return hasInformant;
	}

	public void setHasInformant(String hasInformant) {
		this.hasInformant = hasInformant;
	}

	public String getHasShortName() {
		return hasShortName;
	}

	public void setHasShortName(String hasShortName) {
		this.hasShortName = hasShortName;
	}

	public String getHasLanguage() {
		return hasLanguage;
	}

	public void setHasLanguage(String hasLanguage) {
		this.hasLanguage = hasLanguage;
	}

	public String getHasVersion() {
		return hasVersion;
	}

	public void setHasVersion(String hasVersion) {
		this.hasVersion = hasVersion;
	}

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
   
	public String getHasPriority() {
		return hasPriority;
	}

	public void setHasPriority(String hasPriority) {
		this.hasPriority = hasPriority;
	}
   
    public String getHasReviewNote() {      
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
    }

    public void setWasDerivedFrom(String wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }

    public String getWasDerivedFrom() {
        return wasDerivedFrom;
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public String getHasEditorEmail() {
        return hasEditorEmail;
    }

    public void setHasEditorEmail(String hasEditorEmail) {
        this.hasEditorEmail = hasEditorEmail;
    }

	public String getTypeLabel() {
		/*
    	InstrumentType insType = InstrumentType.find(getTypeUri());
    	if (insType == null || insType.getLabel() == null) {
    		return "";
    	}
    	return insType.getLabel();
		*/
		return getLabel();
    }

    public String getTypeURL() {
		/*
    	InstrumentType insType = InstrumentType.find(getTypeUri());
    	if (insType == null || insType.getLabel() == null) {
    		return "";
    	}
    	return insType.getURL();
		*/
		return getUri();
    }

	@JsonIgnore
    public List<SlotElement> getSlotElements() {
    	List<SlotElement> slotElements = Container.getSlotElements(this);
    	return slotElements;
    }

	public Container () {}

	public Container (String className) {
		super(className);
    }

	@JsonIgnore
    public List<Subcontainer> getSubcontainers() {
		List<Subcontainer> subcontainers = new ArrayList<Subcontainer>();
    	List<SlotElement> slots = Container.getSlotElements(this);
		for (SlotElement slot: slots) {
			if (slot instanceof Subcontainer) {
				subcontainers.add((Subcontainer)slot);
			}
		}
    	return subcontainers;
    }

    @JsonIgnore
    public List<ContainerSlot> getContainerSlots() {
		List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
    	List<SlotElement> slots = Container.getSlotElements(this);
		for (SlotElement slot: slots) {
			if (slot instanceof ContainerSlot) {
				containerSlots.add((ContainerSlot)slot);
			}
		}
    	return containerSlots;
    }

    public static List<SlotElement> getSlotElements(Container container) {
        //Container container = Container.find(containerUri);
        if (container == null || container.getHasFirst() == null || container.getHasFirst().isEmpty()) {
            return new ArrayList<SlotElement>();
        }
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   <" + container.getHasFirst() + "> vstoi:hasNext* ?uri . " +
                "} ";
        return findByQuery(queryString);        
    }

    private static List<SlotElement> findByQuery(String queryString) {
        List<SlotElement> slotElements = new ArrayList<SlotElement>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (!resultsrw.hasNext()) {
            return null;
        }
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            SlotElement slotElement = SlotOperations.findSlotElement(soln.getResource("uri").getURI());
            slotElements.add(slotElement);
        }
        //java.util.Collections.sort((List<ContainerSlot>) containerSlots);
        return slotElements;
    }

    public List<Annotation> getAnnotations() {
    	List<Annotation> annotations = Annotation.findByContainer(uri);
    	return annotations;
    }

    @JsonIgnore
	public List<Component> getComponents() {
		List<Component> components = new ArrayList<Component>();
    	List<SlotElement> slots = getSlotElements(this);
		for (SlotElement slot : slots) {
			if (slot instanceof ContainerSlot) {
				Component component = ((ContainerSlot)slot).getComponent();
				components.add(component);
			}
		} 
    	return components;
    }
    
	@Override
	public boolean equals(Object o) {
		if((o instanceof Container) && (((Container)o).getUri().equals(this.getUri()))) {
			return true;
		} else {
			return false;
		}
	}


	private static String retrieveTypeUri(String uri) {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?type WHERE { " +
                " <" + uri + "> hasco:hascoType ?type ." +
                "} ";
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            return soln.getResource("type").getURI();
        }
		return null;
    }

	/** 
    public static Container find(String uri) {
        //System.out.println("inside SlotOperations.findSlotElement() with uri: " + uri);
        Instrument instrument = Instrument.find(uri);
        if (instrument == null) {
            Subcontainer subcontainer = Subcontainer.find(uri);
            if (subcontainer == null) {
                return null;
            } else {
                //System.out.println("  Found as SUBCONTAINER");
                return (Container)subcontainer;
            }
        }
        //System.out.println("  Found as INSTRUMENT");
        return (Container)instrument;    
    }
	*/
 
	public static Container find(String uri) {
		if (uri == null || uri.isEmpty()) {
			return null;
		}
		Container container = null;
		String typeUri = retrieveTypeUri(uri);
		//System.out.println("Container.find(): typeUri = [" + typeUri + "]");
		if (typeUri.equals(VSTOI.INSTRUMENT)) {
			container = new Instrument(VSTOI.INSTRUMENT);
		} else if (typeUri.equals(VSTOI.SUBCONTAINER)) {
			container = new Subcontainer(VSTOI.SUBCONTAINER);
		} else {
			return null;
		}

		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			container = new Instrument(VSTOI.INSTRUMENT);
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				container.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

				if (predicate.equals(RDFS.LABEL)) {
					container.setLabel(object);
				} else if (predicate.equals(RDFS.SUBCLASS_OF)) {
					container.setSuperUri(object); 
				} else if (predicate.equals(HASCO.HASCO_TYPE)) {
					container.setHascoTypeUri(object);
				} else if (predicate.equals(HASCO.HAS_IMAGE)) {
					container.setHasImageUri(object);
				} else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
					container.setHasWebDocument(object);
				} else if (predicate.equals(VSTOI.HAS_STATUS)) {
					container.setHasStatus(object);
				} else if (predicate.equals(VSTOI.BELONGS_TO)) {
					container.setBelongsTo(object);
				} else if (predicate.equals(VSTOI.HAS_FIRST)) {
					container.setHasFirst(object);
				} else if (predicate.equals(VSTOI.HAS_NEXT)) {
					container.setHasNext(object);
				} else if (predicate.equals(VSTOI.HAS_PREVIOUS)) {
					container.setHasPrevious(object);
				} else if (predicate.equals(VSTOI.HAS_INFORMANT)) {
					container.setHasInformant(object);
				} else if (predicate.equals(RDFS.COMMENT)) {
					container.setComment(object);
				} else if (predicate.equals(VSTOI.HAS_SHORT_NAME)) {
					container.setHasShortName(object);
				} else if (predicate.equals(VSTOI.HAS_LANGUAGE)) {
					container.setHasLanguage(object);
				} else if (predicate.equals(VSTOI.HAS_VERSION)) {
         			container.setHasVersion(object);
				} else if (predicate.equals(VSTOI.HAS_REVIEW_NOTE)) {
					container.setHasReviewNote(object);
                } else if (predicate.equals(PROV.WAS_DERIVED_FROM)) {
                    container.setWasDerivedFrom(object);
				} else if (predicate.equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					container.setHasSIRManagerEmail(object);
				} else if (predicate.equals(VSTOI.HAS_EDITOR_EMAIL)) {
					container.setHasEditorEmail(object);
				}
			}
		}

		container.setUri(uri);
		
		return container;
	}

	@Override
	public int hashCode() {
		return getUri().hashCode();
	}

	@Override
    public int compareTo(Container another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override public void save() {
		if (this instanceof Subcontainer) {
			((Subcontainer)this).save();
		} else if (this instanceof Instrument) {
			((Instrument)this).save();
		} else {
			saveToTripleStore();
		}
	}

    @Override public void delete() {
		deleteFromTripleStore();
	}

}
