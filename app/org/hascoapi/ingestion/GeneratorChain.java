package org.hascoapi.ingestion;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;
//import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeneratorChain {

    private static final Logger log = LoggerFactory.getLogger(GeneratorChain.class);

    private List<BaseGenerator> chain = new ArrayList<BaseGenerator>();
    private Map<String,String> uris = new HashMap<String,String>();
    private DataFile dataFile = null;
    private DataFile codebookFile = null;
    private boolean bValid = true;
    private boolean pv = false;
    private String sddName = "";
    private String studyUri = "";
    private String namedGraphUri = "";

    public String getStudyUri() {
        return studyUri;
    }

    public void setStudyUri(String studyUri) {
        this.studyUri = studyUri;
    }

    public String getNamedGraphUri() {
        return namedGraphUri;
    }

    public void setNamedGraphUri(String namedGraphUri) {
        this.namedGraphUri = namedGraphUri;
    }

    public DataFile getDataFile() {
        return dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }

    public DataFile getCodebookFile() {
        return codebookFile;
    }

    public void setCodebookFile(DataFile codebookFile) {
        this.codebookFile = codebookFile;
    }

    public boolean isValid() {
        return bValid;
    }

    public void setInvalid() {
        bValid = false;
    }

    public boolean getPV() {
        return pv;
    }

    public void setPV(boolean pv) {
        this.pv = pv;
    }

    public String getSddName() {
        return sddName;
    }

    public void setSddName(String sddName) {
        this.sddName = sddName;
    }

    public void addGenerator(BaseGenerator generator) {
        chain.add(generator);
    }

    public void setInfoSheetMetadata(String hasStudyKG, String hasVariableDesign, String hasVersion) {
        System.out.println("GeneratorChain.setInfoSheetMetadata() called with:");
        System.out.println("  hasStudyKG: " + hasStudyKG);
        System.out.println("  hasVariableDesign: " + hasVariableDesign);
        System.out.println("  hasVersion: " + hasVersion);

        if (hasStudyKG != null && !hasStudyKG.isEmpty()) {
            uris.put("hasStudyKG", hasStudyKG);
            System.out.println("  ✅ Added hasStudyKG to uris map: " + hasStudyKG);
        } else {
            System.out.println("  ❌ hasStudyKG is null or empty, NOT adding to uris map");
        }
        if (hasVariableDesign != null && !hasVariableDesign.isEmpty()) {
            uris.put("hasVariableDesign", hasVariableDesign);
            System.out.println("  ✅ Added hasVariableDesign to uris map: " + hasVariableDesign);
        } else {
            System.out.println("  ❌ hasVariableDesign is null or empty, NOT adding to uris map");
        }
        if (hasVersion != null && !hasVersion.isEmpty()) {
            uris.put("hasVersion", hasVersion);
            System.out.println("  ✅ Added hasVersion to uris map: " + hasVersion);
        } else {
            System.out.println("  ❌ hasVersion is null or empty, NOT adding to uris map");
        }
    }

    public boolean generate() {
        return generate(true);
    }

    public boolean generate(boolean bCommit) {
        System.out.println("GeneratorChain.generate() called, checking validity...");
        System.out.println("Chain isValid(): " + isValid());
        System.out.println("Chain has " + chain.size() + " generators");
        
        if (!isValid()) {
            System.out.println("[ERROR] GeneratorChain is not valid, aborting generation");
            return false;
        }
        System.out.println("✅ Chain is VALID - proceeding with generation");
        System.out.println("\n========================================");
        System.out.println("GeneratorChain: Executing [NORMAL] generator chain");
        System.out.println("Number of generators: " + chain.size());
        System.out.println("Named Graph URI: " + getNamedGraphUri());
        System.out.println("Commit mode: " + (bCommit ? "YES" : "NO"));
        System.out.println("========================================\n");

        //int i = 0;
        //for (BaseGenerator generator : chain) {
        //    log.info("GeneratorChain: Position " + i++ + " has generator of type [" + generator.getClass(). getSimpleName() + "]");
        //}

        int generatorIndex = 0;
        for (BaseGenerator generator : chain) {
            generatorIndex++;
            String elementType = generator.getElementType();
            if (elementType == null || elementType.isEmpty()) {
                elementType = "NONE";
            }
            System.out.println("\n╔══════════════════════════════════════════════════════════════");
            System.out.println("║ GENERATOR [" + generatorIndex + "/" + chain.size() + "]: " + generator.getClass().getSimpleName());
            System.out.println("║ Element Type: " + elementType);
            System.out.println("║ Named Graph: " + generator.getNamedGraphUri());
            System.out.println("╚══════════════════════════════════════════════════════════════");

            try {
                System.out.println("  → Step 1/5: PreProcess");
                generator.preprocess();
                generator.preprocessuris(uris);

                System.out.println("  → Step 2/5: CreateRows");
                generator.createRows();
                System.out.println("  ✓ Created " + generator.getRows().size() + " rows");

                System.out.println("  → Step 3/5: CreateObjects");
                generator.createObjects();
                System.out.println("  ✓ Created " + generator.getObjects().size() + " objects");

                System.out.println("  → Step 4/5: PostProcess");
                generator.postprocess();
                // Get new URIs from generator and merge with existing map (preserving InfoSheet metadata)
                Map<String,String> newUris = generator.postprocessuris();
                System.out.println("GeneratorChain: Generator " + generator.getClass().getSimpleName() + " returned " + newUris.size() + " URIs from postprocessuris()");
                System.out.println("GeneratorChain: Current uris map size BEFORE merge: " + uris.size());
                // Merge new URIs into existing map instead of replacing (preserves hasStudyKG, hasVariableDesign, hasVersion from setInfoSheetMetadata)
                uris.putAll(newUris);
                System.out.println("GeneratorChain: After merge, uris map now has " + uris.size() + " entries");
                System.out.println("  ✓ Generator completed successfully");
            } catch (Exception e) {
                System.out.println("  ✗ GENERATOR FAILED with exception:");
                System.out.println("     " + e.getClass().getSimpleName() + ": " + e.getMessage());
                // Use the generator's logger instead of chain dataFile (which may be null).
                if (generator.getLogger() != null) {
                    generator.getLogger().printExceptionByIdWithArgs("GBL_00044", generator.getErrorMsg(e));
                    generator.getLogger().printException(generator.getErrorMsg(e));
                } else if (getDataFile() != null && getDataFile().getLogger() != null) {
                    getDataFile().getLogger().printExceptionByIdWithArgs("GBL_00044", generator.getErrorMsg(e));
                }
                e.printStackTrace();
                return false;
            }
            System.out.println("╚══════════════════════════════════════════════════════════════\n");
        }

        if (!bCommit) {
            System.out.println("\n[INFO] Commit phase SKIPPED (bCommit=false)\n");
            return true;
        }

        System.out.println("\n========================================");
        System.out.println("GeneratorChain: Starting COMMIT PHASE");
        System.out.println("========================================\n");

        // Commit if no errors occurred
        generatorIndex = 0;
        for (BaseGenerator generator : chain) {
            generatorIndex++;
            String elementType = generator.getElementType();
            if (elementType == null || elementType.isEmpty()) {
                elementType = "NONE";
            }
            System.out.println("\n╔══════════════════════════════════════════════════════════════");
            System.out.println("║ COMMIT [" + generatorIndex + "/" + chain.size() + "]: " + generator.getClass().getSimpleName());
            System.out.println("║ Element Type: " + elementType);
            System.out.println("╚══════════════════════════════════════════════════════════════");
            System.out.println("  Rows to commit: " + generator.getRows().size());
            System.out.println("  Objects to commit: " + generator.getObjects().size());
            System.out.println("  Rows to commit: " + generator.getRows().size());
            System.out.println("  Objects to commit: " + generator.getObjects().size());

            if (!getNamedGraphUri().isEmpty()) {
                generator.setNamedGraphUri(getNamedGraphUri());
                System.out.println("  Set named graph from chain: " + getNamedGraphUri());
            }
            if (!generator.getStudyUri().isEmpty()) {
                setStudyUri(generator.getStudyUri());
            }

            if (generator.getStudyUri().isEmpty() && !getStudyUri().isEmpty()) {
                generator.setStudyUri(getStudyUri());
            }

            try {
                if (generator.getRows().size() > 0){
                    System.out.println("  → Committing " + generator.getRows().size() + " rows to triple store...");
                    generator.commitRowsToTripleStore(generator.getRows());
                    System.out.println("  ✓ Rows committed successfully");
                } else {
                    System.out.println("  ℹ No rows to commit");
                }
                if (generator.getObjects().size() > 0){
                    System.out.println("  → Committing " + generator.getObjects().size() + " objects to triple store...");
                    generator.commitObjectsToTripleStore(generator.getObjects());
                    System.out.println("  ✓ Objects committed successfully");
                } else {
                    System.out.println("  ℹ No objects to commit");
                }
            } catch (Exception e) {
                System.out.println("  ✗ COMMIT FAILED with exception:");
                System.out.println("     " + e.getClass().getSimpleName() + ": " + e.getMessage());
                System.out.println(generator.getErrorMsg(e));
                e.printStackTrace();

                generator.getLogger().printException(generator.getErrorMsg(e));
                return false;
            }
            System.out.println("╚══════════════════════════════════════════════════════════════\n");
            System.out.println("╚══════════════════════════════════════════════════════════════\n");
        }

        for (BaseGenerator generator : chain) {
            if (!generator.getStudyUri().equals("")) {
                setStudyUri(generator.getStudyUri());
            }
        }
        postprocess();

        System.out.println("\n========================================");
        System.out.println("GeneratorChain: COMPLETED SUCCESSFULLY");
        System.out.println("Final Study URI: " + getStudyUri());
        System.out.println("========================================\n");

        return true;
    }

    public boolean generateImmediateCommit() {
        if (!isValid()) {
            return false;
        }
        System.out.println("GeneratorChain: Executing [IMMEDIATE COMMIT] generator chain.");

        for (BaseGenerator generator : chain) {
            System.out.println("GeneratorChain: Executing generator of type [" + generator.getClass().getSimpleName() + "]");
            if (!getNamedGraphUri().isEmpty()) {
                generator.setNamedGraphUri(getNamedGraphUri());
            }
            try {
                //System.out.println("  - GenerationChain: PreProcess");
                generator.preprocess();
                generator.preprocessuris(uris);
                //System.out.println("  - GenerationChain: CreateRows");
                generator.createRows();
                //System.out.println("  - GenerationChain: CreateObjects");
                generator.createObjects();
                //System.out.println("  - GenerationChain:PostProcess");
                generator.postprocess();
                uris = generator.postprocessuris();
                if (generator.getRows().size() > 0){
                    generator.commitRowsToTripleStore(generator.getRows());
                }
                if (generator.getObjects().size() > 0){
                    generator.commitObjectsToTripleStore(generator.getObjects());
                }
            } catch (Exception e) {
                getDataFile().getLogger().printExceptionByIdWithArgs("GBL_00044", generator.getErrorMsg(e));
                //System.out.println("[ERROR] GenerationChain: " + generator.getErrorMsg(e));
                e.printStackTrace();
                generator.getLogger().printException(generator.getErrorMsg(e));
                return false;
            }
        }
       postprocess();
        return true;
    }

    public void disposeChain() {
        for (BaseGenerator generator : chain) {
            if (generator != null) {
                try {
                    generator.dispose(); // Let each generator clean itself
                } catch (Exception e) {
                    System.out.println("Error disposing generator: " + generator.getClass().getSimpleName());
                }
            }
        }
        chain.clear(); // Release references to the generators
    }


    public void delete() {
        for (BaseGenerator generator : chain) {

            if (!getNamedGraphUri().isEmpty()) {
                generator.setNamedGraphUri(getNamedGraphUri());
                log.info("deleting ... and setting the graph names...");
            } else if (!generator.getStudyUri().isEmpty()) {
                generator.setNamedGraphUri(generator.getStudyUri());
                log.info("deleting ... and setting the graph names...");
            }

            try {

                generator.preprocess();
                generator.createRows();
                generator.createObjects();
                generator.postprocess();

                generator.deleteRowsFromTripleStore(generator.getRows());
                generator.deleteObjectsFromTripleStore(generator.getObjects());

            } catch (Exception e) {
                System.out.println(generator.getErrorMsg(e));
                e.printStackTrace();

                generator.getLogger().printException(e);
            }
        }
    }

    public void postprocess() {}
}
