package com.devtracker.backend.config;

import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class DatabaseInitializer {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void initialize() {

        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE,
                    password TEXT,
                    google_id TEXT UNIQUE,
                    auth_provider TEXT DEFAULT 'LOCAL',
                    profile_picture TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS remember_tokens (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    token_hash TEXT NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                        ON DELETE CASCADE
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS roadmaps (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    title TEXT,
                    description TEXT,
                    start_date DATE,
                    target_date DATE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    is_ai BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    roadmap_id INTEGER,
                    name TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (roadmap_id)
                        REFERENCES roadmaps(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS topics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_id INTEGER,
                    name TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (subject_id)
                        REFERENCES subjects(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sub_topics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    topic_id INTEGER,
                    name TEXT,
                    completed BOOLEAN DEFAULT FALSE,
                    difficulty TEXT DEFAULT 'Medium',
                    estimated_minutes INTEGER DEFAULT 60,
                    weight INTEGER DEFAULT 120,
                    ai_notes TEXT,
                    ai_learning TEXT,
                    ai_keywords TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    is_deleted BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (topic_id)
                        REFERENCES topics(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS study_plan (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    roadmap_id INTEGER,
                    total_days INTEGER,
                    start_date DATE,
                    daily_minutes INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id),
                    FOREIGN KEY (roadmap_id)
                        REFERENCES roadmaps(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS daily_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    plan_id INTEGER,
                    subtopic_id INTEGER,
                    day_number INTEGER,
                    task_order INTEGER,
                    weight INTEGER,
                    schedule_date DATE,
                    planned_minutes INTEGER,
                    completed BOOLEAN DEFAULT FALSE,
                    status TEXT DEFAULT 'NOT_STARTED',
                    start_time TEXT,
                    end_time TEXT,
                    FOREIGN KEY (plan_id)
                        REFERENCES study_plan(id),
                    FOREIGN KEY (subtopic_id)
                        REFERENCES sub_topics(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS learning_resources (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subtopic_id INTEGER,
                    video_id TEXT,
                    title TEXT,
                    channel_name TEXT,
                    thumbnail TEXT,
                    rank_no INTEGER,
                    reason TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (subtopic_id)
                        REFERENCES sub_topics(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ai_study_plan (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    roadmap_id INTEGER,
                    total_days INTEGER,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id),
                    FOREIGN KEY (roadmap_id)
                        REFERENCES roadmaps(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ai_daily_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    ai_plan_id INTEGER,
                    subtopic_id INTEGER,
                    day_number INTEGER,
                    task_order INTEGER,
                    completed BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (ai_plan_id)
                        REFERENCES ai_study_plan(id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (subtopic_id)
                        REFERENCES sub_topics(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ai_jobs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    roadmap_id INTEGER NOT NULL,
                    current_subtopic_id INTEGER,
                    job_type TEXT NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    total_tasks INTEGER DEFAULT 0,
                    completed_tasks INTEGER DEFAULT 0,
                    failed_tasks INTEGER DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    started_at TIMESTAMP,
                    finished_at TIMESTAMP,
                    FOREIGN KEY (user_id)
                        REFERENCES users(id),
                    FOREIGN KEY (roadmap_id)
                        REFERENCES roadmaps(id),
                    FOREIGN KEY (current_subtopic_id)
                        REFERENCES sub_topics(id)
                )
            """);

            statement.executeUpdate("""
                CREATE TABLE IF NOT EXISTS ai_job_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    job_id INTEGER NOT NULL,
                    subtopic_id INTEGER NOT NULL,
                    status TEXT DEFAULT 'PENDING',
                    attempts INTEGER DEFAULT 0,
                    error_message TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    started_at TIMESTAMP,
                    completed_at TIMESTAMP,
                    FOREIGN KEY (job_id)
                        REFERENCES ai_jobs(id)
                        ON DELETE CASCADE,
                    FOREIGN KEY (subtopic_id)
                        REFERENCES sub_topics(id)
                )
            """);

            System.out.println("=================================");
            System.out.println("DevTracker SQLite database ready");
            System.out.println("=================================");

        } catch (Exception e) {
            System.err.println("Failed to initialize DevTracker database");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}