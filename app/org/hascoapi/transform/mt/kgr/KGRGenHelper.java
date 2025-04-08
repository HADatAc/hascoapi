package org.hascoapi.transform.mt.kgr;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.FundingScheme;
import org.hascoapi.entity.pojo.Project;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Person;
import org.hascoapi.entity.pojo.Place;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class KGRGenHelper {

    public Map<String,NameSpace> namespaces;
    public Map<String,FundingScheme> fundingSchemes;
    public Map<String,Project> projects;
    public Map<String,Organization> organizations;
    public Map<String,Person> persons;
    public Map<String,Place> places;
    public Map<String,PostalAddress> postalAddresses;
    public Workbook workbook;
    
    public KGRGenHelper() {
        namespaces = new HashMap<String,NameSpace>();
        fundingSchemes = new HashMap<String,FundingScheme>();
        projects = new HashMap<String,Project>();
        organizations = new HashMap<String,Organization>();
        persons = new HashMap<String,Person>();
        places = new HashMap<String,Place>();
        postalAddresses = new HashMap<String,PostalAddress>();
    }

    public void addNamespace(NameSpace namespace) {
        if (namespace == null || namespace.getUri() == null) {
            return;
        }
        if (!namespaces.containsKey(namespace.getUri())) {
            namespaces.put(namespace.getUri(),namespace);
        }
    }

    public void addFundingScheme(FundingScheme scheme) {
        if (scheme == null || scheme.getUri() == null) {
            return;
        }
        if (!fundingSchemes.containsKey(scheme.getUri())) {
            fundingSchemes.put(scheme.getUri(),scheme);
        }
    }

    public void addProject(Project project) {
        if (project == null || project.getUri() == null) {
            return;
        }
        if (!projects.containsKey(project.getUri())) {
            projects.put(project.getUri(),project);
        }
    }

    public void addOrganization(Organization organization) {
        if (organization == null || organization.getUri() == null) {
            return;
        }
        if (!organizations.containsKey(organization.getUri())) {
            organizations.put(organization.getUri(),organization);
        }
    }

    public void addPerson(Person person) {
        if (person == null || person.getUri() == null) {
            return;
        }
        if (!persons.containsKey(person.getUri())) {
            persons.put(person.getUri(),person);
        }
    }

    public void addPlace(Place place) {
        if (place == null || place.getUri() == null) {
            return;
        }
        if (!places.containsKey(place.getUri())) {
            places.put(place.getUri(),place);
        }
    }

    public void addPostalAddress(PostalAddress postAddress) {
        if (postalAddresses == null || postAddress.getUri() == null) {
            return;
        }
        if (!postalAddresses.containsKey(postAddress.getUri())) {
            postalAddresses.put(postAddress.getUri(),postAddress);
        }
    }

}
