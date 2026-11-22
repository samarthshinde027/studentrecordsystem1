# Student Record Management System

Java 17 + Spring Boot REST API + HTML/CSS/JS frontend. SQLite locally, PostgreSQL in production.

## Run locally (Eclipse)
File -> Import -> Maven -> Existing Maven Projects -> select this folder.
Right-click `Application.java` -> Run As -> Java Application. Open http://localhost:8080

## Run locally (terminal)
mvn spring-boot:run

## API
GET    /api/students?q=      list / search by PRN or name
GET    /api/students/{prn}   one student
POST   /api/students         add
PUT    /api/students/{prn}   update
DELETE /api/students/{prn}   delete

## Environment variables (production)
DB_URL   jdbc:postgresql://HOST/DBNAME?sslmode=require
DB_USER  database user
DB_PASS  database password
DB_POOL  5
PORT     set automatically by the host
