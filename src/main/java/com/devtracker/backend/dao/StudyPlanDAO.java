package com.devtracker.backend.dao;

import com.devtracker.backend.model.DailyTask;
import com.devtracker.backend.model.StudyPlan;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class StudyPlanDAO {

    private final DataSource dataSource;

    public StudyPlanDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int createPlan(StudyPlan plan) {
        String sql = """
                INSERT INTO study_plan
                (user_id, roadmap_id, total_days, start_date, daily_minutes)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, plan.getUserId());
            ps.setInt(2, plan.getRoadmapId());
            ps.setInt(3, plan.getTotalDays());

            if (plan.getStartDate() != null) {
                ps.setDate(4, plan.getStartDate());
            } else {
                ps.setNull(4, java.sql.Types.DATE);
            }

            ps.setInt(5, plan.getDailyMinutes());

            if (ps.executeUpdate() == 0) {
                return 0;
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public void addTask(DailyTask task) {
        String sql = """
                INSERT INTO daily_tasks
                (plan_id, subtopic_id, day_number, task_order, weight,
                 schedule_date, planned_minutes, completed, status,
                 start_time, end_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, task.getPlanId());
            ps.setInt(2, task.getSubtopicId());
            ps.setInt(3, task.getDayNumber());
            ps.setInt(4, task.getTaskOrder());
            ps.setInt(5, task.getWeight());

            if (task.getScheduleDate() != null) {
                ps.setDate(6, task.getScheduleDate());
            } else {
                ps.setNull(6, java.sql.Types.DATE);
            }

            ps.setInt(7, task.getPlannedMinutes());
            ps.setBoolean(8, task.isCompleted());
            ps.setString(9, task.getStatus() != null
                    ? task.getStatus() : "NOT_STARTED");
            ps.setString(10, task.getStartTime());
            ps.setString(11, task.getEndTime());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<DailyTask> getTasks(int roadmapId) {
        List<DailyTask> list = new ArrayList<>();

        String sql = """
                SELECT
                    dt.*,
                    st.name AS subtopic_name,
                    st.difficulty,
                    st.estimated_minutes,
                    t.name AS topic_name,
                    s.name AS subject_name
                FROM daily_tasks dt
                JOIN study_plan sp ON dt.plan_id = sp.id
                JOIN sub_topics st ON dt.subtopic_id = st.id
                JOIN topics t ON st.topic_id = t.id
                JOIN subjects s ON t.subject_id = s.id
                WHERE sp.roadmap_id = ?
                  AND s.is_deleted = FALSE
                  AND t.is_deleted = FALSE
                  AND st.is_deleted = FALSE
                ORDER BY dt.schedule_date, dt.task_order
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    DailyTask task = new DailyTask();

                    task.setId(rs.getInt("id"));
                    task.setPlanId(rs.getInt("plan_id"));
                    task.setSubtopicId(rs.getInt("subtopic_id"));
                    task.setDayNumber(rs.getInt("day_number"));
                    task.setTaskOrder(rs.getInt("task_order"));
                    task.setWeight(rs.getInt("weight"));
                    task.setScheduleDate(rs.getDate("schedule_date"));
                    task.setPlannedMinutes(rs.getInt("planned_minutes"));
                    task.setCompleted(rs.getBoolean("completed"));
                    task.setSubtopicName(rs.getString("subtopic_name"));
                    task.setTopicName(rs.getString("topic_name"));
                    task.setSubjectName(rs.getString("subject_name"));
                    task.setDifficulty(rs.getString("difficulty"));
                    task.setEstimatedMinutes(rs.getInt("estimated_minutes"));
                    task.setStatus(rs.getString("status"));
                    task.setStartTime(rs.getString("start_time"));
                    task.setEndTime(rs.getString("end_time"));

                    list.add(task);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void updateTaskStatus(int taskId, boolean completed) {
        String sql = """
                UPDATE daily_tasks
                SET completed = ?, status = ?
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, completed);
            ps.setString(2, completed ? "COMPLETED" : "NOT_STARTED");
            ps.setInt(3, taskId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTaskState(int taskId, String status) {
        String sql = "UPDATE daily_tasks SET status = ? WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, status);
            ps.setInt(2, taskId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteExistingPlan(int roadmapId) {
        try (Connection con = dataSource.getConnection()) {
            con.setAutoCommit(false);

            try {
                String deleteTasksSql = """
                        DELETE FROM daily_tasks
                        WHERE plan_id IN (
                            SELECT id FROM study_plan WHERE roadmap_id = ?
                        )
                        """;

                try (PreparedStatement ps =
                             con.prepareStatement(deleteTasksSql)) {
                    ps.setInt(1, roadmapId);
                    ps.executeUpdate();
                }

                String deletePlanSql =
                        "DELETE FROM study_plan WHERE roadmap_id = ?";

                try (PreparedStatement ps =
                             con.prepareStatement(deletePlanSql)) {
                    ps.setInt(1, roadmapId);
                    ps.executeUpdate();
                }

                con.commit();

            } catch (Exception e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateTaskBySubTopic(int subtopicId, boolean completed) {
        String sql = """
                UPDATE daily_tasks
                SET completed = ?, status = ?
                WHERE subtopic_id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, completed);
            ps.setString(2, completed ? "COMPLETED" : "NOT_STARTED");
            ps.setInt(3, subtopicId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addNewSubtopicToSchedule(
            int roadmapId, int subtopicId, int weight) {

        try (Connection con = dataSource.getConnection()) {

            String planSql = """
                    SELECT id FROM study_plan
                    WHERE roadmap_id = ?
                    ORDER BY id DESC LIMIT 1
                    """;

            int planId;

            try (PreparedStatement ps = con.prepareStatement(planSql)) {
                ps.setInt(1, roadmapId);

                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return;
                    }
                    planId = rs.getInt("id");
                }
            }

            String daySql = """
                    SELECT COALESCE(MAX(day_number), 0) + 1 AS new_day
                    FROM daily_tasks
                    WHERE plan_id = ?
                    """;

            int newDay;

            try (PreparedStatement ps = con.prepareStatement(daySql)) {
                ps.setInt(1, planId);

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    newDay = rs.getInt("new_day");
                }
            }

            String insertSql = """
                    INSERT INTO daily_tasks
                    (plan_id, subtopic_id, day_number, task_order,
                     weight, completed, status)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """;

            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, planId);
                ps.setInt(2, subtopicId);
                ps.setInt(3, newDay);
                ps.setInt(4, 1);
                ps.setInt(5, weight);
                ps.setBoolean(6, false);
                ps.setString(7, "NOT_STARTED");

                ps.executeUpdate();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}