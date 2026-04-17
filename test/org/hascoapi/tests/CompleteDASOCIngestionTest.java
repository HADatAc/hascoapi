package org.hascoapi.tests;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.StudyObjectCollection;
import org.hascoapi.ingestion.AnnotateDASOC;
import org.hascoapi.utils.CollectionUtil;
import org.hascoapi.utils.ConfigProp;
import org.hascoapi.utils.SPARQLUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.File;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSetRewindable;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;

/**
 * Test completo de ingestão de DA-SOC:
 * 1. Cria manualmente objetos no SOC-LOCATION
 * 2. Ingere o arquivo DA-SOC-LOCATION.csv
 * 3. Verifica os dados enriquecidos no triplestore
 */
public class CompleteDASOCIngestionTest {

    private static final String SOC_URI = "http://hadatac.org/kb/default/SOC-LOCATION";
    private static final String DA_URI = "http://hadatac.org/kb/default/DA-TEST-COMPLETE";
    
    @Before
    public void setUp() {
        System.out.println("\n========================================");
        System.out.println("SETUP: Criando objetos no SOC-LOCATION");
        System.out.println("========================================\n");
        
        // Criar objetos manualmente no SOC-LOCATION
        createTestObjects();
    }
    
    //@After
    public void tearDown() {
        System.out.println("\n========================================");
        System.out.println("TEARDOWN: Limpando dados de teste");
        System.out.println("========================================\n");
        
        // COMENTADO PARA PRESERVAR DADOS E PERMITIR VERIFICAÇÃO MANUAL
        // Descomentar para limpar após testes
        // cleanupTestData();
        
        System.out.println("⚠️  CLEANUP DESABILITADO - dados preservados para verificação manual");
        System.out.println("Para limpar, execute: sbt \"testOnly org.hascoapi.tests.CleanupTestData\"");
    }

