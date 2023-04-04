package org.sirapi.entity.pojo;

public interface SIRElement {

    /*
     *  Possible Status values: "Draft", "Published", "Deprecated"
     */
    public String getHasStatus();
    public String getHasVersion();
    public String getHasLanguage();
    public String getHasSIRMaintainerEmail();

}
