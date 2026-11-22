# Student Record Management System

Java 17 + Spring Boot REST API + HTML/CSS/JS frontend. SQLite locally, PostgreSQL in production.

Features: admin login, dashboard charts, add/edit/delete students, search, filter, sort, pagination,
CSV import/export, photos, attendance, marks + printable report card, dark/light theme, mobile layout.

## Run locally (Eclipse)
File -> Import -> Maven -> Existing Maven Projects -> select this folder.
Right-click `Application.java` -> Run As -> Java Application. Open http://localhost:8080
Default login: admin / admin123

## Environment variables (production)
ADMIN_USER   admin username (default admin)
ADMIN_PASS   admin password  (SET THIS)
AUTH_SECRET  long random text used to sign login tokens (SET THIS)
DB_URL       jdbc:postgresql://HOST/DBNAME?sslmode=require   (optional; SQLite is used if empty)
DB_USER, DB_PASS, DB_POOL=5
