"""One-off relocate: layered packages -> feature folders.

WARNING: Do not run again — source paths under model/repository/controller/service were deleted on first run.
Re-running would fail unless you restore from git. Run from repo root:
    python backend/scripts/relocate_features.py
"""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1] / "src/main/java/com/fast/fsf"

# (source_relative_to_fs_root, dest_relative_to_fs_root, new_package)
MOVES: list[tuple[str, str, str]] = [
    ("model/ActivityLog.java", "shared/model/ActivityLog.java", "com.fast.fsf.shared.model"),
    ("repository/ActivityLogRepository.java", "shared/persistence/ActivityLogRepository.java", "com.fast.fsf.shared.persistence"),
    ("model/Admin.java", "admin/domain/Admin.java", "com.fast.fsf.admin.domain"),
    ("repository/AdminRepository.java", "admin/persistence/AdminRepository.java", "com.fast.fsf.admin.persistence"),
    ("controller/AdminController.java", "admin/web/AdminController.java", "com.fast.fsf.admin.web"),
    ("model/User.java", "identity/domain/User.java", "com.fast.fsf.identity.domain"),
    ("model/Role.java", "identity/domain/Role.java", "com.fast.fsf.identity.domain"),
    ("repository/UserRepository.java", "identity/persistence/UserRepository.java", "com.fast.fsf.identity.persistence"),
    ("controller/UserController.java", "identity/web/UserController.java", "com.fast.fsf.identity.web"),
    ("controller/AuthController.java", "auth/web/AuthController.java", "com.fast.fsf.auth.web"),
    ("model/Ride.java", "carpool/domain/Ride.java", "com.fast.fsf.carpool.domain"),
    ("repository/RideRepository.java", "carpool/persistence/RideRepository.java", "com.fast.fsf.carpool.persistence"),
    ("controller/RideController.java", "carpool/web/RideController.java", "com.fast.fsf.carpool.web"),
    ("model/PastPaper.java", "pastpapers/domain/PastPaper.java", "com.fast.fsf.pastpapers.domain"),
    ("model/PaperComment.java", "pastpapers/domain/PaperComment.java", "com.fast.fsf.pastpapers.domain"),
    ("model/PaperRating.java", "pastpapers/domain/PaperRating.java", "com.fast.fsf.pastpapers.domain"),
    ("model/PaperReport.java", "pastpapers/domain/PaperReport.java", "com.fast.fsf.pastpapers.domain"),
    ("repository/PastPaperRepository.java", "pastpapers/persistence/PastPaperRepository.java", "com.fast.fsf.pastpapers.persistence"),
    ("repository/PaperCommentRepository.java", "pastpapers/persistence/PaperCommentRepository.java", "com.fast.fsf.pastpapers.persistence"),
    ("repository/PaperRatingRepository.java", "pastpapers/persistence/PaperRatingRepository.java", "com.fast.fsf.pastpapers.persistence"),
    ("repository/PaperReportRepository.java", "pastpapers/persistence/PaperReportRepository.java", "com.fast.fsf.pastpapers.persistence"),
    ("controller/PastPaperController.java", "pastpapers/web/PastPaperController.java", "com.fast.fsf.pastpapers.web"),
    ("model/LostFoundListing.java", "lostfound/domain/LostFoundListing.java", "com.fast.fsf.lostfound.domain"),
    ("repository/LostFoundRepository.java", "lostfound/persistence/LostFoundRepository.java", "com.fast.fsf.lostfound.persistence"),
    ("service/LostFoundService.java", "lostfound/service/LostFoundService.java", "com.fast.fsf.lostfound.service"),
    ("controller/LostFoundController.java", "lostfound/web/LostFoundController.java", "com.fast.fsf.lostfound.web"),
    ("model/CampusLocation.java", "campusmap/domain/CampusLocation.java", "com.fast.fsf.campusmap.domain"),
    ("model/CampusMapRoute.java", "campusmap/domain/CampusMapRoute.java", "com.fast.fsf.campusmap.domain"),
    ("model/LocationSuggestion.java", "campusmap/domain/LocationSuggestion.java", "com.fast.fsf.campusmap.domain"),
    ("repository/CampusLocationRepository.java", "campusmap/persistence/CampusLocationRepository.java", "com.fast.fsf.campusmap.persistence"),
    ("repository/CampusMapRouteRepository.java", "campusmap/persistence/CampusMapRouteRepository.java", "com.fast.fsf.campusmap.persistence"),
    ("repository/LocationSuggestionRepository.java", "campusmap/persistence/LocationSuggestionRepository.java", "com.fast.fsf.campusmap.persistence"),
    ("controller/CampusMapController.java", "campusmap/web/CampusMapController.java", "com.fast.fsf.campusmap.web"),
    ("config/CampusMapSeeder.java", "campusmap/config/CampusMapSeeder.java", "com.fast.fsf.campusmap.config"),
    ("model/TimetableEntry.java", "timetable/domain/TimetableEntry.java", "com.fast.fsf.timetable.domain"),
    ("repository/TimetableEntryRepository.java", "timetable/persistence/TimetableEntryRepository.java", "com.fast.fsf.timetable.persistence"),
    ("controller/TimetableEntryController.java", "timetable/web/TimetableEntryController.java", "com.fast.fsf.timetable.web"),
    ("model/FastNote.java", "notes/domain/FastNote.java", "com.fast.fsf.notes.domain"),
    ("model/NoteVote.java", "notes/domain/NoteVote.java", "com.fast.fsf.notes.domain"),
    ("repository/FastNoteRepository.java", "notes/persistence/FastNoteRepository.java", "com.fast.fsf.notes.persistence"),
    ("repository/NoteVoteRepository.java", "notes/persistence/NoteVoteRepository.java", "com.fast.fsf.notes.persistence"),
    ("service/FastNoteService.java", "notes/service/FastNoteService.java", "com.fast.fsf.notes.service"),
    ("controller/FastNoteController.java", "notes/web/FastNoteController.java", "com.fast.fsf.notes.web"),
    ("model/BookListing.java", "books/domain/BookListing.java", "com.fast.fsf.books.domain"),
    ("repository/BookListingRepository.java", "books/persistence/BookListingRepository.java", "com.fast.fsf.books.persistence"),
    ("controller/BookListingController.java", "books/web/BookListingController.java", "com.fast.fsf.books.web"),
    ("model/CampusEvent.java", "events/domain/CampusEvent.java", "com.fast.fsf.events.domain"),
    ("repository/CampusEventRepository.java", "events/persistence/CampusEventRepository.java", "com.fast.fsf.events.persistence"),
    ("controller/CampusEventController.java", "events/web/CampusEventController.java", "com.fast.fsf.events.web"),
    ("model/Reminder.java", "reminders/domain/Reminder.java", "com.fast.fsf.reminders.domain"),
    ("repository/ReminderRepository.java", "reminders/persistence/ReminderRepository.java", "com.fast.fsf.reminders.persistence"),
    ("controller/ReminderController.java", "reminders/web/ReminderController.java", "com.fast.fsf.reminders.web"),
]