    private void createTestObjects() {
        String[] objectIds = {
            "LIBRARY-L0", "LIBRARY-L1", "LIBRARY-L2", "LIBRARY-L3", "LIBRARY-ROOF",
            "SCIENCE-L0", "SCIENCE-L1", "SCIENCE-L2", "SCIENCE-L3", "SCIENCE-ROOF"
        };
        
        for (String objId : objectIds) {
            String objUri = "http://hadatac.org/kb/default/" + objId;
            String insertQuery = 
                "PREFIX hasco: <http://hadatac.org/ont/hasco#> " +
                "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> " +
                "INSERT DATA { " +
                "  <" + objUri + "> a hasco:StudyObject ; " +
                "    hasco:isMemberOf <" + SOC_URI + "> ; " +
                "    hasco:originalID \"" + objId + "\" ; " +
                "    rdfs:label \"Test Location " + objId + "\" ; " +
                "    rdfs:comment \"Test object for DA-SOC ingestion\" . " +
                "}";
            
            try {
                UpdateRequest request = UpdateFactory.create(insertQuery);
                UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                    request, 
                    CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)
                );
                processor.execute();
                System.out.println("✓ Criado objeto: " + objId);
            } catch (Exception e) {
                System.err.println("✗ Erro ao criar objeto " + objId + ": " + e.getMessage());
            }
        }
        
        // Verificar quantos objetos foram criados
        String countQuery = 
            "PREFIX hasco: <http://hadatac.org/ont/hasco#> " +
            "SELECT (COUNT(?obj) as ?count) WHERE { " +
            "  ?obj hasco:isMemberOf <" + SOC_URI + "> " +
            "}";
        
        ResultSetRewindable results = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
            countQuery
        );
        
        if (results.hasNext()) {
            QuerySolution soln = results.next();
            int count = soln.getLiteral("count").getInt();
            System.out.println("\n✓ Total de objetos criados: " + count);
        }
    }
    
    @Test
    public void testCompleteDASOCIngestion() throws Exception {
        System.out.println("\n========================================");
        System.out.println("TESTE: Ingestão completa do DA-SOC");
        System.out.println("========================================\n");
        
        // 1. Verificar que objetos existem
        System.out.println("1. Verificando objetos no SOC-LOCATION...");
        StudyObjectCollection soc = StudyObjectCollection.find(SOC_URI);
        assertNotNull("SOC-LOCATION deve existir", soc);
        
        int objectCount = soc.getCollectionSize();
        System.out.println("   ✓ Objetos encontrados: " + objectCount);
        assertTrue("Deve haver pelo menos 10 objetos", objectCount >= 10);
        
        // 2. Preparar DataFile e arquivo
        System.out.println("\n2. Preparando DataFile para ingestão...");
        File sourceFile = new File("test/resources/da/DA-SOC-LOCATION.csv");
        assertTrue("Arquivo DA-SOC-LOCATION.csv deve existir", sourceFile.exists());
        
        DataFile dataFile = new DataFile("TEST-ID", "DA-SOC-LOCATION.csv");
        dataFile.setUri(DA_URI + "-file");
        dataFile.setDasocSOCUri(SOC_URI);
        dataFile.setDasocDataAcquisitionUri(DA_URI);
        dataFile.setHasSIRManagerEmail("test@example.com");
        System.out.println("   ✓ DataFile preparado");
        
        // 3. Executar ingestão do DA-SOC
        System.out.println("\n3. Executando ingestão do DA-SOC...");
        AnnotateDASOC.IngestionResult result = AnnotateDASOC.processDASOC(
            dataFile,
            sourceFile,
            DA_URI,
            SOC_URI
        );
        
        System.out.println("\n========================================");
        System.out.println("RESULTADOS DA INGESTÃO");
        System.out.println("========================================");
        System.out.println("Status: " + (result.isSuccess() ? "✓ SUCESSO" : "✗ FALHA"));
        System.out.println("Total de linhas: " + result.getRowCount());
        System.out.println("Mensagem: " + result.getErrorMessage());
        System.out.println("========================================\n");
        
        // 4. Validar resultado
        assertTrue("Ingestão deve ter sucesso", result.isSuccess());
        assertTrue("Deve processar pelo menos 10 linhas", result.getRowCount() >= 10);
        
        // 5. Verificar dados enriquecidos no triplestore
        System.out.println("4. Verificando dados enriquecidos no triplestore...");
        
        // Primeiro, verificar se o named graph existe
        String graphCheckQuery =
            "SELECT DISTINCT ?g WHERE { " +
            "  GRAPH ?g { ?s ?p ?o } " +
            "  FILTER(CONTAINS(STR(?g), '-dasoc')) " +
            "} LIMIT 10";
        
        ResultSetRewindable graphResults = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
            graphCheckQuery
        );
        
        System.out.println("   Named graphs -dasoc encontrados:");
        while (graphResults.hasNext()) {
            QuerySolution soln = graphResults.next();
            System.out.println("     - " + soln.get("g").toString());
        }
        
        // Buscar objetos enriquecidos em qualquer graph -dasoc
        String verifyQuery =
            "PREFIX hasco: <http://hadatac.org/ont/hasco#> " +
            "PREFIX pharma: <http://hadatac.org/ont/pharma#> " +
            "SELECT ?g ?obj ?altitude ?floor ?zone " +
            "WHERE { " +
            "  GRAPH ?g { " +
            "    ?obj pharma:altitude_m ?altitude . " +
            "    OPTIONAL { ?obj pharma:floor ?floor } " +
            "    OPTIONAL { ?obj pharma:zone_code ?zone } " +
            "  } " +
            "  FILTER(CONTAINS(STR(?g), '-dasoc')) " +
            "} LIMIT 10";
        
        ResultSetRewindable verifyResults = SPARQLUtils.select(
            CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_QUERY),
            verifyQuery
        );
        
        int enrichedCount = 0;
        System.out.println("   Objetos enriquecidos:");
        while (verifyResults.hasNext()) {
            QuerySolution soln = verifyResults.next();
            enrichedCount++;
            System.out.println("   ✓ Objeto " + enrichedCount + ":");
            System.out.println("     Graph: " + soln.get("g").toString());
            System.out.println("     URI: " + soln.get("obj").toString());
            System.out.println("     Altitude: " + soln.getLiteral("altitude").getString());
            if (soln.getLiteral("floor") != null) {
                System.out.println("     Floor: " + soln.getLiteral("floor").getString());
            }
            if (soln.getLiteral("zone") != null) {
                System.out.println("     Zone: " + soln.getLiteral("zone").getString());
            }
            System.out.println("");
        }
        
        System.out.println("   Total de objetos enriquecidos encontrados: " + enrichedCount);
        assertTrue("Deve haver pelo menos 5 objetos enriquecidos", enrichedCount >= 5);
        
        System.out.println("\n✓ TESTE COMPLETO DE INGESTÃO DO DA-SOC: SUCESSO!");
    }
    
   /* private void cleanupTestData() {
        // Deletar named graph do DA-SOC
        String deleteGraphQuery = "DROP SILENT GRAPH <" + DA_URI + "-dasoc>";
        try {
            UpdateRequest request = UpdateFactory.create(deleteGraphQuery);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request,
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)
            );
            processor.execute();
            System.out.println("✓ Named graph deletado: " + DA_URI + "-dasoc");
        } catch (Exception e) {
            System.err.println("✗ Erro ao deletar named graph: " + e.getMessage());
        }
        
        // Deletar objetos de teste
        String deleteObjectsQuery =
            "PREFIX hasco: <http://hadatac.org/ont/hasco#> " +
            "DELETE WHERE { " +
            "  ?obj hasco:isMemberOf <" + SOC_URI + "> ; " +
            "       ?p ?o . " +
            "}";
        
        try {
            UpdateRequest request = UpdateFactory.create(deleteObjectsQuery);
            UpdateProcessor processor = UpdateExecutionFactory.createRemote(
                request,
                CollectionUtil.getCollectionPath(CollectionUtil.Collection.SPARQL_UPDATE)
            );
            processor.execute();
            System.out.println("✓ Objetos de teste deletados do SOC-LOCATION");
        } catch (Exception e) {
            System.err.println("✗ Erro ao deletar objetos: " + e.getMessage());
        }
    }*/
}

