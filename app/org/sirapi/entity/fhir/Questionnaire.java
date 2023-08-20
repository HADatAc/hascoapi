package org.sirapi.entity.fhir;

import java.util.ArrayList;
import java.util.List;

import org.sirapi.entity.pojo.DetectorSlot;
import org.sirapi.entity.pojo.Detector;
import org.sirapi.entity.pojo.Instrument;

public class Questionnaire {

    private Instrument instrument;
    private List<Item> items;

    public Questionnaire(Instrument instrument) {
        this.instrument = instrument;
        items = new ArrayList<Item>();
        List<DetectorSlot> detectorSlots = instrument.getDetectorSlots();
		for (DetectorSlot detectorSlot : detectorSlots) {
			Detector detector = detectorSlot.getDetector();
            Item item = new Item(detector);
            items.add(item);
		}
    }

    public org.hl7.fhir.r4.model.Questionnaire getFHIRObject() {
		org.hl7.fhir.r4.model.Questionnaire questionnaire = new org.hl7.fhir.r4.model.Questionnaire();
		questionnaire.setUrl(instrument.getUri());
		questionnaire.setTitle(instrument.getLabel());
		questionnaire.setName(instrument.getComment());
		questionnaire.setVersion(instrument.getHasVersion());

		for (Item item : items) {
			questionnaire.addItem(item.getFHIRObject());
		}

		return questionnaire;
	}
}
