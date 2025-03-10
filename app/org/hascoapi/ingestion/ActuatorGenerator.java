package org.hascoapi.ingestion;

import java.lang.String;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.vocabularies.VSTOI;

public class ActuatorGenerator extends BaseGenerator {

	protected String hasStatus = "";

	public String getHasStatus() {
		return this.hasStatus;
	}
    
	public void setHasStatus(String hasStatus) {
		this.hasStatus = hasStatus;
	}

	public ActuatorGenerator(DataFile dataFile, String hasStatus) {
		super(dataFile);
		this.setHasStatus(hasStatus);
	}

	@Override
    public void createRows() throws Exception {    		

		if (records == null) {
			System.out.println("[ERROR] ActuatorGenerator: no records to process.");
            return;
        }

		System.out.println("inside of ActuatorGenerator's createRows");
		System.out.println("inside of ActuatorGenerator's: total of records=" + records.size());

		int priority = 1;
		
        int rowNumber = 0;
        int skippedRows = 0;
        Record lastRecord = null;
        for (Record record : records) {
        	if (lastRecord != null && record.equals(lastRecord)) {
        		skippedRows++;
        	} else {
        		Map<String, Object> tempRow = createRow(record, ++rowNumber);
				//for (Map.Entry<String, Object> entry : tempRow.entrySet()) {
				//	System.out.println(entry.getKey() + ": " + entry.getValue());
				//}
				if (tempRow != null) {
					tempRow.put("rdf:subClassOf", VSTOI.ACTUATOR);
					tempRow.put("hasco:hascoType", VSTOI.ACTUATOR);
					if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
						tempRow.put("vstoi:hasStatus", this.getHasStatus());
					}
					tempRow.put("rdfs:label", "Actuator" );
					tempRow.put("rdfs:comment", "Actuator");
					rows.add(tempRow);
        		}
        	}
        }

        if (skippedRows > 0) {
        	System.out.println("Skipped rows: " + skippedRows);
        }

    }
	
	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
		Map<String, Object> row = new HashMap<String, Object>();
		
		for (String header : file.getHeaders()) {
		    if (!header.trim().isEmpty()) {
		        String value = rec.getValueByColumnName(header);
		        if (value != null && !value.isEmpty()) {
		            row.put(header, value);
		        }
		    }
		}
		row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		return row;
	}

	@Override
	public String getTableName() {
		return "Actuator";
	}

	@Override
	public String getErrorMsg(Exception e) {
		e.printStackTrace();
		return "Error in ActuatorGenerator: " + e.getMessage();
	}
}
