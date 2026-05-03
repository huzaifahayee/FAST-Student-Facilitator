package com.fast.fsf.books.web;

import com.fast.fsf.shared.model.ActivityLog;
import com.fast.fsf.books.domain.BookListing;
import com.fast.fsf.shared.persistence.ActivityLogRepository;
import com.fast.fsf.books.persistence.BookListingRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/books")
@CrossOrigin(originPatterns = {"http://localhost:*"})
public class BookListingController {

    private final BookListingRepository bookRepository;
    private final ActivityLogRepository activityLogRepository;

    public BookListingController(BookListingRepository bookRepository, ActivityLogRepository activityLogRepository) {
        this.bookRepository = bookRepository;
        this.activityLogRepository = activityLogRepository;
    }

    @GetMapping
    public List<BookListing> getAllBooks(@RequestParam(required = false, defaultValue = "SELL") String type) {
        return bookRepository.findByListingTypeAndApprovedTrueOrderByStatusAsc(type);
    }

    @GetMapping("/my")
    public List<BookListing> getMyBooks(@RequestParam String email) {
        return bookRepository.findByOwnerEmail(email);
    }

    @PostMapping
    public ResponseEntity<BookListing> createBookListing(@RequestBody BookListing book) {
        book.setApproved(false); // Force pending status
        book.setStatus("ACTIVE");
        BookListing saved = bookRepository.save(book);
        
        activityLogRepository.save(new ActivityLog(
            book.getOwnerName() + " posted a new book listing for " + book.getBookTitle(),
            "BOOK_POSTED"
        ));
        
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookListing> updateBookListing(@PathVariable Long id, @RequestBody BookListing updatedBook) {
        return bookRepository.findById(id).map(book -> {
            book.setBookTitle(updatedBook.getBookTitle());
            book.setAuthor(updatedBook.getAuthor());
            book.setCourseCode(updatedBook.getCourseCode());
            book.setBookCondition(updatedBook.getBookCondition());
            book.setPrice(updatedBook.getPrice());
            book.setFrontCoverImage(updatedBook.getFrontCoverImage());
            book.setBackCoverImage(updatedBook.getBackCoverImage());
            book.setListingType(updatedBook.getListingType());
            
            // Re-require approval after edit
            book.setApproved(false);
            
            BookListing saved = bookRepository.save(book);
            activityLogRepository.save(new ActivityLog(
                book.getOwnerName() + " updated their book listing for " + book.getBookTitle(),
                "BOOK_UPDATED"
            ));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/pending")
    public List<BookListing> getPendingBooks() {
        return bookRepository.findByApprovedFalse();
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<BookListing> approveBook(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return bookRepository.findById(id).map(book -> {
            book.setApproved(true);
            book.setModerationReason(reason);
            BookListing saved = bookRepository.save(book);
            activityLogRepository.save(new ActivityLog("BookListing #" + id + " was approved.", "BOOK_APPROVED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/flagged/count")
    public long getFlaggedCount() {
        return bookRepository.countByFlaggedTrue();
    }

    @GetMapping("/flagged")
    public List<BookListing> getFlaggedBooks() {
        return bookRepository.findByFlaggedTrue();
    }

    @PutMapping("/{id}/flag")
    public ResponseEntity<BookListing> flagBook(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return bookRepository.findById(id).map(book -> {
            book.setFlagged(true);
            book.setModerationReason(reason);
            BookListing saved = bookRepository.save(book);
            activityLogRepository.save(new ActivityLog("BookListing #" + id + " flagged. Reason: " + reason, "BOOK_FLAGGED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/resolve")
    public ResponseEntity<BookListing> resolveBook(@PathVariable Long id) {
        return bookRepository.findById(id).map(book -> {
            book.setFlagged(false);
            book.setModerationReason(null);
            BookListing saved = bookRepository.save(book);
            activityLogRepository.save(new ActivityLog("Moderation flag cleared for BookListing #" + id + ".", "FLAG_RESOLVED"));
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/close")
    public ResponseEntity<BookListing> closeBookListing(@PathVariable Long id) {
        return bookRepository.findById(id).map(book -> {
            book.setStatus("CLOSED");
            BookListing saved = bookRepository.save(book);
            return ResponseEntity.ok(saved);
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/count/active")
    public long getActiveCount() {
        return bookRepository.countByApprovedTrue();
    }

    @GetMapping("/search")
    public List<BookListing> searchBooks(@RequestParam String query, @RequestParam(defaultValue = "SELL") String type) {
        return bookRepository.searchApprovedListings(query, type);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookListing> getBook(@PathVariable Long id) {
        Optional<BookListing> book = bookRepository.findById(id).filter(BookListing::isApproved);
        return book.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBook(@PathVariable Long id, @RequestParam(required = false) String reason) {
        return bookRepository.findById(id).map(book -> {
            activityLogRepository.save(new ActivityLog(
                "BookListing #" + id + " deleted. Reason: " + (reason != null ? reason : "Compliance violation"),
                "CONTENT_DELETED"
            ));
            bookRepository.deleteById(id);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
