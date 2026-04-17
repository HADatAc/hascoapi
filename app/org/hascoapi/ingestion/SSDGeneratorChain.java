package org.hascoapi.ingestion;

import java.util.List;
import org.hascoapi.entity.pojo.*;


public class SSDGeneratorChain extends GeneratorChain {

    @Override
    public void postprocess() {
        if (this.getStudyUri() == null) {
            return;
        }
        
        String studyUri = this.getStudyUri();
        
        // FASE 1: Processamento padrão (label computation)
        List<StudyObjectCollection> studySOCs = StudyObjectCollection.findStudyObjectCollectionsByStudy(studyUri);
        
        if (studySOCs == null || studySOCs.isEmpty()) {
            return;
        }
        
        for (StudyObjectCollection soc: studySOCs) {
            String labelResult = StudyObjectCollection.computeRouteLabel(soc, studySOCs);
            if (labelResult == null) {
                getDataFile().getLogger().println("Label for " + soc.getSOCReference() + ": ERROR could not find path to colletion with grounding label");
            } else {
                soc.setNamedGraph(getNamedGraphUri());
                soc.saveRoleLabel(labelResult);
            }
        }
        
        // NOTA: As entidades vstoi (Instruments, Components, etc.) já foram criadas
        // durante a ingestão pelo StudyObjectGenerator, não precisamos fazer nada aqui
        getDataFile().getLogger().println("Label computation completed for " + studySOCs.size() + " SOCs");
    }
}
