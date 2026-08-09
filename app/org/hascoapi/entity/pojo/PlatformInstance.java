package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
import org.hascoapi.vocabularies.SIO;
import org.hascoapi.vocabularies.VSTOI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hascoapi.Constants.*;

@JsonFilter("platformInstanceFilter")
public class PlatformInstance extends VSTOIInstance {

    private static final Logger log = LoggerFactory.getLogger(PlatformInstance.class);
    public static String LAT = SIO.LATITUDE;
    public static String LONG = SIO.LONGITUDE;

    private String elevation;

    @PropertyField(uri="hasco:hasFirstCoordinate")
    private Float  firstCoordinate;

    @PropertyField(uri="hasco:hasFirstCoordinateUnit")
    private String firstCoordinateUnit;

    @PropertyField(uri="hasco:hasFirstCoordinateCharacteristic")
    private String firstCoordinateCharacteristic;

    @PropertyField(uri="hasco:hasSecondCoordinate")
    private Float  secondCoordinate;

    @PropertyField(uri="hasco:hasSecondCoordinateUnit")
    private String secondCoordinateUnit;

    @PropertyField(uri="hasco:hasSecondCoordinateCharacteristic")
    private String secondCoordinateCharacteristic;

    @PropertyField(uri="hasco:hasThirdCoordinate")
    private Float  thirdCoordinate;

    @PropertyField(uri="hasco:hasThirdCoordinateUnit")
    private String thirdCoordinateUnit;

    @PropertyField(uri="hasco:hasThirdCoordinateCharacteristic")
    private String thirdCoordinateCharacteristic;

    @PropertyField(uri="hasco:partOf")
    private String partOf;

    @PropertyField(uri="hasco:hasLayout")
    private String layout;

    @PropertyField(uri="hasco:hasReferenceLayout")
    private String referenceLayout;

    @PropertyField(uri="hasco:hasUrl")
    private String url;

    @PropertyField(uri="hasco:hasLayoutWidth")
    private Float  width;

    @PropertyField(uri="hasco:hasLayoutWidthUnit")
    private String widthUnit;

    @PropertyField(uri="hasco:hasLayoutDepth")
    private Float  depth;

    @PropertyField(uri="hasco:hasLayoutDepthUnit")
    private String depthUnit;

    @PropertyField(uri="hasco:hasLayoutHeight")
    private Float  height;

    @PropertyField(uri="hasco:hasLayoutHeightUnit")
    private String heightUnit;

	public PlatformInstance() {
		this.setTypeUri(VSTOI.PLATFORM_INSTANCE);
		this.setHascoTypeUri(VSTOI.PLATFORM_INSTANCE); 
	}
    public String getElevation() {
        return elevation;
    }
    public void setElevation(String elevation) {
        this.elevation = elevation;
    }

