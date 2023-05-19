package org.sirapi.console.controllers.documentation;

import org.sirapi.console.views.html.documentation.*;
import play.mvc.Controller;
import play.mvc.Result;

public class RetrievalMethodList extends Controller {

    public Result index() {
        return ok(retrievalmethodlist.render());
    }

    public Result queryURIExample() {
        return ok(queryURIExample.render());
    }

    public Result instrumentRenderingExample() {
        return ok(instrumentRenderingExample.render());
    }

}