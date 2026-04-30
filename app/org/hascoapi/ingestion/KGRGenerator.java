package org.hascoapi.ingestion;

import java.io.IOException;
import java.lang.String;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.Organization;
import org.hascoapi.entity.pojo.Place;
import org.hascoapi.entity.pojo.PostalAddress;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.URIUtils;
import org.hascoapi.vocabularies.SCHEMA;


public class KGRGenerator extends BaseGenerator {

    private static final Map<String, String> LEGACY_MEDIA_ALIASES = createLegacyMediaAliases();

    protected String hasStatus = "";

    protected String hasMediaFolder = "";

    protected boolean verifyUri = false;

    private boolean mediaFolderMissingLogged = false;

    private long timestamp;

    final String kbPrefix = ConfigProp.getKbPrefix();

    private static Map<String, String> createLegacyMediaAliases() {
        Map<String, String> aliases = new HashMap<String, String>();
        aliases.put("gz.png", "ps.png");
        aliases.put("an.png", "aw.png");
        // Workbook typo tolerance (seen in KGR-INSTITUTOS-URI.xlsx)
        aliases.put("ipbejapng", "IPBEJA.png");
        return aliases;
    }

    public String getHasStatus() {
        return this.hasStatus;
    }

    public void setHasStatus(String hasStatus) {
        this.hasStatus = hasStatus;
    }

    public String getHasMediaFolder() {
        return this.hasMediaFolder;
    }

    public void setHasMediaFolder(String hasMediaFolder) {
        this.hasMediaFolder = hasMediaFolder;
    }

    public boolean getVerifyUri() {
        return this.verifyUri;
    }

    public void setVerifyUri(boolean verifyUri) {
        this.verifyUri = verifyUri;
    }

    public KGRGenerator(String elementType, String hasStatus, DataFile dataFile, String hasMediaFolder, boolean verifyUri) {
        super(dataFile);
        this.setElementType(elementType);
        this.setHasStatus(hasStatus);
        this.setHasMediaFolder(hasMediaFolder);
        this.setVerifyUri(verifyUri);
        //System.out.println("KGRGenerator: mediaFolder [" + hasMediaFolder + "] for element type [" + elementType + "]");
        this.timestamp = System.currentTimeMillis();

    }

    @Override
    public Map<String, Object> createRow(Record rec, int rowNumber) throws Exception {
        Map<String, Object> row = new HashMap<String, Object>();

        // From Metadata Template
        for (String header : file.getHeaders()) {
            if (!header.trim().isEmpty()) {
                String value = rec.getValueByColumnName(header);
                if (value != null && !value.isEmpty()) {
                    UriHint uriHint = this.extractUriAndHint(header);
                    String possibleObjectProperty = uriHint.uri;
                    String hint = uriHint.hint;

                    if (this.isHeaderOfUri(possibleObjectProperty)) {

                        if (value != null && URIUtils.isValidURI(value)) {
                            String fullUri = URIUtils.replacePrefixEx(value);
                            if (!this.getVerifyUri() || this.uriExists(possibleObjectProperty,fullUri)) {
                                row.put(possibleObjectProperty, fullUri);
                            }

                        } else {
                            if (hint == null || hint.trim().isEmpty()) {
                                row.put(possibleObjectProperty,value);
                            } else {
                                String uri = this.uriFromLabel(possibleObjectProperty, hint, value);
                                if (uri != null) {
                                    row.put(possibleObjectProperty,uri);
                                }
                            }

                        }

                    } else {
                        row.put(header, value);
                    }
                }
            }
        }

        row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
        if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
            row.put("vstoi:hasStatus", this.getHasStatus());
        }

        if (this.getElementType().equals("fundingscheme")) {
            row.put("hasco:hascoType", SCHEMA.FUNDING_SCHEME);
        } else if (this.getElementType().equals("project") ||
                this.getElementType().equals("projectorganization")) {
            row.put("hasco:hascoType", SCHEMA.PROJECT);
        } else if (this.getElementType().equals("organization")) {
            row.put("hasco:hascoType", SCHEMA.ORGANIZATION);
        } else if (this.getElementType().equals("person")) {
            row.put("hasco:hascoType", SCHEMA.PERSON);
        } else if (this.getElementType().equals("place")) {
            row.put("hasco:hascoType", SCHEMA.PLACE);
        } else if (this.getElementType().equals("postaladdress")) {
            row.put("hasco:hascoType", SCHEMA.POSTAL_ADDRESS);
        }

        String uri = null;

        if (!row.containsKey("hasURI") || row.get("hasURI").toString().trim().isEmpty()) {
            if (!elementType.equals("projectorganization")) {
                uri = URIUtils.replacePrefixEx(this.createUri());
                if (uri == null) {
                    return null;
                }
                row.put("hasURI", uri);
            }
        }

        if (row.containsKey("hasco:hasImage") && !row.get("hasco:hasImage").toString().trim().isEmpty()) {
            uri = row.get("hasURI").toString().trim();
            String image = row.get("hasco:hasImage").toString().trim();
            this.copyMediaToUri(hasMediaFolder, uri, image);
        }

