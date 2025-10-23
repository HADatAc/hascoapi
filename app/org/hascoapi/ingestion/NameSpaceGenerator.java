package org.hascoapi.ingestion;

import java.lang.String;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.HADatAcThing;
import org.hascoapi.entity.pojo.NameSpace;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.vocabularies.HASCO;

public class NameSpaceGenerator extends BaseGenerator {

	public NameSpaceGenerator(DataFile dataFile, String templateFile) {
        super(dataFile, null, templateFile);
        this.fileName = dataFile.getFilename();
	}

	@Override
	public void initMapping() {
		try {
			System.out.println("initMapping of NameSpaceGenerator");
			mapCol.clear();
			mapCol.put("nsAbbrev", templates.getNSABBREV());
			mapCol.put("nsUri", templates.getNSNAME());
			mapCol.put("nsFormat", templates.getNSFORMAT());
			mapCol.put("nsSource", templates.getNSSOURCE());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private String getNSAbbreviation(Record rec) {
		return rec.getValueByColumnName(mapCol.get("nsAbbrev"));
	}
	
	private String getNSUri(Record rec) {
		System.out.println("mapCol: [" + mapCol.get("nsUri") + "]");
		System.out.println("value: [" + rec.getValueByColumnName(mapCol.get("nsUri")) + "]");
		return rec.getValueByColumnName(mapCol.get("nsUri"));
	}
	
	private String getNSFormat(Record rec) {
		return rec.getValueByColumnName(mapCol.get("nsFormat"));
	}
	
	private String getNSSource(Record rec) {
		return rec.getValueByColumnName(mapCol.get("nsSource"));
	}
		
	@Override
	public String getTableName() {
		return "NameSpace";
	}

    public HADatAcThing createNameSpace(Record record) throws Exception {

		//System.out.println("Inside createNameSpace: ");

		//System.out.println("Printing record:");
		//for (int aux=0; aux < record.size(); aux++) {
		//	System.out.println("  - [" + record.getValueByColumnIndex(aux) + "]");
		//}

		if (record.size() <= 0) {
			return null;
		}

		String nsUri = getNSUri(record); 
		if (nsUri == null || nsUri.isEmpty()) {
			return null;
			//throw new Exception("[ERROR] NameSpaceGenerator: no NS URI has been provided.");
    	}

		String nsAbbrev = getNSAbbreviation(record);
		if (nsAbbrev == null || nsAbbrev.isEmpty()) {
			throw new Exception("[ERROR] NameSpaceGenerator: no NS Abbreviation has been provided.");
		}
		if (nsAbbrev.matches(".*[A-Z].*")) {
			throw new Exception("[ERROR] NameSpaceGenerator: NS Abbreviation cannot have captial letters.");
		}

		if (NameSpace.find(nsUri) != null) {
			System.out.println("[WARNING] NameSpaceGenerator: NS with URI [" + nsUri + "] was not created - it already exists.");
			return null;
		}

		String source = getNSSource(record);

        NameSpace ns = new NameSpace();
		ns.setNamedGraph(Constants.DEFAULT_REPOSITORY);
        ns.setLabel(nsAbbrev);
        ns.setUri(nsUri);
        ns.setTypeUri(HASCO.ONTOLOGY);
        ns.setHascoTypeUri(HASCO.ONTOLOGY);
        ns.setSourceMime(getNSFormat(record));
        ns.setSource(source);
        ns.setComment("Ingested by NameSpaceGenerator");
        ns.setPriority(100);
        ns.setPermanent(false);

		ns.setNumberOfLoadedTriples();

		if (NameSpace.findInMemoryByAbbreviation(nsAbbrev) != null) {
			System.out.println("[WARNING] NameSpaceGenerator: NS with Abbreviation [" + nsAbbrev + "] was not created - it already exists.");
			return null;
		}

		System.out.println("NameSpaceGenerator: adding " + nsAbbrev + " into In-Memory NameSpaces.");

		NameSpaces.getInstance().addNamespace(ns);

		// ** ATTENTION **: the namespace needs to be added to the cache in NameSpaces.getInstance()
		//                  before the newly created namespace ns can be saved with ns.save(). Otherwise
		//                  the save command is going to cause the ns's uri to be invalid.  

		System.out.println("NameSpaceGenerator: adding " + nsAbbrev + " into triple store.");

		ns.save();

		if (source != null && !source.isEmpty()) {
			ns.deleteTriples();
			String url = ns.getSource();
			if (!url.isEmpty()) {
				ns.loadTriples(url, true);
			}
			ns.setNumberOfLoadedTriples();
		}

		ns.save();

		return ns;
    }

    @Override
    public HADatAcThing createObject(Record rec, int rowNumber, String selector) throws Exception {
        createNameSpace(rec);
		return null;
    }
	
	@Override
	public String getErrorMsg(Exception e) {
		return "Error in NameSpaceGenerator: " + e.getMessage();
	}
 	 
}