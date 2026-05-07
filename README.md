# ExamPortal - Web Mode

This project can run as a web application with an embedded Java HTTP server.

- Backend entry point: com.examportal.web.WebApp
- Web URL: http://localhost:8080
- Static pages served from: src/main/resources/web

## Prerequisites

- JDK 21
- Maven 3.9+
- MySQL 8+

## 1. Database Setup

Create schema:

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

Seed demo data:

```bash
mysql -u root -p exam_portal < src/main/resources/db/seed.sql
```

## 2. Configure App

Edit src/main/resources/config.properties:

- db.url
- db.user
- db.password

Optional proctor settings:

- opencv.lib.path
- proctor.frame.interval.ms
- proctor.save.dir
- proctor.critical.auto.submit.count

## 3. Build

```bash
mvn clean package -DskipTests
```

## 4. Run as Web App (Not JavaFX)

Start the web server:

```bash
mvn exec:java -Dexec.mainClass=com.examportal.web.WebApp
```

Open in browser:

```text
http://localhost:8080
```

## Test Login Accounts

Admin:

- Email: admin@exam.local
- Password: Admin@1234

Students:

- Email: student1@exam.local | Password: Student@1234
- Email: student2@exam.local | Password: Student@1234

## Useful Commands

Run tests:

```bash
mvn test
```

## Troubleshooting

- Port already in use: stop the process on port 8080, then start again.
- Database connection failed: verify db.url, db.user, db.password in config.properties.
- Static files not loading: ensure src/main/resources/web contains index.html and related assets.

## Notes

- The repository still contains JavaFX classes for desktop UI, but web mode runs from com.examportal.web.WebApp.
- For your requirement, use the web run command above and open localhost:8080.
