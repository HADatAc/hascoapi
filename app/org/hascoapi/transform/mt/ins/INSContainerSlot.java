package org.hascoapi.transform.mt.ins;

import java.util.ArrayList;
import java.util.List;
import org.hascoapi.entity.pojo.Container;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.SlotElement;
import org.hascoapi.entity.pojo.Subcontainer;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSContainerSlot {

    public static Workbook addByInstrument(Workbook workbook, Instrument instrument) {
        if (instrument == null || instrument.getUri() == null) {
            return workbook;
        }
        return INSContainerSlot.addByContainer(workbook, instrument, instrument, ""); 
    }

    private static Workbook addByContainer(Workbook workbook, Instrument instrument, Container container, String id) {
        List<Container> containers = new ArrayList<Container>(); 
        int componentCount = 0;
        int subcontainerCount = 0;
        List<SlotElement> elements = Container.getSlotElements(container);
        String currentId = id;
        if (id.equals("")) {
            currentId = URIUtils.replaceNameSpaceEx(instrument.getUri());
        } else {
            currentId = "??" + id;
        }
        for (SlotElement element: elements) {
            if (element instanceof Subcontainer) {
                String newId = id + INSContainerSlot.numberToLetter(1 + subcontainerCount++);
                containers.add((Container)element);
                workbook = INSContainerSlot.add(workbook, instrument, "??" + newId, currentId, "");
            } else {
                ContainerSlot slot = (ContainerSlot)element;
                String hasComponent = "";
                if (slot.getComponent() != null && slot.getComponent().getUri() != null) {
                    hasComponent = URIUtils.replaceNameSpaceEx(slot.getComponent().getUri());
                }
                workbook = INSContainerSlot.add(workbook, instrument, String.valueOf(componentCount++), currentId, hasComponent);
            }
        }
        if (containers.size() > 0) {
            for (int pos = 0; pos < containers.size(); pos++) {
                workbook = INSContainerSlot.addByContainer(workbook, instrument, containers.get(pos), id + INSContainerSlot.numberToLetter(pos + 1));
            }
        }
        return workbook;

    }

    private static String numberToLetter(int number) {
        if (number < 1 || number > 26) {
            return "Z";
        }
        return String.valueOf((char) ('A' + number - 1));
    }

    private static Workbook add(Workbook workbook, Instrument instrument, String originalId, String belongsTo, String hasComponent) {

        if (instrument == null   || instrument.getUri() == null || 
            originalId == null   || originalId.isEmpty()        || 
            belongsTo == null    || belongsTo.isEmpty()) { 
            return workbook;
        }

        // Get the "ContainerSlots" sheet
        Sheet containerSlotSheet = workbook.getSheet(INSGen.CONTAINER_SLOTS);

        // Calculate the index for the new row
        int rowIndex = containerSlotSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = containerSlotSheet.createRow(rowIndex);

        // 0 "hinstrument"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(instrument.getUri()));

        // "hasco:originalID"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(originalId);  

        // "vstoi:belongsTo"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(belongsTo);  

        // "vstoi:hasComponent"
        Cell cell4 = newRow.createCell(3);
        if (hasComponent != null && !hasComponent.isEmpty()) {
            cell4.setCellValue(URIUtils.replaceNameSpaceEx(hasComponent));
        } else {
            cell4.setCellValue("");
        }  

        return workbook;
    }

}
