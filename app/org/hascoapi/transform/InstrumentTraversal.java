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
import java.util.StringTokenizer;

public class InstrumentTraversal {

	public static int updateStatusRecursive(String uri) {
		Instrument instr = Instrument.find(uri);
		if (instr == null) {
			return -1;
		}
		int total = 0;
		return traverseContainer(total,(Container)instr);
	}


	private static int traverseContainer(int total, Container container) {
		//String html = "Printing subcontainer " + subcontainer.getLabel() + "<br>";
		total++;

		List<SlotElement> slots = container.getSlotElements();
		if (slots == null || slots.size() <= 0) {
			return total;
		} else {
			// System.out.println("Renderings.java: total containerSlots: " +
			// instr.getSlotElements().size());
			for (SlotElement slotElement: slots) {
				if (slotElement instanceof ContainerSlot) {
					ContainerSlot containerSlot = (ContainerSlot)slotElement;
					Detector detector = containerSlot.getDetector();
					if (detector != null) {
						total++;
						if (detector.getDetectorStem() != null && detector.getDetectorStem().getHasContent() != null) {
							total++;
						}
						Codebook codebook = detector.getCodebook();
						if (codebook != null) {
							total++; 
							List<CodebookSlot> cbslots = codebook.getCodebookSlots();
							if (cbslots != null && cbslots.size() > 0) {
								for (CodebookSlot cbslot : cbslots) {
									if (cbslot.getResponseOption() != null) {
										ResponseOption responseOption = cbslot.getResponseOption();
										if (responseOption != null && responseOption.getHasContent() != null) {
											total++;
										}
									}
								}
							}
						}
					}
				} else if (slotElement instanceof Subcontainer) {
					Subcontainer subsubcontainer = (Subcontainer)slotElement;
					total += traverseContainer(total, subsubcontainer);
				}
			}
		}

		return total;
	}

}
