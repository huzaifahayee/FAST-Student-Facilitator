package com.fast.fsf.events.template;

import com.fast.fsf.events.domain.CampusEvent;
import com.fast.fsf.events.factory.EventFactory;
import org.apache.poi.ss.usermodel.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class AbstractPlanParser {

    protected final EventFactory eventFactory;

    protected static final Pattern HOLIDAY_DATE = Pattern.compile(
            "(\\d{1,2})\\s*(?:[-\u2013]\\s*\\d{0,2})?\\s*([A-Za-z]+)\\s*[,\\s]\\s*(\\d{4})");
    protected static final Pattern HOLIDAY_DATE_MONTH_FIRST = Pattern.compile(
            "([A-Za-z]+)\\s*(\\d{1,2})\\s*(?:[-\u2013]\\s*\\d{0,2})?\\s*[,\\s]\\s*(\\d{4})");
    protected static final Pattern WEEK_NUMBER = Pattern.compile("^\\s*\\d+[A-Za-z]?(?:\\s*-\\s*\\d+[A-Za-z]?)?\\s*$");

    protected AbstractPlanParser(EventFactory eventFactory) {
        this.eventFactory = eventFactory;
    }

    public final void parse(Sheet sheet, DataFormatter formatter, int defaultYear, String ownerEmail, List<CampusEvent> out) {
        doParse(sheet, formatter, defaultYear, ownerEmail, out);
    }

    protected abstract void doParse(Sheet sheet, DataFormatter formatter, int defaultYear, String ownerEmail, List<CampusEvent> out);

    protected LocalDate parseShortDate(String raw, int defaultYear) {
        if (raw == null) return null;
        String cleaned = raw.replaceAll("\\(.*?\\)", "").trim();
        if (cleaned.isEmpty()) return null;

        if (cleaned.matches(".*\\d{4}\\s*$")) {
            for (String pat : new String[] {"d-MMM-yyyy", "dd-MMM-yyyy", "d MMM yyyy", "dd MMM yyyy"}) {
                try { return LocalDate.parse(cleaned, DateTimeFormatter.ofPattern(pat, Locale.ENGLISH)); }
                catch (Exception ignored) {}
            }
        }
        for (String pat : new String[] {"d-MMM", "dd-MMM", "d MMM", "dd MMM"}) {
            try {
                return LocalDate.parse(cleaned + "-" + defaultYear,
                        DateTimeFormatter.ofPattern(pat + "-yyyy", Locale.ENGLISH));
            } catch (Exception ignored) {
                try {
                    return LocalDate.parse(cleaned + " " + defaultYear,
                            DateTimeFormatter.ofPattern(pat + " yyyy", Locale.ENGLISH));
                } catch (Exception ignored2) {}
            }
        }
        return null;
    }

    protected LocalDate parseHolidayDate(String raw, int defaultYear) {
        if (raw == null) return null;
        String cleaned = raw.replace('\u2013', '-').replaceAll("\\s+", " ").trim();

        Matcher m = HOLIDAY_DATE.matcher(cleaned);
        if (m.find()) {
            try {
                int day = Integer.parseInt(m.group(1));
                String month = m.group(2);
                int year = Integer.parseInt(m.group(3));
                return LocalDate.parse(day + "-" + month + "-" + year,
                        DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH));
            } catch (Exception e) {
                try {
                    int day = Integer.parseInt(m.group(1));
                    String month = m.group(2);
                    int year = Integer.parseInt(m.group(3));
                    return LocalDate.parse(day + "-" + month + "-" + year,
                            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH));
                } catch (Exception ignored) {}
            }
        }

        Matcher m2 = HOLIDAY_DATE_MONTH_FIRST.matcher(cleaned);
        if (m2.find()) {
            try {
                String month = m2.group(1);
                int day = Integer.parseInt(m2.group(2));
                int year = Integer.parseInt(m2.group(3));
                return LocalDate.parse(day + "-" + month + "-" + year,
                        DateTimeFormatter.ofPattern("d-MMMM-yyyy", Locale.ENGLISH));
            } catch (Exception e) {
                try {
                    String month = m2.group(1);
                    int day = Integer.parseInt(m2.group(2));
                    int year = Integer.parseInt(m2.group(3));
                    return LocalDate.parse(day + "-" + month + "-" + year,
                            DateTimeFormatter.ofPattern("d-MMM-yyyy", Locale.ENGLISH));
                } catch (Exception ignored) {}
            }
        }
        return null;
    }
}
