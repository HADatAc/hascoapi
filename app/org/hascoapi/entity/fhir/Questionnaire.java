package org.hascoapi.entity.fhir;

import java.util.ArrayList;
import java.util.List;

import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.entity.pojo.Container;

public class Questionnaire {

    private Container container;
    private List<Item> items;

    public Questionnaire(Container container) {
        this.container = container;
        items = new ArrayList<Item>();
        List<ContainerSlot> containerSlots = container.getContainerSlots();
		for (ContainerSlot containerSlot : containerSlots) {
			Detector detector = containerSlot.getDetector();
            Item item = new Item(detector);
            items.add(item);
		}
    }

    public org.hl7.fhir.r4.model.Questionnaire getFHIRObject() {
		org.hl7.fhir.r4.model.Questionnaire questionnaire = new org.hl7.fhir.r4.model.Questionnaire();
		questionnaire.setUrl(container.getUri());
		questionnaire.setTitle(container.getLabel());
		questionnaire.setName(container.getComment());
		questionnaire.setVersion(container.getHasVersion());

		for (Item item : items) {
			questionnaire.addItem(item.getFHIRObject());
		}

		return questionnaire;
	}
}
