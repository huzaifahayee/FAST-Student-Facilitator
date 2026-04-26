import org.apache.poi.ss.usermodel.*;
import java.io.File;

public class ExcelInspector {
    public static void main(String[] args) throws Exception {
        Workbook workbook = WorkbookFactory.create(new File("c:\\Users\\Hpi7\\Desktop\\arqam version\\FAST-Student-Facilitator-arqam_fsf-feature-updates\\FSC TT Spring 2026 v1.1.xlsx"));
        Sheet sheet = workbook.getSheetAt(0);
        DataFormatter formatter = new DataFormatter();
        System.out.println("--- EXCEL INSPECTION (FIRST 10 ROWS) ---");
        for (int i = 0; i < 10; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            System.out.print("Row " + i + ": ");
            for (Cell cell : row) {
                System.out.print("[" + formatter.formatCellValue(cell) + "] ");
            }
            System.out.println();
        }
        workbook.close();
    }
}
