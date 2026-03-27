package org.hascoapi.ingestion;

import java.util.Map;
import org.hascoapi.Constants;
import org.hascoapi.entity.pojo.DataFile;

/**
 * ═══════════════════════════════════════════════════════════════════════════
 * ⚠️  DEPRECATED - INS FORMAT IS NO LONGER RECOMMENDED
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * This class processes INS (Instrument Namespace Specification) files,
 * which are DEPRECATED and will be removed in a future release.
 * 
 * RECOMMENDED ALTERNATIVE: Use DSG + DA-SOC workflow instead
 * 
 * WHY DEPRECATED:
 *   • INS creates parallel metadata management (separate from study framework)
 *   • DSG provides unified metadata across all research elements
 *   • DA-SOC enables flexible property extension
 *   • DSG supports better version control and collaboration
 * 
 * MIGRATION PATH:
 *   1. Create DSG file with VSTOI-typed SOCs:
 *      - SOC-INSTRUMENT-<name>
 *      - SOC-COMPONENT-<name>
 *      - SOC-COMPONENT-STEM-<name>
 *      - SOC-SLOT-ELEMENT-<name>
 *      - SOC-CODEBOOK-<name>
 *      - SOC-RESPONSE-OPTION-<name>
 * 
 *   2. Create DA-SOC files for extended properties:
 *      - DA-SOC-INSTRUMENT-<name>.csv (vstoi:hasFirst, vstoi:hasShortName, ...)
 *      - DA-SOC-COMPONENT-<name>.csv (vstoi:hasComponentStem, vstoi:hasCodebook, ...)
 *      - DA-SOC-SLOT-ELEMENT-<name>.csv (vstoi:belongsTo, vstoi:hasNext, ...)
 * 
 * DOCUMENTATION:
 *   See docs/INS-TO-DSG-TRANSFORMATION-PLAN.md for detailed migration guide
 * 
 * REMOVAL TIMELINE:
 *   - Current: Generates deprecation warnings
 *   - Future: Will be removed entirely
 * 
 * ═══════════════════════════════════════════════════════════════════════════
 * 
 * @deprecated Use DSG + DA-SOC workflow instead
 */
@Deprecated
public class AnnotateINS extends BaseAnnotator {

    public static GeneratorChain exec(DataFile dataFile, String templateFile, String status) {
        dataFile.getLogger().println("Processing INS meta-template ...");

        // Load catalog with sheet validation
        Map<String, String> mapCatalog = loadCatalog(dataFile, Constants.MT_INS);
        if (mapCatalog == null) {
           // dataFile.getLogger().printExceptionById("INS_00001"); // "INS InfoSheet validation failed"
            return null;
        }

        // Namespace and annotation generation
        IngestionWorker.nameSpaceGen(dataFile, mapCatalog, templateFile);
        IngestionWorker.annotationGen(dataFile, mapCatalog, templateFile, status);

        // Build generator chain
        GeneratorChain chain = new GeneratorChain();

        // Set the named graph URI so data is stored in the DataFile's graph
        chain.setNamedGraphUri(dataFile.getUri());

        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "ResponseOptions", status, chain,
                (df, st) -> new INSGenerator("responseoption", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBooks", status, chain,
                (df, st) -> new INSGenerator("codebook", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "CodeBookSlots", status, chain,
                new CodeBookSlotGeneratorFactory());
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "ComponentStems", status, chain,
                (df, st) -> new INSGenerator("componentstem", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Components", status, chain,
                new ComponentGeneratorFactory());
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "SlotElements", status, chain,
                (df, st) -> new INSGenerator("slotelement", df, st));
        addCustomGeneratorIfSheetExists(dataFile, mapCatalog, "Instruments", status, chain,
                (df, st) -> new INSGenerator("instrument", df, st));

        return chain;
    }

    static class CodeBookSlotGeneratorFactory implements GeneratorFactory {
        public BaseGenerator create(DataFile dataFile, String status) {
            return new CodeBookSlotGenerator(dataFile);
        }
    }

    static class ComponentGeneratorFactory implements GeneratorFactory {
        public BaseGenerator create(DataFile dataFile, String status) {
            return new ComponentGenerator(dataFile, status);
        }
    }
}
