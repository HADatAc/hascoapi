package org.hascoapi.entity.pojo;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.console.controllers.restapi.URIPage;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.TreeNode;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.DCTERMS;
import org.hascoapi.vocabularies.PROV;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.HASCO;

@JsonFilter("hascoClassFilter")
public class HADatAcClass extends HADatAcThing {

    String className = "";

    @PropertyField(uri="rdfs:subClassOf")
    String superUri = "";

    String localName = "";

    public  List<String> isDomainOf = null;
    public  List<String> isRangeOf = null;
    public  List<String> isDisjointWith = null;

    public HADatAcClass () {
        this.className = "";
    }

    public HADatAcClass (String currentClassName) {
        if (currentClassName == null) {
            currentClassName = "";
        }
        this.className = currentClassName;
        this.isDomainOf =  new ArrayList<String>();
        this.isRangeOf =  new ArrayList<String>();
        this.isDisjointWith = new ArrayList<String>();
    }

    public String getClassName() {
        return className;
    }

    public String getSuperUri() {
        return superUri;
    }

    public void setSuperUri(String superUri) {
        if (superUri == null) {
            superUri = "";
        }
        this.superUri = superUri;
    }

    public String getHasStatus() {
        try {
            // Check if the field "hasStatus" exists in the current class
            Field field = this.getClass().getDeclaredField("hasStatus");
            field.setAccessible(true); // Allow access to private fields if needed
            return (String) field.get(this); // Get the field value from the current object
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // If field does not exist or cannot be accessed, return VSTOI.CURRENT
            return VSTOI.CURRENT;
        }
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        if (localName == null) {
            localName = "";
        }
        this.localName = localName;
    }

    public List<String> getIsDomainOf() {
        return isDomainOf;
    }

    public void addDomain(String domainUri) {
        this.isDomainOf.add(domainUri);
    }

    public List<String> getIsRangeOf() {
        return isRangeOf;
    }

    public void addRange(String rangeUri) {
        this.isRangeOf.add(rangeUri);
    }

    public List<String> getIsDisjointWith() {
        return isDisjointWith;
    }

    public void addDisjointWith(String disjointWithUri) {
        this.isDisjointWith.add(disjointWithUri);
    }

    public static int getNumberClasses() {
        String query = "";
        query += NameSpaces.getInstance().printSparqlNameSpaceList();
        query += "SELECT (COUNT(?type) as ?tot) WHERE {" +
                "   ?uri a ?type . " +
                " }";
//        query += "select (COUNT(?categ) as ?tot) where " +
//                " { SELECT ?c (COUNT(?c) as ?categ) " +
//                "     WHERE {" +
//                "             [] a ?c . " +
//                "     } " +
//                " GROUP BY ?c " +
//                " }";

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

    public String getSuperClassLabel() {
        HADatAcClass superType = findGeneric(this.getSuperUri());
        if (superType == null || superType.getLabel() == null) {
            return "";
        }
        return superType.getLabel();
    }

    public List<HADatAcClass> findGeneric() {
        List<HADatAcClass> typeClasses = new ArrayList<HADatAcClass>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* " + className + " . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            HADatAcClass typeClass = findGeneric(soln.getResource("uri").getURI());
            typeClasses.add(typeClass);
        }


        typeClasses.sort(Comparator.comparing(HADatAcClass::getLabel, (label1, label2) -> {
            int compare = label1.compareTo(label2);
            return compare;
        }));

        return typeClasses;
    }

    public HADatAcClass findGeneric(String uri) {
        return HADatAcClass.find(uri);
    }

