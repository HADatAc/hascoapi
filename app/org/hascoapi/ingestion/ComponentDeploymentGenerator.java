package org.hascoapi.ingestion;

import java.util.HashMap;
import java.util.Map;

import org.hascoapi.entity.pojo.DataFile;

/**
 * Ingests DP2 ComponentDeployments rows and materializes two derived links:
 * 1) deployment -> hasComponentInstance -> componentInstance
 * 2) slot -> hasComponentInstance -> componentInstance
 */
public class ComponentDeploymentGenerator extends BaseGenerator {

    private static final String HASCO_COMPONENT_DEPLOYMENT_TYPE = "http://hadatac.org/ont/hasco/ComponentDeployment";

    public ComponentDeploymentGenerator(DataFile dataFile) {
        super(dataFile);
        this.setElementType("componentdeployment");
    }

    @Override
    public void createRows() throws Exception {
        if (records == null) {
            System.out.println("[WARNING] ComponentDeploymentGenerator.createRows(): records is NULL");
            return;
        }

        int rowNumber = 0;
        int valid = 0;
        int skipped = 0;

        for (Record rec : records) {
            rowNumber++;

            String componentDeploymentUri = pick(rec,
                "hasURI", "uri", "ComponentDeployment URI", "componentDeploymentUri", "componentDeploymentURI");
            String componentDeploymentType = pick(rec,
                "rdf:type", "a");

            String deploymentUri = pick(rec,
                    "deployment URI", "Deployment URI", "deploymentUri", "DeploymentUri",
                "hasco:hascoDeployment", "hasco:hasDeployment", "hasDeployment");
            String slotUri = pick(rec,
                    "Instrument slot URI", "Instrument Slot URI", "instrumentSlotUri", "InstrumentSlotUri",
                "hasco:hasInstrumentSlot", "vstoi:hasContainerSlot", "hasContainerSlot", "Slot URI", "slot URI");
            String componentInstanceUri = pick(rec,
                    "Component instance URI", "Component Instance URI", "componentInstanceUri", "ComponentInstanceUri",
                "hasco:hasComponentInstance", "vstoi:hasComponentInstance", "hasComponentInstance");

            if (isBlank(componentDeploymentUri)) {
            skipped++;
            System.out.println("[ComponentDeploymentGenerator] WARNING: Row #" + rowNumber + " missing hasURI for ComponentDeployment, skipping");
            continue;
            }

            if (isBlank(deploymentUri) || isBlank(slotUri) || isBlank(componentInstanceUri)) {
                skipped++;
                System.out.println("[ComponentDeploymentGenerator] WARNING: Row #" + rowNumber + " missing required values (deployment/slot/component), skipping");
                continue;
            }

            // Persist ComponentDeployment as first-class entity.
            Map<String, Object> componentDeploymentRow = new HashMap<String, Object>();
            componentDeploymentRow.put("hasURI", componentDeploymentUri.trim());
            componentDeploymentRow.put("a", isBlank(componentDeploymentType) ? "vstoi:ComponentDeployment" : componentDeploymentType.trim());
            componentDeploymentRow.put("hasco:hascoType", HASCO_COMPONENT_DEPLOYMENT_TYPE);
            componentDeploymentRow.put("hasco:hascoDeployment", deploymentUri.trim());
            componentDeploymentRow.put("hasco:hasInstrumentSlot", slotUri.trim());
            componentDeploymentRow.put("hasco:hasComponentInstance", componentInstanceUri.trim());
            componentDeploymentRow.put("hasco:hasDataFile", this.dataFile.getUri());
            componentDeploymentRow.put("vstoi:hasSIRManagerEmail", this.dataFile.getHasSIRManagerEmail());
            rows.add(componentDeploymentRow);

            // Derived row 1: deployment -> hasComponentInstance -> componentInstance
            Map<String, Object> depRow = new HashMap<String, Object>();
            depRow.put("hasURI", deploymentUri.trim());
            depRow.put("vstoi:hasComponentInstance", componentInstanceUri.trim());
            depRow.put("hasco:hasDataFile", this.dataFile.getUri());
            rows.add(depRow);

            // Derived row 2: slot -> hasComponentInstance -> componentInstance
            Map<String, Object> slotRow = new HashMap<String, Object>();
            slotRow.put("hasURI", slotUri.trim());
            slotRow.put("vstoi:hasComponentInstance", componentInstanceUri.trim());
            slotRow.put("hasco:hasDataFile", this.dataFile.getUri());
            rows.add(slotRow);

            valid++;
        }

        System.out.println("[ComponentDeploymentGenerator] Completed: " + valid + " valid rows, " + skipped + " skipped rows");
    }

    private String pick(Record rec, String... keys) {
        for (String key : keys) {
            String val = rec.getValueByColumnName(key);
            if (val != null && !val.trim().isEmpty()) {
                return val;
            }
        }
        return null;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Override
    public String getTableName() {
        return "DP2-ComponentDeployments";
    }

    @Override
    public String getErrorMsg(Exception e) {
        return "Error in ComponentDeploymentGenerator: " + e.getMessage();
    }
}
