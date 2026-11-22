CREATE TABLE IF NOT EXISTS students (
    prn    TEXT PRIMARY KEY,
    name   TEXT NOT NULL,
    course TEXT NOT NULL,
    year   INTEGER NOT NULL,
    phone  TEXT
);
