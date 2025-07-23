package org.hascoapi.ingestion;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.monitorjbl.xlsx.StreamingReader;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;

public class SpreadsheetRecordFile implements RecordFile {

    private File file;
    private String fileName = "";
    private String sheetName = "";
    private int numberOfSheets = 1;
    private int numberOfRows;
    private List<String> headers = new ArrayList<>();

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
        this.fileName = fileName;
        this.sheetName = sheetName;
        init();
    }

    private boolean init() {
        if (file == null || file.getName() == null || file.getName().isEmpty()) {
            System.out.println("[ERROR] SpreadsheetRecordFile.init() failed: file is null or file name is empty.");
            return false;
        }

        if (fileName.isEmpty()) {
            fileName = file.getName();
        }

        try (Workbook workbook = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(new FileInputStream(file))) {

            Sheet sheet = sheetName.isEmpty() ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);

            if (sheet == null) {
                System.out.println("Could not find sheet [" + sheetName + "]");
                return false;
            }

            Iterator<Row> rows = sheet.iterator();
            int rowCount = 0;
            boolean headerFound = false;

            while (rows.hasNext()) {
                Row row = rows.next();
                if (isEmptyRow(row)) break;

                if (!headerFound) {
                    headers = getRowValues(row);
                    headerFound = true;
                }

                rowCount++;
            }

            numberOfRows = rowCount;
            numberOfSheets = workbook.getNumberOfSheets();

        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public List<Record> getRecords() {
        List<Record> records = new ArrayList<>();

        try (Workbook workbook = StreamingReader.builder()
                .rowCacheSize(100)
                .bufferSize(4096)
                .open(new FileInputStream(file))) {

            Sheet sheet = sheetName.isEmpty() ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);

            if (sheet == null) return null;

            Iterator<Row> rows = sheet.iterator();
            boolean isFirstRow = true;

            while (rows.hasNext()) {
                Row row = rows.next();

                if (isFirstRow) {
                    isFirstRow = false; // skip header
                    continue;
                }

                if (isEmptyRow(row)) break;

                records.add(new SpreadsheetFileRecord(row));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return records;
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
        try (Workbook workbook = StreamingReader.builder()
                .rowCacheSize(10)
                .bufferSize(2048)
                .open(new FileInputStream(file))) {

            Sheet sheet = sheetName.isEmpty() ? workbook.getSheetAt(0) : workbook.getSheet(sheetName);
            return sheet != null;

        } catch (IOException | IllegalArgumentException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean isEmptyRow(Row row) {
        if (row == null) return true;

        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK &&
                cell.getCellType() != CellType._NONE &&
                !cell.toString().trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private List<String> getRowValues(Row row) {
        List<String> values = new ArrayList<>();
        int lastCellNum = row.getLastCellNum();

        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = row.getCell(i);
            values.add(cell != null ? cell.toString() : "");
        }

        return values;
    }
}
