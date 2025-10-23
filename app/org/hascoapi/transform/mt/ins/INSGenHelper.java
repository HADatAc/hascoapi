package org.hascoapi.transform.mt.ins;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.entity.pojo.Instrument;
import org.hascoapi.entity.pojo.ContainerSlot;
import org.hascoapi.entity.pojo.ComponentStem;
import org.hascoapi.entity.pojo.Component;
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
    public Map<String,ComponentStem> componentStems;
    public Map<String,Component> components;
    public Map<String,Codebook> codebooks;
    public Map<String,ResponseOption> respOptions;
    public Map<String,AnnotationStem> annStems;
    public Map<String,Annotation> anns;
    public Workbook workbook;
    
    public INSGenHelper() {
        namespaces = new HashMap<String,NameSpace>();
        componentStems = new HashMap<String,ComponentStem>();
        components = new HashMap<String,Component>();
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

    public void addComponentStem(ComponentStem componentStem) {
        if (componentStem == null || componentStem.getUri() == null) {
            return;
        }
        if (!componentStems.containsKey(componentStem.getUri())) {
            componentStems.put(componentStem.getUri(),componentStem);
        }
    }

    public void addComponent(Component component) {
        if (component == null || component.getUri() == null) {
            return;
        }
        if (!components.containsKey(component.getUri())) {
            components.put(component.getUri(),component);
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
