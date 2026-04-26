package com.fast.fsf.util;

import org.apache.poi.ss.usermodel.*;
import java.io.File;
import java.io.FileInputStream;

public class ExcelDiscovery {
    public static void main(String[] args) {
        try {
            File file = new File("c:/Users/billa/OneDrive/Desktop/FSF/FAST-Student-Facilitator/Schedule for ExamTests Spring 2026 (1).xls");
            FileInputStream fis = new FileInputStream(file);
            Workbook workbook = WorkbookFactory.create(fis);
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            System.out.println("--- EXCEL DISCOVERY START ---");
            for (int r = 0; r < Math.min(20, sheet.getLastRowNum()); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;
                System.out.print("Row " + r + ": ");
                for (int c = 0; c < 15; c++) {
                    String val = formatter.formatCellValue(row.getCell(c));
                    System.out.print("[" + val + "] ");
                }
                System.out.println();
            }
            System.out.println("--- EXCEL DISCOVERY END ---");
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
