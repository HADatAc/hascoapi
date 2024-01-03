package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.annotations.ReversedPropertyField;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;

@JsonFilter("virtualColumnFilter")
public class VirtualColumn extends HADatAcClass implements Comparable<VirtualColumn> {

    static String className = "hasco:VirtualColumn";

    public List<VirtualColumn> virtualColumns;

    //@ReversedPropertyField(uri="hasco:hasVirtualColumn")
    //private String studyUri = "";
    
    @PropertyField(uri="hasco:isMemberOf")
    private String isMemberOf = "";
    
    @PropertyField(uri="hasco:hasGroundingLabel")
    private String hasGroundingLabel = "";
    
    @PropertyField(uri="hasco:hasSOCReference")
    private String hasSOCReference = "";

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail = "";

    private VirtualColumn() {
        super(className);
        virtualColumns = new ArrayList<VirtualColumn>();
    }

    public VirtualColumn(
            String studyUri,
            String hasGroundingLabel,
            String hasSOCReference) {
        super(className);
        String vcUri="";
        if(studyUri.contains("SSD")){
            vcUri = studyUri.replace("SSD", "VC") + "-" + hasSOCReference.replace("??", "");
        }
        if (studyUri.contains("STD")){
            vcUri = studyUri.replace("STD", "VC") + "-" + hasSOCReference.replace("??", "");
        }
        if (studyUri.contains("ST")){
            vcUri = studyUri.replace("ST", "VC") + "-" + hasSOCReference.replace("??", "");
        }
        this.setUri(vcUri);
        this.setIsMemberOf(studyUri);
        this.setGroundingLabel(hasGroundingLabel);
        this.setSOCReference(hasSOCReference);
        virtualColumns = new ArrayList<VirtualColumn>();
    }

    @Override
    public String getUri() {
        return uri;
    }
    
    @Override
    public void setUri(String uri) {
        this.uri = uri;
    }
    
    public Study getStudy() {
        if (isMemberOf == null || isMemberOf.isEmpty()) {
            return null;
        }
        return Study.find(isMemberOf);
    }
    
    //public String getStudyUri() {
    //    return studyUri;
    //}

    //public void setStudyUri(String studyUri) {
    //    this.studyUri = studyUri;
    //}
    
    public String getIsMemberOf() {
        return isMemberOf;
    }

    public void setIsMemberOf(String isMemberOf) {
        this.isMemberOf = isMemberOf;
    }
    
    public String getGroundingLabel() {
        return hasGroundingLabel;
    }
    
    public void setGroundingLabel(String hasGroundingLabel) {
        this.hasGroundingLabel = hasGroundingLabel;
    }
    
    public String getSOCReference() {
        return hasSOCReference;
    }
    
    public void setSOCReference(String hasSOCReference) {
        this.hasSOCReference = hasSOCReference;
    }
    
    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }
    
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }
    
    public static List<VirtualColumn> findVCsByStudy(String studyUri) {
        if (studyUri == null) {
            return null;
        }
        System.out.println("findVCsByStudy: studyUri = [" + studyUri + "]");
        List<VirtualColumn> vcList = new ArrayList<VirtualColumn>();

        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                "SELECT ?uri WHERE { \n" + 
                "   ?uri hasco:isMemberOf <" + studyUri + "> . \n" +
                "   ?uri hasco:hascoType hasco:VirtualColumn . \n" +
                " } ";
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            if (soln != null && 
                soln.getResource("uri") != null &&
                soln.getResource("uri").getURI() != null) { 
                String vcUri = soln.getResource("uri").getURI();
                //System.out.println("VirtualColumn: findByStudyUri() : " + soln.getResource("uri").getURI());
                VirtualColumn vc = VirtualColumn.find(vcUri);
                vcList.add(vc);
            }
        }
        System.out.println("findVCsByStudy: total results is " + vcList.size());
        return vcList;
    }

    /*
    public static Map<String,String> getMap() {
        List<VirtualColumn> list = find();
        Map<String,String> map = new HashMap<String,String>();
        for (VirtualColumn vc: list) 
            map.put(vc.getUri(),vc.getLabel());
        return map;
    }
    */
    
    public static List<String> getSubclasses(String uri) {
        List<String> subclasses = new ArrayList<String>();
        
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() 
                + " SELECT ?uri WHERE { \n"
                + " ?uri rdfs:subClassOf* <" + uri + "> . \n"
                + " } \n";

        //System.out.println("queryString: " + queryString);
        
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            subclasses.add(soln.get("uri").toString());
        }
        
        return subclasses;
    }

    public static VirtualColumn find(String studyUri, String SOCReference) {
        String vcUri="";
        if(studyUri.contains("SSD")){
            vcUri = studyUri.replace("SSD", "VC") + "-" + SOCReference.replace("??", "");
        }
        if (studyUri.contains("STD")){
            vcUri = studyUri.replace("STD", "VC") + "-" + SOCReference.replace("??", "");
        }
        return VirtualColumn.find(vcUri);
    }
    
    public static VirtualColumn find(String uri) {
        if ("".equals(uri.trim())) {
            return null;
        }
        
		VirtualColumn vc = new VirtualColumn();
	    Statement statement;
	    RDFNode object;
	    
	    String queryString = "DESCRIBE <" + uri + ">";
	    Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
                CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();

		if (!stmtIterator.hasNext()) {
			return null;
		} 
		
		while (stmtIterator.hasNext()) {
		    statement = stmtIterator.next();
		    object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			if (uri != null && !uri.isEmpty()) {
				if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                    // prefer longer one
                    if (vc.getLabel() != null && str.length() > vc.getLabel().length()) {
                        vc.setLabel(str);
                    }
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					vc.setTypeUri(str); 
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					vc.setHascoTypeUri(str);
				} else if (statement.getPredicate().getURI().equals(HASCO.IS_MEMBER_OF)) {
					vc.setIsMemberOf(str);
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					vc.setComment(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_GROUNDING_LABEL)) {
                    vc.setGroundingLabel(str);
                } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SOC_REFERENCE)) {
                    vc.setSOCReference(str);
                //} else if (statement.getPredicate().getURI().equals(HASCO.HAS_VIRTUAL_COLUMN)) {
                //    vc.setStudyUri(str);
                } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                    vc.setHasSIRManagerEmail(str);
                }
            }

        }
        
        vc.setUri(uri);
        vc.setLocalName(uri.substring(uri.indexOf('#') + 1));

        return vc;
    }

    @Override
    public int compareTo(VirtualColumn another) {
        if (this.getLabel() != null && another.getLabel() != null) {
            return this.getLabel().compareTo(another.getLabel());
        }
        return this.getLocalName().compareTo(another.getLocalName());
    }

    @Override
    public boolean saveToTripleStore() {
        return super.saveToTripleStore();
    }
    
    @Override
    public void deleteFromTripleStore() {
        super.deleteFromTripleStore();
    }

    @Override
    public void save() {
        System.out.println("saving with Label: " + this.label);
        saveToTripleStore();
    }
    
    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}

