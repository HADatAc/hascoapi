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
    
	protected String hasStatus = "";

	protected String hasMediaFolder = "";

    private long timestamp;

	final String kbPrefix = ConfigProp.getKbPrefix();

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

	public KGRGenerator(String elementType, String hasStatus, DataFile dataFile, String hasMediaFolder) {
		super(dataFile);
		this.setElementType(elementType);
		this.setHasStatus(hasStatus);
		this.setHasMediaFolder(hasMediaFolder);
		System.out.println("KGRGenerator: mediaFolder [" + hasMediaFolder + "] for element type [" + elementType + "]");
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
					//System.out.println("Header: [" + header + "] Value: [" + value + "]");
					UriHint uriHint = this.extractUriAndHint(header);
					String possibleObjectProperty = uriHint.uri;
					String hint = uriHint.hint;
					//System.out.println("PossibleObjectProperty: [" + possibleObjectProperty + "]   Hint: [" + hint + "]");

					// the property is an URI
					if (this.isHeaderOfUri(possibleObjectProperty)) {

						// the value is URI
						if (value != null && URIUtils.isValidURI(value)) {
							String fullUri = URIUtils.replacePrefixEx(value);
							//System.out.println("Value: [" + value + "]    FullUri: [" + fullUri + "]");
							if (this.uriExists(possibleObjectProperty,fullUri)) {
								row.put(possibleObjectProperty, fullUri);
							}
	
						// the value is not an URI and needs to be retrieved from given value (and provided hint)
						} else {
							//System.out.println("Hint: [" + hint + "]    Value: [" + value + "]");
							String uri = this.uriFromLabel(possibleObjectProperty, hint, value);
							//System.out.println("Found uri: [" + uri + "]");
							if (uri != null) {
								row.put(possibleObjectProperty,uri);
							}

						}

					// the property is not an URI
					} else {
		            	row.put(header, value);
					}
		        }
		    }
		}

		// From Context
		row.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
		if (this.getHasStatus() != null && !this.getHasStatus().equals("_")) {
			row.put("vstoi:hasStatus", this.getHasStatus());
		}

		// From ElementType
		if (this.getElementType().equals("fundingscheme")) {
			row.put("hasco:hascoType", SCHEMA.FUNDING_SCHEME);
		} else if (this.getElementType().equals("project")) {
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

		// Creates an URI if no URI (hasURI) is provided in the KGR MT. 
		if (!row.containsKey("hasURI") || row.get("hasURI").toString().trim().isEmpty()) {
			uri = URIUtils.replacePrefixEx(this.createUri());
			if (uri == null) {
				return null;
			}
			row.put("hasURI", uri);
		}
	
		// If image is provided, it is copied into the resource folder.
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
		if (// GENERIC
			predicate.equals("a") ||
			predicate.equals("vstoi:hasStatus") ||
			predicate.equals("schema:address") ||

			// PERSON	
			predicate.equals("foaf:member") || 

			// PLACE
			predicate.equals("schema:containedInPlace") ||

			// ORGANIZATION
			predicate.equals("schema:parentOrganization") ||

			// POSTAL_ADDRESS
			predicate.equals("schema:addressLocality") ||
			predicate.equals("schema:addressRegion") || 
			predicate.equals("schema:addressCountry")
		) {
			return true;
		}  
		return false;
	}

	public boolean uriExists(String predicate, String uri) {
		if (predicate.equals("schema:containedInPlace")) {
			Place place = Place.find(uri);
			if (place == null) {
				System.out.println("KGRGenerator: Ingesting PLACE -> there is no schema:containedInPlace with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		if (predicate.equals("schema:parentOrganization")) {
			Organization organization = Organization.find(uri);
			if (organization == null) {
				System.out.println("KGRGenerator: Ingesting ORGANIZATION -> there is no schema:parentOrganization with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		if (predicate.equals("schema:address")) {
			PostalAddress postalAddress = PostalAddress.find(uri);
			if (postalAddress == null) {
				System.out.println("KGRGenerator: Ingesting ORGANIZATION/PERSON -> there is no schema:address with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		if (predicate.equals("foaf:member")) {
			Organization organization = Organization.find(uri);
			if (organization == null) {
				System.out.println("KGRGenerator: Ingesting PERSON -> there is no foaf:member with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		if (predicate.equals("schema:addressLocality")) {
			Place place = Place.find(uri);
			if (place == null) {
				System.out.println("KGRGenerator: Ingesting POSTAL_ADDRESS -> there is no schema:addressLocality with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		if (predicate.equals("schema:addressRegion")) {
			Place place = Place.find(uri);
			if (place == null) {
				System.out.println("KGRGenerator: Ingesting POSTAL_ADDRESS -> there is no schema:addressRegion with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		if (predicate.equals("schema:addressCountry")) {
			Place place = Place.find(uri);
			if (place == null) {
				System.out.println("KGRGenerator: Ingesting POSTAL_ADDRESS -> there is no schema:addressCountry with URI=[" + uri + "]");
				return false;
			}
			return true;
		}
		return true;  

	}

	public String uriFromLabel(String predicate, String hint, String label) {
		if (predicate.equals("schema:containedInPlace")) {
			if (hint.equals("schema:Country")) {
				//System.out.println("Place.findByName() with label=[" + label + "]");
				Place place = Place.findByName(label);
				if (place != null && place.getUri() != null) {
					return place.getUri();
				}
				return null;
		
			} else if (hint.equals("schema:State")) {
				Place place = Place.findByName(label);
				if (place != null && place.getUri() != null) {
					return place.getUri();
				}
				return null;
		
			} else if (hint.equals("schema:City")) {
				Place place = Place.findByName(label);
				if (place != null && place.getUri() != null) {
					return place.getUri();
				}
				return null;

			}
			return null; 
		}

		return null;
	}

	/**
     * Moves the file from the given folder to the location where it can be retrieved with its URI.
     * This method is static and can be used without needing to expose it as an API endpoint.
     */
    public void copyMediaToUri(String foldername, String uri, String filename) {
        // Step 1: Validate Inputs
        if (foldername == null || foldername.trim().isEmpty()) {
            System.out.println("[ERROR] No foldername value has been provided.");
            return;
        }

        if (uri == null || uri.trim().isEmpty()) {
            System.out.println("[ERROR] No URI value has been provided.");
            return;
        }

        if (filename == null || filename.trim().isEmpty()) {
            System.out.println("[ERROR] No filename value has been provided.");
            return;
        }

        // Step 2: Construct source and destination paths
        //String basePath = ConfigProp.getPathIngestion();
        String basePath = "/var/hascoapi";
        if (basePath == null || basePath.trim().isEmpty()) {
            System.out.println("[ERROR] Invalid file storage path.");
            return;
        }

        // Source path is where the file was uploaded initially
        Path sourcePath = Paths.get(basePath, Constants.MEDIA_FOLDER, foldername, filename);

        // Destination path is based on the URI, we derive the last segment from the URI
        String uriTerm = URIUtils.uriLastSegment(URIUtils.replacePrefixEx(uri));
        Path destinationDir = Paths.get(basePath, Constants.RESOURCE_FOLDER, uriTerm);
        Path destinationPath = destinationDir.resolve(filename);

        // Ensure the destination directory exists
        try {
            Files.createDirectories(destinationDir);
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to create destination directory: " + e.getMessage());
            return;
        }

        // Step 3: Move the file to the destination
        try {
            if (Files.exists(sourcePath)) {
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
                System.out.println("File successfully copied to: " + destinationPath.toString());
            } else {
                System.out.println("[ERROR] File not found at source path: " + sourcePath.toString());
            }
        } catch (IOException e) {
            System.out.println("[ERROR] Failed to copy file: " + e.getMessage());
        }
    }

	public String createUri() throws Exception {

        // Generate a random integer between 10000 and 99999
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
            System.out.println("[ERROR] Failed to create URI for element type [" + this.getElementType() + "]");
			return null;
		}
		String newUri = kbPrefix + prefix + timestamp + randomNumber;
		//System.out.println("Created new URI [" + newUri + "]");
		return newUri;
	}

	// Method to extract URI and hint from the given string
    protected UriHint extractUriAndHint(String input) {
        // Regular expression for extracting URI and hint
        String uriPattern = "^[^\\(]+";
        String hintPattern = "\\(([^\\)]+)\\)"; // Text inside parentheses

        String uri = "";
        String hint = "";

        // Extract URI
        Pattern uriRegex = Pattern.compile(uriPattern);
        Matcher uriMatcher = uriRegex.matcher(input);
        if (uriMatcher.find()) {
            uri = uriMatcher.group(0);  // Get the URI part before any parentheses
        }

        // Extract hint (if available)
        Pattern hintRegex = Pattern.compile(hintPattern);
        Matcher hintMatcher = hintRegex.matcher(input);
        if (hintMatcher.find()) {
            hint = hintMatcher.group(1);  // Get the hint inside parentheses
        }

        return new UriHint(uri, hint);
    }

    // Helper class to store URI and hint
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
