package org.sirapi.entity.pojo;

public interface SIRElement {

    /*
     *  Possible Status values: "Draft", "UnderReview", "Published", "Deprecated"
     */
    public String getHasStatus();
    public String getHasVersion();
    public String getHasLanguage();
    public String getHasSIRManagerEmail();

    public static int getNumberElements(String elementType) {
        if (elementType.equals("instrument")) {
            return GenericFind.getNumberElements(Instrument.class);
        } else if (elementType.equals("detectorslot")) {
            return GenericFind.getNumberElements(DetectorSlot.class);
        } else if (elementType.equals("detectorstem")) {
            return GenericFind.getNumberElements(DetectorStem.class);
        } else if (elementType.equals("detector")) {
            return GenericFind.getNumberElements(Detector.class);
        } else if (elementType.equals("codebook")) {
            return GenericFind.getNumberElements(Codebook.class);
        } else if (elementType.equals("responseoptionslot")) {
            return GenericFind.getNumberElements(ResponseOptionSlot.class);
        } else if (elementType.equals("responseoption")) {
            return GenericFind.getNumberElements(ResponseOption.class);
        } else if (elementType.equals("semanticvariable")) {
            return GenericFind.getNumberElements(SemanticVariable.class);
        } else if (elementType.equals("entity")) {
            return GenericFind.getNumberElements(Entity.class);
        } else if (elementType.equals("attribute")) {
            return GenericFind.getNumberElements(Attribute.class);
        } else if (elementType.equals("unit")) {
            return GenericFind.getNumberElements(Unit.class);
        }
        return -1;
    }



}
