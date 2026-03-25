package org.hascoapi.ingestion;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;


public class CSVRecordFile implements RecordFile {

    private File file;
    private int numberOfRows;
    private List<String> headers;

    public CSVRecordFile(File file) {
        this.file = file;
        init();
    }
    
    private void init() {
        System.out.println("CSVRecordFile.init() - Starting to parse file: " + (file != null ? file.getAbsolutePath() : "NULL"));
        try {
            // Use withAllowMissingColumnNames(true) to handle CSV files with empty trailing columns
            CSVParser parser = CSVFormat.DEFAULT.withHeader().withAllowMissingColumnNames(true).parse(new FileReader(file));
            Map<String, Integer> headerMap = parser.getHeaderMap();

            // Find the maximum column index to properly size the headers list
            int maxIndex = 0;
            for (Integer index : headerMap.values()) {
                if (index > maxIndex) {
                    maxIndex = index;
                }
            }
            
            // Pre-fill headers list with empty strings up to maxIndex
            headers = new ArrayList<String>(maxIndex + 1);
            for (int i = 0; i <= maxIndex; i++) {
                headers.add("");
            }
            
            // Now set the actual header names at their correct positions
            for (String key : headerMap.keySet()) {
                int index = headerMap.get(key).intValue();
                headers.set(index, key);
            }
            
            numberOfRows = parser.getRecords().size();
            System.out.println("CSVRecordFile.init() - Successfully parsed CSV: " + numberOfRows + " rows, " + headers.size() + " columns");
        } catch (FileNotFoundException e) {
            System.out.println("[ERROR] CSVRecordFile.init() - FileNotFoundException: " + e.getMessage());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[ERROR] CSVRecordFile.init() - IOException: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("[ERROR] CSVRecordFile.init() - Unexpected exception: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public List<Record> getRecords() {
        List<CSVRecord> records = null;
        try {
            // Use withAllowMissingColumnNames(true) to match init() behavior
            records = CSVFormat.DEFAULT.withHeader().withAllowMissingColumnNames(true).parse(new FileReader(file)).getRecords();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return records.stream().map(rec -> {
            return new CSVFileRecord(rec);
        }).collect(Collectors.toList());
    }
    
    public int getNumberOfSheets() {
        return 1;
    }

    @Override
    public List<String> getHeaders() {
        return headers;
    }
    
    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }
    

    @Override
    public File getFile() {
        return file;
    }
    
    @Override
    public String getStorageFileName() {
        return file.getName();
    }
    
    @Override
    public String getSheetName() {
        return "";
    }

    @Override
    public boolean isValid() {
        return file != null;
    }

    @Override
    public int getNumberOfRows() {
        return numberOfRows;
    }

    public void appendRecord(Record record) throws IOException {
        boolean fileExists = file.exists();
        boolean writeHeaders = !fileExists || numberOfRows == 0;
    
        if (headers == null || headers.isEmpty()) {
            System.err.println("[WARN] Headers are null or empty, cannot write headers or data correctly.");
        } else {
            System.out.println("[DEBUG] Headers: " + headers);
        }
    
        try (FileWriter fw = new FileWriter(file, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
    
            if (writeHeaders && headers != null && !headers.isEmpty()) {
                String headerLine = String.join(",", headers);
                System.out.println("[DEBUG] Writing headers: " + headerLine);
                bw.write(headerLine);
                bw.newLine();
            }
    
            List<String> values = new ArrayList<>();
            for (String header : headers) {
                String value = record.getValueByColumnName(header);
                if (value == null) {
                    System.out.println("[DEBUG] Value for header [" + header + "] is null, substituindo por string vazia.");
                    value = "";
                } else {
                    System.out.println("[DEBUG] Value for header [" + header + "]: " + value);
                }
    
                if (value.contains(",") || value.contains("\"")) {
                    value = "\"" + value.replace("\"", "\"\"") + "\"";
                }
                values.add(value);
            }
            String line = String.join(",", values);
            System.out.println("[DEBUG] Writing line: " + line);
            bw.write(line);
            bw.newLine();
    
            numberOfRows++;
        }
    }

}
