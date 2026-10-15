
package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.devtracker.backend.model.AIDailyTask;
import com.devtracker.backend.model.AIStudyPlan;

@Repository
public class AITimetableDAO {

    private final DataSource dataSource;

    public AITimetableDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // CREATE AI PLAN
    public int createPlan(AIStudyPlan plan) {

        String sql = """
                INSERT INTO ai_study_plan
                    (user_id, roadmap_id, total_days)
                VALUES (?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, plan.getUserId());
            ps.setInt(2, plan.getRoadmapId());
            ps.setInt(3, plan.getTotalDays());

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
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

    // ADD AI TASK
    public void addTask(AIDailyTask task) {

        String sql = """
                INSERT INTO ai_daily_tasks
                    (ai_plan_id, subtopic_id, day_number, task_order)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, task.getPlanId());
            ps.setInt(2, task.getSubtopicId());
            ps.setInt(3, task.getDayNumber());
            ps.setInt(4, task.getTaskOrder());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // DELETE EXISTING AI PLAN
    public void deleteExistingPlan(int roadmapId) {

        String sql = """
                DELETE FROM ai_study_plan
                WHERE roadmap_id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GET AI TASKS FOR A ROADMAP
    public List<AIDailyTask> getTasks(int roadmapId) {

        List<AIDailyTask> list = new ArrayList<>();

        String sql = """
                SELECT
                    adt.*,
                    st.name AS subtopic_name
                FROM ai_daily_tasks adt
                JOIN ai_study_plan asp
                    ON adt.ai_plan_id = asp.id
                JOIN sub_topics st
                    ON adt.subtopic_id = st.id
                WHERE asp.roadmap_id = ?
                AND st.is_deleted = FALSE
                ORDER BY adt.day_number, adt.task_order
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {

                    AIDailyTask task = new AIDailyTask();

                    task.setId(rs.getInt("id"));
                    task.setPlanId(rs.getInt("ai_plan_id"));
                    task.setSubtopicId(rs.getInt("subtopic_id"));
                    task.setDayNumber(rs.getInt("day_number"));
                    task.setTaskOrder(rs.getInt("task_order"));
                    task.setCompleted(rs.getBoolean("completed"));
                    task.setSubtopicName(rs.getString("subtopic_name"));

                    list.add(task);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // CREATE AI PLAN USING EXPLICIT PARAMETERS
    public int createAIPlan(int userId, int roadmapId, int days) {

        String sql = """
                INSERT INTO ai_study_plan
                    (user_id, roadmap_id, total_days)
                VALUES (?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, userId);
            ps.setInt(2, roadmapId);
            ps.setInt(3, days);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
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

    // ADD AI TASK USING EXPLICIT PARAMETERS
    public void addAITask(
            int aiPlanId,
            int subtopicId,
            int day,
            int order) {

        String sql = """
                INSERT INTO ai_daily_tasks
                    (ai_plan_id, subtopic_id, day_number, task_order)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, aiPlanId);
            ps.setInt(2, subtopicId);
            ps.setInt(3, day);
            ps.setInt(4, order);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // UPDATE AI TASK COMPLETION
    public void updateCompleted(int taskId, boolean completed) {

        String sql = """
                UPDATE ai_daily_tasks
                SET completed = ?
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, completed);
            ps.setInt(2, taskId);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GET AI ROADMAP PROGRESS
    public int getAIProgress(int roadmapId) {

        int progress = 0;

        String sql = """
                SELECT
                    COUNT(*) AS total,
                    COALESCE(SUM(
                        CASE WHEN adt.completed THEN 1 ELSE 0 END
                    ), 0) AS done
                FROM ai_daily_tasks adt
                JOIN ai_study_plan asp
                    ON adt.ai_plan_id = asp.id
                WHERE asp.roadmap_id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {

                    int total = rs.getInt("total");
                    int done = rs.getInt("done");

                    if (total > 0) {
                        progress = (done * 100) / total;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return progress;
    }

    // CHECK WHETHER AI TASKS EXIST
    public boolean hasAITimetable(int roadmapId) {

        String sql = """
                SELECT EXISTS (
                    SELECT 1
                    FROM ai_daily_tasks adt
                    JOIN ai_study_plan asp
                        ON adt.ai_plan_id = asp.id
                    WHERE asp.roadmap_id = ?
                )
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 1;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // GET LATEST AI PLAN ID
    public int getAIPlanId(int roadmapId) {

        String sql = """
                SELECT id
                FROM ai_study_plan
                WHERE roadmap_id = ?
                ORDER BY id DESC
                LIMIT 1
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }
}