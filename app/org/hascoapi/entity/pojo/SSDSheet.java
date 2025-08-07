package org.hascoapi.entity.pojo;

import java.io.File;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.RecordFile;
import org.hascoapi.utils.Utils;


public class SSDSheet {

    private Map<String, String> mapCatalog = new HashMap<String, String>();
    private Map<String, List<String>> mapContent = new HashMap<String, List<String>>();
    private Map<String, String> mapReferences = new HashMap<String, String>();
    private DataFile ssdfile = null;

    public SSDSheet(DataFile dataFile) {
        this.ssdfile = dataFile;
        readCatalog(dataFile.getRecordFile());
        readContent(dataFile.getRecordFile());
        readReferences(dataFile.getRecordFile());
    }

    public String getNameFromFileName() {
        return ssdfile.getBaseName().replace("_", "-").replace("SSD-", "");
    }

    public String getFileName() {
        return ssdfile.getFilename();
    }

    public Map<String, String> getCatalog() {
        return mapCatalog;
    }

    public Map<String, List<String>> getMapContent() {
        return mapContent;
    }

    public Map<String, String> getMapReferences() {
        return mapReferences;
    }

    private void readCatalog(RecordFile file) {
        if (!file.isValid()) {
            return;
        }
        for (Record record : file.getRecords()) {
        	if ((record.getValueByColumnIndex(1) == null || record.getValueByColumnIndex(1).isEmpty()) && 
        	    (record.getValueByColumnIndex(0) == null || record.getValueByColumnIndex(0).isEmpty())) {
        		return;
        	}
        	//System.out.println("Catalog: [" + record.getValueByColumnIndex(1) + "]  [" + record.getValueByColumnIndex(0) + "]");
        	mapCatalog.put(record.getValueByColumnIndex(1), record.getValueByColumnIndex(0));
        }
    }

    private void readContent(RecordFile file) {
        if (file == null || !file.isValid()) {
            return;
        }
        for (Record record : file.getRecords()) {
            List<String> tmp = new ArrayList<String>();
            // item 0
            tmp.add(record.getValueByColumnName("hasURI"));
            // item 1
            tmp.add(record.getValueByColumnName("type"));
            // item 2
            tmp.add(record.getValueByColumnName("hasScope"));
            // item 3
            tmp.add(record.getValueByColumnName("hasTimeScope"));
            // item 4
            tmp.add(record.getValueByColumnName("hasSpaceScope"));
            // item 5
            tmp.add(record.getValueByColumnName("role"));
            // item 6
            tmp.add(record.getValueByColumnName("hasSOCReference"));
            // item 7
            tmp.add(record.getValueByColumnName("groundingLabel"));

            mapContent.put(record.getValueByColumnName("hasURI"), tmp);
        }
    }

    private void readReferences(RecordFile file) {
        if (file == null || !file.isValid()) {
            return;
        }
        for (Record record : file.getRecords()) {

            String domainSOC = record.getValueByColumnName("hasScope");
            if (domainSOC != null && !domainSOC.isEmpty()) {
                addSOCReference(domainSOC);
            }
            String timeSOC = record.getValueByColumnName("hasTimeScope");
            if (timeSOC != null && !timeSOC.isEmpty()) {
                addSOCReference(timeSOC);
            } 
            String spaceSOC = record.getValueByColumnName("hasSpaceScope");
            if (spaceSOC != null && !spaceSOC.isEmpty()) {
                addSOCReference(spaceSOC);
            } 

        }
    }

    private void addSOCReference(String socUri) {
        if (socUri == null || socUri.isEmpty()) {
            return;
        }
        System.out.println("SSDSheet: socUri=[" + socUri + "]");
        List<String> list = mapContent.get(socUri);
        if (list != null) {
            System.out.println("SSDSheet:      soc=[found] socReference=[" + list.get(6) + "]");
        } else {
            System.out.println("SSDSheet:      soc=[NOT found]");
        }
        if (list != null && list.get(6) != null && !list.get(6).isEmpty()) {
            mapReferences.put(socUri, list.get(6));
            return;
        }
        return;
    }

    public File downloadFile(String fileURL, String fileName) {
        if (fileURL == null || fileURL.length() == 0) {
            return null;
        } else {
            try {
                URL url = new URL(fileURL);
                File file = new File(fileName);
                FileUtils.copyURLToFile(url, file);
                return file;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
    }

    public boolean checkCellValue(String str) {
        if(str.contains(",")){
            return false;
        }
        if(str.contains(" ")){
            return false;
        }
        return true;
    }

}
