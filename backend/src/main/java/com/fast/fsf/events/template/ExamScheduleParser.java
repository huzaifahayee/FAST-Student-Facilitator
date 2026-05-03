package com.fast.fsf.events.template;

import com.fast.fsf.events.domain.CampusEvent;
import com.fast.fsf.events.factory.EventFactory;
import org.apache.poi.ss.usermodel.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

public class ExamScheduleParser extends AbstractPlanParser {

    public ExamScheduleParser(EventFactory eventFactory) {
        super(eventFactory);
    }

    @Override
    protected void doParse(Sheet sheet, DataFormatter formatter, int defaultYear, String ownerEmail, List<CampusEvent> out) {
        for (int r = 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            String dateStr = formatter.formatCellValue(row.getCell(0)).trim();
            String courseStr = formatter.formatCellValue(row.getCell(2)).trim();
            String timeStr = formatter.formatCellValue(row.getCell(3)).trim();
            String venue = formatter.formatCellValue(row.getCell(4)).trim();
            if (dateStr.isEmpty() || courseStr.isEmpty()) continue;

            LocalDate eventDate = null;
            Cell dateCell = row.getCell(0);
            if (dateCell != null && dateCell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(dateCell)) {
                eventDate = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            } else {
                eventDate = parseShortDate(dateStr, defaultYear);
            }
            if (eventDate == null) continue;

            out.add(eventFactory.createPlanEntry(courseStr, "Exam/Test - " + timeStr, eventDate,
                    venue.isEmpty() ? "See Dept Notice Board" : venue,
                    "Academic Office", "ACADEMIC", ownerEmail));
        }
    }
}