    public static HADatAcClass find(String classUri) {
 		if (classUri == null || classUri.isEmpty()) {
			return null;
		}
        HADatAcClass typeClass = null;
		// Construct the SELECT query to retrieve named graphs
		String queryString = "SELECT DISTINCT ?graph ?p ?o WHERE { GRAPH ?graph { <" + classUri + "> ?p ?o } }";
		ResultSet resultSet = SPARQLUtils.select(CollectionUtil.getCollectionPath(
        	CollectionUtil.Collection.SPARQL_QUERY), queryString);

		if (!resultSet.hasNext()) {
			return null;
		} else {
            typeClass = new HADatAcClass("");
		}

		// Iterate over results
		while (resultSet.hasNext()) {
			QuerySolution qs = resultSet.next();
			
			// Retrieve the named graph URI
			if (qs.contains("graph")) {
				typeClass.setNamedGraph(qs.get("graph").toString());
				//System.out.println("Graph: " + graphURI);
			}
			
			// Retrieve predicate and object (optional)
			if (qs.contains("p") && qs.contains("o")) {
				String predicate = qs.get("p").toString();
				String object = qs.get("o").toString();
				//System.out.println("Predicate: " + predicate + " | Object: " + object);

                if (predicate.equals(RDFS.LABEL)) {
                    typeClass.setLabel(object);
                } else if (predicate.equals(RDF.TYPE)) {
                    typeClass.setTypeUri(object);
                } else if (predicate.equals(RDFS.SUBCLASS_OF)) {
                    typeClass.setSuperUri(object);
                } else if (predicate.equals(RDFS.DOMAIN)) {
                    typeClass.addDomain(object);
                } else if (predicate.equals(RDFS.RANGE)) {
                    typeClass.addRange(object);
                } else if (predicate.equals(RDFS.DISJOINT_WITH)) {
                    typeClass.addDisjointWith(object);
                } else if (predicate.equals(RDFS.COMMENT) || predicate.equals(PROV.DEFINITION)) {
                    typeClass.setComment(object);
                } else if (predicate.equals(DCTERMS.DESCRIPTION)) {
                    typeClass.setDescription(object);
                } else if (predicate.equals(HASCO.HAS_IMAGE)) {
                    typeClass.setHasImageUri(object);
                } else if (predicate.equals(HASCO.HAS_WEB_DOCUMENT)) {
                    typeClass.setHasWebDocument(object);
                }
            }
        }

        typeClass.setUri(classUri);
        typeClass.setLocalName(classUri.substring(classUri.indexOf('#') + 1));

        return typeClass;
    }

    public static HADatAcClass lightWeightedFind(String classUri) {
        HADatAcClass typeClass = null;
        Statement statement;
        RDFNode subject;
        RDFNode object;

        String queryString = "DESCRIBE <" + classUri + ">";
        Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);

        StmtIterator stmtIterator = model.listStatements();

        // returns null if not statement is found
        if (!stmtIterator.hasNext()) {
            return typeClass;
        }

