package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.FirstLabel;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.VSTOI;

public class FieldOfView extends HADatAcThing implements Comparable<FieldOfView> {

    @PropertyField(uri="hasco:hasGeometry")
    private String geometry;

    @PropertyField(uri="hasco:isFieldOfVisionOf")
    private String isFOVOf;

    @PropertyField(uri="hasco:hasFirstCoordinate")
    private Float firstParameter;

    @PropertyField(uri="hasco:hasFirstCoordinateUnit")
    private String firstParameterUnit;

    @PropertyField(uri="hasco:hasFirstCoordinateCharacteristic")
    private String firstParameterCharacteristic;

    @PropertyField(uri="hasco:hasSecondCoordinate")
    private Float secondParameter;

    @PropertyField(uri="hasco:hasSecondCoordinateUnit")
    private String secondParameterUnit;

    @PropertyField(uri="hasco:hasSecondCoordinateCharacteristic")
    private String secondParameterCharacteristic;

    @PropertyField(uri="hasco:hasThirdCoordinate")
    private Float thirdParameter;

    @PropertyField(uri="hasco:hasThirdCoordinateUnit")
    private String thirdParameterUnit;

    @PropertyField(uri="hasco:hasThirdCoordinateCharacteristic")
    private String thirdParameterCharacteristic;

    @PropertyField(uri="vstoi:hasVersion")
	private String hasVersion;

	@PropertyField(uri="vstoi:hasSIRManagerEmail")
	private String hasSIRManagerEmail;

    public FieldOfView(String uri,
                       String typeUri,
                       String label,
                       String comment) {
        this.uri = uri;
        this.typeUri = typeUri;
        this.label = label;
        this.comment = comment;
    }

    public FieldOfView() {
        this.uri = "";
        this.typeUri = "";
        this.label = "";
        this.comment = "";
        this.geometry = "";
        this.isFOVOf = "";
    }

