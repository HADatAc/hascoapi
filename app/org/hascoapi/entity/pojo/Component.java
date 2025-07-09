package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFilter;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.console.controllers.restapi.URIPage;
import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.*;

@JsonFilter("componentFilter")
public class Component extends HADatAcThing implements SIRElement  {

    @PropertyField(uri="vstoi:hasCodebook")
    private String hasCodebook;

    @PropertyField(uri="vstoi:isAttributeOf")
    private String isAttributeOf;

    @PropertyField(uri = "vstoi:hasStatus")    
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasContent")
    String hasContent;

    @PropertyField(uri = "vstoi:hasLanguage")
    private String hasLanguage;

    @PropertyField(uri = "vstoi:hasVersion")
    String hasVersion;

    @PropertyField(uri = "vstoi:hasReviewNote")
    String hasReviewNote;

    @PropertyField(uri="prov:wasDerivedFrom")
    private String wasDerivedFrom;

    @PropertyField(uri="prov:wasGeneratedBy")
    private String wasGeneratedBy;

    @PropertyField(uri = "vstoi:hasMaker")
    private String hasMakerUri;

    @PropertyField(uri = "vstoi:hasSIRManagerEmail")
    private String hasSIRManagerEmail;

    @PropertyField(uri = "vstoi:hasEditorEmail")
    private String hasEditorEmail;

    public String getHasCodebook() {
        return hasCodebook;
    }

    public void setHasCodebook(String hasCodebook) {
        this.hasCodebook = hasCodebook;
    }

    public Codebook getCodebook() {
        if (hasCodebook == null || hasCodebook.equals("")) {
            return null;
        }
        return Codebook.find(hasCodebook);
    }

    public void setIsAttributeOf(String isAttributeOf) {
        this.isAttributeOf = isAttributeOf;
    }

    public String getIsAttributeOf() {
        return isAttributeOf;
    }

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getHasContent() {
        return hasContent;
    }

    public void setHasContent(String hasContent) {
        this.hasContent = hasContent;
    }

    public String getHasLanguage() {
        return hasLanguage;
    }

    public void setHasLanguage(String hasLanguage) {
        this.hasLanguage = hasLanguage;
    }

    public String getHasVersion() {      
        return hasVersion;
    }

    public void setHasVersion(String hasVersion) {
        this.hasVersion = hasVersion;
    }

    public String getHasReviewNote() {      
        return hasReviewNote;
    }

    public void setHasReviewNote(String hasReviewNote) {
        this.hasReviewNote = hasReviewNote;
    }

    public void setWasDerivedFrom(String wasDerivedFrom) {
        this.wasDerivedFrom = wasDerivedFrom;
    }

    public String getWasDerivedFrom() {
        return wasDerivedFrom;
    }

    public void setWasGeneratedBy(String wasGeneratedBy) {
        this.wasGeneratedBy = wasGeneratedBy;
    }

    public String getWasGeneratedBy() {
        return wasGeneratedBy;
    }

    public String getHasMakerUri() {
        return hasMakerUri;
    }

    public void setHasMakerUri(String hasMakerUri) {
        this.hasMakerUri = hasMakerUri;
    }

    public Organization getHasMaker() {
        if (hasMakerUri == null || hasMakerUri.isEmpty()) {
			return null;
		}
		return Organization.find(hasMakerUri);
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }

    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    public String getHasEditorEmail() {
        return hasEditorEmail;
    }

    public void setHasEditorEmail(String hasEditorEmail) {
        this.hasEditorEmail = hasEditorEmail;
    }

    public static boolean attach(ContainerSlot containerSlot, Component component) {
        //System.out.println("called Component.attach()");
        if (containerSlot == null) {
            System.out.println("A valid container slot is required to attach a component");
            return false;
        }
        return containerSlot.updateContainerSlotComponent(component);
    }

    public static boolean detach(ContainerSlot containerSlot) {
        //System.out.println("called Component.detach()");
        if (containerSlot == null) {
            return false;
        }
        return containerSlot.updateContainerSlotComponent(null);
    }

    public static Component find(String uri) {

        HADatAcThing component = URIPage.objectFromUri(uri);

        if (component == null) {
            return null;
        }

        if (component instanceof Detector || component instanceof Actuator) {
            return (Component)component;
        }

        return null;
    }


    @Override public void save() {
        saveToTripleStore();
    }

    @Override public void delete() {
        deleteFromTripleStore();
    }

}
