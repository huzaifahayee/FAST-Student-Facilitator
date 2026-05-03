package com.fast.fsf.events.template;

import com.fast.fsf.events.domain.CampusEvent;
import com.fast.fsf.events.factory.EventFactory;
import org.apache.poi.ss.usermodel.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public class AcademicCalendarParser extends AbstractPlanParser {

    private final int headerRow;

    public AcademicCalendarParser(EventFactory eventFactory, int headerRow) {
        super(eventFactory);
        this.headerRow = headerRow;
    }

    @Override
    protected void doParse(Sheet sheet, DataFormatter formatter, int defaultYear, String ownerEmail, List<CampusEvent> out) {
        int last = sheet.getLastRowNum();
        boolean inHolidays = false;

        for (int r = headerRow + 1; r <= last; r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String col0 = formatter.formatCellValue(row.getCell(0)).trim();
            String col1 = formatter.formatCellValue(row.getCell(1)).trim();
            String col2 = formatter.formatCellValue(row.getCell(2)).trim();
            String col3 = formatter.formatCellValue(row.getCell(3)).trim();
            String col4 = formatter.formatCellValue(row.getCell(4)).trim();

            if (col0.isEmpty() && col1.isEmpty() && col2.isEmpty() && col3.isEmpty() && col4.isEmpty()) continue;

            if (col0.toLowerCase(Locale.ROOT).startsWith("holiday")) {
                inHolidays = true;
                continue;
            }

            if (!inHolidays) {
                if (!WEEK_NUMBER.matcher(col0).matches()) continue;

                boolean hasQuiz = !col3.isEmpty();
                boolean hasSessional = !col4.isEmpty();
                if (!hasQuiz && !hasSessional) continue;

                LocalDate fromDate = parseShortDate(col1, defaultYear);
                if (fromDate == null) continue;

                String title;
                if (hasQuiz && hasSessional) title = col3 + " / " + col4;
                else title = hasQuiz ? col3 : col4;

                String desc = "Week " + col0 + " (" + col1 + (col2.isEmpty() ? "" : " to " + col2) + ")";
                out.add(eventFactory.createPlanEntry(title, desc, fromDate, "See Dept Notice Board",
                        "Academic Office", "ACADEMIC", ownerEmail));
            } else {
                if (col0.startsWith("*") || col0.toLowerCase(Locale.ROOT).contains("subject to appearance")) continue;
                if (col1.isEmpty()) continue;

                LocalDate holidayDate = parseHolidayDate(col1, defaultYear);
                if (holidayDate == null) continue;
                
                out.add(eventFactory.createPlanEntry(col0.replace("*", "").trim(), col1, holidayDate,
                        "Campus-wide", "Administration", "HOLIDAY", ownerEmail));
            }
        }
    }
}
