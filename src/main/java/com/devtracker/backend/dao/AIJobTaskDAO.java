package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.devtracker.backend.model.AIJobTask;

@Repository
public class AIJobTaskDAO {

    private final DataSource dataSource;

    public AIJobTaskDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void createTask(AIJobTask task) {

        String sql = """
                INSERT INTO ai_job_tasks
                    (job_id, subtopic_id, status, attempts)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, task.getJobId());
            ps.setInt(2, task.getSubtopicId());
            ps.setString(3, task.getStatus());
            ps.setInt(4, task.getAttempts());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public AIJobTask getNextPendingTask() {

        String sql = """
                SELECT *
                FROM ai_job_tasks
                WHERE status = 'PENDING'
                ORDER BY id
                LIMIT 1
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {

                AIJobTask task = new AIJobTask();

                task.setId(rs.getInt("id"));
                task.setJobId(rs.getInt("job_id"));
                task.setSubtopicId(rs.getInt("subtopic_id"));
                task.setStatus(rs.getString("status"));
                task.setAttempts(rs.getInt("attempts"));

                return task;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateStatus(int taskId, String status) {

        String sql = """
                UPDATE ai_job_tasks
                SET status = ?
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, taskId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void incrementAttempts(int taskId) {

        String sql = """
                UPDATE ai_job_tasks
                SET attempts = attempts + 1
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markCompleted(int taskId) {

        String sql = """
                UPDATE ai_job_tasks
                SET
                    status = 'COMPLETED',
                    completed_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markFailed(int taskId) {

        String sql = """
                UPDATE ai_job_tasks
                SET status = 'FAILED'
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void markRunning(int taskId) {

        String sql = """
                UPDATE ai_job_tasks
                SET
                    status = 'RUNNING',
                    started_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, taskId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<AIJobTask> getTasksByJobId(int jobId) {

        List<AIJobTask> tasks = new ArrayList<>();

        String sql = """
                SELECT
                    ajt.*,
                    s.name AS subject_name,
                    t.name AS topic_name,
                    st.name AS subtopic_name
                FROM ai_job_tasks ajt
                JOIN sub_topics st
                    ON ajt.subtopic_id = st.id
                JOIN topics t
                    ON st.topic_id = t.id
                JOIN subjects s
                    ON t.subject_id = s.id
                WHERE ajt.job_id = ?
                ORDER BY ajt.id
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, jobId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    AIJobTask task = new AIJobTask();

                    task.setId(rs.getInt("id"));
                    task.setJobId(rs.getInt("job_id"));
                    task.setSubtopicId(rs.getInt("subtopic_id"));
                    task.setStatus(rs.getString("status"));
                    task.setAttempts(rs.getInt("attempts"));

                    task.setSubjectName(rs.getString("subject_name"));
                    task.setTopicName(rs.getString("topic_name"));
                    task.setSubtopicName(rs.getString("subtopic_name"));

                    tasks.add(task);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return tasks;
    }
}