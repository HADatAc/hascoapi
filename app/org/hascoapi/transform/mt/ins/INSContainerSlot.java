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

    public static INSGenHelper addByInstrument(INSGenHelper helper, Instrument instrument) {
        if (instrument == null || instrument.getUri() == null) {
            return helper;
        }
        return INSContainerSlot.addByContainer(helper, instrument, instrument, ""); 
    }

    private static INSGenHelper addByContainer(INSGenHelper helper, Instrument instrument, Container container, String id) {
        if (container == null) {
            return helper;
        }
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
                helper = INSContainerSlot.add(helper, instrument, "??" + newId, currentId, "");
            } else {
                ContainerSlot slot = (ContainerSlot)element;
                String hasComponent = "";
                if (slot.getComponent() != null && slot.getComponent().getUri() != null) {
                    hasComponent = URIUtils.replaceNameSpaceEx(slot.getComponent().getUri());
                }
                helper = INSContainerSlot.add(helper, instrument, String.valueOf(componentCount++), currentId, hasComponent);
            }
        }
        if (containers.size() > 0) {
            for (int pos = 0; pos < containers.size(); pos++) {
                helper = INSContainerSlot.addByContainer(helper, instrument, containers.get(pos), id + INSContainerSlot.numberToLetter(pos + 1));
            }
        }
        return helper;

    }

    private static String numberToLetter(int number) {
        if (number < 1 || number > 26) {
            return "Z";
        }
        return String.valueOf((char) ('A' + number - 1));
    }

    private static INSGenHelper add(INSGenHelper helper, Instrument instrument, String originalId, String belongsTo, String hasComponent) {

        if (instrument == null   || instrument.getUri() == null || 
            originalId == null   || originalId.isEmpty()        || 
            belongsTo == null    || belongsTo.isEmpty()) { 
            return helper;
        }

        // Get the "ContainerSlots" sheet
        Sheet containerSlotSheet = helper.workbook.getSheet(INSGen.CONTAINER_SLOTS);

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

        return helper;
    }

}
