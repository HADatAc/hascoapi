package org.hascoapi.transform.mt.ins;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.DetectorStem;
import org.hascoapi.entity.pojo.Detector;
import org.hascoapi.entity.pojo.ActuatorStem;
import org.hascoapi.entity.pojo.Actuator;
import org.hascoapi.entity.pojo.Codebook;
import org.hascoapi.entity.pojo.CodebookSlot;
import org.hascoapi.entity.pojo.ResponseOption;
import org.hascoapi.entity.pojo.AnnotationStem;
import org.hascoapi.entity.pojo.Annotation;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.hascoapi.utils.ConfigProp;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class INSGenHelper {

    public Map<String,NameSpace> namespaces;
    public Map<String,DetectorStem> detStems;
    public Map<String,Detector> dets;
    public Map<String,ActuatorStem> actStems;
    public Map<String,Actuator> acts;
    public Map<String,Codebook> codebooks;
    public Map<String,ResponseOption> respOptions;
    public Map<String,AnnotationStem> annStems;
    public Map<String,Annotation> anns;
    public Workbook workbook;
    
    public INSGenHelper() {
        namespaces = new HashMap<String,NameSpace>();
        detStems = new HashMap<String,DetectorStem>();
        dets = new HashMap<String,Detector>();
        actStems = new HashMap<String,ActuatorStem>();
        acts = new HashMap<String,Actuator>();
        codebooks = new HashMap<String,Codebook>();
        respOptions = new HashMap<String,ResponseOption>();
        annStems = new HashMap<String,AnnotationStem>();
        anns = new HashMap<String,Annotation>();
    }

    public void addNamespace(NameSpace namespace) {
        if (namespace == null || namespace.getUri() == null) {
            return;
        }
        if (!namespaces.containsKey(namespace.getUri())) {
            namespaces.put(namespace.getUri(),namespace);
        }
    }

    public void addDetectorStem(DetectorStem detStem) {
        if (detStem == null || detStem.getUri() == null) {
            return;
        }
        if (!detStems.containsKey(detStem.getUri())) {
            detStems.put(detStem.getUri(),detStem);
        }
    }

    public void addDetector(Detector det) {
        if (det == null || det.getUri() == null) {
            return;
        }
        if (!dets.containsKey(det.getUri())) {
            dets.put(det.getUri(),det);
        }
    }

    public void addActuatorStem(ActuatorStem actStem) {
        if (actStem == null || actStem.getUri() == null) {
            return;
        }
        if (!actStems.containsKey(actStem.getUri())) {
            actStems.put(actStem.getUri(),actStem);
        }
    }

    public void addActuator(Actuator act) {
        if (act == null || act.getUri() == null) {
            return;
        }
        if (!acts.containsKey(act.getUri())) {
            acts.put(act.getUri(),act);
        }
    }

    public void addCodebook(Codebook codebook) {
        if (codebook == null || codebook.getUri() == null) {
            return;
        }
        if (!codebooks.containsKey(codebook.getUri())) {
            codebooks.put(codebook.getUri(),codebook);
        }
    }

    public void addResponseOption(ResponseOption respOption) {
        if (respOption == null || respOption.getUri() == null) {
            return;
        }
        if (!respOptions.containsKey(respOption.getUri())) {
            respOptions.put(respOption.getUri(),respOption);
        }
    }

    public void addAnnotationStem(AnnotationStem annStem) {
        if (annStem == null || annStem.getUri() == null) {
            return;
        }
        if (!annStems.containsKey(annStem.getUri())) {
            annStems.put(annStem.getUri(),annStem);
        }
    }

    public void addAnnotation(Annotation ann) {
        if (ann == null || ann.getUri() == null) {
            return;
        }
        if (!anns.containsKey(ann.getUri())) {
            anns.put(ann.getUri(),ann);
        }
    }

}
