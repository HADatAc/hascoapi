package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.*;
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

import java.util.ArrayList;
import java.util.List;

import static org.hascoapi.Constants.*;

@JsonFilter("containerSlotFilter")
public class ContainerSlot extends HADatAcThing implements SlotElement, Comparable<ContainerSlot>  {

    @PropertyField(uri="vstoi:belongsTo")
    private String belongsTo;

    @PropertyField(uri="vstoi:hasComponent")
    private String hasComponent;

    @PropertyField(uri="vstoi:hasNext")
    private String hasNext;

    @PropertyField(uri="vstoi:hasPrevious")
    private String hasPrevious;

    @PropertyField(uri="vstoi:hasPriority")
    private String hasPriority;

    public String getBelongsTo() {
        return belongsTo;
    }

    public void setBelongsTo(String belongsTo) {
        this.belongsTo = belongsTo;
    }

    public String getHasComponent() {
        return hasComponent;
    }

    public void setHasComponent(String hasComponent) {
        this.hasComponent = hasComponent;
    }

    public String getHasNext() {
        return hasNext;
    }

    public void setHasNext(String hasNext) {
        this.hasNext = hasNext;
    }

    public String getHasPrevious() {
        return hasPrevious;
    }

    public void setHasPrevious(String hasPrevious) {
        this.hasPrevious = hasPrevious;
    }

    public String getHasPriority() {
        return hasPriority;
    }

    public void setHasPriority(String hasPriority) {
        this.hasPriority = hasPriority;
    }

    public Component getComponent() {
        if (hasComponent == null || hasComponent.isEmpty()) {
            return null;
        }
        GenericInstance genericInstance = GenericInstance.find(hasComponent);
        if (genericInstance.getHascoTypeUri().equals(VSTOI.ACTUATOR)) {
            return Actuator.find(hasComponent);
        } else if (genericInstance.getHascoTypeUri().equals(VSTOI.DETECTOR)) {
            return Detector.find(hasComponent);
        }
        return null;
    }

