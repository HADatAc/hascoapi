package org.hascoapi.entity.pojo;

import java.util.HashMap;
import java.util.Map;

import org.hascoapi.ingestion.Record;
import org.hascoapi.ingestion.RecordFile;


public class DPL {

	private DataFile dplfile = null;
	private Map<String, String> mapCatalog = new HashMap<String, String>();
	
	public DPL(DataFile dataFile) {
		this.dplfile = dataFile;
		readCatalog(dataFile.getRecordFile());
	}
	
	public String getFileName() {
	    return dplfile.getFilename();
    }
	
	public Map<String, String> getCatalog() {
		return mapCatalog;
	}
	
	private void readCatalog(RecordFile file) {
	    if (!file.isValid()) {
            return;
        }
	    
	    for (Record record : file.getRecords()) {
	        mapCatalog.put(record.getValueByColumnIndex(0), record.getValueByColumnIndex(1));
	    }
	}	
}
