package org.hascoapi.ingestion;

import java.io.UnsupportedEncodingException;
import java.lang.String;
import java.net.URLDecoder;
import java.util.*;

import org.hascoapi.entity.pojo.DataFile;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.Stream;
import org.hascoapi.utils.URIUtils;

public class AnnotateDA {

    public static GeneratorChain exec(DataFile dataFile) {
        System.out.println("Processing DA file " + dataFile.getFilename());

        GeneratorChain chain = new GeneratorChain();

        Stream stream = null;
        String stream_uri = null;
        String deployment_uri = null;
        String semanticdatadictionary_uri = null;
        String study_uri = null;

        if (dataFile != null) {
            stream_uri = URIUtils.replacePrefixEx(dataFile.getStreamUri());
            stream = Stream.find(stream_uri);
            if (stream != null) {
                if (!stream.isComplete()) {
                    dataFile.getLogger().printWarningByIdWithArgs("DA_00003", stream_uri);
                    chain.setInvalid();
                    return chain;
                } else {
                    dataFile.getLogger().println(String.format("Stream <%s> has been located", stream_uri));
                }
                study_uri = stream.getStudy().getUri();
                deployment_uri = stream.getDeploymentUri();
                semanticdatadictionary_uri = stream.getSemanticDataDictionaryUri();
            } else {
                dataFile.getLogger().printWarningByIdWithArgs("DA_00004", stream_uri);
                chain.setInvalid();
                return chain;
            }
        }

        if (study_uri == null || study_uri.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DA_00008", stream_uri);
            chain.setInvalid();
            return chain;
        } else {
            try {
                study_uri = URLDecoder.decode(study_uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                dataFile.getLogger().printException(String.format("URL decoding error for study uri <%s>", study_uri));
                chain.setInvalid();
                return chain;
            }
            dataFile.getLogger().println(String.format("Study <%s> specified for stream <%s>", study_uri, stream_uri));
        }

        if (semanticdatadictionary_uri == null || semanticdatadictionary_uri.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DA_00005", stream_uri);
            chain.setInvalid();
            return chain;
        } else {
            dataFile.getLogger().println(String.format("Semantic data dictionary <%s> specified for stream: <%s>", semanticdatadictionary_uri, stream_uri));
        }

        if (deployment_uri == null || deployment_uri.isEmpty()) {
            dataFile.getLogger().printExceptionByIdWithArgs("DA_00006", stream_uri);
            chain.setInvalid();
            return chain;
        } else {
            try {
                deployment_uri = URLDecoder.decode(deployment_uri, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                dataFile.getLogger().printException(String.format("URL decoding error for deployment uri <%s>", deployment_uri));
                chain.setInvalid();
                return chain;
            }
            dataFile.getLogger().println(String.format("Deployment <%s> specified for data acquisition <%s>", deployment_uri, stream_uri));
        }

        if (stream != null) {
            dataFile.setStudyUri(stream.getStudy().getUri());
            // TODO
            //dataFile.setDatasetUri(DataFactory.getNextDatasetURI(str.getUri()));
            stream.addDatasetUri(dataFile.getDatasetUri());

            SemanticDataDictionary semanticdatadictionary = SemanticDataDictionary.find(stream.getSemanticDataDictionaryUri());
            if (semanticdatadictionary == null) {
                dataFile.getLogger().printExceptionByIdWithArgs("DA_00007", stream.getSemanticDataDictionaryUri());
                chain.setInvalid();
                return chain;
            }

            if (!stream.hasCellScope()) {
            	// Need to be fixed here by getting codeMap and codebook from sparql query
            	DASOInstanceGenerator dasoInstanceGen = new DASOInstanceGenerator(dataFile, stream, dataFile.getFilename());
            	chain.addGenerator(dasoInstanceGen);
            	chain.addGenerator(new ValueGenerator(ValueGenerator.FILEMODE, dataFile, stream, semanticdatadictionary, dasoInstanceGen));
            } else {
                chain.addGenerator(new ValueGenerator(ValueGenerator.FILEMODE, dataFile, stream, semanticdatadictionary, null));
            }
            chain.setNamedGraphUri(URIUtils.replacePrefixEx(dataFile.getStreamUri()));
        }

        return chain;
    }

}
