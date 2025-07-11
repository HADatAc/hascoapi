package org.hascoapi.ingestion;

import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.TriggeringEvent;
//import org.hascoapi.console.models.SysUser;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.HADatAcThing;
//import org.hascoapi.entity.pojo.Measurement;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.Templates;
import org.hascoapi.vocabularies.HASCO;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.lang.Exception;

public class STRMessageGenerator extends BaseGenerator {

    final String kbPrefix = ConfigProp.getKbPrefix();
    private String startTime = "";
    private Study study = null;
    private RecordFile specRecordFile = null;
    private String errorMessage = "";
    private String errorArgument = "";

    public STRMessageGenerator(DataFile dataFile, Study study, RecordFile specRecordFile, String startTime) {
        super(dataFile);
    	System.out.println("...inside STRMessageGenerator.");
		this.file = specRecordFile;
		this.records = file.getRecords();
        this.study = study;
        this.specRecordFile = specRecordFile;
        this.startTime = startTime;
        dataFile.getLogger().println("STRMessageGenerator: End of constructor -> Number of records: " + specRecordFile.getNumberOfRows());
    }

    @Override
    public void initMapping() {
        mapCol.clear();
        mapCol.put("hasURI", templates.getMESSAGEURI());
        mapCol.put("DataDict", templates.getDATADICTIONARYNAME());
        mapCol.put("MessageProtocol", templates.getMESSAGEPROTOCOL());
        mapCol.put("MessageIP", templates.getMESSAGEIP());
        mapCol.put("MessagePort", templates.getMESSAGEPORT());
        mapCol.put("MessageName", templates.getMESSAGENAME());
        mapCol.put("OwnerEmail", templates.getOWNEREMAIL());
        mapCol.put("PermissionUri", templates.getPERMISSIONURI());

    }

    public boolean isValid() {
    	for (Record rec: specRecordFile.getRecords()) {
            String uri = URIUtils.replacePrefixEx(getUri(rec));
            if (uri.isEmpty()) {
            	errorMessage = "STR_00011";
                errorArgument = "";
                return false;
            }
            String sddUri = URIUtils.replacePrefixEx(getSdd(rec));
            if (sddUri.isEmpty()) {
            	errorMessage = "STR_00012";
                errorArgument = "";
                return false;
            }
            SDD sdd = SDD.find(sddUri);
            if (sdd == null) {
            	errorMessage = "STR_00013";
                errorArgument = getSddUri(rec);
                return false;
            } 
            String protocol = getProtocol(rec);
            if (protocol.isEmpty()) {
            	errorMessage = "STR_00017";
                errorArgument = "";
                return false;
            }
            String ip = getIP(rec);
            if (ip.isEmpty()) {
            	errorMessage = "STR_00018";
                errorArgument = "";
                return false;
            }
            String name = getMessageName(rec);
            if (name.isEmpty()) {
            	errorMessage = "STR_00019";
                errorArgument = "";
                return false;
            }
    	}
    	return true;
    }
    
    public String getErrorMessage() {
    	return errorMessage;
    }
    
    public String getErrorArgument() {
    	return errorArgument;
    }
    
    private String getUri(Record rec) {
        String uri = rec.getValueByColumnName(mapCol.get("hasURI")).equalsIgnoreCase("NULL")? 
                "" : rec.getValueByColumnName(mapCol.get("hasURI"));
        return uri;
    }

    private String getSddUri(Record rec) {
        String sdd = rec.getValueByColumnName(mapCol.get("DataDict")).equalsIgnoreCase("NULL")? 
                "" : rec.getValueByColumnName(mapCol.get("DataDict"));
        return sdd;
    }

    private String getSdd(Record rec) {
        String sdd = getSddUri(rec);
        if (!sdd.isEmpty()) {
        	sdd = kbPrefix + "DAS-" + sdd;
        }
        return sdd;
    }

    private String getMessageName(Record rec) {
        String messageName = rec.getValueByColumnName(mapCol.get("MessageName")).equalsIgnoreCase("NULL")? 
                "" : rec.getValueByColumnName(mapCol.get("MessageName"));
        return messageName;
    }

    public String getProtocol(Record rec) {
        String messageProtocol = rec.getValueByColumnName(mapCol.get("MessageProtocol")).equalsIgnoreCase("NULL")? 
                "" : rec.getValueByColumnName(mapCol.get("MessageProtocol"));
        return messageProtocol;
    }
    
    public String getIP(Record rec) {
        String messageIP = rec.getValueByColumnName(mapCol.get("MessageIP")).equalsIgnoreCase("NULL")? 
                "" : rec.getValueByColumnName(mapCol.get("MessageIP"));
        return messageIP;
    }
    
    public String getPort(Record rec) {
        String messagePort = rec.getValueByColumnName(mapCol.get("MessagePort")).equalsIgnoreCase("NULL")? 
                "" : rec.getValueByColumnName(mapCol.get("MessagePort"));
        return messagePort;
    }

