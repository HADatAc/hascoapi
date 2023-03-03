package org.sirapi.console.controllers.ontologies;

import org.sirapi.console.views.html.ontologies.maintenance;
import org.sirapi.utils.NameSpaces;
import play.mvc.Controller;
import play.mvc.Result;

public class Maintenance extends Controller {

    public Result index(String oper) {
        return ok(maintenance.render(oper, NameSpaces.getInstance().getOrderedNamespacesAsList()));
    }

    public Result postIndex(String oper) {
        return index(oper);
    }

}