        typeClass = new HADatAcClass("");

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            subject = statement.getSubject();
            object = statement.getObject();
            //System.out.println("pred: " + statement.getPredicate().getURI());
            String predUri = statement.getPredicate().getURI();
            if (predUri.equals(RDFS.LABEL)) {
                typeClass.setLabel(object.asLiteral().getString());
            } else if (predUri.equals(RDF.TYPE)) {
                String objUri = object.asResource().getURI();
                //System.out.println("obj: " + objUri);
                if (objUri != null && !objUri.equals(classUri)) {
                    typeClass.setTypeUri(objUri);
                }
            } else if (predUri.equals(RDFS.SUBCLASS_OF)) {
                String objUri = object.asResource().getURI();
                //System.out.println("is subClass of [" + objUri + "]");
                if (objUri != null && !objUri.equals(classUri)) {
                    typeClass.setSuperUri(objUri);
                }
            } else if (predUri.equals(RDFS.COMMENT) ||
                       predUri.equals(PROV.DEFINITION)) {
                String textStr = object.asLiteral().getString();
                if (textStr != null) {
                    typeClass.setComment(textStr);
                }
            } else if (predUri.equals(DCTERMS.DESCRIPTION)) {
                 String textStr = object.asLiteral().getString();
                if (textStr != null) {
                    typeClass.setDescription(textStr);
                }
            }
        }

        typeClass.setUri(classUri);
        typeClass.setLocalName(classUri.substring(classUri.indexOf('#') + 1));

        return typeClass;
    }             

    @JsonIgnore
    public String getHierarchyJson() {
        //System.out.println("Inside HADatAcClass's getHierarchyJson: [" + className + "]");
        String q =
                "SELECT ?id ?superId ?label ?superLabel ?comment ?description WHERE { " +
                        "   ?id rdfs:subClassOf* " + className + " . " +
                        "   ?id rdfs:subClassOf ?superId .  " +
                        "   OPTIONAL { ?id rdfs:label ?label . } " +
                        "   OPTIONAL { ?superId rdfs:label ?superLabel . } " +
                        "   OPTIONAL { ?id rdfs:comment ?comment . } " +
                        "   OPTIONAL { ?id dcterms:description ?description . } " +
                        "}";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + q;
            ResultSetRewindable resultsrw = SPARQLUtils.select(CollectionUtil.
                    getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
            ResultSetFormatter.outputAsJSON(outputStream, resultsrw);

            return outputStream.toString("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    @JsonIgnore
    public String getHierarchyJson2() {
        //System.out.println("Inside HADatAcClass's getHierarchyJson: [" + className + "]");
        String q =
                "SELECT ?model ?superModel ?modelName ?superModelName ?comment ?description WHERE { " +
                        "   ?model rdfs:subClassOf* " + className + " . " +
                        "   ?model rdfs:subClassOf ?superModel .  " +
                        "   OPTIONAL { ?model rdfs:label ?modelName . } " +
                        "   OPTIONAL { ?superModel rdfs:label ?superModelName . } " +
                        "   OPTIONAL { ?model rdfs:comment ?comment . } " +
                        "   OPTIONAL { ?model dcterms:description ?description . } " +
                        "}";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + q;
            ResultSetRewindable resultsrw = SPARQLUtils.select(CollectionUtil.
                    getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
            ResultSetFormatter.outputAsJSON(outputStream, resultsrw);

            return outputStream.toString("UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    @JsonIgnore
    public TreeNode getSuperClasses() {
        ArrayList<TreeNode> branchCollection = new ArrayList<TreeNode>();
        if (this.uri == null) {
            // return null;
            return new TreeNode("");
        }
        HADatAcClass current = find(this.uri);
        while (current != null) {
            //System.out.println("Class: " + current.getUri() + " (" + current.getLabel() + ")" +
            //        "  SuperClass: " + current.getSuperUri() + " (" + current.getSuperClassLabel() + ")");
            TreeNode currentBranch = new TreeNode(current.getSuperClassLabel());
            currentBranch.setUri(current.getSuperUri());
            TreeNode childBranch = new TreeNode(current.getLabel());
            childBranch.setUri(current.getUri());
            currentBranch.addChild(childBranch);
            branchCollection.add(currentBranch);
            if (current.getSuperUri() != null && !current.getSuperUri().isEmpty()) {
                current = find(current.getSuperUri());
            } else {
                current = null;
            }
        }
        TreeNode result = buildTree(branchCollection);
        if (result.getChildren() == null || result.getChildren().size() <= 0) {
            return new TreeNode("");
            //return null;
        }
        return result.getChildren().get(0);
    }

    @JsonIgnore
    public TreeNode getSubClasses() {
        String node = null;
        String nodeLabel = null;
        String superNode = null;
        String superNodeLabel = null;
        ArrayList<TreeNode> branchCollection = new ArrayList<TreeNode>();
        String q =
                "SELECT ?id ?superId ?label ?superLabel WHERE { " +
                        "   ?id rdfs:subClassOf* <" + this.uri + "> .  " +
                        "   ?id rdfs:subClassOf ?superId .  " +
                        "   OPTIONAL { ?id rdfs:label ?label } .  " +
                        "   OPTIONAL { ?superId rdfs:label ?superLabel } .  " +
                        "}";
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + q;

            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                node = "";
                label = "";
                superNode = "";
                superNodeLabel = "";
                if (soln != null && soln.getResource("id") != null && soln.getResource("id").getURI() != null) {
                    node  = soln.getResource("id").getURI();
                }
                if (soln != null && soln.getResource("superId") != null && soln.getResource("superId").getURI() != null) {
                    superNode = soln.getResource("superId").getURI();
                }
                if (soln != null && soln.getLiteral("label") != null && soln.getLiteral("label").getString() != null) {
                    nodeLabel = soln.getLiteral("label").getString();
                }
                if (soln != null && soln.getLiteral("superLabel") != null && soln.getLiteral("superLabel").getString() != null) {
                    superNodeLabel = soln.getLiteral("superLabel").getString();
                }
                TreeNode currentBranch = new TreeNode(superNodeLabel);
                currentBranch.setUri(superNode);
                TreeNode childBranch = new TreeNode(nodeLabel);
                childBranch.setUri(node);
                currentBranch.addChild(childBranch);
                branchCollection.add(currentBranch);
            }

            TreeNode result = buildTree(branchCollection);
            if (result.getChildren() == null || result.getChildren().size() <= 0) {
                return new TreeNode("");
            }
            if (result.getChildren().get(0) == null) {
                return new TreeNode("");
            }
            return result.getChildren().get(0);

        } catch (Exception e) {
            e.printStackTrace();
            return new TreeNode("");
        }
    }

    public static List<HADatAcClass> getImmediateSubclasses(String superUri) {
        System.out.println("HADatAcClass.ImmediateSubClasses of [" + superUri + "]");
        List<HADatAcClass> subclasses = new ArrayList<HADatAcClass>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                "   ?uri rdfs:subClassOf <" + superUri + "> .  " +
                "   OPTIONAL { ?uri rdfs:label ?label } " +
                "} " + 
                " ORDER BY ASC(?label) ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            HADatAcClass subclass;
            try {
                subclass = (HADatAcClass)URIPage.objectFromUri(soln.getResource("uri").getURI());
            } catch (Exception e) { 
                subclass = HADatAcClass.find(soln.getResource("uri").getURI());
            }
            if (subclass != null) {
                subclass.setNodeId(HADatAcThing.createUrlHash(subclass.getUri()));
                subclasses.add(subclass);
            }
        }

        return subclasses;
    }
    
    public static List<HADatAcClass> findSuperclasses(String uri) {
        //System.out.println("HADatAcClass.ImmediateSubClasses of [" + superUri + "]");
        List<HADatAcClass> superclasses = new ArrayList<HADatAcClass>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?superuri WHERE { " +
                "   <" + uri + "> rdfs:subClassOf* ?superuri  .  " +
                "   ?superuri rdfs:label ?label . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            HADatAcClass superclass;
            try {
                superclass = (HADatAcClass)URIPage.objectFromUri(soln.getResource("superuri").getURI());
            } catch (Exception e) { 
                superclass = HADatAcClass.find(soln.getResource("superuri").getURI());
            }
            if (superclass != null) {
               superclasses.add(superclass);
            }
        }

        return superclasses;
    }
    
    public static List<HADatAcClass> findSubclassesByKeyword(String superUri, String keyword) {
        //System.out.println("GenericFind.findClassesByKeywordWithPages: " + superclassName + "  " + keyword + " " + pageSize + "  " + offset);
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT DISTINCT ?uri ?label WHERE { " +
                " ?uri rdfs:subClassOf* <" + superUri + "> . " +
                " ?uri rdfs:label ?label . " +
                "   FILTER regex(?label, \"" + keyword + "\", \"i\") " +
                "} " +
                " ORDER BY ASC(?label) ";

        //System.out.println("GenericFind.findInstancesByKeywordWithPages: [" + queryString + "]");
        return findClassesByQuery(queryString);
    }

    public static List<HADatAcClass> findClassesByQuery(String queryString) {
        List<HADatAcClass> list = new ArrayList<HADatAcClass>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);
        if (!resultsrw.hasNext()) {
            return null;
        }
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null) {
                if (soln.getResource("uri") != null && soln.getResource("uri").getURI() != null) {
                    String uri = soln.getResource("uri").getURI();
                    HADatAcClass clazz = HADatAcClass.find(uri);
                    if (clazz != null) {
                        list.add(clazz);
                    }
                } 
            }
        }
        return list;
    }

    @JsonIgnore
    public List<GenericInstance> getInstances() {
        List<GenericInstance> instances = new ArrayList<GenericInstance>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                "SELECT ?uri WHERE { " +
                "   ?uri a <" + this.uri + "> . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            GenericInstance instance = GenericInstance.find(soln.getResource("uri").getURI());
            instances.add(instance);
        }

        instances.sort(Comparator.comparing(GenericInstance::getLabel, (label1, label2) -> {
            int compare = label1.compareTo(label2);
            return compare;
        }));

        return instances;
    }

    @JsonIgnore
    public TreeNode getHierarchy() {
        String node = null;
        String superNode = null;
        String nodeLabel = null;
        ArrayList<TreeNode> branchCollection = new ArrayList<TreeNode>();
        String q =
                "SELECT ?id ?superId ?label WHERE { " +
                        "   ?id rdfs:subClassOf* " + className + " .  " +
                        "   ?id rdfs:subClassOf ?superId .  " +
                        "   ?id rdfs:label ?label .  " +
                        "}";
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + q;

            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                uri = "";
                superUri = "";
                label = "";
                if (soln != null && soln.getResource("id") != null && soln.getResource("id").getURI() != null) {
                    node  = soln.getResource("id").getURI();
                }
                if (soln != null && soln.getResource("superId") != null && soln.getResource("superId").getURI() != null) {
                    superNode = soln.getResource("superId").getURI();
                }
                if (soln != null && soln.getLiteral("label") != null && soln.getLiteral("label").getString() != null) {
                    nodeLabel = soln.getLiteral("label").getString();
                }
                TreeNode currentBranch = new TreeNode(superNode);
                currentBranch.addChild(node);
                branchCollection.add(currentBranch);
            }

            TreeNode result = buildTree(branchCollection);
            if (result.getChildren() == null) {
                return null;
            }
            return result.getChildren().get(0);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return new TreeNode("");
    }

    @JsonIgnore
    public String mapTypeLabelToUri() {
        String nodeUri = null;
        String nodeLabel = null;
        Map<String,String> resp = new HashMap<String,String>();
        String q =
                "SELECT ?id ?label WHERE { " +
                        "   ?id rdfs:subClassOf* " + className + " . " +
                        "   ?id rdfs:label ?label .  " +
                        "}";
        try {
            String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + q;

            ResultSetRewindable resultsrw = SPARQLUtils.select(
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

            while (resultsrw.hasNext()) {
                QuerySolution soln = resultsrw.next();
                if (soln != null && soln.getResource("id") != null && soln.getResource("id").getURI() != null) {
                    nodeUri  = soln.getResource("id").getURI();
                }
                if (soln != null && soln.getLiteral("label") != null && soln.getLiteral("label").getString() != null) {
                    nodeLabel = soln.getLiteral("label").getString();
                    nodeLabel = nodeLabel.replaceAll(" ", "");
                }
                resp.put(nodeLabel, nodeUri);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        String output = "{";
        boolean first = true;
        Iterator<Map.Entry<String, String>> itr = resp.entrySet().iterator();
        while(itr.hasNext()) {
            if (first) {
                first = false;
            } else {
                output = output + ",";
            }
            Map.Entry<String, String> entry = itr.next();
            output = output + "\"" + entry.getKey() + "\":\"" + entry.getValue() + "\"";
        }
        output = output + "}";
        //System.out.println("[" + output + "]");
        return output;
    }

    public static String getLabelByUri(String uri, Class<?> cls) {
        try {
            Method method = cls.getMethod("find", String.class);
            HADatAcClass instance = (HADatAcClass)method.invoke(null, uri);
            if (instance != null) {
                return instance.getLabel();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        return "";
    }

    private TreeNode buildTree(List<TreeNode> inputTree){
        TreeNode resultsTree = new TreeNode("Empty");
        ArrayList<TreeNode> assignedBranches = new ArrayList<TreeNode>();
        int numIterations = 0;
        int maxIterations = 20;
        while (assignedBranches.size() != inputTree.size() && numIterations<maxIterations){
            numIterations++;
            for (TreeNode tn : inputTree){
                if (!assignedBranches.contains(tn)){
                    if (resultsTree.getName().equals("Empty")) {
                        resultsTree = new TreeNode(tn.getName());
                        resultsTree.setUri(tn.getUri());
                        resultsTree.setComment(tn.getComment());
                        resultsTree.setDescription(tn.getDescription());
                        resultsTree.addChild(tn.getChildren().get(0));
                        assignedBranches.add(tn);
                    } else {
                        if (resultsTree.hasValue(tn.getName())!=null){
                            TreeNode branchOfInterest = resultsTree.hasValue(tn.getName());
                            branchOfInterest.addChild(tn.getChildren().get(0));
                            assignedBranches.add(tn);
                        } else {
                            if (tn.hasValue(resultsTree.getName())!=null) {
                                TreeNode newBranch = new TreeNode(tn.getName());
                                newBranch.setUri(tn.getUri());
                                newBranch.setComment(tn.getComment());
                                newBranch.setDescription(tn.getDescription());
                                newBranch.addChild(resultsTree);
                                resultsTree = newBranch;
                                assignedBranches.add(tn);
                            }
                        }
                    }
                }
            }
        }

        return resultsTree;
    }

    public String toJSON() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            String output =  mapper.writeValueAsString(this);
            return output;
        } catch (Exception e) {
            System.out.println("[ERROR] HADatAcClass.toJSON() - Exception message: " + e.getMessage());
        }
        return "";
    }

    @Override
    public void save() { 
        super.saveToTripleStore();
    }

    @Override
    public void delete() {
        super.deleteFromTripleStore();
    }

}