    public static int getNumberContainerSlots() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?attModel rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?attModel ." +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getNumberContainerSlotsWithComponents() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?model rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?model ." +
                " ?uri vstoi:hasComponent ?component . " +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int getNumberContainerSlotsByContainer(String containerUri) {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += " select (count(?uri) as ?tot) where { " +
                " ?type rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?type ." +
                " ?uri vstoi:belongsTo <" + containerUri + ">. " +
                "}";

        try {
            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);

            if (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                return Integer.parseInt(soln.getLiteral("tot").getString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<ContainerSlot> findByContainerWithPages(String containerUri, int pageSize, int offset) {
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?type . } " +
                " ?uri vstoi:belongsTo <" + containerUri + ">. " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && soln.getResource("uri").getURI() != null) {
                ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("uri").getURI());
                containerSlots.add(containerSlot);
            }
        }
        return containerSlots;
    }

    public static List<ContainerSlot> find() {
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?type rdfs:subClassOf* vstoi:ContainerSlot . " +
                " ?uri a ?type ." +
                "} ";

        return findContainerSlotByQuery(queryString);
    }

    private static List<ContainerSlot> findContainerSlotByQuery(String queryString) {
        List<ContainerSlot> containerSlots = new ArrayList<ContainerSlot>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        if (!resultsrw.hasNext()) {
            return null;
        }

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            ContainerSlot containerSlot = ContainerSlot.find(soln.getResource("uri").getURI());
            containerSlots.add(containerSlot);
        }

        //java.util.Collections.sort((List<ContainerSlot>) containerSlots);
        return containerSlots;

    }

    public static ContainerSlot find(String uri) {
        if (uri == null || uri.isEmpty()) {
			return null;
		}
		ContainerSlot containerSlot = null;

		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + uri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
			containerSlot = new ContainerSlot();
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				containerSlot.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    containerSlot.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    containerSlot.setTypeUri(object);
                } else if (predicate.equals(RDFS.COMMENT)) {
                    containerSlot.setComment(object);
                } else if (predicate.equals(VSTOI.HAS_NEXT)) {
                    containerSlot.setHasNext(object);
                } else if (predicate.equals(VSTOI.HAS_PREVIOUS)) {
                    containerSlot.setHasPrevious(object);
                } else if (predicate.equals(HASCO.HASCO_TYPE)) {
                    containerSlot.setHascoTypeUri(object);
                } else if (predicate.equals(VSTOI.BELONGS_TO)) {
                    containerSlot.setBelongsTo(object);
                } else if (predicate.equals(VSTOI.HAS_COMPONENT)) {
                    containerSlot.setHasComponent(object);
                } else if (predicate.equals(VSTOI.HAS_PRIORITY)){
                    containerSlot.setHasPriority(object);
                }
            }
        }

        containerSlot.setUri(uri);

        if (containerSlot.getHascoTypeUri().equals(VSTOI.CONTAINER_SLOT)) {
            return containerSlot;
        } 
        return null;
    }

	/** 
	 *  Creates a containerslot and includes it as a slot in the slotElement list of the container that it belongs to.
	 *  When deleting a containerslot that is also going to be removed from the slotElement list, use the 
	 *  SlotOperation.deleteSlotElement()
	 */
	public static boolean createContainerSlots(Container container, int totNewContainerSlots) {
		if (totNewContainerSlots <= 0) {
			return false;
		}
            
        //System.out.println("Received this way: belongsTo = [" + container.getBelongsTo() + "]");

		List<SlotElement> slotElements = Container.getSlotElements(container);

		// Compute MAXID of existing container slots in slot elements 
		int maxid = 0;
		if (slotElements != null) {
			for (SlotElement slot: slotElements) {
				if (slot != null) {
					if (slot.getHasPriority() != null &&
					    slot.getHascoTypeUri().equals(VSTOI.CONTAINER_SLOT)) {
						int priority = Integer.parseInt(slot.getHasPriority());
						if (priority > maxid) {
							maxid = priority;
						}
					}
				}
			}
		}

		int currentTotal = -1;
		SlotElement lastSlot = null; 

		if (slotElements == null || slotElements.size() == 0) {
			currentTotal = 0;
		} else {
			currentTotal = slotElements.size();
			lastSlot = slotElements.get(currentTotal - 1);
		}

		int newTotal = currentTotal + totNewContainerSlots;

		for (int aux = 1; aux <= totNewContainerSlots; aux++) {
			String auxstr = Utils.adjustedPriority(String.valueOf(maxid + aux), 1000);
			String newUri = container.getUri() + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
			String newNextUri = null;
			String newPreviousUri = null;
			if (aux + 1 <= totNewContainerSlots) {
			  String auxNextstr = Utils.adjustedPriority(String.valueOf(maxid + aux + 1), 1000);
			  newNextUri = container.getUri() + "/" + CONTAINER_SLOT_PREFIX + "/" + auxNextstr;
			}
			if (aux > 1) {
			  String auxPrevstr = Utils.adjustedPriority(String.valueOf(maxid + aux - 1), 1000);
			  newPreviousUri = container.getUri() + "/" + CONTAINER_SLOT_PREFIX + "/" + auxPrevstr;
			}
		    //System.out.println("Creating slot: [" + newUri + "]  with prev: [" + newPreviousUri + "]  next: [" + newNextUri + "]");
			String nullstr = null;
			ContainerSlot.createContainerSlot(container, newUri, newNextUri, newPreviousUri, auxstr, nullstr);
		}

		// IF THE LIST WAS EMPTY
		if (currentTotal <= 0) {
		    String auxstr = Utils.adjustedPriority("1", 1000);
		  	String firstUri = container.getUri() + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
            //System.out.println("Before update: belongsTo = [" + container.getBelongsTo() + "]");
		  	container.setHasFirst(firstUri);
		    container.save();
            //System.out.println("After update: belongsTo = [" + container.getBelongsTo() + "]");

		// IF THE LIST WAS NOT EMPTY	
		} else {
			String auxstr = Utils.adjustedPriority(String.valueOf(maxid + 1), 1000);
			String nextUri = container.getUri() + "/" + CONTAINER_SLOT_PREFIX + "/" + auxstr;
			if (lastSlot != null) {
				lastSlot.setHasNext(nextUri);
				lastSlot.save();
			}
		}

		return true;
	}

    static public boolean createContainerSlot(Container container, String containerSlotUri, 
                            String containerSlotUriNext, String containerSlotUriPrevious,  
                            String priority, String hasComponent) {
        if (container == null) {
            return false;
        }
        if (priority == null || priority.isEmpty()) {
            return false;
        }
        ContainerSlot containerSlot = new ContainerSlot();
        containerSlot.setUri(containerSlotUri);
        containerSlot.setLabel("ContainerSlot " + priority);
        containerSlot.setTypeUri(VSTOI.CONTAINER_SLOT);
        containerSlot.setHascoTypeUri(VSTOI.CONTAINER_SLOT);
        containerSlot.setComment("ContainerSlot " + priority + " of container with URI " + container.getUri());
        containerSlot.setBelongsTo(container.getUri());
        containerSlot.setHasPriority(priority);
        if (containerSlotUriNext != null && !containerSlotUriNext.isEmpty()) {
            containerSlot.setHasNext(containerSlotUriNext);
        }
        if (containerSlotUriPrevious != null && !containerSlotUriPrevious.isEmpty()) {
            containerSlot.setHasPrevious(containerSlotUriPrevious);
        }
        if (hasComponent != null) {
            containerSlot.setHasComponent(hasComponent);
        }
        containerSlot.save();
        //System.out.println("ContainerSlot.createContainerSlot: creating containerSlot with URI [" + containerSlotUri + "]" );
        return true;
    }

    public boolean updateContainerSlotComponent(Component component) {
        //System.out.println("Called ContainerSlot.updateContainerSlotComponent.");
        //System.out.println("Component: " + component);
        if (component == null) {
            this.setHasComponent(null);
        }
        ContainerSlot newContainerSlot = new ContainerSlot();
        newContainerSlot.setUri(this.uri);
        newContainerSlot.setLabel(this.getLabel());
        newContainerSlot.setTypeUri(this.getTypeUri());
        newContainerSlot.setComment(this.getComment());
        newContainerSlot.setHascoTypeUri(this.getHascoTypeUri());
        newContainerSlot.setBelongsTo(this.getBelongsTo());
        newContainerSlot.setHasPriority(this.getHasPriority());
        newContainerSlot.setHasNext(this.getHasNext());
        newContainerSlot.setHasPrevious(this.getHasPrevious());
        // if component is null, the property setHasComponent is not called. This is how
        // a component is removed from a container slot.
        if (component != null && component.getUri() != null && !component.getUri().isEmpty()) {
            newContainerSlot.setHasComponent(component.getUri());
        } 
        this.delete();
        newContainerSlot.save();
        return true;
    }

    @Override
    public int compareTo(ContainerSlot another) {
        return this.getHasPriority().compareTo(another.getHasPriority());
    }

    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}
