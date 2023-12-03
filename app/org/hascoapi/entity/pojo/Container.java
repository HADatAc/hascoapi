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

	public String gecontainerSlottTypeLabel() {
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

    public List<ContainerSlot> getContainerSlots() {
    	List<ContainerSlot> detSlots = ContainerSlot.findByContainer(uri);
    	return detSlots;
    }

    public List<Annotation> getAnnotations() {
    	List<Annotation> annotations = Annotation.findByContainer(uri);
    	return annotations;
    }

    @JsonIgnore
	public List<Detector> getDetectors() {
		List<Detector> detectors = new ArrayList<Detector>();
    	List<ContainerSlot> dets = ContainerSlot.findByContainer(uri);
		for (ContainerSlot det : dets) {
			Detector detector = det.getDetector();
			detectors.add(detector);
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

	@Override
	public int hashCode() {
		return getUri().hashCode();
	}

	public boolean deleteContainerSlots() {
		if (this.getContainerSlots() == null || uri == null || uri.isEmpty()) {
			return true;
		}
		List<ContainerSlot> containerSlots = ContainerSlot.findByContainer(uri);
		if (containerSlots == null) {
			return true;
		}
		for (ContainerSlot containerSlot: containerSlots) {
			containerSlot.delete();
		}

		// update list on container itself 
		setHasFirst(null);
		save();
		containerSlots = ContainerSlot.findByContainer(uri);
		return (containerSlots == null);
	}

	public boolean createContainerSlots(int totNewContainerSlots) {
		if (totNewContainerSlots <= 0) {
			return false;
		}

		System.out.println("inside create Container Slots");

		List<ContainerSlot> containerSlotList = ContainerSlot.findByContainer(uri);

		System.out.println("printing slot list");
		if (containerSlotList != null) {
			for (ContainerSlot slot: containerSlotList) {
				System.out.println(slot.getUri() + "  Next: " + slot.getHasNext());
			}
		}

		int currentTotal = -1;
		ContainerSlot lastContainerSlot = null; 

		if (containerSlotList == null || containerSlotList.size() == 0) {
			currentTotal = 0;
		} else {
			currentTotal = containerSlotList.size();
			lastContainerSlot = containerSlotList.get(currentTotal - 1);
			System.out.println("Last slot: " + lastContainerSlot.getUri());
		}

		int newTotal = currentTotal + totNewContainerSlots;

		System.out.println("New total of slots: " + newTotal);
		
		for (int aux = currentTotal + 1; aux <= newTotal; aux++) {
			String auxstr = Utils.adjustedPriority(String.valueOf(aux), 1000);
			String newUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
			String newNextUri = null;
			if (aux + 1 <= newTotal) {
			  String auxNextstr = Utils.adjustedPriority(String.valueOf(aux + 1), 1000);
			  newNextUri = uri + "/" + CONTAINER_SLOT_PREFIX + "/" + auxNextstr;
			}
		    System.out.println("Creating slot: [" + newUri + "]  with next: [" + newNextUri + "]");
			String nullstr = null;
			ContainerSlot.createContainerSlot(uri, newUri, newNextUri, VSTOI.DETECTOR, auxstr, nullstr, nullstr);
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
			System.out.println("NextUri: [" + nextUri + "] updated at [" + lastContainerSlot.getUri() + "]");
			lastContainerSlot.setHasNext(nextUri);
			lastContainerSlot.save();
		}

		containerSlotList = ContainerSlot.findByContainer(uri);
		if (containerSlotList == null) {
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
