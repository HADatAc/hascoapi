package org.hascoapi.transform.mt.dsg;

import java.util.List;
import org.hascoapi.entity.pojo.StudyObject;
import org.hascoapi.entity.pojo.SemanticDataDictionary;
import org.hascoapi.entity.pojo.GenericFindWithStatus;
import org.apache.poi.ss.usermodel.*;

public class DSGVD {

    public static DSGGenHelper add(DSGGenHelper helper, StudyObject studyObject) {
        Sheet sheet = helper.workbook.getSheet(DSGGen.VD);
        if (sheet == null) {
            sheet = helper.workbook.createSheet(DSGGen.VD);
            // Colunas baseadas no Excel VD (0 linhas x 3 colunas)
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("hasExploratoryVariable");
            headerRow.createCell(1).setCellValue("hasResponseVariable");
            headerRow.createCell(2).setCellValue("hasConfoundingVariable");
            // ... (Assumindo que estas são as colunas principais para VD)
        }
        /*
        // Lógica para adicionar o StudyObject (VD)
        int rowNum = sheet.getLastRowNum() + 1;
        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(studyObject.getUri());
        row.createCell(1).setCellValue(studyObject.getLabel());
        // Assumindo que StudyObject tem um getter para hasValueType
        row.createCell(2).setCellValue(studyObject.getValueTypeUri());

         */
        
        return helper;
    }

    public static DSGGenHelper addBySDD(DSGGenHelper helper, SemanticDataDictionary sdd) {
        // A lógica de busca real deve ser implementada aqui, usando GenericFindWithStatus<StudyObject>
        
        // Exemplo de como seria a lógica (comentado):
        /*
        GenericFindWithStatus<StudyObject> soQuery = new GenericFindWithStatus<StudyObject>();
        List<StudyObject> studyObjects = soQuery.findBySDD(sdd.getUri());
        if (studyObjects != null) {
            for (StudyObject so : studyObjects) {
                helper.studyObjects.put(so.getUri(), so);
                helper = DSGVD.add(helper, so);
            }
        }
        */
        
        return helper;
    }
}
