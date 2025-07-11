package org.hascoapi.ingestion;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

public class SpreadsheetRecordFile implements RecordFile {

    private File file = null;
    private String fileName = "";
    private String sheetName = "";
    private int numberOfSheets = 1;
    private int numberOfRows;
    private List<String> headers;
    List<String> cellValues = new ArrayList<String>();
    
    public SpreadsheetRecordFile(File file) {
        this.file = file;
        init();
    }
    
    public SpreadsheetRecordFile(File file, String sheetName) {
        this.file = file;
        this.sheetName = sheetName;
        init();
    }
    
    public SpreadsheetRecordFile(File file, String fileName, String sheetName) {
        this.file = file;
        this.sheetName = sheetName;
        this.fileName = fileName;
        init();
    }
    
    private boolean init() {

        StringBuilder sb = new StringBuilder(sheetName);
        if (sb.charAt(0) == '#') {
            sb.deleteCharAt(0);
            sheetName = sb.toString();
        }
        //System.out.println("SpreadsheetRecordFile: file's filename is [" + file.getName() + "]");
        //System.out.println("SpreadsheetRecordFile: RecordFile's filename is [" + fileName + "]");
        //System.out.println("SpreadsheetRecordFile: RecordFile's sheetname is [" + sheetName + "]");

        if (file == null || file.getName() == null || file.getName().isEmpty()) {
            System.out.println("[ERROR] SpreadsheetRecordFile.init() failed: file is null of file.getName() is null.");
            return false;
        }

        if (fileName.isEmpty()) {
            fileName = file.getName();
        }
        
        try (Workbook workbook = WorkbookFactory.create(new FileInputStream(file))) {
            numberOfSheets = workbook.getNumberOfSheets();
            
            //if (numberOfSheets > 0) {
            //    for (int aux = 0; aux < numberOfSheets; aux++) {
            //        Sheet sheet = workbook.getSheetAt(aux);
            //        System.out.println("Sheet " + aux + ": " + sheet.getSheetName());
            //    }
            //}
            Sheet sheet = null;
            if (sheetName.isEmpty()) {
                sheet = workbook.getSheetAt(0);
            } else {
                sheet = workbook.getSheet(sheetName);
            }
            
            if (sheet == null) {
                System.out.println("Could not find sheet [" + sheetName + "]");
                return false;
            }
            
            //numberOfRows = sheet.getLastRowNum() + 1;

            Iterator<Row> rows = sheet.iterator();
            int nonEmptyRowCount = 0;
            boolean headerFound = false;
            
            while (rows.hasNext()) {
                Row row = rows.next();
            
                if (isEmptyRow(row)) {
                    // Stop processing at the first empty row
                    break;
                }
            
                if (!headerFound) {
                    headers = getRowValues(row);  // First non-empty row is treated as header
                    headerFound = true;
                }
            
                nonEmptyRowCount++;
            }
            
            numberOfRows = nonEmptyRowCount;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (EncryptedDocumentException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public List<Record> getRecords() {

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = sheetName.isEmpty() ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);

            if (sheet == null) {
                return null;
            }
            
            Iterator<Row> rows = sheet.iterator();

            Iterable<Row> iterable = () -> rows;
            Stream<Row> stream = StreamSupport.stream(iterable.spliterator(), false);

            return stream.skip(1)
                    .filter(row -> !isEmptyRow(row))
                    .map(row -> {
                        return new SpreadsheetFileRecord(row);
                    }).collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (EncryptedDocumentException e) {
            e.printStackTrace();
        /** 
        } catch (InvalidFormatException e) {
            e.printStackTrace(); */
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }
    
    @Override
    public int getNumberOfSheets() {
        return numberOfSheets;
    }
    
    @Override
    public int getNumberOfRows() {
        return numberOfRows;
    }
    
    @Override
    public String getSheetName() {
    	return sheetName;
    }

    @Override
    public List<String> getHeaders() {
        return headers;
    }

    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public String getStorageFileName() {
        return fileName;
    }

    @Override
    public boolean isValid() {
        //System.out.println("(SpreadsheetRecordFile) Init with following filename: [" + fileName + "]");
        //System.out.println("(SpreadsheetRecordFile) Init with following sheetname: [" + sheetName + "]");
        try (FileInputStream fis = new FileInputStream(file);
            Workbook workbook = WorkbookFactory.create(fis)) {

            Sheet sheet = sheetName.isEmpty() ?
                workbook.getSheetAt(0) :
                workbook.getSheet(sheetName);

            return sheet != null;

        } catch (IOException | EncryptedDocumentException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isEmptyRow(Row row) {
        //if (row == null || row.getFirstCellNum() < 0 || row.getLastCellNum() < 0) {
        if (row == null) {
            return true;
        }

        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK &&
                cell.getCellType() != CellType._NONE &&
                !cell.toString().trim().isEmpty()) {
                return false;
            }
        }

        //for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); i++) {
        //    if (row.getCell(i) != null && !row.getCell(i).toString().trim().isEmpty()) {
        //        return false;
        //    }
        //}

        return true;
    }

    private List<String> getRowValues(Row row) {
        List<String> values = new ArrayList<String>();
        for (int i = row.getFirstCellNum(); i <= row.getLastCellNum(); i++) {
            if (row.getCell(i) != null) {
                values.add(row.getCell(i).toString());
                
            } else {
                values.add("");
            }
        }

        return values;
    }
}
    