    public String getGeometry() {
        return geometry;
    }
    public void setGeometry(String geometry) {
        this.geometry = geometry;
    }
    public String getIsFOVOf() {
        return isFOVOf;
    }
    public void setIsFOVOf(String isFOVOf) {
        this.isFOVOf = isFOVOf;
    }
    public Float getFirstParameter() {
        return firstParameter;
    }
    public void setFirstParameter(Float firstParameter) {
        this.firstParameter = firstParameter;
    }
    public String getFirstParameterUnit() {
        return firstParameterUnit;
    }
    public String getFirstParameterUnitLabel() {
        if (firstParameterUnit == null || firstParameterUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(firstParameterUnit);
    }

    public void setFirstParameterUnit(String firstParameterUnit) {
        this.firstParameterUnit = firstParameterUnit;
    }

    public String getFirstParameterCharacteristic() {
        return firstParameterCharacteristic;
    }

    public String getFirstParameterCharacteristicLabel() {
        if (firstParameterCharacteristic == null || firstParameterCharacteristic.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(firstParameterCharacteristic);
    }

    public void setFirstParameterCharacteristic(String firstParameterCharacteristic) {
        this.firstParameterCharacteristic = firstParameterCharacteristic;
    }

    public Float getSecondParameter() {
        return secondParameter;
    }

    public void setSecondParameter(Float secondParameter) {
        this.secondParameter = secondParameter;
    }

    public String getSecondParameterUnit() {
        return secondParameterUnit;
    }

    public String getSecondParameterUnitLabel() {
        if (secondParameterUnit == null || secondParameterUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(secondParameterUnit);
    }

    public void setSecondParameterUnit(String secondParameterUnit) {
        this.secondParameterUnit = secondParameterUnit;
    }

    public String getSecondParameterCharacteristic() {
        return secondParameterCharacteristic;
    }

    public String getSecondParameterCharacteristicLabel() {
        if (secondParameterCharacteristic == null || secondParameterCharacteristic.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(secondParameterCharacteristic);
    }

    public void setSecondParameterCharacteristic(String secondParameterCharacteristic) {
        this.secondParameterCharacteristic = secondParameterCharacteristic;
    }

    public Float getThirdParameter() {
        return thirdParameter;
    }

    public void setThirdParameter(Float thirdParameter) {
        this.thirdParameter = thirdParameter;
    }

    public String getThirdParameterUnit() {
        return thirdParameterUnit;
    }

    public String getThirdParameterUnitLabel() {
        if (thirdParameterUnit == null || thirdParameterUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(thirdParameterUnit);
    }

    public void setThirdParameterUnit(String thirdParameterUnit) {
        this.thirdParameterUnit = thirdParameterUnit;
    }

    public String getThirdParameterCharacteristic() {
        return thirdParameterCharacteristic;
    }

    public String getThirdParameterCharacteristicLabel() {
        if (thirdParameterCharacteristic == null || thirdParameterCharacteristic.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(thirdParameterCharacteristic);
    }

    public void setThirdParameterCharacteristic(String thirdParameterCharacteristic) {
        this.thirdParameterCharacteristic = thirdParameterCharacteristic;
    }

    public String getHasVersion() {
		return hasVersion;
	}

	public void setHasVersion(String hasVersion) {
		this.hasVersion = hasVersion;
	}
   
	public String getHasSIRManagerEmail() {
		return hasSIRManagerEmail;
	}

	public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
		this.hasSIRManagerEmail = hasSIRManagerEmail;
	}


    @Override
    public boolean equals(Object o) {
        if((o instanceof FieldOfView) && (((FieldOfView)o).getUri().equals(this.getUri()))) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getUri().hashCode();
    }

    public static FieldOfView find(String uri) {
        FieldOfView fov = null;
        Model model;
        Statement statement;
        RDFNode object;

        String queryString = "DESCRIBE <" + uri + ">";
        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.sparqlService(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        model = qexec.execDescribe();

        fov = new FieldOfView();
        StmtIterator stmtIterator = model.listStatements();

        while (stmtIterator.hasNext()) {
            statement = stmtIterator.next();
            object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
            if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
                fov.setLabel(str);
            } else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
                fov.setTypeUri(str); 
            } else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
                fov.setHascoTypeUri(str);
            } else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
                fov.setComment(str);
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_GEOMETRY)) {
                fov.setGeometry(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.IS_FIELD_OF_VIEW_OF)) {
                fov.setIsFOVOf(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_FIRST_COORDINATE)) {
                fov.setFirstParameter(object.asLiteral().getFloat());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_FIRST_COORDINATE_UNIT)) {
                fov.setFirstParameterUnit(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_FIRST_COORDINATE_CHARACTERISTIC)) {
                fov.setFirstParameterCharacteristic(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SECOND_COORDINATE)) {
                fov.setSecondParameter(object.asLiteral().getFloat());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SECOND_COORDINATE_UNIT)) {
                fov.setSecondParameterUnit(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_SECOND_COORDINATE_CHARACTERISTIC)) {
                fov.setSecondParameterCharacteristic(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_THIRD_COORDINATE)) {
                fov.setThirdParameter(object.asLiteral().getFloat());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_THIRD_COORDINATE_UNIT)) {
                fov.setThirdParameterUnit(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(HASCO.HAS_THIRD_COORDINATE_CHARACTERISTIC)) {
                fov.setThirdParameterCharacteristic(object.asResource().getURI());
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_VERSION)) {
                fov.setHasVersion(str);
            } else if (statement.getPredicate().getURI().equals(VSTOI.HAS_SIR_MANAGER_EMAIL)) {
                fov.setHasSIRManagerEmail(str);
            }
        }

        fov.setUri(uri);

        return fov;
    }

    public static List<FieldOfView> find() {
        List<FieldOfView> fovs = new ArrayList<FieldOfView>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri a hasco:FieldOfView ." +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
            FieldOfView fov = find(soln.getResource("uri").getURI());
            fovs.add(fov);
        }

        java.util.Collections.sort((List<FieldOfView>) fovs);

        return fovs;
    }

    @Override
    public int compareTo(FieldOfView another) {
        return this.getLabel().compareTo(another.getLabel());
    }

    @Override
    public void save() {
        //System.out.println("Saving platform [" + uri + "]");
        saveToTripleStore();
    }

    @Override
    public void delete() {
        deleteFromTripleStore();
    }

}
