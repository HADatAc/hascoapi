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
import org.hascoapi.vocabularies.RDF;
import org.hascoapi.vocabularies.RDFS;
import org.hascoapi.vocabularies.HASCO;
import org.hascoapi.vocabularies.VSTOI;
import org.hascoapi.vocabularies.FOAF;


public class Agent extends HADatAcThing implements Comparable<Agent> {

  	@PropertyField(uri="vstoi:hasStatus")
  	private String hasStatus;

  	@PropertyField(uri="hasco:originalID")
  	private String originalID;

    @PropertyField(uri="schema:alternateName")
    private String hasShortName;

    @PropertyField(uri="foaf:name")
    protected String name;

    @PropertyField(uri="foaf:mbox")
    protected String mbox;

    @PropertyField(uri="schema:telephone")
    protected String telephone;

    @PropertyField(uri="schema:address")
    protected String hasAddressUri;

    @PropertyField(uri="vstoi:hasSIRManagerEmail")
    protected String hasSIRManagerEmail;

    public String getHasStatus() {
        return hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getOriginalID() {
        return originalID;
    }

    public void setOriginalID(String originalID) {
        this.originalID = originalID;
    }

    public String getHasShortName() {
        return hasShortName;
    }
    public void setHasShortName(String hasShortName) {
        this.hasShortName = hasShortName;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getMbox() {
        return mbox;
    }
    public void setMbox(String mbox) {
        this.mbox = mbox;
    }

    public String getTelephone() {
        return telephone;
    }
    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getHasAddressUri() {
        return hasAddressUri;
    }
    public void setHasAddressUri(String hasAddressUri) {
        this.hasAddressUri = hasAddressUri;
    }
    public PostalAddress getHasAddress() {
        if (this.hasAddressUri == null || this.hasAddressUri.isEmpty()) {
            return null;
        }
        return PostalAddress.find(this.hasAddressUri);
    }

    public String getHasSIRManagerEmail() {
        return hasSIRManagerEmail;
    }
    public void setHasSIRManagerEmail(String hasSIRManagerEmail) {
        this.hasSIRManagerEmail = hasSIRManagerEmail;
    }

    @Override
    public int compareTo(Agent another) {
        if (this.getName() == null || another == null || another.getName() == null) {
            return 0;
        }
        return this.getName().compareTo(another.getName());
    }

}
