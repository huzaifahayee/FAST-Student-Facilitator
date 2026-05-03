package com.fast.fsf.config;

import com.fast.fsf.admin.domain.Admin;
import com.fast.fsf.pastpapers.domain.PastPaper;
import com.fast.fsf.timetable.domain.TimetableEntry;
import com.fast.fsf.admin.persistence.AdminRepository;
import com.fast.fsf.pastpapers.persistence.PastPaperRepository;
import com.fast.fsf.timetable.persistence.TimetableEntryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
public class DatabaseSeeder {

    /**
     * Seed the admins table with the 5 FSF developer/admin accounts.
     * Runs first (Order 1). Only inserts if not already present so
     * re-starts don't duplicate rows.
     */
    @Bean
    @Order(1)
    CommandLineRunner seedAdmins(AdminRepository adminRepo) {
        return args -> {
            System.out.println("DEBUG: Clearing and re-seeding FSF admins table...");
            adminRepo.deleteAll();

            adminRepo.saveAll(Arrays.asList(
                new Admin("l240363@lhr.nu.edu.pk", "Admin L240363"),
                new Admin("l243063@lhr.nu.edu.pk", "Admin L243063"),
                new Admin("l243023@lhr.nu.edu.pk", "Admin L243023"),
                new Admin("l243019@lhr.nu.edu.pk", "Admin L243019"),
                new Admin("l240001@lhr.nu.edu.pk", "Admin L240001"),
                new Admin("l243095@lhr.nu.edu.pk", "Admin L243095")
            ));

            System.out.println("DEBUG: Admins seeded. Count=" + adminRepo.count());
            adminRepo.findAll().forEach(a ->
                System.out.println("DEBUG:   -> '" + a.getEmail() + "'")
            );
        };
    }

    @Bean
    CommandLineRunner seedPastPapers(PastPaperRepository repo,
                                     com.fast.fsf.pastpapers.persistence.PaperRatingRepository ratingRepo,
                                     com.fast.fsf.pastpapers.persistence.PaperCommentRepository commentRepo,
                                     com.fast.fsf.pastpapers.persistence.PaperReportRepository reportRepo) {
        return args -> {
            System.out.println("DEBUG: Wiping Database explicitly for test run...");
            ratingRepo.deleteAll();
            commentRepo.deleteAll();
            reportRepo.deleteAll();
            repo.deleteAll();
            
            System.out.println("DEBUG: Seeding default Past Papers...");

                List<PastPaper> defaultPapers = Arrays.asList(
                    createPaper("Database Systems", "CS-3005", "Spring 2025", "MIDTERM", "Dr. Irfan", "https://drive.google.com/drive/folders/1b8syVaHAJ1jCM70t8LvxRqeaAoGeHyK9", true),
                    createPaper("Applied Physics", "PHY-1001", "Spring 2025", "FINAL", "Dr. Usman", "https://drive.google.com/drive/folders/1Iy6uJGHFmvTd3pMe1jkKuEFUkCOc0IJN", true),
                    createPaper("Calculus", "MTH-1001", "Spring 2025", "MIDTERM", "Dr. Kamran", "https://drive.google.com/drive/folders/1PvyVrVdYE5DaMN1LGM-Zk5UmECXbcPvd", true),
                    createPaper("Discrete Structures", "CS-1005", "Spring 2025", "FINAL", "Dr. Asghar", "https://drive.google.com/drive/folders/1VhK2MaXjLo-O5oGzOM6v5-kDYg94Ry54", true),
                    createPaper("Cloud Computing", "CS-4020", "Spring 2025", "MIDTERM", "Dr. Syed", "https://drive.google.com/drive/folders/1qHoYQsuz-jkgLdozkh1HQb_DcTbPdWBR", true),
                    createPaper("Digital Logic Design", "CS-2001", "Spring 2025", "MIDTERM", "Dr. Naveed", "https://drive.google.com/drive/folders/1SZ2HkZJ02xq9oy5_RdFOeAur7IiSvHaN", true),
                    createPaper("Digital Logic Design Lab", "CS-2001L", "Spring 2025", "QUIZ", "Dr. Naveed", "https://drive.google.com/drive/folders/1MtjPz-sLc0WhQFeQHmsnRUUxwpBdfjAv", true),
                    createPaper("Islamic Studies", "HUM-1001", "Spring 2025", "FINAL", "Dr. Bilal", "https://drive.google.com/drive/folders/1mw8pSWsPhIFM9rRcSQQWF-OfYKvqz8WE", true),
                    createPaper("Linear Algebra", "MTH-1002", "Spring 2025", "MIDTERM", "Dr. Hira", "https://drive.google.com/drive/folders/1SUkRnSiQkyVHohHoIDXOZ6T_gWkFHyrF", true),
                    createPaper("Probability and Statistics", "MTH-2001", "Spring 2025", "FINAL", "Dr. Sadaf", "https://drive.google.com/drive/folders/1knOsNuexBD1a86aFrgHUp4gym6U6ja1V", true),
                    createPaper("Object Oriented Programming", "CS-1004", "Fall 2024", "MIDTERM", "Dr. Ali", "https://drive.google.com/drive/folders/PENDING_TEST", false)
                );

                repo.saveAll(defaultPapers);
                System.out.println("DEBUG: Successfully seeded 11 past papers (10 approved, 1 pending).");
        };
    }

