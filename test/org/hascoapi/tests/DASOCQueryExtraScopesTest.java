package org.hascoapi.tests;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.NameSpaces;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

/**
 * DA-SOC EXTRA SCOPES QUERY TEST
 * 
 * Este teste demonstra como consultar as propriedades extras (scopes)
 * adicionadas pelo DA-SOC-LOCATION.csv aos 50 objetos do SOC-LOCATION.
 * 
 * Propriedades base (do DSG):
 *   - rdf:type
 *   - hasco:isMemberOf
 *   - hasco:originalID
 *   - hasco:label
 *   - rdfs:comment
 * 
 * Propriedades extras (do DA-SOC):
 *   - pharma:altitude_m
 *   - pharma:floor
 *   - pharma:zone_code
 *   - pharma:area_m2
 *   - pharma:orientation
 *   - hasco:hasTimestamp
 */
public class DASOCQueryExtraScopesTest {

    private static final String FUSEKI_QUERY_ENDPOINT = "http://localhost:3030/store/query";
    private static final String PHARMA_PREFIX = "http://hadatac.org/ont/pharma#";
    private static final String HASCO_PREFIX = "http://hadatac.org/ont/hasco#";

    @Test
    public void testQueryExtraScopes() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("DA-SOC EXTRA SCOPES QUERY TEST");
        System.out.println("=".repeat(80) + "\n");

        // Buscar o SOC-LOCATION
        String socUri = findSOCLocation();
        assertNotNull("SOC-LOCATION deve existir", socUri);
        System.out.println("✓ SOC encontrado: " + socUri + "\n");

        // Consultar objetos com propriedades extras
        List<Map<String, String>> results = queryObjectsWithExtraScopes(socUri);
        
        System.out.println("Total de objetos encontrados: " + results.size());
        System.out.println();

        // Verificar que temos os 50 objetos esperados
        assertEquals("Deve ter 50 objetos no SOC-LOCATION", 50, results.size());

        // Mostrar alguns exemplos
        System.out.println("EXEMPLOS DE OBJETOS COM SCOPES EXTRAS:");
        System.out.println("-".repeat(80));
        
        for (int i = 0; i < Math.min(10, results.size()); i++) {
            Map<String, String> obj = results.get(i);
            System.out.println("\n" + (i + 1) + ". " + obj.get("originalID"));
            System.out.println("   Altitude: " + obj.get("altitude") + " m");
            System.out.println("   Andar: " + obj.get("floor"));
            System.out.println("   Zona: " + obj.get("zone"));
            System.out.println("   Área: " + obj.get("area") + " m²");
            System.out.println("   Orientação: " + obj.get("orientation"));
            if (obj.get("timestamp") != null) {
                System.out.println("   Timestamp: " + obj.get("timestamp"));
            }
        }

        // Verificar que todos têm as propriedades extras
        for (Map<String, String> obj : results) {
            assertNotNull("Altitude deve existir para " + obj.get("originalID"), obj.get("altitude"));
            assertNotNull("Floor deve existir para " + obj.get("originalID"), obj.get("floor"));
            assertNotNull("Zone deve existir para " + obj.get("originalID"), obj.get("zone"));
            assertNotNull("Area deve existir para " + obj.get("originalID"), obj.get("area"));
            assertNotNull("Orientation deve existir para " + obj.get("originalID"), obj.get("orientation"));
        }

        // Estatísticas por zona
        System.out.println("\n" + "-".repeat(80));
        System.out.println("\nESTATÍSTICAS POR ZONA:");
        System.out.println("-".repeat(80));
        
        Map<String, Integer> zoneCount = new HashMap<>();
        for (Map<String, String> obj : results) {
            String zone = obj.get("zone");
            zoneCount.put(zone, zoneCount.getOrDefault(zone, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : zoneCount.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue() + " objetos");
        }

        // Estatísticas por andar
        System.out.println("\nESTATÍSTICAS POR ANDAR:");
        System.out.println("-".repeat(80));
        
        Map<String, Integer> floorCount = new HashMap<>();
        for (Map<String, String> obj : results) {
            String floor = obj.get("floor");
            floorCount.put(floor, floorCount.getOrDefault(floor, 0) + 1);
        }
        
        // Ordenar por andar
        List<String> floors = new ArrayList<>(floorCount.keySet());
        floors.sort(Comparator.comparingInt(Integer::parseInt));
        
        for (String floor : floors) {
            System.out.println("  Andar " + floor + ": " + floorCount.get(floor) + " objetos");
        }

        System.out.println("\n" + "=".repeat(80));
        System.out.println("✓ TESTE COMPLETO - Todos os 50 objetos têm scopes extras do DA-SOC");
        System.out.println("=".repeat(80) + "\n");
    }

