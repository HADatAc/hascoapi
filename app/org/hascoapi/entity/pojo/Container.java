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

@JsonFilter("containerFilter")
public abstract class Container extends HADatAcThing implements SIRElement, Comparable<Container> {

	private static final Logger log = LoggerFactory.getLogger(Container.class);

	@PropertyField(uri="vstoi:hasStatus")
	private String hasStatus;

	@PropertyField(uri="vstoi:hasFirst")
	private String hasFirst;

	@PropertyField(uri="vstoi:hasSerialNumber")
	private String serialNumber;

	@PropertyField(uri="vstoi:hasInformant")
	private String hasInformant;

	@PropertyField(uri="hasco:hasImage")
	private String image;

	@PropertyField(uri="vstoi:hasShortName")
	private String hasShortName;

	@PropertyField(uri="vstoi:hasLanguage")
	private String hasLanguage;

	@PropertyField(uri="vstoi:hasVersion")
	private String hasVersion;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

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

	public String getSerialNumber() {
		return serialNumber;
	}

	public void setSerialNumber(String serialNumber) {
		this.serialNumber = serialNumber;
	}

	public String getHasInformant() {
		return hasInformant;
	}

	public void setHasInformant(String hasInformant) {
		this.hasInformant = hasInformant;
	}

	public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
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

	public String getHasSIRManagerEmail() {
		return hasSIRManagerEmail;
	}