    private PastPaper createPaper(String courseName, String code, String semester, String examType, String instructor, String link, boolean isApproved) {
        PastPaper paper = new PastPaper();
        paper.setCourseName(courseName);
        paper.setCourseCode(code);
        paper.setSemesterYear(semester);
        paper.setExamType(examType);
        paper.setInstructorName(instructor);
        paper.setGoogleDriveLink(link);
        paper.setOwnerEmail("admin@nu.edu.pk");
        paper.setOwnerName("FSF Admin");
        paper.setUploadedAt(LocalDateTime.now());
        paper.setApproved(isApproved);
        paper.setFlagged(false);
        paper.setAverageRating(0.0);
        paper.setRatingCount(0);
        return paper;
    }
    @Bean
    CommandLineRunner seedTimetable(TimetableEntryRepository repo) {
        return args -> {
            System.out.println("DEBUG: Seeding default Timetable entries...");
            repo.deleteAll();

            repo.saveAll(Arrays.asList(
                createEntry("SE", "24", "B", "Monday", "08:30", "09:50", "Software Engineering", "E-01", "Dr. Huzaifa"),
                createEntry("SE", "24", "B", "Monday", "10:00", "11:20", "Database Systems", "E-02", "Dr. Arqam"),
                createEntry("SE", "24", "B", "Tuesday", "11:30", "12:50", "Data Structures", "B-05", "Dr. Bilal"),
                createEntry("SE", "24", "B", "Wednesday", "08:30", "09:50", "Operating Systems", "C-01", "Dr. Zain"),
                createEntry("SE", "24", "B", "Thursday", "01:00", "02:20", "Web Dev", "Lab 2", "Dr. Usman")
            ));

            System.out.println("DEBUG: Successfully seeded 5 timetable entries.");
        };
    }

    private TimetableEntry createEntry(String dept, String batch, String section, String day, String start, String end, String course, String room, String instructor) {
        TimetableEntry entry = new TimetableEntry();
        entry.setDepartment(dept);
        entry.setBatch(batch);
        entry.setSection(section);
        entry.setDayOfWeek(day);
        entry.setStartTime(start);
        entry.setEndTime(end);
        entry.setCourseName(course);
        entry.setRoomNumber(room);
        entry.setInstructorName(instructor);
        entry.setApproved(true);
        entry.setOwnerName("System Seeder");
        entry.setOwnerEmail("admin@nu.edu.pk");
        return entry;
    }
}
