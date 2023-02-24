package org.sirapi.console.controllers.ontologies;

import org.sirapi.utils.Feedback;
import org.sirapi.utils.Triplestore;
import play.mvc.*;

import org.sirapi.entity.pojo.NameSpace;
import org.sirapi.utils.NameSpaces;

import com.typesafe.config.ConfigFactory;

public class LoadOnt extends Controller {

    public static final String LAST_LOADED_NAMESPACE = "/last-loaded-namespaces-properties";

    public static String playLoadOntologies(String oper) {
        NameSpaces.getInstance();
        Triplestore ts = new
                Triplestore("user",
                "password",
                ConfigFactory.load().getString("sirapi.repository.triplestore"),
                false);
        return ts.loadOntologies(Feedback.WEB);
    }

    public Result reloadNamedGraphFromRemote(String abbreviation) {
        NameSpace ns = NameSpaces.getInstance().getNamespaces().get(abbreviation);
        ns.deleteTriples();

        String url = ns.getURL();
        if (!url.isEmpty()) {
            ns.loadTriples(url, true);
        }
        ns.updateNumberOfLoadedTriples();
        ns.updateFromTripleStore();

        return redirect(routes.Maintenance.index("init"));
    }

    public Result deleteNamedGraph(String abbreviation) {
        NameSpace ns = NameSpaces.getInstance().getNamespaces().get(abbreviation);
        ns.deleteTriples();
        ns.updateNumberOfLoadedTriples();
        ns.updateFromTripleStore();

        return redirect(routes.Maintenance.index("init"));
    }

    public Result deleteAllNamedGraphs() {
        for (NameSpace ns : NameSpaces.getInstance().getNamespaces().values()) {
            ns.deleteTriples();
            ns.updateNumberOfLoadedTriples();
            ns.updateFromTripleStore();
        }

        return redirect(routes.Maintenance.index("init"));
    }

}