	public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
		this.hasSIRManagerEmail = hasSIRManagerEmail;
	}

	public String getTypeLabel() {
    	InstrumentType insType = InstrumentType.find(getTypeUri());
    	if (insType == null || insType.getLabel() == null) {
    		return "";
    	}
    	return insType.getLabel();
    }

    public String getTypeURL() {
    	InstrumentType insType = InstrumentType.find(getTypeUri());
    	if (insType == null || insType.getLabel() == null) {
    		return "";
    	}
    	return insType.getURL();
    }

    public List<SlotElement> getSlotElements() {
    	List<SlotElement> slotElements = Container.getSlotElements(uri);
    	return slotElements;
    }

    public List<Subcontainer> getSubcontainers() {
		List<Subcontainer> subcontainers = new ArrayList<Subcontainer>();
    	List<SlotElement> slots = Container.getSlotElements(uri);
		for (SlotElement slot: slots) {
			if (slot instanceof Subcontainer) {
				subcontainers.add((Subcontainer)slot);
			}
		}
    	return subcontainers;
    }

    public List<ContainerSlot> getContainerSlots() {
		List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
    	List<SlotElement> slots = Container.getSlotElements(uri);
		for (SlotElement slot: slots) {
			System.out.println("In slots: has " + slot.getUri());
			if (slot instanceof ContainerSlot) {
			    System.out.println("   ===> Is ContainerSlot");
				containerSlots.add((ContainerSlot)slot);
			}
		}
    	return containerSlots;
    }

    public static List<SlotElement> getSlotElements(String containerUri) {
        System.out.println("findByContainer: [" + containerUri + "]");
        Container container = Container.find(containerUri);
        if (container != null) {
            System.out.println("findByContainer: getHasFirst(): " + container.getHasFirst());
        }
        if (container == null || container.getHasFirst() == null || container.getHasFirst().isEmpty()) {
            return new ArrayList<SlotElement>();
        }
        System.out.println("First : [" + container.getHasFirst() + "]");
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                "   <" + container.getHasFirst() + "> vstoi:hasNext* ?uri . " +
                "} ";
        System.out.println(queryString);
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
	public List<Detector> getDetectors() {
		List<Detector> detectors = new ArrayList<Detector>();
    	List<SlotElement> slots = getSlotElements(uri);
		for (SlotElement slot : slots) {
			if (slot instanceof ContainerSlot) {
				Detector detector = ((ContainerSlot)slot).getDetector();
				detectors.add(detector);
			}
		} 
    	return detectors;
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

	public static Container find(String uri) {
		System.out.println("Container.find(): uri = [" + uri + "]");
		Container container;
		String typeUri = retrieveTypeUri(uri);
		System.out.println("Container.find(): typeUri = [" + typeUri + "]");
		if (typeUri.equals(VSTOI.INSTRUMENT)) {
			container = new Instrument();
		} else if (typeUri.equals(VSTOI.SUBCONTAINER)) {
			container = new Subcontainer();
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
					container.setLabel(str);
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					container.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					container.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_STATUS)) {
					container.setHasStatus(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_FIRST)) {
					container.setHasFirst(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SERIAL_NUMBER)) {
					container.setSerialNumber(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_INFORMANT)) {
					container.setHasInformant(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.HAS_IMAGE)) {
					container.setImage(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					container.setComment(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SHORT_NAME)) {
					container.setHasShortName(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_LANGUAGE)) {
					container.setHasLanguage(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
         			container.setHasVersion(str);
				} else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
					container.setHasSIRManagerEmail(str);
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

	public boolean deleteContainerSlots() {
		if (uri == null || uri.isEmpty()) {
			return true;
		}
		List<SlotElement> slotElements = getSlotElements(uri);
		if (slotElements == null) {
			return true;
		}
		/**
		 *   TODO: Need to be properly remove from the list
		 */
		for (SlotElement slot: slotElements) {
			if (slot instanceof ContainerSlot) {
				((ContainerSlot)slot).delete();
			}
		}

		// update list on container itself 
		setHasFirst(null);
		save();
		return true;
	}

	public boolean createContainerSlots(int totNewContainerSlots) {
		if (totNewContainerSlots <= 0) {
			return false;
		}

		System.out.println("inside create Container Slots");

		List<SlotElement> slotElements = getSlotElements(uri);

		System.out.println("printing slot list");
		if (slotElements != null) {
			for (SlotElement slot: slotElements) {
				System.out.println(slot.getUri() + "  Next: " + slot.getHasNext());
			}
		}

		int currentTotal = -1;
		SlotElement lastSlot = null; 

		if (slotElements == null || slotElements.size() == 0) {
			currentTotal = 0;
		} else {
			currentTotal = slotElements.size();
			lastSlot = slotElements.get(currentTotal - 1);
			System.out.println("Last slot: " + lastSlot.getUri());
		}

		int newTotal = currentTotal + totNewContainerSlots;

		System.out.println("New total of slots: " + newTotal);
		
		for (int aux = currentTotal + 1; aux <= newTotal; aux++) {
			String auxstr = Utils.adjustedPriority(String.valueOf(aux), 1000);
			String newUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
			String newNextUri = null;
			String newPreviousUri = null;
			if (aux + 1 <= newTotal) {
			  String auxNextstr = Utils.adjustedPriority(String.valueOf(aux + 1), 1000);
			  newNextUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxNextstr;
			}
			if (aux > 1) {
			  String auxPrevstr = Utils.adjustedPriority(String.valueOf(aux - 1), 1000);
			  newPreviousUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxPrevstr;
			}
		    System.out.println("Creating slot: [" + newUri + "]  with prev: [" + newPreviousUri + "  next: [" + newNextUri + "]");
			String nullstr = null;
			ContainerSlot.createContainerSlot(uri, newUri, newNextUri, newPreviousUri, auxstr, nullstr);
		}

		if (currentTotal <= 0) {
		    String auxstr = Utils.adjustedPriority("1", 1000);
		  	String firstUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
		  	System.out.println("Container [" + uri + "] adding FirstUri: [" + firstUri + "]");
		  	setHasFirst(firstUri);
		    save();
		} else {
			String auxstr = Utils.adjustedPriority(String.valueOf(currentTotal + 1), 1000);
			String nextUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
			System.out.println("NextUri: [" + nextUri + "] updated at [" + lastSlot.getUri() + "]");
			lastSlot.setHasNext(nextUri);
			lastSlot.save();
		}

		slotElements = getSlotElements(uri);
		if (slotElements == null) {
			return false;
		}
		return true;
		//return (containerSlotList.size() == newTotal);
	}

	@Override
    public int compareTo(Container another) {
        return this.getLabel().compareTo(another.getLabel());
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
