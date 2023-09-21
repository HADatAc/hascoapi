package org.sirapi.entity.pojo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.sirapi.utils.SPARQLUtils;
import org.sirapi.utils.CollectionUtil;
import org.sirapi.utils.NameSpaces;
import org.sirapi.vocabularies.HASCO;
import org.sirapi.vocabularies.RDF;
import org.sirapi.vocabularies.RDFS;
import org.sirapi.vocabularies.SIO;


public class Unit extends HADatAcClass implements Comparable<Unit> {

    static String className = SIO.UNIT;

    public Unit() {
        super(className);
    }

    public static List<Unit> find() {
        List<Unit> units = new ArrayList<Unit>();
        String queryString = NameSpaces.getInstance().printSparqlNameSpaceList() +
                " SELECT ?uri WHERE { " +
                " ?uri rdfs:subClassOf* sio:SIO_000052 . " +
                "} ";

        ResultSetRewindable resultsrw = SPARQLUtils.select(
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY), queryString);

		while (resultsrw.hasNext()) {
			QuerySolution soln = resultsrw.next();
			Unit unit = find(soln.getResource("uri").getURI());
			units.add(unit);
			break;
		}			

		java.util.Collections.sort((List<Unit>) units);
		return units;
	}

	public static Map<String,String> getMap() {
		List<Unit> list = find();
		Map<String,String> map = new HashMap<String,String>();
		for (Unit ent: list) 
			map.put(ent.getUri(),ent.getLabel());
		return map;
	}

	public static Unit find(String uri) {
		System.out.println("Unit.find(uri) with uri [" + uri + "]");
		Unit unit = null;
		Model model;
		Statement statement;
		RDFNode object;

		String queryString = "DESCRIBE <" + uri + ">";
		try {
		    model = SPARQLUtils.describe(CollectionUtil.getCollectionPath(
		            CollectionUtil.Collection.SPARQL_QUERY), queryString);
		} catch (Exception e) {
		    System.out.println("[ERROR] Unit.find(uri) failed to execute describe query");
		    return null;
		}

		System.out.println("Unit.find(uri) [1]");
		StmtIterator stmtIterator = model.listStatements();

        if (!stmtIterator.hasNext()) {
            return null;
        }

		System.out.println("Unit.find(uri) [2]");
		if (model.size() > 0) {
			unit = new Unit();
			while (stmtIterator.hasNext()) {
				System.out.println("Unit.find(uri) [3]");

				statement = stmtIterator.next();
				object = statement.getObject();
				System.out.println("Unit.find(uri) " + statement.getPredicate().getURI());
				if (statement.getPredicate().getURI().equals(RDFS.SUBCLASS_OF)) {
					unit.setSuperUri(object.asResource().getURI());
					System.out.println("Unit.find(uri): subClassOf is " + object.asResource().getURI());
				} else if (statement.getPredicate().getURI().equals(RDF.TYPE)) {
					unit.setTypeUri(object.asResource().getURI());
				} else if (statement.getPredicate().getURI().equals(HASCO.HASCO_TYPE)) {
					unit.setHascoTypeUri(object.asResource().getURI());
				} else if (statement.getPredicate().getURI().equals(RDFS.COMMENT)) {
					unit.setComment(object.asLiteral().getString());
				} else if (statement.getPredicate().getURI().equals(RDFS.LABEL)) {
					unit.setLabel(object.asLiteral().getString());
				}
			}

			unit.setUri(uri);
			unit.setLocalName(uri.substring(uri.indexOf('#') + 1));
			if (unit.getLabel() == null || unit.getLabel().equals("")) {
				unit.setLabel(unit.getLocalName());
			}
		}

		return unit;
	}


	@Override
	public int compareTo(Unit another) {
		if (this.getLabel() != null && another.getLabel() != null) {
			return this.getLabel().compareTo(another.getLabel());
		}
		return this.getLocalName().compareTo(another.getLocalName());
	}
}
