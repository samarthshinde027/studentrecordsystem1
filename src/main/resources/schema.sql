CREATE TABLE IF NOT EXISTS students (
    prn    TEXT PRIMARY KEY,
    name   TEXT NOT NULL,
    course TEXT NOT NULL,
    year   INTEGER NOT NULL,
    phone  TEXT
);

CREATE TABLE IF NOT EXISTS attendance (
    prn      TEXT NOT NULL,
    att_date TEXT NOT NULL,
    status   TEXT NOT NULL,
    PRIMARY KEY (prn, att_date)
);

CREATE TABLE IF NOT EXISTS marks (
    prn       TEXT NOT NULL,
    subject   TEXT NOT NULL,
    marks     INTEGER NOT NULL,
    max_marks INTEGER NOT NULL,
    PRIMARY KEY (prn, subject)
);

CREATE TABLE IF NOT EXISTS student_photos (
    prn   TEXT PRIMARY KEY,
    photo TEXT NOT NULL
);
