package org.hascoapi.transform;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import org.hascoapi.Constants;
import org.hascoapi.console.controllers.restapi.URIPage;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.vocabularies.VSTOI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;

public class InstrumentTraversal {

	public static int updateStatusRecursive(String uri, String newStatus) {
		Instrument instr = Instrument.find(uri);
		System.out.println("Instrument's URI: ["+ uri + "]");
		List<String> list = new ArrayList<String>();
		if (instr == null) {
			return -1;
		}
		int total = 0;
		list.addAll(traverseContainer(list, (Container)instr));
		Set<String> set = new HashSet<>(list);
        List<String> uniqueList = new ArrayList<>(set);
		for (String str: uniqueList) {
			System.out.println("Elements's URI: ["+ str + "]");
 			HADatAcThing object = URIPage.objectFromUri(str);
			if (object instanceof Instrument) {
				Instrument instrument = (Instrument)object;
				String oldStatus = instrument.getHasStatus();
				System.out.println("OldStatus:  ["+ oldStatus + "]");
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					instrument.setHasStatus(newStatus);
					instrument.save();
				}
			} else if (object instanceof Actuator) {
				Actuator actuator = (Actuator)object;
				String oldStatus = actuator.getHasStatus();
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					actuator.setHasStatus(newStatus);
					actuator.save();
				}
			} else if (object instanceof ActuatorStem) {
				ActuatorStem actuatorStem = (ActuatorStem)object;
				String oldStatus = actuatorStem.getHasStatus();
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					actuatorStem.setHasStatus(newStatus);
					actuatorStem.save();
				}
			} else if (object instanceof Detector) {
				Detector detector = (Detector)object;
				String oldStatus = detector.getHasStatus();
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					detector.setHasStatus(newStatus);
					detector.save();
				}
			} else if (object instanceof DetectorStem) {
				DetectorStem detectorStem = (DetectorStem)object;
				String oldStatus = detectorStem.getHasStatus();
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					detectorStem.setHasStatus(newStatus);
					detectorStem.save();
				}
			} else if (object instanceof Codebook) {
				Codebook codebook = (Codebook)object;
				String oldStatus = codebook.getHasStatus();
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					codebook.setHasStatus(newStatus);
					codebook.save();
				}
			} else if (object instanceof ResponseOption) {
				ResponseOption responseOption = (ResponseOption)object;
				String oldStatus = responseOption.getHasStatus();
				if (!oldStatus.equals(VSTOI.CURRENT) && !oldStatus.equals(VSTOI.DEPRECATED)) {					
					responseOption.setHasStatus(newStatus);
					responseOption.save();
				}
			}
		}
		System.out.println("Number of elements:" + uniqueList.size());
		return uniqueList.size();
	}

	private static List<String> traverseContainer(List<String> list, Container container) {
		System.out.println("  - Container: " + container.getUri());
		if (!list.contains(container.getUri())) {
			list.add(container.getUri());
		}

		List<SlotElement> slots = container.getSlotElements();
		if (slots == null || slots.size() <= 0) {
			return list;
		} else {
			// System.out.println("Renderings.java: total containerSlots: " +
			// instr.getSlotElements().size());
			for (SlotElement slotElement: slots) {
				if (slotElement instanceof ContainerSlot) {
					ContainerSlot containerSlot = (ContainerSlot)slotElement;
					Component component = containerSlot.getComponent();
					if (component != null) {
						System.out.println("    - Component: " + component.getUri());
						if (!list.contains(component.getUri())) {
							list.add(component.getUri());
						}
						if (component.getHascoTypeUri().equals(VSTOI.DETECTOR)) {
							Detector detector = (Detector)component;
							if (detector.getDetectorStem() != null && detector.getDetectorStem().getHasContent() != null) {
								System.out.println("      - Detector Stem: " + detector.getDetectorStem().getUri());
								if (!list.contains(detector.getDetectorStem().getUri())) {
									list.add(detector.getDetectorStem().getUri());
								}
							}
						}
						if (component.getHascoTypeUri().equals(VSTOI.ACTUATOR)) {
							Actuator actuator = (Actuator)component;
							if (actuator.getActuatorStem() != null && actuator.getActuatorStem().getHasContent() != null) {
								System.out.println("      - Actuator Stem: " + actuator.getActuatorStem().getUri());
								if (!list.contains(actuator.getActuatorStem().getUri())) {
									list.add(actuator.getActuatorStem().getUri());
								}
							}
						}
						Codebook codebook = component.getCodebook();
						if (codebook != null) {
						    System.out.println("      - Codebook: " + codebook.getUri());
							if (!list.contains(codebook.getUri())) {
								list.add(codebook.getUri());
							}
							List<CodebookSlot> cbslots = codebook.getCodebookSlots();
							if (cbslots != null && cbslots.size() > 0) {
								for (CodebookSlot cbslot : cbslots) {
									if (cbslot.getResponseOption() != null) {
										ResponseOption responseOption = cbslot.getResponseOption();
										if (responseOption != null && responseOption.getHasContent() != null) {
						    				System.out.println("        - ResponseOption: " + responseOption.getUri());
											if (!list.contains(responseOption.getUri())) {
												list.add(responseOption.getUri());
											}
										}
									}
								}
							}
						}
					}
				} else if (slotElement instanceof Subcontainer) {
					Subcontainer subsubcontainer = (Subcontainer)slotElement;
					list.addAll(traverseContainer(list, subsubcontainer));
				}
			}
		}

		return list;
	}

	public static List<String> retrieveInstrumentComponents(String uri) {
		Instrument instr = Instrument.find(uri);
		List<String> list = new ArrayList<String>();
		if (instr == null) {
			return list;
		}
		list.addAll(traverseContainerComponent(list, (Container)instr));
		Set<String> set = new HashSet<>(list);
        List<String> uniqueList = new ArrayList<>(set);
		return uniqueList;
	}

	private static List<String> traverseContainerComponent(List<String> list, Container container) {
		//System.out.println("  - Container: " + container.getUri());
		List<SlotElement> slots = container.getSlotElements();
		if (slots == null || slots.size() <= 0) {
			return list;
		} else {
			for (SlotElement slotElement: slots) {
				if (slotElement instanceof ContainerSlot) {
					ContainerSlot containerSlot = (ContainerSlot)slotElement;
					Component component = containerSlot.getComponent();
					if (component != null) {
						System.out.println("    - Component: " + component.getUri());
						if (!list.contains(component.getUri())) {
							list.add(component.getUri());
						}
					}
				} else if (slotElement instanceof Subcontainer) {
					Subcontainer subsubcontainer = (Subcontainer)slotElement;
					list.addAll(traverseContainer(list, subsubcontainer));
				}
			}
		}
		return list;
	}

}
