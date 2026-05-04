# ExamPortal — Secure Online Examination System with AI Proctoring

A production-ready desktop application for conducting proctored online examinations.  
Built with **Java 17 · JavaFX 21 · MySQL 8 · OpenCV 4 (JavaCV) · HikariCP · BCrypt**.

---

## System Overview

ExamPortal provides a complete end-to-end examination pipeline:

- **Admin** users create exams, manage the question bank, monitor live proctoring logs, and export results.
- **Students** log in, attempt time-limited MCQ exams, and view detailed per-question results.
- **AI Proctor** captures webcam frames every 5 s via OpenCV, detects face absence / multiple faces / gaze deviation, logs WARN/CRITICAL events asynchronously, and auto-submits the exam after 3 CRITICAL violations.
- **Window Monitor** detects tab switches, focus loss, and 60-second inactivity, logging each as a behavioural event.
- **Exam Lockdown** disables right-click, Ctrl+C/V/A/X, and logs every blocked shortcut as a COPY_ATTEMPT event.

---

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│  JavaFX UI Layer                                          │
│  LoginView · AdminDashboardView · StudentDashboardView    │
│  ExamView  · ResultView · ProctorOverlay                  │
└───────────────────┬──────────────────────────────────────┘
                    │ thin controllers
┌───────────────────▼──────────────────────────────────────┐
│  Controller Layer                                         │
│  LoginController · AdminController · StudentController    │
│  ExamController  · ProctorController · ResultController   │
└───────────────────┬──────────────────────────────────────┘
                    │ business logic
┌───────────────────▼──────────────────────────────────────┐
│  Service Layer                                            │
│  AuthService · ExamService · EvaluatorService            │
│  ProctorService · ResultService · SessionManager          │
└─────────┬─────────────────────┬────────────────────────  ┘
          │                     │
┌─────────▼────────┐  ┌─────────▼──────────────────────────┐
│  DAO Layer       │  │  Proctor Module                      │
│  UserDAO         │  │  FrameCapture (ScheduledExecutor)    │
│  ExamDAO         │  │  FaceDetector (Haar Cascade / OpenCV)│
│  QuestionDAO     │  │  ProctorEngine (BlockingQueue async) │
│  AttemptDAO      │  │  WindowMonitor (focus + inactivity)  │
│  ResponseDAO     │  └────────────────────────────────────  ┘
│  ActivityLogDAO  │
└─────────┬────────┘
          │ HikariCP pool
┌─────────▼────────────────────────────────────────────────┐
│  MySQL 8.0 — exam_portal database                        │
│  users · exams · questions · attempts · responses         │
│  activity_logs                                            │
└──────────────────────────────────────────────────────────┘
```

---

## Prerequisites

| Tool           | Version  |
|----------------|----------|
| JDK            | 17+      |
| Maven          | 3.9+     |
| MySQL          | 8.0+     |
| OpenCV         | 4.8 (optional — degrades gracefully if absent) |

---

## Setup Instructions

### 1. Clone the repository

```bash
git clone https://github.com/your-org/exam-portal.git
cd exam-portal
```

### 2. Create the MySQL database and schema

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. Seed demo data

```bash
mysql -u root -p exam_portal < src/main/resources/db/seed.sql
```

### 4. Configure the application

Edit `src/main/resources/config.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/exam_portal?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
db.user=root
db.password=YOUR_PASSWORD

# OpenCV native library path
# Windows: C:/opencv/build/java/x64
# Linux/macOS: /usr/local/lib
opencv.lib.path=C:/opencv/build/java/x64

# Proctoring
proctor.save.dir=C:/tmp/proctor
```

### 5. (Optional) Install OpenCV native libraries

Download OpenCV 4.8 from https://opencv.org/releases/  
Extract and add the native library path to your system PATH or pass it via `-Djava.library.path`.

> **Without OpenCV** the system still runs — proctoring will simulate FACE_ABSENT events instead of using the webcam.

### 6. Build

```bash
mvn clean package -DskipTests
```

### 7. Run

```bash
mvn javafx:run
```

Or with an explicit JavaFX module path:

```bash
java --module-path $PATH_TO_FX \
     --add-modules javafx.controls,javafx.fxml,javafx.media,javafx.swing \
     -Djava.library.path=$OPENCV_LIB \
     -jar target/exam-portal-1.0.0.jar
