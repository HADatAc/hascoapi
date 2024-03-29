package org.hascoapi.utils;

import org.apache.commons.configuration2.INIConfiguration;

public class Templates {

    public String templateFile = null;
    public INIConfiguration iniConfig = null;
    
    public Templates(String templateFile) {
        this.templateFile = templateFile;
        this.iniConfig = new HASCOConfig(templateFile); 
    }

    // STD Template (Study)
    public String getSTUDYID() {
        return iniConfig.getSection("STD").getString("studyID");  // also in ACQ, PID and SID
    }
    public String getSTUDYTITLE() { 
        return iniConfig.getSection("STD").getString("studyTitle"); 
    }
    public String getSTUDYAIMS() { 
        return iniConfig.getSection("STD").getString("studyAims");
    }
    public String getSTUDYSIGNIFICANCE() { 
        return iniConfig.getSection("STD").getString("studySignificance"); 
    }
    public String getNUMSUBJECTS() { 
        return iniConfig.getSection("STD").getString("numSubjects"); 
    }
    public String getNUMSAMPLES() { 
        return iniConfig.getSection("STD").getString("numSamples"); 
    }
    public String getINSTITUTION() { 
        return iniConfig.getSection("STD").getString("institution"); }
    public String getPI() { 
        return iniConfig.getSection("STD").getString("PI"); }
    public String getPIADDRESS() { 
        return iniConfig.getSection("STD").getString("PIAddress"); }
    public String getPICITY() { 
        return iniConfig.getSection("STD").getString("PICity"); }
    public String getPISTATE() { 
        return iniConfig.getSection("STD").getString("PIState"); }
    public String getPIZIPCODE() { 
        return iniConfig.getSection("STD").getString("PIZipCode"); }
    public String getPIEMAIL() { 
        return iniConfig.getSection("STD").getString("PIEmail"); }
    public String getPIPHONE() { 
        return iniConfig.getSection("STD").getString("PIPhone"); }
    public String getCPI1FNAME() { 
        return iniConfig.getSection("STD").getString("CPI1FName"); }
    public String getCPI1LNAME() { 
        return iniConfig.getSection("STD").getString("CPI1LName"); }
    public String getCPI1EMAIL() { 
        return iniConfig.getSection("STD").getString("CPI1Email"); }
    public String getCPI2FNAME() { 
        return iniConfig.getSection("STD").getString("CPI2FName"); }
    public String getCPI2LNAME() { 
        return iniConfig.getSection("STD").getString("CPI2LName"); }
    public String getCPI2EMAIL() { 
        return iniConfig.getSection("STD").getString("CPI2Email"); }
    public String getCONTACTFNAME() { 
        return iniConfig.getSection("STD").getString("contactFName"); }
    public String getCONTACTLNAME() { 
        return iniConfig.getSection("STD").getString("contactLName"); }
    public String getCONTACTEMAIL() { 
        return iniConfig.getSection("STD").getString("contactEmail"); }
    public String getCREATEDDATE() { 
        return iniConfig.getSection("STD").getString("createdDate"); }
    public String getUPDATEDDATE() { 
        return iniConfig.getSection("STD").getString("updatedDate"); }
    public String getDCACCESSBOOL() { 
        return iniConfig.getSection("STD").getString("DCAccessBool"); }
    public String getEXTSRC() { 
        return iniConfig.getSection("STD").getString("externalSource"); }

    // ACQ Template
    public String getACQ_DATAACQUISITIONNAME() { 
        return iniConfig.getSection("ACQ").getString("DataAcquisitionName"); }
    public String getACQ_METHOD() { 
        return iniConfig.getSection("ACQ").getString("Method"); }
    public String getACQ_SDDTUDYID() { 
        return iniConfig.getSection("ACQ").getString("SDDtudyName"); }
    public String getACQ_DATADICTIONARYNAME() { 
        return iniConfig.getSection("ACQ").getString("DataDictionaryName"); }
    public String getACQ_EPILAB() { 
        return iniConfig.getSection("ACQ").getString("Epi/Lab"); }
    public String getACQ_OWNEREMAIL() { 
        return iniConfig.getSection("ACQ").getString("OwnerEmail"); }
    public String getACQ_PERMISSIONURI() { 
        return iniConfig.getSection("ACQ").getString("PermissionURI"); }