    public Float getFirstCoordinate() {
        return firstCoordinate;
    }
    public void setFirstCoordinate(Float firstCoordinate) {
        this.firstCoordinate = firstCoordinate;
    }
    public String getFirstCoordinateUnit() {
        return firstCoordinateUnit;
    }
    public String getFirstCoordinateUnitLabel() {
        if (firstCoordinateUnit == null || firstCoordinateUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(firstCoordinateUnit);
    }

    public void setFirstCoordinateUnit(String firstCoordinateUnit) {
        this.firstCoordinateUnit = firstCoordinateUnit;
    }

    public String getFirstCoordinateCharacteristic() {
        return firstCoordinateCharacteristic;
    }

    public String getFirstCoordinateCharacteristicLabel() {
        if (firstCoordinateCharacteristic == null || firstCoordinateCharacteristic.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(firstCoordinateCharacteristic);
    }

    public void setFirstCoordinateCharacteristic(String firstCoordinateCharacteristic) {
        this.firstCoordinateCharacteristic = firstCoordinateCharacteristic;
    }

    public Float getSecondCoordinate() {
        return secondCoordinate;
    }

    public void setSecondCoordinate(Float secondCoordinate) {
        this.secondCoordinate = secondCoordinate;
    }

    public String getSecondCoordinateUnit() {
        return secondCoordinateUnit;
    }

    public String getSecondCoordinateUnitLabel() {
        if (secondCoordinateUnit == null || secondCoordinateUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(secondCoordinateUnit);
    }

    public void setSecondCoordinateUnit(String secondCoordinateUnit) {
        this.secondCoordinateUnit = secondCoordinateUnit;
    }

    public String getSecondCoordinateCharacteristic() {
        return secondCoordinateCharacteristic;
    }

    public String getSecondCoordinateCharacteristicLabel() {
        if (secondCoordinateCharacteristic == null || secondCoordinateCharacteristic.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(secondCoordinateCharacteristic);
    }

    public void setSecondCoordinateCharacteristic(String secondCoordinateCharacteristic) {
        this.secondCoordinateCharacteristic = secondCoordinateCharacteristic;
    }

    public Float getThirdCoordinate() {
        return thirdCoordinate;
    }

    public void setThirdCoordinate(Float thirdCoordinate) {
        this.thirdCoordinate = thirdCoordinate;
    }

    public String getThirdCoordinateUnit() {
        return thirdCoordinateUnit;
    }

    public String getThirdCoordinateUnitLabel() {
        if (thirdCoordinateUnit == null || thirdCoordinateUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(thirdCoordinateUnit);
    }

    public void setThirdCoordinateUnit(String thirdCoordinateUnit) {
        this.thirdCoordinateUnit = thirdCoordinateUnit;
    }

    public String getThirdCoordinateCharacteristic() {
        return thirdCoordinateCharacteristic;
    }

    public String getThirdCoordinateCharacteristicLabel() {
        if (thirdCoordinateCharacteristic == null || thirdCoordinateCharacteristic.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(thirdCoordinateCharacteristic);
    }

    public void setThirdCoordinateCharacteristic(String thirdCoordinateCharacteristic) {
        this.thirdCoordinateCharacteristic = thirdCoordinateCharacteristic;
    }

    public Float getWidth() {
        return width;
    }
    public void setWidth(Float width) {
        this.width = width;
    }

    public String getWidthUnit() {
        return widthUnit;
    }
    public String getWidthUnitLabel() {
        if (widthUnit == null || widthUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(widthUnit);
    }

    public void setWidthUnit(String widthUnit) {
        this.widthUnit = widthUnit;
    }

    public Float getDepth() {
        return depth;
    }

    public void setDepth(Float depth) {
        this.depth = depth;
    }

    public String getDepthUnit() {
        return depthUnit;
    }

    public String getDepthUnitLabel() {
        if (depthUnit == null || depthUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(depthUnit);
    }

    public void setDepthUnit(String depthUnit) {
        this.depthUnit = depthUnit;
    }

    public Float getHeight() {
        return height;
    }

    public void setHeight(Float height) {
        this.height = height;
    }

    public String getHeightUnit() {
        return heightUnit;
    }

    public String getHeightUnitLabel() {
        if (heightUnit == null || heightUnit.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(heightUnit);
    }

    public void setHeightUnit(String heightUnit) {
        this.heightUnit = heightUnit;
    }

    public String getURL() {
        return url;
    }
    public void setURL(String url) {
        this.url = url;
    }

    public String getPartOf() {
        return partOf;
    }
    public String getPartOfLabel() {
        if (partOf == null || partOf.isEmpty()) {
            return "";
        }
        return FirstLabel.getPrettyLabel(partOf);
    }

    public void setPartOf(String partOf) {
        this.partOf = partOf;
    }

    public String getLayout() {
        return layout;
    }

    public void setLayout(String layout) {
        this.layout = layout;
    }

    public String getReferenceLayout() {
        return referenceLayout;
    }

    public void setReferenceLayout(String referenceLayout) {
        this.referenceLayout = referenceLayout;
    }

    public boolean hasGeoReference() {
        return getFirstCoordinate() != null && getSecondCoordinate() != null &&
                getFirstCoordinateCharacteristic() != null && getSecondCoordinate() != null &&
                getFirstCoordinateCharacteristic().equals(LAT) &&
                getSecondCoordinateCharacteristic().equals(LONG);
    }

	public static PlatformInstance find(String uri) {
		if (uri == null || uri.isEmpty()) {
			return null;
		}

		// First get basic properties from parent class
		PlatformInstance platform = new PlatformInstance();
		VSTOIInstance.find(platform, uri);
		
		// Now get platform-specific properties
		Statement statement;
		RDFNode object;
		
		String queryString = "DESCRIBE <" + uri + ">";
		Model model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
				CollectionUtil.Collection.SPARQL_QUERY), queryString);
		
		StmtIterator stmtIterator = model.listStatements();
		
		while (stmtIterator.hasNext()) {
			statement = stmtIterator.next();
			object = statement.getObject();
			String str = URIUtils.objectRDFToString(object);
			
			if (str != null && !str.isEmpty()) {
				try {
					if (statement.getPredicate().getURI().equals(HASCO.HAS_FIRST_COORDINATE)) {
						platform.setFirstCoordinate(Float.parseFloat(str));
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_FIRST_COORDINATE_UNIT)) {
						platform.setFirstCoordinateUnit(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_FIRST_COORDINATE_CHARACTERISTIC)) {
						platform.setFirstCoordinateCharacteristic(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SECOND_COORDINATE)) {
						platform.setSecondCoordinate(Float.parseFloat(str));
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SECOND_COORDINATE_UNIT)) {
						platform.setSecondCoordinateUnit(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_SECOND_COORDINATE_CHARACTERISTIC)) {
						platform.setSecondCoordinateCharacteristic(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_THIRD_COORDINATE)) {
						platform.setThirdCoordinate(Float.parseFloat(str));
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_THIRD_COORDINATE_UNIT)) {
						platform.setThirdCoordinateUnit(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_THIRD_COORDINATE_CHARACTERISTIC)) {
						platform.setThirdCoordinateCharacteristic(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.PART_OF)) {
						platform.setPartOf(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT)) {
						platform.setLayout(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_REFERENCE_LAYOUT)) {
						platform.setReferenceLayout(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT_WIDTH)) {
						platform.setWidth(Float.parseFloat(str));
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT_WIDTH_UNIT)) {
						platform.setWidthUnit(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT_DEPTH)) {
						platform.setDepth(Float.parseFloat(str));
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT_DEPTH_UNIT)) {
						platform.setDepthUnit(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT_HEIGHT)) {
						platform.setHeight(Float.parseFloat(str));
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_LAYOUT_HEIGHT_UNIT)) {
						platform.setHeightUnit(str);
					} else if (statement.getPredicate().getURI().equals(HASCO.HAS_URL)) {
						platform.setURL(str);
					}
				} catch (NumberFormatException e) {
					// Silently skip if coordinate values can't be parsed as Float
					log.debug("Could not parse numeric value for property {}: {}", statement.getPredicate().getURI(), str);
				}
			}
		}
		
		return platform;
	} 

    public static List<PlatformInstance> findByPlaformWithPage(String platformUri, int pageSize, int offset) {
        if (platformUri == null || platformUri.isEmpty()) {
            return new ArrayList<PlatformInstance>();
        }
        String query = 
                "SELECT ?uri " +
                " WHERE {  ?uri rdf:type <" + platformUri + "> .  " +
				"          ?uri hasco:hascoType vstoi:PlatformInstance . " +
                " } " +
                " LIMIT " + pageSize +
                " OFFSET " + offset;
        return findManyByQuery(query);
    }        

    public static int findTotalByPlatform(String platformUri) {
        if (platformUri == null || platformUri.isEmpty()) {
            return 0;
        }
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + 
                " SELECT (count(?uri) as ?tot)  " +
                " WHERE {  ?uri rdf:type <" + platformUri + "> .  " +
				"          ?uri hasco:hascoType vstoi:PlatformInstance . " +
                " }";
        return GenericFind.findTotalByQuery(query);
    }        

    public static List<PlatformInstance> findAll() {
        String query =
                "SELECT ?uri " +
                " WHERE { ?uri hasco:hascoType vstoi:PlatformInstance . }";
        return findManyByQuery(query);
    }

	private static List<PlatformInstance> findManyByQuery(String queryString) {
        String query = NameSpaces.getInstance().printSparqlNameSpaceList() + queryString;

		List<PlatformInstance> instances = new ArrayList<PlatformInstance>();
        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), query);
        if (!resultsrw.hasNext()) {
            return null;
        }
        while (resultsrw.hasNext()) {
            QuerySolution soln = resultsrw.next();
        	PlatformInstance instance = PlatformInstance.find(soln.getResource("uri").getURI());
            instances.add(instance);
        }
        return instances;
    }

}
