package org.hascoapi.entity.pojo;

import org.hascoapi.annotations.PropertyField;

public class SensingPerspective extends HADatAcThing implements Comparable<SensingPerspective> {
    
    @PropertyField(uri="vstoi:perspectiveOf")
    private String perspectiveOf;

    @PropertyField(uri="hasco:hasPerspectiveEntity")
    private String hasPerspectiveEntity;

    @PropertyField(uri = "hasco:hasPerspectiveCharacteristic")
    private String hasPerspectiveCharacteristic;

    @PropertyField(uri = "vstoi:hasAccuracyPercentage")
    private String hasAccuracyPercentage;

    @PropertyField(uri = "vstoi:hasAccuracyR2")
    private String hasAccuracyR2;

    @PropertyField(uri = "vstoi:hasOutputResolution")
    private String hasOutputResolution;

    @PropertyField(uri = "vstoi:hasMaxResponseTimeValue")
    private String hasMaxResponseTimeValue;

    @PropertyField(uri = "hasco:hasResponseTimeUnit")
    private String hasResponseTimeUnit;

    @PropertyField(uri = "vstoi:hasLowRangeValue")
    private String hasLowRangeValue;

    @PropertyField(uri = "vstoi:hasHighRangeValue")
    private String hasHighRangeValue;

    public String getPerspectiveOf() {
        return perspectiveOf;
    }

    public void setPerspectiveOf(String perspectiveOf) {
        this.perspectiveOf = perspectiveOf;
    }

    public String getHasPerspectiveEntity() {
        return hasPerspectiveEntity;
    }

    public void setHasPerspectiveEntity(String hasPerspectiveEntity) {
        this.hasPerspectiveEntity = hasPerspectiveEntity;
    }

    public String getHasPerspectiveCharacteristic() {
        return hasPerspectiveCharacteristic;
    }

    public void setHasPerspectiveCharacteristic(String hasPerspectiveCharacteristic) {
        this.hasPerspectiveCharacteristic = hasPerspectiveCharacteristic;
    }

    public String getHasAccuracyPercentage() {
        return hasAccuracyPercentage;
    }

    public void setHasAccuracyPercentage(String hasAccuracyPercentage) {
        this.hasAccuracyPercentage = hasAccuracyPercentage;
    }

    public String getHasAccuracyR2() {
        return hasAccuracyR2;
    }

    public void setHasAccuracyR2(String hasAccuracyR2) {
        this.hasAccuracyR2 = hasAccuracyR2;
    }

    public String getHasOutputResolution() {
        return hasOutputResolution;
    }

    public void setHasOutputResolution(String hasOutputResolution) {
        this.hasOutputResolution = hasOutputResolution;
    }

    public String getHasMaxResponseTimeValue() {
        return hasMaxResponseTimeValue;
    }

    public void setHasMaxResponseTimeValue(String hasMaxResponseTimeValue) {
        this.hasMaxResponseTimeValue = hasMaxResponseTimeValue;
    }

    public String getHasResponseTimeUnit() {
        return hasResponseTimeUnit;
    }

    public void setHasResponseTimeUnit(String hasResponseTimeUnit) {
        this.hasResponseTimeUnit = hasResponseTimeUnit;
    }

    public String getHasLowRangeValue() {
        return hasLowRangeValue;
    }

    public void setHasLowRangeValue(String hasLowRangeValue) {
        this.hasLowRangeValue = hasLowRangeValue;
    }

    public String getHasHighRangeValue() {
        return hasHighRangeValue;
    }

    public void setHasHighRangeValue(String hasHighRangeValue) {
        this.hasHighRangeValue = hasHighRangeValue;
    }

    @Override
    public int compareTo(SensingPerspective o) {
        return 0;
    }
}
