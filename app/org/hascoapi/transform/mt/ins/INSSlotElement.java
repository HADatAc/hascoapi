package org.hascoapi.transform.mt.ins;

import java.util.List;
import java.util.ArrayList;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.Container;
import org.hascoapi.entity.pojo.Subcontainer;
import org.hascoapi.entity.pojo.Component;
import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.SlotElement;
import org.hascoapi.utils.URIUtils;
import org.apache.poi.ss.usermodel.*;

public class INSSlotElement {

    public static INSGenHelper addByInstrument(INSGenHelper helper, Instrument instrument) {
        if (instrument == null || instrument.getUri() == null) {
            return helper;
        }
        return INSSlotElement.addByContainer(helper, instrument, (Container)instrument); 
    }

    private static INSGenHelper addByContainer(INSGenHelper helper, Instrument instrument, Container container) {
        if (container == null) {
            return helper;
        }
        List<Container> containers = new ArrayList<Container>(); 
        List<SlotElement> elements = Container.getSlotElements(container);
        for (SlotElement element: elements) {
            if (element instanceof Subcontainer) {
                containers.add((Container)element);
            } 
            helper = INSSlotElement.add(helper, element);
        }
        if (containers.size() > 0) {
            for (Container subcontainer: containers) {
                helper = INSSlotElement.addByContainer(helper, instrument, subcontainer);
            }
        }
        return helper;

    }

    private static INSGenHelper add(INSGenHelper helper, SlotElement slotElement) {

        if (helper == null) {
            System.out.println("[ERROR] INSSlotElement: helper is null");
            return helper;
        }

        if (helper.workbook == null) {
            System.out.println("[ERROR] INSSlotElement: helper's workbook is null");
            return helper;
        }

        if (slotElement == null) {
            return helper;
        }

        // Get the "SlotElements" sheet
        Sheet slotElementSheet = helper.workbook.getSheet(INSGen.SLOT_ELEMENTS);

        // Calculate the index for the new row
        int rowIndex = slotElementSheet.getLastRowNum() + 1;

        // Create the new row
        Row newRow = slotElementSheet.createRow(rowIndex);

        // 0 "hasURI"
        Cell cell1 = newRow.createCell(0);
        cell1.setCellValue(URIUtils.replaceNameSpaceEx(slotElement.getUri()));

        // "hasco:hascoType"
        Cell cell2 = newRow.createCell(1);
        cell2.setCellValue(URIUtils.replaceNameSpaceEx(slotElement.getHascoTypeUri()));  

        // "vstoi:belongsTo"
        Cell cell3 = newRow.createCell(2);
        cell3.setCellValue(URIUtils.replaceNameSpaceEx(slotElement.getBelongsTo()));  

        // "vstoi:hasComponent"
        String hasFirst = "";
        Cell cell4 = newRow.createCell(3);
        String componentUri = "";
        if (slotElement instanceof ContainerSlot) {
            componentUri = ((ContainerSlot)slotElement).getHasComponent();
            if (componentUri == null) {
                componentUri = "";
            } else {
                Component component = Component.find(componentUri);
                if (component instanceof Detector) {
                    Detector detector = (Detector)component;
                    helper.dets.put(detector.getUri(),detector);
                } else if (component instanceof Actuator) {
                    Actuator actuator = (Actuator)component; 
                    helper.acts.put(actuator.getUri(),actuator);
                }
            }
        } else {
            String subcontainerUri = ((Subcontainer)slotElement).getUri();
            if (subcontainerUri != null) {
                Subcontainer subcontainer = Subcontainer.find(subcontainerUri);
                hasFirst = subcontainer.getHasFirst();
            }

        }
        cell4.setCellValue(URIUtils.replaceNameSpaceEx(componentUri)); 

        // "vstoi:hasNext"
        Cell cell5 = newRow.createCell(4);
        if (slotElement.getHasNext() != null) {
            cell5.setCellValue(URIUtils.replaceNameSpaceEx(slotElement.getHasNext()));  
        } else {
            cell5.setCellValue("");  
        }

        // "vstoi:hasPrevious"
        Cell cell6 = newRow.createCell(5);
        if (slotElement.getHasPrevious() != null) {
           cell6.setCellValue(URIUtils.replaceNameSpaceEx(slotElement.getHasPrevious()));  
        } else {
            cell6.setCellValue("");  
        }

        // "vstoi:hasFirst"
        Cell cell7 = newRow.createCell(6);
        if (hasFirst != null && !hasFirst.isEmpty()) {
            cell7.setCellValue(URIUtils.replaceNameSpaceEx(hasFirst));  
        } else {
            cell7.setCellValue("");  
        }

        // "vstoi:hasPriority"
        Cell cell8 = newRow.createCell(7);
        cell8.setCellValue(slotElement.getHasPriority());  

        // "rdfs:label"
        Cell cell9 = newRow.createCell(8);
        if (slotElement.getLabel() != null) {
           cell9.setCellValue(URIUtils.replaceNameSpaceEx(slotElement.getLabel()));  
        } else {
            cell8.setCellValue("");  
        }
        return helper;

    }

}