```

### 8. Seed credentials

| Role    | Email                | Password     |
|---------|----------------------|--------------|
| Admin   | admin@exam.local     | Admin@1234   |
| Student | student1@exam.local  | Student@1234 |
| Student | student2@exam.local  | Student@1234 |

> Passwords are stored as **BCrypt** hashes — they are never logged or returned in any response.

---

## Running Tests

```bash
mvn test
```

Surefire HTML report is generated at:

```
target/surefire-reports/index.html
```

Test classes:

| Class                    | Scope                                  |
|--------------------------|----------------------------------------|
| `AuthServiceTest`        | Mockito — login, register, logout      |
| `EvaluatorServiceTest`   | Mockito — scoring, pass/fail, partial  |
| `UserDAOTest`            | H2 in-memory — CRUD + session token    |
| `ExamDAOTest`            | H2 in-memory — CRUD + publish toggle   |
| `FaceDetectorTest`       | Unit — face count → event classification |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/examportal/
│   │   ├── App.java                      # JavaFX entry point
│   │   ├── AppServices.java              # Service locator
│   │   ├── config/
│   │   │   ├── AppConfig.java            # config.properties reader
│   │   │   └── DatabaseConfig.java       # HikariCP pool
│   │   ├── model/                        # Java records: User, Exam, Question,
│   │   │   └── ...                       # Attempt, Response, ActivityLog
│   │   ├── dao/                          # Interfaces + JDBC implementations
│   │   ├── service/                      # Business logic services
│   │   ├── controller/                   # Thin JavaFX controllers
│   │   ├── ui/                           # JavaFX view classes
│   │   ├── proctor/                      # FrameCapture, FaceDetector,
│   │   │   └── ...                       # ProctorEngine, WindowMonitor
│   │   └── util/                         # BCryptUtil, CsvExporter, Validator
│   └── resources/
│       ├── config.properties
│       ├── db/schema.sql
│       ├── db/seed.sql
│       ├── css/app.css
│       ├── css/exam.css
│       └── haarcascade_frontalface_default.xml
└── test/
    └── java/com/examportal/
        ├── service/AuthServiceTest.java
        ├── service/EvaluatorServiceTest.java
        ├── dao/UserDAOTest.java
        ├── dao/ExamDAOTest.java
        └── proctor/FaceDetectorTest.java
```

---

## Security Notes

- Passwords are hashed with **BCrypt (work factor 12)** and never stored or logged in plaintext.
- SQL injection is prevented by using **PreparedStatement exclusively** — zero string concatenation in SQL.
- Session tokens are **SecureRandom UUIDs** stored in the DB and verified on every privileged action.
- Single-session enforcement: a new login **always invalidates the previous session token**.
- Exam view disables right-click, Ctrl+C/V/A/X, and logs every interception.

---

## Configuration Reference

| Key                                  | Default                    | Description                         |
|--------------------------------------|----------------------------|-------------------------------------|
| `db.url`                             | *(required)*               | JDBC URL                            |
| `db.user`                            | *(required)*               | MySQL username                      |
| `db.password`                        | *(required)*               | MySQL password                      |
| `db.pool.size`                       | `10`                       | HikariCP max pool size              |
| `opencv.lib.path`                    | empty                      | Path to OpenCV native libs          |
| `proctor.frame.interval.ms`          | `5000`                     | Webcam capture interval             |
| `proctor.save.dir`                   | `<tmp>/proctor`            | Frame JPEG save directory           |
| `proctor.gaze.deviation.threshold`   | `0.30`                     | Fraction of frame width for gaze    |
| `proctor.critical.auto.submit.count` | `3`                        | CRITICAL events before force-submit |
| `session.timeout.minutes`            | `120`                      | Session validity window             |

---

BUILD COMPLETE — all files generated.

---

## 🧪 Test Credentials

Use these accounts to test the web application at **http://localhost:8080**

### 👤 Admin Account

| Field    | Value                    |
|----------|--------------------------|
| Email    | `admin@exam.local`       |
| Password | `Admin@1234`             |
| Role     | ADMIN                    |
| Access   | Admin Dashboard → Exam & Student Management, Activity Logs |

### 🎓 Student Accounts

| Field    | Account 1                  | Account 2                  |
|----------|----------------------------|----------------------------|
| Email    | `student1@exam.local`      | `student2@exam.local`      |
| Password | `Student@1234`             | `Student@1234`             |
| Role     | STUDENT                    | STUDENT                    |
| Access   | Student Dashboard → Take Exams, View Results |

---

### 🐍 Pre-Loaded Exam

| Field            | Value                              |
|------------------|------------------------------------|
| Title            | Python Programming Fundamentals    |
| Questions        | 10 MCQ (auto-randomised)           |
| Duration         | 30 minutes                         |
| Total Marks      | 10                                 |
| Pass Threshold   | 60% (6 / 10 correct)               |
| Difficulty Mix   | 5 Easy · 3 Medium · 1 Hard         |

---

### 🚀 How to Run

```bash
# 1. Start the web server
mvn exec:java -Dexec.mainClass=com.examportal.web.WebApp

# 2. Open browser
http://localhost:8080

# 3. Login with any account above and take the Python exam
```

> **Proctoring features active during exam:**
> - 🔒 Fullscreen lock (auto re-enters on exit)
> - 📷 Live webcam feed (top-right camera panel)
> - 🚫 ESC / F11 / Ctrl shortcuts blocked
> - ⚡ Auto-submit on tab switch (7 violations) or timer expiry
> - ✅ "Exam Submitted" screen → auto-redirect to results
