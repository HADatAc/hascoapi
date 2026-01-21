import org.hascoapi.ingestion.SpreadsheetRecordFile;

import java.io.File;

/**
 * Tiny smoke test helper to validate that SpreadsheetRecordFile doesn't create
 * hundreds of empty records for formatted-but-empty sheets.
 *
 * Usage:
 *   sbt "runMain ReadSpreadsheetSmoke /path/to/file.xlsx FILESTREAM"
 */
public class ReadSpreadsheetSmoke {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: ReadSpreadsheetSmoke <xlsx-path> <sheetName>");
            System.exit(2);
        }
        File f = new File(args[0]);
        String sheet = args[1];
        SpreadsheetRecordFile rf = new SpreadsheetRecordFile(f, sheet);
        System.out.println("sheet=" + sheet + " headers=" + rf.getHeaders().size() + " records=" + (rf.getRecords() == null ? -1 : rf.getRecords().size()) + " numberOfRows=" + rf.getNumberOfRows());
        if (rf.getRecords() != null && !rf.getRecords().isEmpty()) {
            System.out.println("firstRowSize=" + rf.getRecords().get(0).size());
        }
    }
}

