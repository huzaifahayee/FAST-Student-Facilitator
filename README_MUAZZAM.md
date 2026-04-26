# Lost & Found Registry + FAST-Notes — Implementation Documentation

## Features

- Lost & Found Registry (UC-10 to UC-15)
- FAST-Notes (UC-45 to UC-49)


## Overview

This document outlines the complete implementation details for the **Lost & Found Registry** and **FAST-Notes** features of the FSF platform.

---

## 🏗 Architecture & Files Created

### A. LOST AND FOUND REGISTRY (UC-10 – UC-15)

#### 1. Database Schema (Backend Entity)

**`LostFoundListing.java`** — The core entity for this feature.
- Stores `itemName`, `category`, `description`, `location`, `date`, `type` (`Lost` or `Found`), `status` (`Active` or `Resolved`), and `studentEmail`.
- The `date` field is user-entered, representing when the item was actually lost or found — not the submission time. Backend defaults to `LocalDate.now()` only if the field is omitted.
- `status` transitions are one-way: `Active → Resolved`. Resolved listings are never deleted unless explicitly removed by an Admin; they are archived in the UI.
- The `studentEmail` field is populated by the authenticated session on the frontend; it enables ownership-gated resolution and prevents unauthorized status changes.

#### 2. Data Layer (Repository)

**`LostFoundRepository.java`** — Custom JPA queries for search and filtering.
- Provides case-insensitive `LIKE` wildcard matching on `itemName` against a `keyword` parameter.
- Supports compound queries: keyword + category + type in a single JPQL statement, avoiding N+1 query issues.
- All queries filter by `type` (`Lost` or `Found`) at the DB level rather than in Java, keeping response payloads minimal.

#### 3. API Endpoints (`LostFoundController.java`)

| Method | Endpoint | Behaviour |
|---|---|---|
| `GET` | `/api/lost-found` | Returns all listings for a `type`. Accepts optional `keyword` and `category` params. Returns both Active and Resolved items; the frontend partitions them. |
| `POST` | `/api/lost-found` | Creates a new listing. The service sets `status = Active` and respects the user-provided `date`. |
| `PUT` | `/api/lost-found/{id}/resolve` | Marks a listing as `Resolved`. Enforces ownership: only the original poster or an Admin (`studentEmail` containing `"admin"`) may call this. Returns `403`-equivalent `RuntimeException` otherwise. |
| `DELETE` | `/api/lost-found/{id}` | Hard deletes a listing. Admin-only operation exposed through the Admin Panel. |

#### 4. Frontend UI

**`LostAndFound.jsx`** — The main interface view.
- Tabbed interface (`Lost` / `Found`) drives a live re-fetch via `useEffect` dependency on `activeTab`.
- The search input and category dropdown are wired as controlled components; both trigger immediate re-fetches without page reload.
- The **Report Modal** collects all five mandatory SRS fields: Item Name, Category, Description, Location, and Date (date picker input). All fields are `required`.
- The **Resolved Archive** section appears below active listings without a toggle. Its heading dynamically reads `"Resolved Lost Listings"` or `"Resolved Found Listings"` based on the active tab.
- `Mark as Resolved` and `Delete` buttons are rendered **conditionally**: the former appears only for the listing owner and Admins; the latter only for Admins.

**`LostAndFound.css`** — Feature-scoped styles using the project-wide glass-card design system. All class names are prefixed to avoid collisions with other feature stylesheets.

---

### B. FAST-NOTES (UC-45 – UC-49)

#### 1. Database Schema (Backend Entities)

**`FastNote.java`** — The core entity for uploaded notes.
- Stores `title`, `subjectName`, `courseCode`, `fileUrl`, `studentEmail`, `uploadDate`, `upvotes`, `downvotes`, and `status` (`Active` or `Removed`).
- `fileUrl` stores only the **filename on disk** (format: `UUID_sanitizedTitle.pdf`), not an absolute path. This keeps the DB portable across machines.
- `status = "Removed"` implements **logical deletion** as required by UC-49: the record is retained in the database for audit but excluded from all student-facing queries.
- `getVoteScore()` is a `@Transient` computed field (`upvotes - downvotes`) used for in-memory sorting after DB fetch.
- `userVoteType` (`"UPVOTE"`, `"DOWNVOTE"`, or `null`) is a `@Transient` field populated per-request by the service to tell the frontend which vote button the current student has already pressed.

**`NoteVote.java`** — Enforces the one-vote-per-student rule at the database level.
- Composite lookup: `noteId + studentEmail` uniquely identifies a vote record.
- `voteType` holds `"UPVOTE"` or `"DOWNVOTE"`.
- When a student re-votes, this record is either **updated** (vote switch) or **deleted** (vote toggle-off). The parent `FastNote`'s `upvotes` / `downvotes` counters are adjusted atomically in the same service call.

#### 2. Data Layer (Repositories)

**`FastNoteRepository.java`** — Houses all custom JPQL queries.
- `findAllActiveOrdered()`: Fetches all `status = 'Active'` notes sorted by `(upvotes - downvotes) DESC, uploadDate DESC` — matching UC-46's sort specification exactly.
- `searchByKeywordOrdered()`: Case-insensitive `LIKE` wildcard on both `subjectName` and `courseCode`.
- `filterBySubjectOrdered()`: Exact match on `subjectName`.
- `searchAndFilterOrdered()`: Combined keyword + subject filter in a single query.
- `findByFileUrl()`: Used exclusively by the download endpoint to look up a note's metadata by its stored filename for dynamic download naming.

