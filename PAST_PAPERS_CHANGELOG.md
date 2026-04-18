# Past Papers Feature - Implementation Documentation

**Author**: Fast Student Facilitator (FSF) Team
**Feature**: UC-16 through UC-20 (Past Papers)

## Overview
This document outlines the complete implementation details for the newly added "Past Papers" feature. 
**Team Notice:** All changes here rigidly follow the Open-Closed Principle. Existing features (like Carpool, Global Search, User Management) were **NOT** refactored or modified to force this feature to work. Do not alter the backend entity structure without testing against the UC-16 to UC-20 specifications.

---

## 🏗 Architecture & Files Created

### 1. Database Schema (Backend Entities)
* **`PastPaper.java`**: The core entity. Includes hard bounds on `examType` (MIDTERM, FINAL, QUIZ) and restricts Google Drive links to `https://`. Tracks `approved` and `flagged` stats.
* **`PaperRating.java`**: Limits ratings strictly between `1` and `5`.
* **`PaperComment.java`**: Captures string comments appended to a specific paper ID.
* **`PaperReport.java`**: Stores moderation flags against a paper ID (automatically transitions the target paper to `flagged = true`).

### 2. Data Layer (Repositories)
* **`PastPaperRepository.java`**: Houses custom JPA Queries, specifically the case-insensitive `@Query` used for wildcard course name and code searching.
* **`PaperRatingRepository.java`**, **`PaperCommentRepository.java`**, **`PaperReportRepository.java`**: Standard JPA bindings.

### 3. API Endpoints (`PastPaperController.java`)
Mirrors the exact styling of `RideController`. All state modifications natively emit to `ActivityLogRepository`.
* `GET /api/past-papers` & `GET /api/past-papers/search`: Automatically filters out unapproved papers.
* `POST /api/past-papers/{id}/rate`: Secures the rating process (recalculates averages natively).
* `DELETE /api/past-papers/{id}/reject`: Admin-specific endpoint that rejects pending papers. It blocks attempts to reject already-approved papers to prevent accidental deletion.
* `DELETE /api/past-papers/{id}`: Master delete that rigorously executes a **Cascade Delete** on all orphaned ratings, comments, and reports before removing the paper.

### 4. Database Seeder / Initialization
* **`DatabaseSeeder.java`**: A `CommandLineRunner` script that triggers on boot. It explicitly wipes the tables and intelligently injects exactly **10 active verified courses** (Database Systems, Applied Physics, etc.) and **1 pending course** (Object Oriented Programming). This guarantees all devs and testers always start with a beautifully populated UI.

### 5. Frontend UI
* **`PastPapers.jsx`**: The main interface view. Filters natively without reloading the page. Utilizes interactive glass-morphism cards and modals to browse details, rate, and comment.
* **`PastPapers.css`**: Styling specific to the custom glass-card flex-grids and modal overlays.

---

## 🔗 How it Integrates with the Rest of FSF

The true magic of this implementation is how it bridged into existing systems seamlessly:
1. **SecurityConfig**: The only backend config touched. `requestMatchers("/api/past-papers/**").permitAll()` was appended.
2. **App.jsx Route**: Replaced the `<ServiceSkeleton />` placeholder with the real `<PastPapers />` component.
3. **AdminPanel.jsx**: Upgraded the `fetchPending` and `fetchFlagged` arrays using a `Promise.all` bridge. It now fetches from both `rides` and `past-papers` simultaneously, tags them dynamically via an `entityType` attribute, and successfully passes them through the unified `handleApprove` pipeline without refactoring the Admin Controller.

## Testing Integrity
This entire module was stressed against a 40+ point autonomous Unit/API testing block spanning everything from SQL cascades, to star-rating limits, to security visibility of `isApproved=false`. **Do not modify the model boundaries or API structures without updating the validation conditions.**
