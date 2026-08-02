package org.hascoapi.ingestion;

import java.lang.String;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.NameSpaces;
import org.hascoapi.utils.SPARQLUtils;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.VSTOI;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ⚠️  DEPRECATED - Use StudyObjectGenerator + AnnotateDASOC instead
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * @deprecated Use DSG + DA-SOC workflow instead (StudyObjectGenerator + AnnotateDASOC)
 */
@Deprecated
public class INSGenerator extends BaseGenerator {
    
	protected String instrumentUri = "";

	protected String hasStatus = "";

	private final Map<String, String> anatomyRawByInstrument = new LinkedHashMap<>();
	private final Map<String, List<String>> anatomyParsedByInstrument = new LinkedHashMap<>();
	private boolean loggedInstrumentHeaders = false;

	public String getInstrumentUri() {
		return this.instrumentUri;
	}
    
	public void setInstrumentUri(String instrumentUri) {
		this.instrumentUri = instrumentUri;
	}

	public String getHasStatus() {
		return this.hasStatus;
	}
    
	public void setHasStatus(String hasStatus) {
		this.hasStatus = hasStatus;
	}

	public INSGenerator(String elementType, DataFile dataFile, String hasStatus) {
		super(dataFile);
		this.setElementType(elementType);
		this.setHasStatus(hasStatus);
	}

	@Override
	public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
		Map<String, Object> row = new HashMap<String, Object>();
		
