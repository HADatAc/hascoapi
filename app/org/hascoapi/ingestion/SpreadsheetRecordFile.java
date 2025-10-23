package org.hascoapi.ingestion;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.xssf.eventusermodel.ReadOnlySharedStringsTable;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xssf.model.StylesTable;
import org.apache.poi.xssf.usermodel.XSSFComment;
import org.apache.poi.xssf.eventusermodel.XSSFSheetXMLHandler;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.util.CellReference;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.File;
import java.io.InputStream;
import java.util.*;

public class SpreadsheetRecordFile implements RecordFile {

    private final File file;
    private final String fileName;
    private final String sheetName;

    private int numberOfSheets = 0;
    private int numberOfRows = 0;

    private List<String> headers = new ArrayList<>();
    private final List<Record> records = new ArrayList<>();

    public SpreadsheetRecordFile(File file) {
        this(file, "", "");
    }

    public SpreadsheetRecordFile(File file, String sheetName) {
        this(file, "", sheetName);
    }

    public SpreadsheetRecordFile(File file, String fileName, String sheetName) {
        this.file = file;
        this.fileName = fileName == null || fileName.isEmpty() ? file.getName() : fileName;
        this.sheetName = sheetName;
        init();
    }

    private boolean init() {

        System.out.println("SpreadsheetRecordFile: looking for sheetName = [" + sheetName + "]");

        if (file == null || !file.exists() || !file.canRead()) {
            System.err.println("SpreadsheetRecordFile.init() failed: file is invalid.");
            return false;
        }

        try (OPCPackage pkg = OPCPackage.open(file)) {
            XSSFReader reader = new XSSFReader(pkg);
            StylesTable styles = reader.getStylesTable();
            ReadOnlySharedStringsTable strings = new ReadOnlySharedStringsTable(pkg);
            DataFormatter formatter = new DataFormatter();

            boolean found = false;
            XSSFReader.SheetIterator iter = (XSSFReader.SheetIterator) reader.getSheetsData();
            while (iter.hasNext()) {
                try (InputStream sheetStream = iter.next()) {
                    String currentSheetName = iter.getSheetName();
                    numberOfSheets++;

                    //System.out.println("SpreadsheetRecordFile: available [" + currentSheetName + "]");

                    if (!sheetName.isEmpty() && !sheetName.equals(currentSheetName)) {
                        continue;
                    }

                    found = true;
                    //System.out.println("SpreadsheetRecordFile: found sheetName = [" + sheetName + "]");

                    InputSource sheetSource = new InputSource(sheetStream);
                    XMLReader parser = XMLReaderFactory.createXMLReader();
                    SheetHandler sheetHandler = new SheetHandler();
                    XSSFSheetXMLHandler handler = new XSSFSheetXMLHandler(styles, null, strings, sheetHandler, formatter, false);
                    parser.setContentHandler(handler);
                    parser.parse(sheetSource);

                    this.headers = sheetHandler.getHeaders();
                    this.numberOfRows = sheetHandler.getRowCount();
                    this.records.addAll(sheetHandler.getRecords());

                    break; // stop after the target sheet
                }
            }
            if (!found) {
                System.err.println("SpreadsheetRecordFile: could not found sheet with the following name: [" + sheetName + "]");
            }
            //System.out.println("SpreadsheetRecordFile: end of search for sheetName = [" + sheetName + "]");
        } catch (Exception e) {
            System.err.println("Error reading spreadsheet: " + e.getMessage());
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public List<Record> getRecords() {
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
        return file.exists() && file.canRead();
    }

    // ================================
    // Internal Sheet Handler Class
    // ================================
    private class SheetHandler implements XSSFSheetXMLHandler.SheetContentsHandler {
        private final List<Record> rowRecords = new ArrayList<>();
        private final List<String> currentRow = new ArrayList<>();
        private boolean isHeaderRow = true;

        private int rowCount = 0;

        @Override
        public void startRow(int rowNum) {
            currentRow.clear();
        }

        @Override
        public void endRow(int rowNum) {
            if (isHeaderRow) {
                headers = new ArrayList<>(currentRow);
                isHeaderRow = false;
            } else {
                rowRecords.add(new SimpleRecord(new ArrayList<>(currentRow), headers));
                rowCount++;
            }
        }

        @Override
        public void cell(String cellReference, String formattedValue, XSSFComment comment) {
            int currentColIndex = (new CellReference(cellReference)).getCol();

            // Ensure the list is big enough
            while (currentRow.size() <= currentColIndex) {
                currentRow.add("");
            }

            // Set the value at the correct column index
            currentRow.set(currentColIndex, formattedValue);
        }

        public int getRowCount() {
            return rowCount;
        }

        public List<String> getHeaders() {
            return headers;
        }

        public List<Record> getRecords() {
            return rowRecords;
        }

        public void headerFooter(String text, boolean isHeader, String tagName) {
            // No-op
        }
    }

    // ================================
    // SimpleRecord Implementation
    // ================================
    public static class SimpleRecord implements Record {
        private final List<String> values;
        private final List<String> headers;

        public SimpleRecord(List<String> values, List<String> headers) {
            this.values = values;
            this.headers = headers;
        }

        @Override
        public String getValueByColumnIndex(int index) {
            return (index >= 0 && index < values.size()) ? values.get(index) : "";
        }

        @Override
        public String getValueByColumnName(String columnName) {
            if (columnName == null || headers == null) return "";
            for (int i = 0; i < headers.size(); i++) {
                if (columnName.equalsIgnoreCase(headers.get(i))) {
                    return getValueByColumnIndex(i);
                }
            }
            return "";
        }

        public List<String> getValues() {
            return values;
        }

        @Override
        public int size() {
            return values.size();
        }
    }
}
