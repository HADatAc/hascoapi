package org.sirapi.console.controllers;

import org.sirapi.entity.pojo.OntologyTripleStore;
import org.sirapi.utils.ConfigProp;
import org.sirapi.utils.NameSpaces;
import play.mvc.Controller;
import play.mvc.Result;

//import org.sirapi.console.controllers.ontologies.LoadOnt;
import org.sirapi.console.views.html.version;

public class Version extends Controller {

    public Result index() {
        String code_version = "0.0.1";
        String base_ontology = "";

        String loaded_base_ontology = NameSpaces.getInstance().getNameByAbbreviation(base_ontology);
        String loaded_base_ontology_version = OntologyTripleStore.getVersionFromAbbreviation(base_ontology);
        //String propfile = LoadOnt.getNameLastLoadedNamespace();
        //return ok(version.render(code_version, base_ontology, loaded_base_ontology, loaded_base_ontology_version, propfile));
        return ok(version.render(code_version, base_ontology, loaded_base_ontology, loaded_base_ontology_version, null));
    }

    public Result postIndex() {
        return index();
    }
}