    // STR Template
    public String getDATAACQUISITIONNAME() { 
        return iniConfig.getSection("STR").getString("DataAcquisitionName"); }
    public String getMETHOD() { 
        return iniConfig.getSection("STR").getString("Method"); }
    public String getDATADICTIONARYNAME() { 
        return iniConfig.getSection("STR").getString("DataDictionaryName"); }
    public String getSDDTUDYID() { return iniConfig.getSection("STR").getString("SDDtudyName"); }
    public String getTOPICNAME() { return iniConfig.getSection("STR").getString("TopicName"); }
    public String getEPILAB() { return iniConfig.getSection("STR").getString("Epi/Lab"); }
    public String getOWNEREMAIL() { return iniConfig.getSection("STR").getString("OwnerEmail"); }
    public String getPERMISSIONURI() { return iniConfig.getSection("STR").getString("PermissionURI"); }
    public String getDEPLOYMENTURI() { return iniConfig.getSection("STR").getString("DeploymentUri"); }
    public String getROWSCOPE() { return iniConfig.getSection("STR").getString("RowScope"); }
    public String getCELLSCOPE() { return iniConfig.getSection("STR").getString("CellScope"); }
    public String getMESSAGESTREAMURI() { return iniConfig.getSection("STR").getString("StreamURI"); }
    public String getMESSAGEURI() { return iniConfig.getSection("STR").getString("hasURI"); }
    public String getMESSAGEPROTOCOL() { return iniConfig.getSection("STR").getString("MessageProtocol"); }
    public String getMESSAGEIP() { return iniConfig.getSection("STR").getString("MessageIP"); }
    public String getMESSAGEPORT() { return iniConfig.getSection("STR").getString("MessagePort"); }
    public String getMESSAGENAME() { return iniConfig.getSection("STR").getString("MessageName"); }

    // SDDA, SDDE, SDDO Template (Part of SDD)
    public String getLABEL() { return iniConfig.getSection("SDDA").getString("Label"); }     // also in PV
    public String getATTRIBUTETYPE() { return iniConfig.getSection("SDDA").getString("AttributeType"); }
    public String getATTTRIBUTEOF() { return iniConfig.getSection("SDDA").getString("AttributeOf"); }
    public String getUNIT() { return iniConfig.getSection("SDDA").getString("Unit"); }
    public String getTIME() { return iniConfig.getSection("SDDA").getString("Time"); }
    public String getENTITY() { return iniConfig.getSection("SDDA").getString("Entity"); }
    public String getROLE() { return iniConfig.getSection("SDDA").getString("Role"); }
    public String getRELATION() { return iniConfig.getSection("SDDA").getString("Relation"); }
    public String getINRELATIONTO() { return iniConfig.getSection("SDDA").getString("InRelationTo"); }
    public String getWASDERIVEDFROM() { return iniConfig.getSection("SDDA").getString("WasDerivedFrom"); }
    public String getWASGENERATEDBY() { return iniConfig.getSection("SDDA").getString("WasGeneratedBy"); }

    // PV Template (Part of SDD)
    public String getCODE() { return iniConfig.getSection("PV").getString("Code"); }
    public String getCODEVALUE() { return iniConfig.getSection("PV").getString("CodeValue"); }
    public String getCLASS() { return iniConfig.getSection("PV").getString("Class"); }

    // SID Template
    public String getSAMPLEID() { return iniConfig.getSection("SID").getString("sampleID"); }
    public String getSAMPLESTUDYID() { return iniConfig.getSection("SID").getString("sampleStudyID"); }
    public String getSAMPLESUFFIX() { return iniConfig.getSection("SID").getString("sampleSuffix"); }
    public String getSUBJECTID() { return iniConfig.getSection("SID").getString("subjectID"); }  // also in PID
    public String getSAMPLETYPE() { return iniConfig.getSection("SID").getString("sampleType"); }
    public String getSAMPLINGMETHOD() { return iniConfig.getSection("SID").getString("samplingMethod"); }
    public String getSAMPLINGVOL() { return iniConfig.getSection("SID").getString("samplingVol"); }
    public String getSAMPLINGVOLUNIT() { return iniConfig.getSection("SID").getString("samplingVolUnit"); }
    public String getSTORAGETEMP() { return iniConfig.getSection("SID").getString("storageTemp"); }
    public String getFTCOUNT() { return iniConfig.getSection("SID").getString("FTcount"); }

    // MAP Template
    public String getORIGINALPID() { return iniConfig.getSection("MAP").getString("originalPID"); }
    public String getORIGINALSID() { return iniConfig.getSection("MAP").getString("originalSID"); }
    public String getOBJECTTYPE() { return iniConfig.getSection("MAP").getString("objecttype"); }
    public String getMAPSTUDYID() { return iniConfig.getSection("MAP").getString("studyId"); }
    public String getTIMESCOPEID() { return iniConfig.getSection("MAP").getString("timeScope"); }
}
