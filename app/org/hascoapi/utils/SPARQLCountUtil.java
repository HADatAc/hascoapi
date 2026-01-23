package org.hascoapi.utils;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;

/** Simple COUNT(*) helper for SPARQL select queries returning ?tot. */
public class SPARQLCountUtil {

    private SPARQLCountUtil() {
    }

    public static long count(String sparqlService, String queryString) {
        ResultSetRewindable rs = SPARQLUtils.select(sparqlService, queryString);
        if (rs == null || !rs.hasNext()) {
            return 0;
        }
        QuerySolution sol = rs.next();
        if (sol == null || sol.get("tot") == null) {
            return 0;
        }
        try {
            return sol.getLiteral("tot").getLong();
        } catch (Exception e) {
            try {
                return Long.parseLong(sol.get("tot").toString());
            } catch (Exception ignored) {
                return 0;
            }
        }
    }
}

