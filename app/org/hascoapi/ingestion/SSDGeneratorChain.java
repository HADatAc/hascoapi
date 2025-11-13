package org.hascoapi.ingestion;

import java.util.List;
import org.hascoapi.entity.pojo.StudyObjectCollection;


public class SSDGeneratorChain extends GeneratorChain {

    @Override
    public void postprocess() {
        if (this.getStudyUri() == null) {
            return;
        }
        List<StudyObjectCollection> studySOCs = StudyObjectCollection.findStudyObjectCollectionsByStudy(this.getStudyUri());
        for (StudyObjectCollection soc: studySOCs) {
            //AnnotationLog.println("SOC has URI  " + oc.getUri() + " and label " + oc.getLabel(), file.getFile().getName());
            String labelResult = StudyObjectCollection.computeRouteLabel(soc, studySOCs);
            if (labelResult == null) {
                getDataFile().getLogger().println("Label for " + soc.getSOCReference() + ": ERROR could not find path to colletion with grounding label");
            } else {
                /*
                Bug here that if some character that isn't from UTF-8 is in the label , the program stuck here.
                 */
                getDataFile().getLogger().println("Label for " + soc.getSOCReference() + ": " + labelResult);
                soc.setNamedGraph(getNamedGraphUri());
                soc.saveRoleLabel(labelResult);
            }
        } 
    }
}