    /**
     * Encontra o URI do SOC-LOCATION
     */
    private String findSOCLocation() {
        String queryStr = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?socUri WHERE { \n" +
            "  ?socUri a hasco:StudyObjectCollection . \n" +
            "  ?socUri hasco:hascoType \"LOCATION\" . \n" +
            "} LIMIT 1";

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(FUSEKI_QUERY_ENDPOINT, queryStr)) {
            ResultSet results = qexec.execSelect();
            if (results.hasNext()) {
                QuerySolution soln = results.nextSolution();
                return soln.getResource("socUri").getURI();
            }
        } catch (Exception e) {
            System.err.println("Erro ao buscar SOC-LOCATION: " + e.getMessage());
        }
        return null;
    }

    /**
     * Consulta todos os objetos do SOC com suas propriedades extras
     */
    private List<Map<String, String>> queryObjectsWithExtraScopes(String socUri) {
        List<Map<String, String>> results = new ArrayList<>();

        String queryStr = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?objUri ?originalID ?altitude ?floor ?zone ?area ?orientation ?timestamp \n" +
            "WHERE { \n" +
            "  ?objUri hasco:isMemberOf <" + socUri + "> . \n" +
            "  ?objUri hasco:originalID ?originalID . \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "altitude_m> ?altitude . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "floor> ?floor . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "zone_code> ?zone . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "area_m2> ?area . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "orientation> ?orientation . } \n" +
            "  OPTIONAL { ?objUri hasco:hasTimestamp ?timestamp . } \n" +
            "} ORDER BY ?originalID";

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(FUSEKI_QUERY_ENDPOINT, queryStr)) {
            ResultSet resultSet = qexec.execSelect();
            
            while (resultSet.hasNext()) {
                QuerySolution soln = resultSet.nextSolution();
                Map<String, String> obj = new HashMap<>();
                
                obj.put("objUri", soln.getResource("objUri").getURI());
                obj.put("originalID", soln.getLiteral("originalID").getString());
                
                if (soln.contains("altitude")) {
                    obj.put("altitude", soln.getLiteral("altitude").getString());
                }
                if (soln.contains("floor")) {
                    obj.put("floor", soln.getLiteral("floor").getString());
                }
                if (soln.contains("zone")) {
                    obj.put("zone", soln.getLiteral("zone").getString());
                }
                if (soln.contains("area")) {
                    obj.put("area", soln.getLiteral("area").getString());
                }
                if (soln.contains("orientation")) {
                    obj.put("orientation", soln.getLiteral("orientation").getString());
                }
                if (soln.contains("timestamp")) {
                    obj.put("timestamp", soln.getLiteral("timestamp").getString());
                }
                
                results.add(obj);
            }
        } catch (Exception e) {
            System.err.println("Erro ao consultar objetos: " + e.getMessage());
            e.printStackTrace();
        }

        return results;
    }

    /**
     * Teste adicional: consulta apenas objetos de uma zona específica
     */
    @Test
    public void testQueryByZone() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TESTE: CONSULTAR OBJETOS POR ZONA");
        System.out.println("=".repeat(80) + "\n");

        String socUri = findSOCLocation();
        assertNotNull("SOC-LOCATION deve existir", socUri);

        String targetZone = "ZONE-A";
        List<Map<String, String>> results = queryObjectsByZone(socUri, targetZone);

        System.out.println("Objetos na " + targetZone + ": " + results.size());
        System.out.println();

        for (Map<String, String> obj : results) {
            System.out.println("  - " + obj.get("originalID") + 
                             " (andar " + obj.get("floor") + 
                             ", " + obj.get("area") + " m², " +
                             obj.get("orientation") + ")");
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }

    private List<Map<String, String>> queryObjectsByZone(String socUri, String zone) {
        List<Map<String, String>> results = new ArrayList<>();

        String queryStr = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?objUri ?originalID ?altitude ?floor ?area ?orientation \n" +
            "WHERE { \n" +
            "  ?objUri hasco:isMemberOf <" + socUri + "> . \n" +
            "  ?objUri hasco:originalID ?originalID . \n" +
            "  ?objUri <" + PHARMA_PREFIX + "zone_code> \"" + zone + "\" . \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "altitude_m> ?altitude . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "floor> ?floor . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "area_m2> ?area . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "orientation> ?orientation . } \n" +
            "} ORDER BY ?floor ?originalID";

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(FUSEKI_QUERY_ENDPOINT, queryStr)) {
            ResultSet resultSet = qexec.execSelect();
            
            while (resultSet.hasNext()) {
                QuerySolution soln = resultSet.nextSolution();
                Map<String, String> obj = new HashMap<>();
                
                obj.put("objUri", soln.getResource("objUri").getURI());
                obj.put("originalID", soln.getLiteral("originalID").getString());
                
                if (soln.contains("altitude")) obj.put("altitude", soln.getLiteral("altitude").getString());
                if (soln.contains("floor")) obj.put("floor", soln.getLiteral("floor").getString());
                if (soln.contains("area")) obj.put("area", soln.getLiteral("area").getString());
                if (soln.contains("orientation")) obj.put("orientation", soln.getLiteral("orientation").getString());
                
                results.add(obj);
            }
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }

        return results;
    }

    /**
     * Teste adicional: consulta objetos por andar
     */
    @Test
    public void testQueryByFloor() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("TESTE: CONSULTAR OBJETOS POR ANDAR");
        System.out.println("=".repeat(80) + "\n");

        String socUri = findSOCLocation();
        assertNotNull("SOC-LOCATION deve existir", socUri);

        int targetFloor = 0;
        List<Map<String, String>> results = queryObjectsByFloor(socUri, targetFloor);

        System.out.println("Objetos no andar " + targetFloor + ": " + results.size());
        System.out.println();

        for (Map<String, String> obj : results) {
            System.out.println("  - " + obj.get("originalID") + 
                             " (zona " + obj.get("zone") + 
                             ", " + obj.get("area") + " m², " +
                             obj.get("altitude") + " m altitude)");
        }

        System.out.println("\n" + "=".repeat(80) + "\n");
    }

    private List<Map<String, String>> queryObjectsByFloor(String socUri, int floor) {
        List<Map<String, String>> results = new ArrayList<>();

        String queryStr = NameSpaces.getInstance().printSparqlNameSpaceList() +
            "SELECT ?objUri ?originalID ?altitude ?zone ?area ?orientation \n" +
            "WHERE { \n" +
            "  ?objUri hasco:isMemberOf <" + socUri + "> . \n" +
            "  ?objUri hasco:originalID ?originalID . \n" +
            "  ?objUri <" + PHARMA_PREFIX + "floor> \"" + floor + "\" . \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "altitude_m> ?altitude . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "zone_code> ?zone . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "area_m2> ?area . } \n" +
            "  OPTIONAL { ?objUri <" + PHARMA_PREFIX + "orientation> ?orientation . } \n" +
            "} ORDER BY ?zone ?originalID";

        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(FUSEKI_QUERY_ENDPOINT, queryStr)) {
            ResultSet resultSet = qexec.execSelect();
            
            while (resultSet.hasNext()) {
                QuerySolution soln = resultSet.nextSolution();
                Map<String, String> obj = new HashMap<>();
                
                obj.put("objUri", soln.getResource("objUri").getURI());
                obj.put("originalID", soln.getLiteral("originalID").getString());
                
                if (soln.contains("altitude")) obj.put("altitude", soln.getLiteral("altitude").getString());
                if (soln.contains("zone")) obj.put("zone", soln.getLiteral("zone").getString());
                if (soln.contains("area")) obj.put("area", soln.getLiteral("area").getString());
                if (soln.contains("orientation")) obj.put("orientation", soln.getLiteral("orientation").getString());
                
                results.add(obj);
            }
        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
        }

        return results;
    }
}