    private String getOwnerEmail(Record rec) {
        //System.out.println("STRGenerator: owner email's label is [" + Templates.OWNEREMAIL + "]");
        String ownerEmail = rec.getValueByColumnName(mapCol.get("OwnerEmail"));
        if(ownerEmail.equalsIgnoreCase("NULL") || ownerEmail.isEmpty()) {
            return "";
        } else {
            return ownerEmail;
        }
    }

    private String getPermissionUri(Record rec) {
        String perm = rec.getValueByColumnName(mapCol.get("PermissionUri"));
        return URIUtils.replacePrefixEx(perm);
    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();
		dataFile.getLogger().println("STRMessageGenerator: At createRow. Stream is [" + getUri(rec) + "].");
		row.put("hasURI", getUri(rec));
		row.put("a", "hasco:DataAcquisition");
		row.put("rdfs:label", getMessageName(rec));
		row.put("hasco:hasProtocol", URIUtils.replacePrefixEx(getProtocol(rec)));
		row.put("hasco:hasIP", getIP(rec));
		row.put("hasco:hasPort", getPort(rec));
		row.put("hasco:isDataAcquisitionOf", study.getUri());
		row.put("hasco:hasSchemaSpec", URIUtils.replacePrefixEx(getSdd(rec)));
        row.put("hasco:hasMessageStatus", HASCO.SUSPENDED);
		if (startTime.isEmpty()) {
			row.put("prov:startedAtTime", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).format(new Date()));
		} else {
			row.put("prov:startedAtTime", startTime);
		}
    	return row;
    }
    
    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
	    Map<String, Object> row = createRow(rec, rowNumber);
	
	    String ownerEmail = getOwnerEmail(rec);
        /*  TODO 
        if (ownerEmail.isEmpty()) {
	        if (null != dataFile) {
	            ownerEmail = dataFile.getOwnerEmail();
	            if (ownerEmail.isEmpty()) {
	                throw new Exception(String.format("Owner Email is empty from records for the uploaded file!"));
	            }
	        } else {
	            throw new Exception(String.format("Owner Email is not specified for Row %s!", rowNumber));
	        }
	    }
        */
	
	    String permissionUri = getPermissionUri(rec);
        /*  TODO
        if (permissionUri.isEmpty()) {
	        SysUser user = SysUser.findByEmail(ownerEmail);
	        if (null != user) {
	            permissionUri = user.getUri();
	            if (permissionUri.isEmpty()) {
	                throw new Exception(String.format("URI is empty for the user with email %s", ownerEmail));
	            }
	        } else {
	            throw new Exception(String.format("Permission URI is not specified for Row %s!", rowNumber));
	        }
	    }
        */

	    return createSTR(row, ownerEmail, permissionUri);
    }

    private Stream createSTR(
            Map<String, Object> row, 
            String ownerEmail, 
            String permissionUri) throws Exception {

        Stream str = new Stream();

        str.setUri(URIUtils.replacePrefixEx((String)row.get("hasURI")));
        //String message = "createStr [1/5] - Creating STR with URI=" + str.getUri();
        //logger.println(message);
        str.setLabel(URIUtils.replacePrefixEx((String)row.get("rdfs:label")));
        str.setStudyUri(URIUtils.replacePrefixEx((String)row.get("hasco:isDataAcquisitionOf")));
        str.setSemanticDataDictionaryUri(URIUtils.replacePrefixEx((String)row.get("hasco:hasSchemaSpec")));
        str.setTriggeringEvent(TriggeringEvent.INITIAL_DEPLOYMENT);
        // TODO
        //str.setNumberDataPoints(Measurement.getNumByDataAcquisition(str));
        str.setNumberDataPoints(0);

        setStudyUri(URIUtils.replacePrefixEx((String)row.get("hasco:isDataAcquisitionOf")));

        str.setMessageProtocol((String)row.get("hasco:hasProtocol"));
        str.setMessageIP((String)row.get("hasco:hasIP"));
        str.setMessagePort((String)row.get("hasco:hasPort"));
        str.addCellScopeName("STREAM");
        str.addCellScopeUri("STREAM");
        String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        if (startTime.isEmpty()) {
            //str.setStartedAt(new DateTime(new Date()));
        } else {
            //str.setStartedAt(DateTimeFormat.forPattern(pattern).parseDateTime(startTime));
        }

        //message = "createStr [3/5] - Specified owner email: [" + ownerEmail + "]";
        //logger.println(message);
        /* TODO 
        SysUser user = SysUser.findByEmail(ownerEmail);
        if (null == user) {
            throw new Exception(String.format("The specified owner email %s is not a valid user!", ownerEmail));
        } else {
            str.setOwnerUri(user.getUri());
            str.setPermissionUri(permissionUri);
        }
        */

        str.setHasStreamStatus(HASCO.DRAFT);

        return str;
    }
        
    @Override
    public String getTableName() {
        return "DataAcquisition";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in STRMessageGenerator: " + e.getMessage();
    }

}

