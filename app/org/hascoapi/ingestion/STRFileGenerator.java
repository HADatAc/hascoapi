package org.hascoapi.ingestion;

import java.lang.String;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.jena.query.QueryParseException;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
//import org.hascoapi.console.models.SysUser;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.entity.pojo.Study;
import org.hascoapi.entity.pojo.SDD;
import org.hascoapi.entity.pojo.SDDAttribute;
import org.hascoapi.entity.pojo.SDDObject;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Deployment;
import org.hascoapi.entity.pojo.HADatAcThing;
//import org.hascoapi.entity.pojo.Measurement;
import org.hascoapi.entity.pojo.MessageTopic;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.entity.pojo.TriggeringEvent;
import org.hascoapi.entity.pojo.VirtualColumn;
//import org.hascoapi.ingestion.DASOInstanceGenerator;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.Templates;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.HASCO;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import java.lang.Exception;

public class STRFileGenerator extends BaseGenerator {

    final String kbPrefix = ConfigProp.getKbPrefix();
    private long timestamp;
    String startTime = "";
    Study study = null;
    String version = "";
    RecordFile specRecordFile = null;

    public STRFileGenerator(DataFile dataFile, Study study, RecordFile specRecordFile, String startTime, String version, String templateFile) {
        super(dataFile);
		this.file = specRecordFile;
		this.records = file.getRecords();
        this.study = study;
        this.version = version;
        this.specRecordFile = specRecordFile;
        this.startTime = startTime;
        this.templates = new Templates(templateFile);
        dataFile.getLogger().println("STRFileGenerator: End of constructor -> Number of records: " + specRecordFile.getNumberOfRows());
    }

    @Override
    public void initMapping() {
        // Get the current timestamp (in milliseconds)
        timestamp = System.currentTimeMillis();
    }

    private String getSTRName(Record rec) {
    	System.out.println("getSTRName: " + templates.getDATAACQUISITIONNAME() + "  [" + rec.getValueByColumnName(templates.getDATAACQUISITIONNAME()) + "]");
        return rec.getValueByColumnName(templates.getDATAACQUISITIONNAME());
    }

    private String getSDDName(Record rec) {
        String SDDName = rec.getValueByColumnName(templates.getDATADICTIONARYNAME()).equalsIgnoreCase("NULL")?
                "" : rec.getValueByColumnName(templates.getDATADICTIONARYNAME());

        //System.out.println("\n\nSTRGenerator SDDName: " + rec.getValueByColumnName(templates.getDATADICTIONARYNAME())+"\n\n ");
        return SDDName.replace("SDD-","");
    }

    private String getDeployment(Record rec) {
        return rec.getValueByColumnName(templates.getDEPLOYMENTURI());
    }

    private String getCellScope(Record rec) {
        return rec.getValueByColumnName(templates.getCELLSCOPE());
    }

    private String getOwnerEmail(Record rec) {
        //System.out.println("STRGenerator: owner email's label is [" + Templates.OWNEREMAIL + "]");
        String ownerEmail = rec.getValueByColumnName(templates.getOWNEREMAIL());
        if(ownerEmail.equalsIgnoreCase("NULL") || ownerEmail.isEmpty()) {
            return "";
        } else {
            return ownerEmail;
        }
    }

    private String getPermissionUri(Record rec) {
        return rec.getValueByColumnName(URIUtils.replacePrefixEx(templates.getPERMISSIONURI()));
    }

    /** 
    $uri = Utils::uriGen('stream');

    $stream = [
      'uri'                       => $uri,
      'typeUri'                   => HASCO::STREAM,
      'hascoTypeUri'              => HASCO::STREAM,
      'label'                     => 'Stream',
      'method'                    => $form_state->getValue('stream_method'),
      'permissionUri'             => $form_state->getValue('permission_uri'),
      'deploymentUri'             => $deployment,
      'hasVersion'                => $form_state->getValue('stream_version') ?? 1,
      'comment'                   => $form_state->getValue('stream_description'),
      'canUpdate'                 => [$email],
      'designedAt'                => $timestamp,
      'studyUri'                  => Utils::uriFromAutocomplete($form_state->getValue('stream_study')),
      'semanticDataDictionaryUri' => Utils::uriFromAutocomplete($form_state->getValue('stream_semanticdatadictionary')),
      'hasSIRManagerEmail'        => $email,
      'hasStreamStatus'           => HASCO::DRAFT,
    ];

    if ($method === 'files') {
      $stream['datasetPattern'] = $form_state->getValue('stream_datafile_pattern');
      $stream['cellScopeUri']    = [$form_state->getValue('stream_cell_scope_uri')];
      $stream['cellScopeName']   = [$form_state->getValue('stream_cell_scope_name')];
      $stream['messageProtocol']  = '';
      $stream['messageIP']        = '';
      $stream['messagePort']      = '';
      $stream['messageArchiveId'] = '';
      // $stream['messageHeader']    = '';
    */

