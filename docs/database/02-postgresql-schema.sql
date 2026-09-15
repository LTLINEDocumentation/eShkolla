-- eShkolla - PostgreSQL baseline schema
-- This migration is intentionally independent from the current in-memory demo repositories.
-- Recommended PostgreSQL: 15+

CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    username VARCHAR(120) NOT NULL UNIQUE,
    full_name VARCHAR(160) NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('ADMINISTRATOR','DREJTOR','MESIMDHENES','NXENES','PRIND')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS classes (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (name, academic_year)
);

CREATE TABLE IF NOT EXISTS students (
    id VARCHAR(64) PRIMARY KEY,
    full_name VARCHAR(160) NOT NULL,
    class_id VARCHAR(64) NOT NULL REFERENCES classes(id),
    birth_date DATE NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS teachers (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) REFERENCES users(id),
    full_name VARCHAR(160) NOT NULL,
    employee_code VARCHAR(64) UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS subjects (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS teacher_subjects (
    teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id),
    subject_id VARCHAR(64) NOT NULL REFERENCES subjects(id),
    class_id VARCHAR(64) NOT NULL REFERENCES classes(id),
    PRIMARY KEY (teacher_id, subject_id, class_id)
);

CREATE TABLE IF NOT EXISTS grades (
    id VARCHAR(64) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL REFERENCES students(id),
    subject_id VARCHAR(64) NOT NULL REFERENCES subjects(id),
    teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id),
    value SMALLINT NOT NULL CHECK (value BETWEEN 1 AND 5),
    period VARCHAR(32) NOT NULL,
    academic_year VARCHAR(20) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS absences (
    id VARCHAR(64) PRIMARY KEY,
    student_id VARCHAR(64) NOT NULL REFERENCES students(id),
    subject_id VARCHAR(64) NOT NULL REFERENCES subjects(id),
    teacher_id VARCHAR(64) NOT NULL REFERENCES teachers(id),
    absence_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (status IN ('E_PAAFTESUAR','E_ARSYESHME')),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_students_class ON students(class_id);
CREATE INDEX IF NOT EXISTS idx_grades_student ON grades(student_id);
CREATE INDEX IF NOT EXISTS idx_grades_teacher ON grades(teacher_id);
CREATE INDEX IF NOT EXISTS idx_absences_student ON absences(student_id);
CREATE INDEX IF NOT EXISTS idx_absences_teacher ON absences(teacher_id);
CREATE INDEX IF NOT EXISTS idx_absences_date ON absences(absence_date);

-- Seed data used only for the first local/demo database installation.
INSERT INTO classes (id, name, academic_year) VALUES
    ('C03', 'VIII/1', '2026/2027'),
    ('C04', 'VIII/2', '2026/2027')
ON CONFLICT (id) DO NOTHING;

INSERT INTO subjects (id, name) VALUES
    ('L01', 'Matematikë')
ON CONFLICT (id) DO NOTHING;

INSERT INTO students (id, full_name, class_id, birth_date) VALUES
    ('NX001', 'Ardit Krasniqi', 'C03', '2012-03-01'),
    ('NX002', 'Era Gashi', 'C03', '2012-07-18'),
    ('NX003', 'Diar Berisha', 'C04', '2012-02-09'),
    ('NX004', 'Suela Hoxha', 'C04', '2012-11-22')
ON CONFLICT (id) DO NOTHING;
