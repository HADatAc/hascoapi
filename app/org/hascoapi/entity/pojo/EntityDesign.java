package org.hascoapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import org.hascoapi.annotations.PropertyField;
import org.hascoapi.vocabularies.VSTOI;

public class EntityDesign extends HADatAcThing {

    @PropertyField(uri = "vstoi:hasStatus")
    private String hasStatus;

    @PropertyField(uri = "vstoi:hasVersion")
    private String hasVersion;

    @PropertyField(uri = "hasco:hasStudy")
    private String hasStudyUri;

    @PropertyField(uri = "hasco:hasEntity")
    private String hasEntityUri;

    @PropertyField(uri = "hasco:hasAttribute")
    private String hasAttributeUri;

    @PropertyField(uri = "hasco:hasUnit")
    private String hasUnitUri;

    @PropertyField(uri = "hasco:hasRole")
    private String hasRoleUri;

    @PropertyField(uri = "hasco:hasInRelationTo")
    private String hasInRelationToUri;

    @PropertyField(uri = "hasco:hasSource")
    private String hasSourceUri;

    @PropertyField(uri = "hasco:originalID")
    private String originalId;

    // Getters e Setters (simplificados para o mock)
    public String getHasStatus() { return hasStatus; }
    public void setHasStatus(String hasStatus) { this.hasStatus = hasStatus; }
    public String getHasVersion() { return hasVersion; }
    public void setHasVersion(String hasVersion) { this.hasVersion = hasVersion; }
    public String getHasStudyUri() { return hasStudyUri; }
    public void setHasStudyUri(String hasStudyUri) { this.hasStudyUri = hasStudyUri; }
    public String getHasEntityUri() { return hasEntityUri; }
    public void setHasEntityUri(String hasEntityUri) { this.hasEntityUri = hasEntityUri; }
    public String getHasAttributeUri() { return hasAttributeUri; }
    public void setHasAttributeUri(String hasAttributeUri) { this.hasAttributeUri = hasAttributeUri; }
    public String getHasUnitUri() { return hasUnitUri; }
    public void setHasUnitUri(String hasUnitUri) { this.hasUnitUri = hasUnitUri; }
    public String getHasRoleUri() { return hasRoleUri; }
    public void setHasRoleUri(String hasRoleUri) { this.hasRoleUri = hasRoleUri; }
    public String getHasInRelationToUri() { return hasInRelationToUri; }
    public void setHasInRelationToUri(String hasInRelationToUri) { this.hasInRelationToUri = hasInRelationToUri; }
    public String getHasSourceUri() { return hasSourceUri; }
    public void setHasSourceUri(String hasSourceUri) { this.hasSourceUri = hasSourceUri; }
    public String getOriginalId() { return originalId; }
    public void setOriginalId(String originalId) { this.originalId = originalId; }



}