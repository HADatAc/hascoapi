package org.hascoapi.ingestion;

import org.hascoapi.entity.pojo.DataFile;

public interface GeneratorFactory {

    BaseGenerator create(DataFile dataFile, String status);
 
}