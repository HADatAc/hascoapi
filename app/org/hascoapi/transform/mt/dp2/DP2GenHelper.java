package org.hascoapi.transform.mt.dp2;

import java.util.Map;
import java.util.HashMap;

import org.hascoapi.entity.pojo.*;
import org.apache.poi.ss.usermodel.*;

public class DP2GenHelper {

    public Map<String,NameSpace> namespaces;
    public Map<String, PlatformInstance> plataforminstances;
    public Map<String,Platform> platforms;
    public Map<String,ComponentInstance> componentinstances;
    public Map<String,InstrumentInstance> instrumentinstances;
    public Map<String,FieldOfView> fieldofview;
    public Workbook workbook;

    public DP2GenHelper() {
        namespaces = new HashMap<String,NameSpace>();
        plataforminstances = new HashMap<String,PlatformInstance>();
        platforms = new HashMap<String,Platform>();
        componentinstances = new HashMap<String,ComponentInstance>();
        instrumentinstances = new HashMap<String,InstrumentInstance>();
        fieldofview = new HashMap<String,FieldOfView>();
    }

    public void addNamespace(NameSpace namespace) {
        if (namespace == null || namespace.getUri() == null) {
            return;
        }
        if (!namespaces.containsKey(namespace.getUri())) {
            namespaces.put(namespace.getUri(),namespace);
        }
    }

    public void addPlatformInstance(PlatformInstance platformInstance) {
        if (platformInstance == null || platformInstance.getUri() == null) {
            return;
        }
        if (!plataforminstances.containsKey(platformInstance.getUri())) {
            plataforminstances.put(platformInstance.getUri(),platformInstance);
        }
    }

    public void addPlatform(Platform platform) {
        if (platform == null || platform.getUri() == null) {
            return;
        }
        if (!platforms.containsKey(platform.getUri())) {
            platforms.put(platform.getUri(),platform);
        }
    }

    public void addComponentInstance(ComponentInstance componentInstance) {
        if (componentInstance == null || componentInstance.getUri() == null) {
            return;
        }
        if (!componentinstances.containsKey(componentInstance.getUri())) {
            componentinstances.put(componentInstance.getUri(),componentInstance);
        }
    }

    public void addInstrumentInstance(InstrumentInstance instrumentInstance) {
        if (instrumentInstance == null || instrumentInstance.getUri() == null) {
            return;
        }
        if (!instrumentinstances.containsKey(instrumentInstance.getUri())) {
            instrumentinstances.put(instrumentInstance.getUri(),instrumentInstance);
        }
    }

    public void addFieldOfView(FieldOfView fieldOfView) {
        if (fieldOfView == null || fieldOfView.getUri() == null) {
            return;
        }
        if (!fieldofview.containsKey(fieldOfView.getUri())) {
            fieldofview.put(fieldOfView.getUri(),fieldOfView);
        }
    }


}