        return row;

    }


    public boolean isHeaderOfUri(String predicate) {
        if (predicate == null || predicate.isEmpty()) {
            return false;
        }
        if (
                predicate.equals("a") ||
                        predicate.equals("vstoi:hasStatus") ||
                        predicate.equals("schema:address") ||
                        predicate.equals("foaf:member") ||
                        predicate.equals("schema:containedInPlace") ||
                        predicate.equals("schema:parentOrganization") ||
                        predicate.equals("schema:addressLocality") ||
                        predicate.equals("schema:addressRegion") ||
                        predicate.equals("schema:addressCountry") ||
                        predicate.equals("schema:contributor")
        ) {
            return true;
        }
        return false;
    }

    public boolean uriExists(String predicate, String uri) {
        if (predicate.equals("schema:containedInPlace")) {
            Place place = Place.find(uri);
            if (place == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting PLACE -> there is no schema:containedInPlace with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00001", uri);
                return false;
            }
            return true;
        }
        if (predicate.equals("schema:parentOrganization")) {
            Organization organization = Organization.find(uri);
            if (organization == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting ORGANIZATION -> there is no schema:parentOrganization with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00002", uri);
                return false;
            }
            return true;
        }
        if (predicate.equals("schema:address")) {
            PostalAddress postalAddress = PostalAddress.find(uri);
            if (postalAddress == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting ORGANIZATION/PERSON -> there is no schema:address with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00003", uri);
                return false;
            }
            return true;
        }
        if (predicate.equals("foaf:member")) {
            Organization organization = Organization.find(uri);
            if (organization == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting PERSON -> there is no foaf:member with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00004", uri);
                return false;
            }
            return true;
        }
        if (predicate.equals("schema:addressLocality")) {
            Place place = Place.find(uri);
            if (place == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting POSTAL_ADDRESS -> there is no schema:addressLocality with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00005", uri);
                return false;
            }
            return true;
        }
        if (predicate.equals("schema:addressRegion")) {
            Place place = Place.find(uri);
            if (place == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting POSTAL_ADDRESS -> there is no schema:addressRegion with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00006", uri);
                return false;
            }
            return true;
        }
        if (predicate.equals("schema:addressCountry")) {
            Place place = Place.find(uri);
            if (place == null) {
                //System.out.println("[WARNING] KGRGenerator: Ingesting POSTAL_ADDRESS -> there is no schema:addressCountry with URI=[" + uri + "]");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00007", uri);
                return false;
            }
            return true;
        }
        return true;

    }

    public String uriFromLabel(String predicate, String hint, String label) {
        if (predicate.equals("schema:containedInPlace")) {
            if (hint.equals("schema:Country")) {
                Place place = Place.findByName(label);
                if (place != null && place.getUri() != null) {
                    return place.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find containedInPlace(Country) " + label + " for place.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00008", label);
                return null;

            } else if (hint.equals("schema:State")) {
                Place place = Place.findByName(label);
                if (place != null && place.getUri() != null) {
                    return place.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find containedInPlace(State) " + label + " for place.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00009", label);
                return null;

            } else if (hint.equals("schema:City")) {
                Place place = Place.findByName(label);
                if (place != null && place.getUri() != null) {
                    return place.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find containedInPlace(City) " + label + " for place.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00010", label);
                return null;
            }
            return null;

        } else if (predicate.equals("schema:parentOrganization")) {
            if (hint.equals("schema:Organization")) {
                Organization parent = Organization.findByName(label);
                if (parent != null && parent.getUri() != null) {
                    return parent.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find parent " + label + " for organization.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00011", label);
                return null;
            }
            return null;

        } else if (predicate.equals("schema:address")) {
            if (hint.equals("schema:PostalAddress")) {

                String[] parts = label.split("\\|", 2);
                PostalAddress postalAddress = PostalAddress.findByAddress(parts[0].trim(), parts[1].trim());
                if (postalAddress != null && postalAddress.getUri() != null) {
                    return postalAddress.getUri();
                } else {
                    postalAddress = PostalAddress.findByPostalCode(parts[1].trim());
                    if (postalAddress != null && postalAddress.getUri() != null) {
                        return postalAddress.getUri();
                    }
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find address " + label + " for organization/person.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00012", label);
                return null;
            }
            return null;

        } else if (predicate.equals("schema:addressLocality")) {
            if (hint.equals("schema:City")) {
                Place place = Place.findByName(label);
                if (place != null && place.getUri() != null) {
                    return place.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find addressLocality " + label + " for postalAddress.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00013", label);
                return null;
            }
            return null;

        } else if (predicate.equals("schema:addressRegion")) {
            if (hint.equals("schema:State")) {
                Place place = Place.findByName(label);
                if (place != null && place.getUri() != null) {
                    return place.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find addressRegion " + label + " for postalAddress.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00014", label);
                return null;
            }
            return null;

        } else if (predicate.equals("schema:addressCountry")) {
            if (hint.equals("schema:Country")) {
                Place place = Place.findByName(label);
                if (place != null && place.getUri() != null) {
                    return place.getUri();
                }
                //System.out.println("[WARNING] KGRGenerator: Could not find addressCountry " + label + " for postalAddress.");
                dataFile.getLogger().printWarningByIdWithArgs("KGR_00015", label);
                return null;
            }
            return null;
        }
        return null;
    }

    public void copyMediaToUri(String foldername, String uri, String filename) {
        // Legacy/optional behavior: if the KGR workbook doesn't configure a media folder,
        // treat media as disabled and skip without noise.
        if (foldername == null || foldername.trim().isEmpty()) {
            if (!mediaFolderMissingLogged) {
                dataFile.getLogger().printExceptionById("KGR_00016");
                mediaFolderMissingLogged = true;
            }
            return;
        }

        if (uri == null || uri.trim().isEmpty()) {
            //System.out.println("[ERROR] No URI value has been provided.");
            dataFile.getLogger().printExceptionById("KGR_00017");
            return;
        }

        if (filename == null || filename.trim().isEmpty()) {
            //System.out.println("[ERROR] No filename value has been provided.");
            dataFile.getLogger().printExceptionById("KGR_00018");
            return;
        }

        String basePath = "/var/hascoapi";
        if (basePath == null || basePath.trim().isEmpty()) {
            //System.out.println("[ERROR] Invalid file storage path.");
            dataFile.getLogger().printExceptionById("KGR_00019");
            return;
        }

        Path sourcePath = Paths.get(basePath, Constants.MEDIA_FOLDER, foldername, filename);
        String aliasFilename = LEGACY_MEDIA_ALIASES.get(filename.trim().toLowerCase());
        if (!Files.exists(sourcePath) && aliasFilename != null) {
            Path aliasSourcePath = Paths.get(basePath, Constants.MEDIA_FOLDER, foldername, aliasFilename);
            if (Files.exists(aliasSourcePath)) {
                sourcePath = aliasSourcePath;
            }
        }

        String uriTerm = URIUtils.uriLastSegment(URIUtils.replacePrefixEx(uri));
        Path destinationDir = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm);
        Path destinationPath = destinationDir.resolve(filename);

        try {
            Files.createDirectories(destinationDir);
        } catch (IOException e) {
            //System.out.println("[ERROR] Failed to create destination directory: " + e.getMessage());
            dataFile.getLogger().printExceptionByIdWithArgs("KGR_00020", e.getMessage());
            return;
        }

        try {
            if (Files.exists(sourcePath)) {
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
            } else {
                //System.out.println("[ERROR] File not found at source path: " + sourcePath.toString());
                dataFile.getLogger().printExceptionByIdWithArgs("KGR_00021", sourcePath.toString());
            }
        } catch (IOException e) {
            //System.out.println("[ERROR] Failed to copy file: " + e.getMessage());
            dataFile.getLogger().printExceptionByIdWithArgs("KGR_00022", e.getMessage());
        }
    }

    public String createUri() throws Exception {
        Random random = new Random();
        int randomNumber = random.nextInt(99999 - 10000 + 1) + 10000;

        String prefix = null;
        if (this.getElementType().equals("fundingscheme")) {
            prefix = Constants.PREFIX_FUNDING_SCHEME;
        } else if (this.getElementType().equals("project")) {
            prefix = Constants.PREFIX_PROJECT;
        } else if (this.getElementType().equals("organization")) {
            prefix = Constants.PREFIX_ORGANIZATION;
        } else if (this.getElementType().equals("person")) {
            prefix = Constants.PREFIX_PERSON;
        } else if (this.getElementType().equals("place")) {
            prefix = Constants.PREFIX_PLACE;
        } else if (this.getElementType().equals("postaladdress")) {
            prefix = Constants.PREFIX_POSTAL_ADDRESS;
        }

        if (prefix == null) {
            //System.out.println("[ERROR] Failed to create URI for element type [" + this.getElementType() + "]");
            dataFile.getLogger().printExceptionByIdWithArgs("KGR_00023", this.getElementType());
            return null;
        }
        String newUri = kbPrefix + prefix + timestamp + randomNumber;
        return newUri;
    }

    protected UriHint extractUriAndHint(String input) {
        String uriPattern = "^[^\\(]+";
        String hintPattern = "\\(([^\\)]+)\\)";
        String uri = "";
        String hint = "";
        Pattern uriRegex = Pattern.compile(uriPattern);
        Matcher uriMatcher = uriRegex.matcher(input);
        if (uriMatcher.find()) {
            uri = uriMatcher.group(0);
        }
        Pattern hintRegex = Pattern.compile(hintPattern);
        Matcher hintMatcher = hintRegex.matcher(input);
        if (hintMatcher.find()) {
            hint = hintMatcher.group(1);
        }
        return new UriHint(uri, hint);
    }

    protected class UriHint {
        String uri;
        String hint;

        UriHint(String uri, String hint) {
            this.uri = uri;
            this.hint = hint;
        }
    }

    @Override
    public String getTableName() {
        return "KGR";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in KGRGenerator: " + e.getMessage();
    }

}
