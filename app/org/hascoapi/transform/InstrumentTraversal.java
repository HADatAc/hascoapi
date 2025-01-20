package org.hascoapi.transform;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.xhtmlrenderer.layout.SharedContext;
import org.xhtmlrenderer.pdf.ITextRenderer;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.*;
import org.hascoapi.vocabularies.VSTOI;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;

public class InstrumentTraversal {

	public static int updateStatusRecursive(String uri) {
		Instrument instr = Instrument.find(uri);
		List<String> list = new ArrayList<String>();
		if (instr == null) {
			return -1;
		}
		int total = 0;
		list.addAll(traverseContainer(list, (Container)instr));
		Set<String> set = new HashSet<>(list);
        List<String> uniqueList = new ArrayList<>(set);
		for (String str: uniqueList) {
			System.out.println("* " + str);
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
					Detector detector = containerSlot.getDetector();
					if (detector != null) {
						System.out.println("    - Detector: " + detector.getUri());
						if (!list.contains(detector.getUri())) {
							list.add(detector.getUri());
						}
						if (detector.getDetectorStem() != null && detector.getDetectorStem().getHasContent() != null) {
							System.out.println("      - Detector Stem: " + detector.getDetectorStem().getUri());
							if (!list.contains(detector.getDetectorStem().getUri())) {
								list.add(detector.getDetectorStem().getUri());
							}
						}
						Codebook codebook = detector.getCodebook();
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

}
