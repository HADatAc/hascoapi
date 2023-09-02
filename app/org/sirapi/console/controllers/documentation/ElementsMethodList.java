package org.sirapi.console.controllers.documentation;

import play.mvc.Result;
import play.mvc.Controller;
import org.sirapi.console.views.html.documentation.*;

public class ElementsMethodList extends Controller {

    public Result index() {
        return ok(elementsmethodlist.render());
    }

    public Result createInstrumentExample() {
        return ok(createInstrumentExample.render());
    }

    public Result deleteInstrumentExample() {
        return ok(deleteInstrumentExample.render());
    }

    public Result createDetectorStemExample() {
        return ok(createDetectorStemExample.render());
    }

    public Result deleteDetectorStemExample() {
        return ok(deleteDetectorStemExample.render());
    }

    public Result createDetectorExample() {
        return ok(createDetectorExample.render());
    }

    public Result deleteDetectorExample() {
        return ok(deleteDetectorExample.render());
    }

}