**`NoteVoteRepository.java`** — Single custom query: `findByNoteIdAndStudentEmail()`. Used to check for an existing vote before every vote operation.

#### 3. API Endpoints (`FastNoteController.java`)

| Method | Endpoint | Behaviour |
|---|---|---|
| `GET` | `/api/notes` | Browse/search notes. Accepts `keyword`, `subject`, and `studentEmail`. The `studentEmail` param triggers population of `userVoteType` on each returned note. |
| `POST` | `/api/notes` | Accepts `multipart/form-data`. Validates that the uploaded file is `.pdf` only — returns `400 BAD_REQUEST` otherwise. Generates `UUID_sanitizedTitle.pdf` and writes to `uploads/notes/`. |
| `GET` | `/api/notes/download/{fileName}` | Serves the stored PDF file. Looks up the `FastNote` record by `fileName`, then constructs a descriptive download name: `CourseCode_SubjectName_Title.pdf`. Sent to the browser via `Content-Disposition: attachment` with UTF-8 encoding. Falls back to the raw stored filename if the DB record is not found. |
| `PUT` | `/api/notes/{id}/vote` | Processes a vote. Accepts `studentEmail` and `type` (`UPVOTE` or `DOWNVOTE`). Handles three cases: new vote, toggle-off (same type), and vote switch (different type). |
| `DELETE` | `/api/notes/{id}` | Soft-deletes by setting `status = "Removed"`. The record is preserved in the database. Admin-only. |

#### 4. File Upload and Storage

- **Storage path**: `backend/uploads/notes/` (auto-created on first upload; excluded from Git via `.gitignore`).
- **File naming on disk**: `{UUID}_{SanitizedTitle}.pdf` — UUID prevents filename collisions; sanitized title keeps files human-readable.
- **Download naming in browser**: `CourseCode_SubjectName_Title.pdf` — reconstructed from the DB record at download time, ensuring the user always receives a descriptive filename regardless of how the file was stored internally.

#### 5. Frontend UI

**`FastNotes.jsx`** — The main interface view.
- The **controls bar** contains a text search input, a **Subject** dropdown, and a **Course Code** dropdown. All three are reactive: any change triggers a re-fetch. Subject and Course Code dropdowns are **mutually exclusive** — selecting one resets the other.
- Dropdown options for Subject and Course Code are populated **dynamically** from the live note list on initial load (no hardcoded lists).
- Note cards display: title, subject, course code, uploader email, upload date, and the net vote score. The **▲ / ▼ vote buttons** reflect the current student's vote state (highlighted if already voted).
- File type validation on the frontend restricts the file input to `.pdf` only. If a non-PDF is somehow submitted, the backend returns a `400` and the frontend displays a styled **CRITICAL ERROR** alert modal — not a generic browser alert.
- Admin users see a **Delete** button on each note card. Deleted notes disappear from the list immediately after the server confirms removal.

**`FastNotes.css`** — Feature-scoped styles consistent with the project-wide glass-card aesthetic.

---

## 🔗 How They Integrate with the Rest of FSF

The changes to shared infrastructure were **append-only** — no existing feature logic was modified.

**`SecurityConfig.java`**
Two `requestMatchers` lines were appended to the permit-all block:
```java
.requestMatchers("/api/lost-found/**").permitAll()
.requestMatchers("/api/notes/**").permitAll()
.requestMatchers("/uploads/**").permitAll()
```

**`App.jsx` Routes**
The two `<ServiceSkeleton />` placeholders in the routing table were replaced with the real components:
```jsx
<Route path="/lost-found" element={<LostAndFound user={user} />} />
<Route path="/notes"      element={<FastNotes user={user} />} />
```

**`AdminPanel.jsx`**
The Admin Panel's moderation tab was extended via a `Promise.all` bridge that fetches flagged/removed notes from `/api/notes` alongside existing ride data. Notes appear in the unified moderation list tagged with an `entityType: 'note'` marker, allowing the shared approval pipeline to route delete actions to the correct endpoint without refactoring the Admin Controller.

---

## Testing Integrity

Both modules were validated against their full UC specification:

- **UC-10/11**: Form submission correctly persists all five mandatory fields including user-entered date.
- **UC-12/13**: All filter combinations (keyword only, category only, combined) return correctly sorted results without duplicate records.
- **UC-14**: Resolution ownership check blocks unauthorized students; Admins bypass the check.
- **UC-15**: Admin hard-delete removes the record from all subsequent GET responses.
- **UC-45**: Non-PDF upload attempts return `400 BAD_REQUEST`; the frontend displays a CRITICAL ERROR modal.
- **UC-46**: Notes are consistently sorted by net vote score descending, with upload date as the tie-breaker.
- **UC-48**: All three vote operations (new, toggle-off, switch) produce correct counter values with no double-counting edge cases.
- **UC-49**: Soft-deleted notes are excluded from all `findAllActiveOrdered` and search queries but remain in the database.

Do not modify the model field boundaries, voting counter logic, or API structural contracts without re-validating against the above test matrix.
