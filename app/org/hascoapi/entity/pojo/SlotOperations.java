package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

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

import static org.hascoapi.Constants.*;

public class SlotOperations  {
    
    private static SlotListElement findSlotListElement(String uri) {
        ContainerSlot containerSlot = ContainerSlot.find(uri);
        if (containerSlot == null) {
            Subcontainer subcontainer = Subcontainer.find(uri);
            if (subcontainer == null) {
                return null;
            } else {
                return (SlotListElement)subcontainer;
            }
        }
        return (SlotListElement)containerSlot;    
    }

    public static boolean moveUp(String uri) {
        SlotListElement current = findSlotListElement(uri);
        if (current == null) {
            System.out.println("[ERROR] SlotOperations.moveUp(): could not retrieve CURRENT.");
            return false;
        }

        if (current.getHasPrevious() == null) {
            return false;
        }
        SlotListElement previous = findSlotListElement(current.getHasPrevious());
        SlotListElement next = findSlotListElement(current.getHasNext());

        if (previous == null) {
            System.out.println("[ERROR] SlotOperations.moveUp(): could not retrieve PREVIOUS.");
            return false;
        } else if (next == null) {
            System.out.println("[ERROR] SlotOperations.moveUp(): could not retrieve NEXT.");
            return false;
        }

        // set previuos
        previous.setHasNext(next.getUri()); 
        previous.setHasPrevious(current.getUri());
        previous.save();

        // set current
        current.setHasNext(previous.getUri());
        current.setHasPrevious(previous.getHasPrevious());
        current.save();

        // set next
        if (next != null) {
            next.setHasPrevious(previous.getUri());
            next.save();
        } 

        // is the first of the list. Needs to update parent container
        if (current.getHasPrevious() == null) {
            Container parent = Container.find(current.getBelongsTo());
            parent.setHasFirst(current.getUri());
            parent.save();
        }

        return true;
    }

    public static boolean moveDown(String uri) {
        SlotListElement current = findSlotListElement(uri);
        if (current == null) {
            System.out.println("[ERROR] SlotOperations.moveUp(): could not retrieve CURRENT.");
            return false;
        }

        if (current.getHasNext() == null) {
            return false;
        }

        SlotListElement previous = findSlotListElement(current.getHasPrevious());
        SlotListElement next = findSlotListElement(current.getHasNext());

        if (previous == null) {
            System.out.println("[ERROR] SlotOperations.moveUp(): could not retrieve PREVIOUS.");
            return false;
        } else if (next == null) {
            System.out.println("[ERROR] SlotOperations.moveUp(): could not retrieve NEXT.");
            return false;
        }

        SlotListElement nextnext = findSlotListElement(next.getHasNext());

        // was the first of the list. Needs to update parent container
        if (current.getHasPrevious() == null) {
            Container parent = Container.find(current.getBelongsTo());
            parent.setHasFirst(next.getUri());
            parent.save();
        }

        // set previous
        previous.setHasNext(next.getUri());
        previous.save();

        // set current 
        current.setHasNext(next.getHasNext());
        current.setHasPrevious(next.getUri());
        current.save();

        // set next
        next.setHasNext(current.getUri()); 
        next.setHasPrevious(previous.getUri());
        next.save();

        // set nextnext
        if (nextnext != null) {
            nextnext.setHasPrevious(current.getUri());
            nextnext.save();
        }

        return true;
    }

}

