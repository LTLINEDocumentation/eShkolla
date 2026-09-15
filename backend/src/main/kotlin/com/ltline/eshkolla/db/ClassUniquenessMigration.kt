package com.ltline.eshkolla.db

import java.sql.Connection

object ClassUniquenessMigration {
    fun run(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeUpdate("""
                CREATE OR REPLACE FUNCTION eshkolla_prevent_duplicate_class()
                RETURNS trigger AS $$
                BEGIN
                    IF NEW.active = TRUE AND EXISTS (
                        SELECT 1 FROM classes c
                        WHERE c.active = TRUE
                          AND c.id <> NEW.id
                          AND LOWER(TRIM(c.name)) = LOWER(TRIM(NEW.name))
                          AND c.grade_level = NEW.grade_level
                          AND COALESCE(c.school_id, '') = COALESCE(NEW.school_id, '')
                    ) THEN
                        RAISE EXCEPTION 'CLASS_ALREADY_EXISTS';
                    END IF;
                    RETURN NEW;
                END;
                $$ LANGUAGE plpgsql;
            """.trimIndent())
            statement.executeUpdate("DROP TRIGGER IF EXISTS trg_prevent_duplicate_class ON classes")
            statement.executeUpdate("""
                CREATE TRIGGER trg_prevent_duplicate_class
                BEFORE INSERT OR UPDATE OF name, grade_level, school_id, active ON classes
                FOR EACH ROW EXECUTE FUNCTION eshkolla_prevent_duplicate_class()
            """.trimIndent())
        }
    }
}
