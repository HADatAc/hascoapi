package org.sirapi.entity.pojo;

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
import org.sirapi.annotations.PropertyField;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.URIUtils;
import org.sirapi.utils.Utils;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.sirapi.Constants.*;

@JsonFilter("containerFilter")
public abstract class Container extends HADatAcThing implements SIRElement, Comparable<Container> {

	private static final Logger log = LoggerFactory.getLogger(Container.class);

	@PropertyField(uri="vstoi:hasStatus")
	private String hasStatus;

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

	public String gedetectorSlottTypeLabel() {
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

    public List<DetectorSlot> getDetectorSlots() {
    	List<DetectorSlot> detSlots = DetectorSlot.findByContainer(uri);
    	return detSlots;
    }

    public List<Annotation> getAnnotations() {
    	List<Annotation> annotations = Annotation.findByContainer(uri);
    	return annotations;
    }

    @JsonIgnore
	public List<Detector> getDetectors() {
		List<Detector> detectors = new ArrayList<Detector>();
    	List<DetectorSlot> dets = DetectorSlot.findByContainer(uri);
		for (DetectorSlot det : dets) {
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

	public boolean deleteDetectorSlots() {
		if (this.getDetectorSlots() == null || uri == null || uri.isEmpty()) {
			return true;
		}
		List<DetectorSlot> detectorSlots = DetectorSlot.findByContainer(uri);
		if (detectorSlots == null) {
			return true;
		}
		for (DetectorSlot detectorSlot: detectorSlots) {
			detectorSlot.delete();
		}
		detectorSlots = DetectorSlot.findByContainer(uri);
		return (detectorSlots == null);
	}

	public boolean createDetectorSlots(int totDetectorSlots) {
		if (totDetectorSlots <= 0) {
			return false;
		}
		if (this.getDetectorSlots() != null || uri == null || uri.isEmpty()) {
			return false;
		}
		for (int aux=1; aux <= totDetectorSlots; aux++) {
			String auxstr = Utils.adjustedPriority(String.valueOf(aux), totDetectorSlots);
			String newUri = uri + "/" + DETECTOR_SLOT_PREFIX + "/" + auxstr;
			DetectorSlot.createDetectorSlot(uri, newUri, auxstr,null);
		}
		List<DetectorSlot> detectorSlotList = DetectorSlot.findByContainer(uri);
		if (detectorSlotList == null) {
			return false;
		}
		return (detectorSlotList.size() == totDetectorSlots);
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