def replace_package(content: str, new_pkg: str) -> str:
    return re.sub(r"^package\s+[\w.]+;", f"package {new_pkg};", content, count=1, flags=re.MULTILINE)


def main() -> None:
    src_root = ROOT
    for old_rel, new_rel, pkg in MOVES:
        src = src_root / old_rel
        dst = src_root / new_rel
        if not src.exists():
            raise SystemExit(f"Missing source: {src}")
        dst.parent.mkdir(parents=True, exist_ok=True)
        text = src.read_text(encoding="utf-8")
        text = replace_package(text, pkg)
        dst.write_text(text, encoding="utf-8")
        src.unlink()

    # Rewrite imports + FQCN references in ALL remaining java files under com/fast/fsf
    subs_ordered = [
        ("com.fast.fsf.model.ActivityLog", "com.fast.fsf.shared.model.ActivityLog"),
        ("com.fast.fsf.repository.ActivityLogRepository", "com.fast.fsf.shared.persistence.ActivityLogRepository"),
        ("com.fast.fsf.model.Admin", "com.fast.fsf.admin.domain.Admin"),
        ("com.fast.fsf.repository.AdminRepository", "com.fast.fsf.admin.persistence.AdminRepository"),
        ("com.fast.fsf.controller.AdminController", "com.fast.fsf.admin.web.AdminController"),
        ("com.fast.fsf.model.User", "com.fast.fsf.identity.domain.User"),
        ("com.fast.fsf.model.Role", "com.fast.fsf.identity.domain.Role"),
        ("com.fast.fsf.repository.UserRepository", "com.fast.fsf.identity.persistence.UserRepository"),
        ("com.fast.fsf.controller.UserController", "com.fast.fsf.identity.web.UserController"),
        ("com.fast.fsf.controller.AuthController", "com.fast.fsf.auth.web.AuthController"),
        ("com.fast.fsf.model.Ride", "com.fast.fsf.carpool.domain.Ride"),
        ("com.fast.fsf.repository.RideRepository", "com.fast.fsf.carpool.persistence.RideRepository"),
        ("com.fast.fsf.controller.RideController", "com.fast.fsf.carpool.web.RideController"),
        ("com.fast.fsf.model.PastPaper", "com.fast.fsf.pastpapers.domain.PastPaper"),
        ("com.fast.fsf.model.PaperComment", "com.fast.fsf.pastpapers.domain.PaperComment"),
        ("com.fast.fsf.model.PaperRating", "com.fast.fsf.pastpapers.domain.PaperRating"),
        ("com.fast.fsf.model.PaperReport", "com.fast.fsf.pastpapers.domain.PaperReport"),
        ("com.fast.fsf.repository.PastPaperRepository", "com.fast.fsf.pastpapers.persistence.PastPaperRepository"),
        ("com.fast.fsf.repository.PaperCommentRepository", "com.fast.fsf.pastpapers.persistence.PaperCommentRepository"),
        ("com.fast.fsf.repository.PaperRatingRepository", "com.fast.fsf.pastpapers.persistence.PaperRatingRepository"),
        ("com.fast.fsf.repository.PaperReportRepository", "com.fast.fsf.pastpapers.persistence.PaperReportRepository"),
        ("com.fast.fsf.controller.PastPaperController", "com.fast.fsf.pastpapers.web.PastPaperController"),
        ("com.fast.fsf.model.LostFoundListing", "com.fast.fsf.lostfound.domain.LostFoundListing"),
        ("com.fast.fsf.repository.LostFoundRepository", "com.fast.fsf.lostfound.persistence.LostFoundRepository"),
        ("com.fast.fsf.service.LostFoundService", "com.fast.fsf.lostfound.service.LostFoundService"),
        ("com.fast.fsf.controller.LostFoundController", "com.fast.fsf.lostfound.web.LostFoundController"),
        ("com.fast.fsf.model.CampusLocation", "com.fast.fsf.campusmap.domain.CampusLocation"),
        ("com.fast.fsf.model.CampusMapRoute", "com.fast.fsf.campusmap.domain.CampusMapRoute"),
        ("com.fast.fsf.model.LocationSuggestion", "com.fast.fsf.campusmap.domain.LocationSuggestion"),
        ("com.fast.fsf.repository.CampusLocationRepository", "com.fast.fsf.campusmap.persistence.CampusLocationRepository"),
        ("com.fast.fsf.repository.CampusMapRouteRepository", "com.fast.fsf.campusmap.persistence.CampusMapRouteRepository"),
        ("com.fast.fsf.repository.LocationSuggestionRepository", "com.fast.fsf.campusmap.persistence.LocationSuggestionRepository"),
        ("com.fast.fsf.controller.CampusMapController", "com.fast.fsf.campusmap.web.CampusMapController"),
        ("com.fast.fsf.config.CampusMapSeeder", "com.fast.fsf.campusmap.config.CampusMapSeeder"),
        ("com.fast.fsf.model.TimetableEntry", "com.fast.fsf.timetable.domain.TimetableEntry"),
        ("com.fast.fsf.repository.TimetableEntryRepository", "com.fast.fsf.timetable.persistence.TimetableEntryRepository"),
        ("com.fast.fsf.controller.TimetableEntryController", "com.fast.fsf.timetable.web.TimetableEntryController"),
        ("com.fast.fsf.model.FastNote", "com.fast.fsf.notes.domain.FastNote"),
        ("com.fast.fsf.model.NoteVote", "com.fast.fsf.notes.domain.NoteVote"),
        ("com.fast.fsf.repository.FastNoteRepository", "com.fast.fsf.notes.persistence.FastNoteRepository"),
        ("com.fast.fsf.repository.NoteVoteRepository", "com.fast.fsf.notes.persistence.NoteVoteRepository"),
        ("com.fast.fsf.service.FastNoteService", "com.fast.fsf.notes.service.FastNoteService"),
        ("com.fast.fsf.controller.FastNoteController", "com.fast.fsf.notes.web.FastNoteController"),
        ("com.fast.fsf.model.BookListing", "com.fast.fsf.books.domain.BookListing"),
        ("com.fast.fsf.repository.BookListingRepository", "com.fast.fsf.books.persistence.BookListingRepository"),
        ("com.fast.fsf.controller.BookListingController", "com.fast.fsf.books.web.BookListingController"),
        ("com.fast.fsf.model.CampusEvent", "com.fast.fsf.events.domain.CampusEvent"),
        ("com.fast.fsf.repository.CampusEventRepository", "com.fast.fsf.events.persistence.CampusEventRepository"),
        ("com.fast.fsf.controller.CampusEventController", "com.fast.fsf.events.web.CampusEventController"),
        ("com.fast.fsf.model.Reminder", "com.fast.fsf.reminders.domain.Reminder"),
        ("com.fast.fsf.repository.ReminderRepository", "com.fast.fsf.reminders.persistence.ReminderRepository"),
        ("com.fast.fsf.controller.ReminderController", "com.fast.fsf.reminders.web.ReminderController"),
        # legacy generic imports inside DatabaseSeeder etc.
        ("import com.fast.fsf.repository.", "import com.fast.fsf."),  # WRONG - don't do blanket
    ]

    # Remove broken last tuple — apply only explicit mappings (longest first to avoid partial replaces)
    subs_ordered = subs_ordered[:-1]
    subs_ordered.sort(key=lambda x: len(x[0]), reverse=True)

    for path in src_root.rglob("*.java"):
        raw = path.read_text(encoding="utf-8")
        new = raw
        for old, rep in subs_ordered:
            new = new.replace(old, rep)
        # Fix DatabaseSeeder fully-qualified repos if any remain
        new = new.replace("com.fast.fsf.repository.PaperRatingRepository", "com.fast.fsf.pastpapers.persistence.PaperRatingRepository")
        new = new.replace("com.fast.fsf.repository.PaperCommentRepository", "com.fast.fsf.pastpapers.persistence.PaperCommentRepository")
        new = new.replace("com.fast.fsf.repository.PaperReportRepository", "com.fast.fsf.pastpapers.persistence.PaperReportRepository")
        if new != raw:
            path.write_text(new, encoding="utf-8")


if __name__ == "__main__":
    main()
