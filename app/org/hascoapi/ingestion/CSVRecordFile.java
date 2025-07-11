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
        try {
            CSVParser parser = CSVFormat.DEFAULT.withHeader().parse(new FileReader(file));
            Map<String, Integer> headerMap = parser.getHeaderMap();

            headers = new ArrayList<String>(headerMap.size());
            for (String key : headerMap.keySet()) {
                headers.add(headerMap.get(key).intValue(), key);
            }
            
            numberOfRows = parser.getRecords().size();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<Record> getRecords() {
        List<CSVRecord> records = null;
        try {
            records = CSVFormat.DEFAULT.withHeader().parse(new FileReader(file)).getRecords();
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
