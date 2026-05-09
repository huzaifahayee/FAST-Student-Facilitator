# FAST Student Facilitator (FSF) – README

This document provides instructions for building and running the application, details about design patterns used, partially implemented features, and currently known bugs.

---

# Section 1: Instructions for Compiling and Running the Application

## Method A: Online Deployment (Recommended)

The application is already deployed online and can be accessed directly without any local setup.

**Live Application:**
[https://fast-student-facilitator.vercel.app/](https://fast-student-facilitator.vercel.app/)

---

## Method B: Local Setup and Execution

### Prerequisites

Ensure the following software is installed on your system:

* Java JDK 17 or higher
* Node.js 18 or higher (with npm)
* PostgreSQL (running on port 5432)

---

## Step 1: Database Configuration

1. Start the PostgreSQL service.
2. Create a database named:

```text id="7l5hqs"
fsf_db
```

3. Configure your own PostgreSQL username and password in the application configuration.

4. You can either:

   * Create `application-local.properties` inside the backend folder, or
   * Set the following environment variables:

```text id="yygl5j"
SPRING_DATASOURCE_USERNAME
SPRING_DATASOURCE_PASSWORD
```

---

## Step 2: Backend Execution

1. Open a terminal.
2. Navigate to the `backend` directory.

```bash id="ukf4d1"
cd backend
```

3. Run the Spring Boot application:

```bash id="d4qcgf"
./mvnw spring-boot:run
```

4. Backend server will start at:

```text id="wjlwmc"
http://localhost:8080
```

---

## Step 3: Frontend Execution

1. Open a new terminal.
2. Navigate to the `frontend` directory.

```bash id="j4pq5o"
cd frontend
```

3. Install project dependencies:

```bash id="fqb51h"
npm install
```

4. Start the frontend development server:

```bash id="nyc3ju"
npm run dev
```

5. Frontend application will be available at:

```text id="jvt1w4"
http://localhost:5173
```

---

# Section 2: Design Patterns Used

The project uses multiple GoF (Gang of Four) design patterns across different modules of the system.

---

## 1. Template Method Pattern

Defines the overall structure of an algorithm while allowing subclasses to customize specific steps.

### Implementations

* **Timetable Module**
  `backend/src/main/java/com/fast/fsf/timetable/template/AbstractTimetableProcessor.java`

* **Campus Map Module**
  `backend/src/main/java/com/fast/fsf/campusmap/template/AbstractLocationModerationWorkflow.java`

* **Carpool Module**
  `backend/src/main/java/com/fast/fsf/carpool/template/AbstractRideMutationWorkflow.java`

* **Past Papers Module**
  `backend/src/main/java/com/fast/fsf/pastpapers/template/AbstractPaperMutationWorkflow.java`

---

## 2. State Pattern

Allows objects to change behavior dynamically according to their internal state.

### Implementations

* **Carpool Module**
  `backend/src/main/java/com/fast/fsf/carpool/state/RideModerationState.java`

* **Past Papers Module**
  `backend/src/main/java/com/fast/fsf/pastpapers/state/PaperModerationState.java`

* **Lost & Found Module**
  `backend/src/main/java/com/fast/fsf/lostfound/state/ListingModerationState.java`

---

## 3. Strategy Pattern

Encapsulates interchangeable search and filtering algorithms.

### Implementations

* **Campus Map Module**
  `backend/src/main/java/com/fast/fsf/campusmap/criterion/LocationSearchCriterion.java`

* **Past Papers Module**
  `backend/src/main/java/com/fast/fsf/pastpapers/criterion/PaperSearchCriterion.java`

---

## 4. Composite Pattern

Combines multiple search criteria into hierarchical AND/OR structures.

### Implementations

* **Campus Map Module**
  `backend/src/main/java/com/fast/fsf/campusmap/criterion/CompositeLocationSearchCriterion.java`

* **Past Papers Module**
  `backend/src/main/java/com/fast/fsf/pastpapers/criterion/CompositePaperSearchCriterion.java`

---

## 5. Adapter Pattern

Bridges repository implementations with higher-level domain interfaces.

### Implementations

* **Campus Map Module**
  `backend/src/main/java/com/fast/fsf/campusmap/adapter/CampusLocationRepositoryAdapter.java`

* **Past Papers Module**
  `backend/src/main/java/com/fast/fsf/pastpapers/adapter/PastPaperRepositoryAdapter.java`

---

## 6. Factory Pattern

Centralizes creation and validation logic of domain entities.

### Implementations

* **Campus Map Module**
  `backend/src/main/java/com/fast/fsf/campusmap/factory/CampusMapFactory.java`

* **Carpool Module**
  `backend/src/main/java/com/fast/fsf/carpool/factory/RideOfferFactory.java`

* **Past Papers Module**
  `backend/src/main/java/com/fast/fsf/pastpapers/factory/PastPaperFactory.java`

---

## 7. Observer Pattern

Implements event-driven communication for notifications and audit logging.

### Implementations

* **Campus Map Module**
  `backend/src/main/java/com/fast/fsf/campusmap/observer/MapEventPublisher.java`

* **Carpool Module**
  `backend/src/main/java/com/fast/fsf/carpool/event/RideEventPublisher.java`

* **Timetable Module**
  `backend/src/main/java/com/fast/fsf/timetable/web/TimetableEntryController.java`

---

## 8. Singleton Pattern

Ensures only one instance of important service/controller components exists.

### Implementations

* **Lost & Found Module**
  `backend/src/main/java/com/fast/fsf/lostfound/web/LostFoundController.java`

* **General Spring Components**
  All Spring `@Service` and `@RestController` classes across the system are managed as singleton beans by the Spring Framework.

---

# Section 3: Features Not Implemented End-to-End

## 1. Campus Map Navigation Coverage

The campus navigation engine is functional; however, only major campus routes and a limited set of locations/images have currently been added due to project scope limitations.

Additional routes and complete campus coverage are planned for future enhancement.

---

# Section 4: Known Bugs

## 1. Timetable Parsing Issues

Certain complex Excel timetable layouts containing heavily merged cells may occasionally produce:

* Duplicate timetable entries
* Incorrect slot positioning

---

## 2. Input Validation Edge Cases

Some uncommon or malformed date/time inputs may bypass validation and generate backend exceptions.

### Example

```text id="z1qgai"
Feb 30th
```

---
3. General Input Validation Edge Cases

Although extensive validation has been implemented across the system, some uncommon or unexpected user inputs may still produce unintended behavior if they were not encountered during testing.

Possible Cases
Extremely large text inputs
Special character combinations
Invalid file formats or corrupted uploads
Unexpected whitespace or encoding issues
Browser-specific input inconsistencies

These cases are considered low-frequency edge cases and may be addressed in future refinements.

## 4. Google OAuth Session Persistence

In some browsers, refreshing the page may clear local session storage before initialization completes, requiring the user to log in again.