		for (String header : file.getHeaders()) {
		    if (!header.trim().isEmpty()) {
		        String value = rec.getValueByColumnName(header);
		        if (value != null && !value.isEmpty()) {
					String normalizedHeader = normalizeInstrumentHeader(header);
		            row.put(normalizedHeader, value);
		        }
		    }
		}
		if (this.getElementType().equals("instrument")) {
			if (!loggedInstrumentHeaders) {
				logInstrumentHeaders();
				logMissingInstrumentHeaders();
				loggedInstrumentHeaders = true;
			}

			String subjectUri = row.get("hasURI") == null ? "" : String.valueOf(row.get("hasURI")).trim();
			String rawAnatomy = firstNonEmptyColumn(rec,
				"vstoi:hasAnatomy",
				"http://hadatac.org/ont/vstoi#hasAnatomy",
				"hasAnatomy");
			if (!rawAnatomy.isEmpty() && !subjectUri.isEmpty()) {
				anatomyRawByInstrument.put(subjectUri, rawAnatomy);
				logDebug("[DEBUG-INS-ANATOMY-RAW] row=" + rowNumber + " uri=" + subjectUri + " raw=" + rawAnatomy);
			}

			ensureInstrumentProperty(row, rec, "vstoi:hasAnatomy",
				"vstoi:hasAnatomy",
				"http://hadatac.org/ont/vstoi#hasAnatomy",
				"hasAnatomy");
			ensureInstrumentProperty(row, rec, "vstoi:hasFidelity",
				"vstoi:hasFidelity",
				"http://hadatac.org/ont/vstoi#hasFidelity",
				"hasFidelity");

			splitMultiValueProperty(row, "vstoi:hasAnatomy");
			splitMultiValueProperty(row, "http://hadatac.org/ont/vstoi#hasAnatomy");
			splitMultiValueProperty(row, "vstoi:hasFidelity");
			splitMultiValueProperty(row, "http://hadatac.org/ont/vstoi#hasFidelity");

			if (!subjectUri.isEmpty()) {
				Object parsedAnatomyValue = row.get("vstoi:hasAnatomy");
				if (parsedAnatomyValue == null) {
					parsedAnatomyValue = row.get("http://hadatac.org/ont/vstoi#hasAnatomy");
				}
				List<String> parsedUris = toAnatomyList(parsedAnatomyValue);
				if (!parsedUris.isEmpty()) {
					anatomyParsedByInstrument.put(subjectUri, parsedUris);
				}
			}

			//row.put("rdfs:subClassOf", VSTOI.INSTRUMENT);
			row.put("hasco:hascoType", VSTOI.INSTRUMENT);
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("componentstem")) {
			row.put("hasco:hascoType", VSTOI.COMPONENT_STEM);
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("codebook")) {
			//row.put("rdfs:subClassOf", VSTOI.CODEBOOK);
			row.put("hasco:hascoType", VSTOI.CODEBOOK);
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("responseoption")) {
			//row.put("rdfs:subClassOf", VSTOI.RESPONSE_OPTION);
			row.put("hasco:hascoType", VSTOI.RESPONSE_OPTION);
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("annotationstem")) {
			row.put("hasco:hascoType", VSTOI.ANNOTATION_STEM);
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("annotation")) {
			row.put("hasco:hascoType", VSTOI.ANNOTATION);
			if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			    row.put("vstoi:hasStatus", this.getHasStatus());
			}
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		} else if (this.getElementType().equals("slotelement")) {
			row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		}

		if (row.containsKey("hasURI") && !row.get("hasURI").toString().trim().isEmpty()) {
		    return row;
		}
		
		return null;
	}

	@Override
	public boolean commitRowsToTripleStore(List<Map<String, Object>> rows) {
		boolean committed = super.commitRowsToTripleStore(rows);

		if (!committed || !"instrument".equals(this.getElementType())) {
			return committed;
		}

		if (anatomyParsedByInstrument.isEmpty()) {
			logDebug("[DEBUG-INS-ANATOMY-PARSED] No instrument anatomy values were captured during INS parsing.");
			return committed;
		}

		logDebug("[DEBUG-INS-ANATOMY-PARSED] Parsed vstoi:hasAnatomy values after full INS instrument commit:");
		for (Map.Entry<String, List<String>> entry : anatomyParsedByInstrument.entrySet()) {
			String uri = entry.getKey();
			String raw = anatomyRawByInstrument.getOrDefault(uri, "");
			logDebug("[DEBUG-INS-ANATOMY-PARSED] uri=" + uri + " raw=" + raw + " parsedUris=" + entry.getValue());
		}

		verifyParsedAnatomyAgainstKG();
		return committed;
	}

	@Override
	public String getTableName() {
		return "INS";
	}

	@Override
	public String getErrorMsg(Exception e) {
		return "Error in INSGenerator: " + e.getMessage();
	}

	private String normalizeInstrumentHeader(String header) {
		String normalized = header == null ? "" : header.trim();
		if (normalized.startsWith("vsoit:")) {
			String warning = "[INSGenerator] WARNING: Predicate prefix typo detected and corrected: "
				+ normalized + " -> vstoi:" + normalized.substring("vsoit:".length());
			System.out.println(warning);
			if (this.dataFile != null && this.dataFile.getLogger() != null) {
				this.dataFile.getLogger().printWarning(warning);
			}
			return "vstoi:" + normalized.substring("vsoit:".length());
		}
		return normalized;
	}

	private void splitMultiValueProperty(Map<String, Object> row, String propertyKey) {
		Object value = row.get(propertyKey);
		if (value == null) {
			return;
		}

		String valueStr = value.toString().trim();
		if (valueStr.isEmpty()) {
			return;
		}

		if (valueStr.contains(";") || valueStr.contains("|")) {
			List<String> values = new ArrayList<>();
			String[] parts = valueStr.contains(";")
				? valueStr.split("\\s*;\\s*")
				: valueStr.split("\\s*\\|\\s*");

			for (String part : parts) {
				String cleanValue = part.trim();
				if (!cleanValue.isEmpty()) {
					values.add(cleanValue);
				}
			}

			if (!values.isEmpty()) {
				row.put(propertyKey, values);
			}
		}
	}

	private void ensureInstrumentProperty(Map<String, Object> row, Record rec, String canonicalKey, String... headerVariants) {
		if (row.containsKey(canonicalKey)) {
			return;
		}

		String resolved = firstNonEmptyColumn(rec, headerVariants);
		if (!resolved.isEmpty()) {
			row.put(canonicalKey, resolved);
		}
	}

	private String firstNonEmptyColumn(Record rec, String... headerVariants) {
		for (String header : headerVariants) {
			if (header == null || header.trim().isEmpty()) {
				continue;
			}
			String value = resolveColumnValue(rec, header);
			if (value != null && !value.trim().isEmpty()) {
				return value.trim();
			}
		}
		return "";
	}

	private String resolveColumnValue(Record rec, String headerName) {
		if (headerName == null || headerName.trim().isEmpty()) {
			return "";
		}

		String direct = rec.getValueByColumnName(headerName);
		if (direct != null && !direct.trim().isEmpty()) {
			return direct.trim();
		}

		int idx = findHeaderIndex(headerName);
		if (idx >= 0) {
			String byIndex = rec.getValueByColumnIndex(idx);
			if (byIndex != null && !byIndex.trim().isEmpty()) {
				return byIndex.trim();
			}
		}

		return "";
	}

	private int findHeaderIndex(String headerName) {
		if (file == null || file.getHeaders() == null) {
			return -1;
		}

		String target = normalizeHeaderToken(headerName);
		for (int i = 0; i < file.getHeaders().size(); i++) {
			String current = file.getHeaders().get(i);
			if (target.equals(normalizeHeaderToken(current))) {
				return i;
			}
		}

		return -1;
	}

	private String normalizeHeaderToken(String token) {
		if (token == null) {
			return "";
		}

		String normalized = token
			.replace("\u00A0", " ")
			.replace("\u2007", " ")
			.replace("\u202F", " ")
			.replace("\u200B", "")
			.replace("\uFEFF", "")
			.trim()
			.toLowerCase();

		return normalized;
	}

	private void logInstrumentHeaders() {
		if (file == null || file.getHeaders() == null || file.getHeaders().isEmpty()) {
			logDebug("[DEBUG-INS-ANATOMY-HEADERS] No headers available from spreadsheet parser.");
			return;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("[DEBUG-INS-ANATOMY-HEADERS] Parsed headers with indexes: ");
		for (int i = 0; i < file.getHeaders().size(); i++) {
			if (i > 0) {
				sb.append(" | ");
			}
			sb.append(i).append("='").append(file.getHeaders().get(i)).append("'");
		}
		logDebug(sb.toString());
	}

	private void logMissingInstrumentHeaders() {
		boolean hasAnatomy = findHeaderIndex("vstoi:hasAnatomy") >= 0;
		boolean hasFidelity = findHeaderIndex("vstoi:hasFidelity") >= 0;
		if (hasAnatomy && hasFidelity) {
			return;
		}

		logDebug("[DEBUG-INS-ANATOMY-HEADERS] Missing expected instrument header(s): "
			+ (hasAnatomy ? "" : "vstoi:hasAnatomy ")
			+ (hasFidelity ? "" : "vstoi:hasFidelity ")
			+ "| parsedHeadersCount=" + (file != null && file.getHeaders() != null ? file.getHeaders().size() : 0));
	}

	private List<String> toAnatomyList(Object value) {
		List<String> result = new ArrayList<>();
		if (value == null) {
			return result;
		}

		if (value instanceof List<?>) {
			for (Object item : (List<?>) value) {
				if (item == null) {
					continue;
				}
				String clean = item.toString().trim();
				if (!clean.isEmpty()) {
					result.add(clean);
				}
			}
			return result;
		}

		String single = value.toString().trim();
		if (single.isEmpty()) {
			return result;
		}

		if (single.contains(";") || single.contains("|")) {
			String[] parts = single.contains(";")
				? single.split("\\s*;\\s*")
				: single.split("\\s*\\|\\s*");
			for (String part : parts) {
				String clean = part.trim();
				if (!clean.isEmpty()) {
					result.add(clean);
				}
			}
		} else {
			result.add(single);
		}

		return result;
	}

	private void verifyParsedAnatomyAgainstKG() {
		String sparqlService = CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY);
		String prefixes = NameSpaces.getInstance().printSparqlNameSpaceList();

		for (Map.Entry<String, List<String>> entry : anatomyParsedByInstrument.entrySet()) {
			String sourceUri = entry.getKey();
			String instrumentUri = URIUtils.replacePrefixEx(sourceUri);
			List<String> expectedRaw = entry.getValue();

			Set<String> expectedExpanded = new LinkedHashSet<>();
			for (String raw : expectedRaw) {
				String expanded = URIUtils.replacePrefixEx(raw);
				if (expanded != null && !expanded.trim().isEmpty()) {
					expectedExpanded.add(expanded.trim());
				}
			}

			Set<String> foundInKG = new LinkedHashSet<>();
			String query = prefixes
				+ "SELECT DISTINCT ?anatomy WHERE { <" + instrumentUri + "> vstoi:hasAnatomy ?anatomy . }";

			try {
				ResultSetRewindable rs = SPARQLUtils.select(sparqlService, query);
				while (rs != null && rs.hasNext()) {
					QuerySolution sol = rs.nextSolution();
					if (sol.contains("anatomy") && sol.get("anatomy") != null) {
						foundInKG.add(sol.get("anatomy").toString().trim());
					}
				}
			} catch (Exception e) {
				logDebug("[DEBUG-INS-ANATOMY-KG] uri=" + sourceUri + " verification query failed: " + e.getMessage());
				continue;
			}

			Set<String> missing = new LinkedHashSet<>(expectedExpanded);
			missing.removeAll(foundInKG);

			logDebug("[DEBUG-INS-ANATOMY-KG] uri=" + sourceUri
				+ " expectedCount=" + expectedExpanded.size()
				+ " foundCount=" + foundInKG.size()
				+ " missing=" + missing
				+ " found=" + foundInKG);
		}
	}

	private void logDebug(String message) {
		if (this.dataFile != null && this.dataFile.getLogger() != null) {
			this.dataFile.getLogger().println(message);
			return;
		}
		System.out.println(message);
	}

    @Override
    public void preprocessuris(Map<String,String> uris) throws Exception {
		if (this.getElementType().equals("instrument")) {
			this.setInstrumentUri(uris.get("instrumentUri"));
		}
	}
	
}