    public String createStreamUri() throws Exception {

        // Generate a random integer between 10000 and 99999
        Random random = new Random();
        int randomNumber = random.nextInt(99999 - 10000 + 1) + 10000;

		return kbPrefix + "/" + Constants.PREFIX_STREAM + timestamp + randomNumber;
	}

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
    	Map<String, Object> row = new HashMap<String, Object>();
		//dataFile.getLogger().println("STRFileGenerator: At createRow. Row Number " + rowNumber + "  record size: " + rec.size());
		//row.put("hasURI", kbPrefix + "DA-" + getSTRName(rec));
		row.put("hasURI", createStreamUri());
		row.put("a", "hasco:Stream");
		row.put("hasco:hascoType", "hasco:Stream");
		row.put("hasco:hasMethod", "files");
		row.put("rdfs:label", getSTRName(rec));
		row.put("hasco:hasDeployment", getDeployment(rec));
		row.put("hasco:hasStudy", study.getUri());
        row.put("hasco:designedAtTime", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).format(new Date()));
		if (startTime.isEmpty()) {
			row.put("prov:startedAtTime", (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")).format(new Date()));
		} else {
			row.put("prov:startedAtTime", startTime);
		}
		//row.put("hasco:hasSchema", kbPrefix + "DAS-" + getSDDName(rec));
        row.put("hasco:hasSDD", getSDDName(rec));
    	return row;
    }

    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
	    Map<String, Object> row = createRow(rec, rowNumber);
	    if (row == null) {
    		return null;
    	}

        Stream stream = new Stream();

        // CONSTANT PROPERTIES
        stream.setTypeUri(HASCO.STREAM);
        stream.setHascoTypeUri(HASCO.STREAM);
        stream.setTriggeringEvent(TriggeringEvent.INITIAL_DEPLOYMENT);
        stream.setNumberDataPoints(0);
        stream.setHasStreamStatus(HASCO.DRAFT);
        if (version == null && !version.isEmpty()) {
            stream.setHasVersion(version);
        }

        //String pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        if (startTime.isEmpty()) {
            //stream.setStartedAt(new DateTime(new Date()));
        } else {
            //stream.setStartedAt(DateTimeFormat.forPattern(pattern).parseDateTime(startTime));
        }

        // DESIGN PATTERN (and LABEL)
        if (getSTRName(rec) == null || getSTRName(rec).isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("STR_00020");
            //throw new Exception();
            return null;
    	}
        stream.setLabel((String)row.get("rdfs:label"));
        stream.setDatasetPattern((String)row.get("rdfs:label"));

        // URI
        stream.setUri(URIUtils.replacePrefixEx((String)row.get("hasURI")));
        dataFile.getLogger().println("createStr [1/7] - assigned URI: [" + stream.getUri() + "]");

        // CELL SCOPE
        String cellScopeStr = getCellScope(rec);
        String[] cellList = null;
        String[] elementList = null;
        if (cellScopeStr != null && !cellScopeStr.equals("")) {
            if (!cellScopeStr.startsWith("<")) {
                dataFile.getLogger().printExceptionById("STR_00022");
                throw new Exception();
            } else if (!cellScopeStr.endsWith(">")) {
                dataFile.getLogger().printExceptionById("STR_00023");
                throw new Exception();
            } else {
                cellScopeStr = cellScopeStr.substring(1, cellScopeStr.length()-1);
                cellList = cellScopeStr.split(";");
                for (String cellSpec : cellList) {
                    cellSpec = cellSpec.trim();
                    if (!cellSpec.startsWith("<")) {
                        dataFile.getLogger().printExceptionByIdWithArgs("STR_00024", cellSpec);
                        throw new Exception();
                    } else if (!cellSpec.endsWith(">")) {
                        dataFile.getLogger().printExceptionByIdWithArgs("STR_00025", cellSpec);
                        throw new Exception();
                    } else {
                        cellSpec = cellSpec.substring(1, cellSpec.length()-1);
                        elementList = cellSpec.split(",");
                        if (elementList.length != 2) {
                            dataFile.getLogger().printExceptionByIdWithArgs("STR_00026", cellSpec);
                            throw new Exception();
                        }
                        stream.addCellScopeName(elementList[0]);
                        stream.addCellScopeUri(URIUtils.replacePrefixEx((String)elementList[1]));
                    }
                }
            }
        }
        dataFile.getLogger().println("createStr [2/7] - Specified CellScope: [" + cellScopeStr + "]");

        // OWNER EMAIL
        String ownerEmail = getOwnerEmail(rec);
        dataFile.getLogger().println("createStr [3/7] - Specified owner email: [" + ownerEmail + "]");
        stream.addCanUpdate(ownerEmail);

	    // PERMISSION URI
	    String permissionUri = URIUtils.replacePrefixEx(getPermissionUri(rec));
	    stream.setPermissionUri(permissionUri);
        dataFile.getLogger().println("createStr [4/7] - Specified permission: [" + permissionUri + "]");

        // STUDY 
        stream.setStudyUri(URIUtils.replacePrefixEx((String)row.get("hasco:hasStudy")));
        dataFile.getLogger().println("createStr [5/7] - Specified study: [" + stream.getStudyUri() + "]");

        // DEPLOYMENT
        if (row.get("hasco:hasDeployment") == null || ((String)row.get("hasco:hasDeployment")).isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("STR_00022");
            throw new Exception();
        }
        stream.setDeploymentUri(URIUtils.replacePrefixEx((String)row.get("hasco:hasDeployment")));
        Deployment deployment = Deployment.find(stream.getDeploymentUri());
        if (deployment == null) {
            dataFile.getLogger().printExceptionByIdWithArgs("STR_00022");
            throw new Exception();
        }
        dataFile.getLogger().println("createStr [6/7] - Specified deployment: [" + stream.getDeploymentUri() + "]");

        // SDD
	    if (getSDDName(rec) == null || getSDDName(rec).isEmpty()) {
            dataFile.getLogger().printExceptionById("STR_00021");
            throw new Exception();
	    }
        stream.setSemanticDataDictionaryUri(URIUtils.replacePrefixEx((String)row.get("hasco:hasSDD")));
        SDD schema = SDD.find(stream.getSemanticDataDictionaryUri());
        if (schema == null) {
            dataFile.getLogger().printExceptionByIdWithArgs("STR_00035", stream.getSemanticDataDictionaryUri());
            throw new Exception();
        }
        dataFile.getLogger().println("createStr [7/7] - Specified SDD: [" + stream.getSemanticDataDictionaryUri() + "]");

	    if (!isFileStreamValid(stream)) {
            throw new Exception();
	    }
        return stream;
    }

    public boolean isFileStreamValid(Stream str) {
    	boolean resp = true;
        //Record record = dataFile.getRecordFile().getRecords().get(0);
        //String studyName = record.getValueByColumnName("Study ID");
        //String studyUri = URIUtils.replacePrefixEx(ConfigProp.getKbPrefix() + "STD-" + studyName);

        //dataFile.getLogger().println("Study ID found: " + studyName);
        List<VirtualColumn> vcList = VirtualColumn.findVCsByStudy(str.getStudy().getUri());
        // map of SOCReference and grounding label
        Map<String, String> refList = new HashMap<String, String>();
        // map of daso uri and SOCReference
        Map<String, String> tarList = new HashMap<String, String>();

        //System.out.println("");
        //System.out.println("");
        //System.out.println("");
        //System.out.println("====>>> Inside IS FILE STREAM VALID " + str.getSchema().getUri());
        //System.out.println("");
        //System.out.println("Target list: ");

        for (VirtualColumn vc: vcList) {
            if (vc.getGroundingLabel().length() > 0) {
                refList.put(vc.getSOCReference(), vc.getGroundingLabel());
                String tarUri = URIUtils.replacePrefixEx(kbPrefix + "DASO-" + study.getId() + "-" + vc.getSOCReference().trim().replace(" ","").replace("_","-").replace("??", ""));
                //System.out.println("  - (RefList)  [" + vc.getGroundingLabel() + "]  [" + vc.getSOCReference() + "]");
                tarList.put(tarUri,  vc.getSOCReference());
                //System.out.println("  - (TarList)  [" + vc.getGroundingLabel() + "]  [" + tarUri + "]");
            }
        }

        String queryString = null;
        ResultSetRewindable resultsrw = null;

        //System.out.println("DASOs requiring role assignments: ");
        Map<String, String> dasoPL = new HashMap<String, String>();
        List<SDDObject> dasos = new ArrayList<SDDObject>();
        List<String> roles = new ArrayList<String>();
        for (SDDAttribute attr : str.getSemanticDataDictionary().getAttributes()) {
            if (attr.getObjectViewLabel().length() > 0) {
                if (!roles.contains(attr.getObjectViewLabel())) {
                    roles.add(attr.getObjectViewLabel());
                    dasos.add(attr.getObject());
                    //System.out.println("  - DASO: " + attr.getObjectViewLabel() + "  " + attr.getObject().getUri());
                }
            }
        }
        dataFile.getLogger().println("DASOs requiring role assignments: " + roles.toString());
        //System.out.println("Existing mappings " + refList.toString());
        String dasUri ="";
        for (SDDObject daso : dasos) {
        	//System.out.println("---->>> Processing DASO " + daso.getUri());
        	if (null == daso) {
                continue;
            }

            if (daso.getEntityLabel() == null || daso.getEntityLabel().length() == 0) {
                dataFile.getLogger().printExceptionByIdWithArgs("STR_00009", daso.getLabel());
            	//System.out.println("DASO with no entity/entity label" + daso.getUri());
                resp = false;
            } else if (refList.containsKey(daso.getLabel())) {
            	dataFile.getLogger().println("PATH: " + daso.getLabel() + " has role \"" + refList.get(daso.getLabel()) + "\"");
            	//System.out.println("DASO skipped");
            } else {
                dasUri = (daso!=null && daso.getPartOfSchema()!=null) ? daso.getPartOfSchema():"";
            	queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                    		"SELECT ?vc ?soc ?socRef ?vcLabel ?role WHERE { " +
                	    "   <" + daso.getUri() + "> rdfs:label ?vcLabel . " +
                	    "   ?soc hasco:hasReference ?vc . " +
                	    "   ?vc hasco:hasSOCReference ?socRef . " +
                	    "   OPTIONAL { ?soc hasco:hasRoleLabel ?role . } . " +
                	    "   FILTER (?socRef = ?vcLabel ) . " +
                	    " }";

                resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(
                		CollectionUtil.Collection.SPARQL_QUERY), queryString);

                if (resultsrw.hasNext()) {
                    QuerySolution soln = resultsrw.next();

                    if (soln.get("role").isLiteral() && soln.getLiteral("role") != null) {
                    	dataFile.getLogger().println("PATH: " + daso.getLabel() + " has role \"" + soln.getLiteral("role").toString() + "\"");
                        dasoPL.put(daso.getUri(), soln.getLiteral("role").toString() );
                        //if (refList.containsKey(soln.getLiteral("x").toString())) {
                            //answer.add(refList.get(soln.getLiteral("x").toString()));
                            //dataFile.getLogger().println("PATH: DASO: " + daso.getLabel() + ": \"" + answer.get(1) + " " + answer.get(0) + "\"");
                            //dasoPL.put(daso.getUri(), answer.get(1) + " " + answer.get(0));
                            //found = true;
                            //break;
                        //}
                    }
                } else {
                	//dataFile.getLogger().println(daso.getUri() + " misses a role");
                	//System.out.println(daso.getUri() + " misses a role");

                	List<String> answer = new ArrayList<String>();
                    answer.add(daso.getEntityLabel());
                    Boolean found = false;

                	//System.out.println(daso.getUri() + " size of refList: " + refList.size());
                    for (String j : refList.keySet()) {

                        //dataFile.getLogger().println("daso.getUri(): [" + daso.getUri() + "];   J is [" + j + "]");
                    	//System.out.println("daso.getUri(): [" + daso.getUri() + "];   J is [" + j + "]");


                        if (found == false) {
                            String target = kbPrefix + "DASO-" + study.getId() + "-" + j.trim().replace(" ","").replace("_","-").replace("??", "");

                            queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                                    "SELECT ?x ?o WHERE { \n" +
                                    "<" + daso.getUri() + "> ?p ?x . \n" +
                                    "   ?x ?p1 ?o .  \n" +
                                    "   OPTIONAL {?o ?p2 " + target + " } " +
                                    "}";
                            //System.out.println(queryString);

                            resultsrw = SPARQLUtils.select(CollectionUtil.getCollectionPath(
                                    CollectionUtil.Collection.SPARQL_QUERY), queryString);

                            //System.out.println("HERE 3");
                            if (!resultsrw.hasNext()) {
                                dataFile.getLogger().printException("STR_00009");
                                resp = false;
                            }

                            while (resultsrw.hasNext()) {
                                QuerySolution soln = resultsrw.next();
                                try {
                                    if (soln != null) {
                                        try {
                                        	//System.out.println("HERE 4");
                                        	if (soln.get("x").isResource()){
                                                if (soln.getResource("x") != null) {
                                                    //System.out.println("Resource X: " + soln.getResource("x").toString());
                                                    if (tarList.containsKey(soln.getResource("x").toString())) {
	                                                    //System.out.println("IS MATCH");
                                                        answer.add(refList.get(tarList.get(soln.getResource("x").toString())));
                                                        dataFile.getLogger().println("PATH: " + daso.getLabel() + " has role \"" + answer.get(1) + " " + answer.get(0) + "\"");
                                                        dasoPL.put(daso.getUri(), answer.get(1) + " " + answer.get(0));
                                                        found = true;
                                                        break;
                                                    } else {
                                                        if (soln.get("o").isResource()){
                                                            if (soln.getResource("o") != null) {
                                                                if (tarList.containsValue(soln.getResource("o").toString())) {
                                                                    answer.add(str.getSemanticDataDictionary().getObject(soln.getResource("o").toString()).getEntityLabel());
                                                                    dasoPL.put(daso.getUri(), answer.get(1) + " " + answer.get(0));
                                                                    found = true;
                                                                    break;
                                                                }
                                                            }
                                                        } else if (soln.get("o").isLiteral()) {
                                                            if (soln.getLiteral("o") != null) {
                                                                if (refList.containsKey(soln.getLiteral("o").toString())) {
                                                                    answer.add(refList.get(soln.getLiteral("o").toString()));
                                                                    dataFile.getLogger().println("PATH: DASO: " + daso.getLabel() + ": \"" + answer.get(1) + " " + answer.get(0) + "\"");
                                                                    dasoPL.put(daso.getUri(), answer.get(1) + " " + answer.get(0));
                                                                    found = true;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            } else if (soln.get("x").isLiteral()) {
                                                if (soln.getLiteral("x") != null) {
                                                    //System.out.println("Resource X (literal): " + str.getSchema().getObject(soln.getLiteral("x").toString()));
                                                    if (refList.containsKey(soln.getLiteral("x").toString())) {
                                                        answer.add(refList.get(soln.getLiteral("x").toString()));
                                                        dataFile.getLogger().println("PATH: DASO: " + daso.getLabel() + ": \"" + answer.get(1) + " " + answer.get(0) + "\"");
                                                        dasoPL.put(daso.getUri(), answer.get(1) + " " + answer.get(0));
                                                        found = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        	//System.out.println("HERE 5");

                                        } catch (Exception e1) {
                                            //return false;
                                        }
                                    }
                                } catch (Exception e) {
                                    dataFile.getLogger().printException(e.getMessage());
                                    resp = false;
                                }
                            }
                        }

                    }
                    if (found == false) {
                        dataFile.getLogger().println("PATH: DASO: " + daso.getLabel() + " Path connections can not be found ! Check for paths in SSD/SDD definition. ");
                        //System.out.println("PATH: DASO: " + daso.getLabel() + " Path connections can not be found ! check the SDD definition. ");
                        resp = false;
                    }
                }
                //System.out.println("<<<---- END OF DASO PROCESSING " + daso.getUri());
            }
        }
        //insert the triples

        for (String uri : dasoPL.keySet()) {
            String insert = "";
            insert += NameSpaces.getInstance().printSparqlNameSpaceList();
            insert += "INSERT DATA {  ";
            insert += "graph  <"+dasUri+"> { " ;
            insert += "<" + uri + ">" + " hasco:hasRoleLabel  \"" + dasoPL.get(uri) + "\" . ";
            insert += "}} ";

            try {
                UpdateRequest request = UpdateFactory.create(insert);
                UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                        request, CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE));
                processor.execute();
            } catch (QueryParseException e) {
                System.out.println("QueryParseException due to update query: " + insert);
                resp = false;
                throw e;
            }
        }
        //System.out.println("<<<===== END OF FILE PROCESSING " + str.getSchema().getUri());
        return resp;
    }

    @Override
    public String getTableName() {
        return "STR";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in STRFileGenerator: " + e.getMessage();
    }

}

