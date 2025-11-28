package org.hascoapi.transform.mt.dsg;

import java.util.Map;
import java.util.HashMap;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.SemanticDataDictionary; // Assumindo que SSD é SemanticDataDictionary
import org.hascoapi.entity.pojo.StudyObject; // Assumindo que VD é StudyObject
import org.apache.poi.ss.usermodel.*;

public class DSGGenHelper {

    public Map<String,NameSpace> namespaces;
    public Map<String,Study> studies;
    public Map<String,SemanticDataDictionary> ssds; // Usando SemanticDataDictionary para SSD
    public Map<String,StudyObject> studyObjects; // Usando StudyObject para VD (Variable Design)
    public Workbook workbook;
    
    public DSGGenHelper() {
        namespaces = new HashMap<String,NameSpace>();
        studies = new HashMap<String,Study>();
        ssds = new HashMap<String,SemanticDataDictionary>();
        studyObjects = new HashMap<String,StudyObject>();
        workbook = null;
    }

    public void addNamespace(NameSpace namespace) {
        if (namespace == null || namespace.getUri() == null) {
            return;
        }
        if (!namespaces.containsKey(namespace.getUri())) {
            namespaces.put(namespace.getUri(),namespace);
        }
    }

    public Map<String, NameSpace> getNamespaces() {
        return namespaces;
    }

    public void setNamespaces(Map<String, NameSpace> namespaces) {
        this.namespaces = namespaces;
    }

    public Map<String, Study> getStudies() {
        return studies;
    }

    public void setStudies(Map<String, Study> studies) {
        this.studies = studies;
    }

    public Map<String, SemanticDataDictionary> getSsds() {
        return ssds;
    }

    public void setSsds(Map<String, SemanticDataDictionary> ssds) {
        this.ssds = ssds;
    }

    public Map<String, StudyObject> getStudyObjects() {
        return studyObjects;
    }

    public void setStudyObjects(Map<String, StudyObject> studyObjects) {
        this.studyObjects = studyObjects;
    }

    public Workbook getWorkbook() {
        return workbook;
    }

    public void setWorkbook(Workbook workbook) {
        this.workbook = workbook;
    }
}
