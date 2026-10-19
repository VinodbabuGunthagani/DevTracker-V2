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
import java.sql.Date;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

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
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

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
			ps.setString(9, task.getStatus() != null ? task.getStatus() : "NOT_STARTED");
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

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

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

		} catch (SQLException e) {
			throw new IllegalStateException("Could not load study schedule.", e);
		}

		return list;
	}

	public void updateTaskStatus(int taskId, boolean completed) {
		String sql = """
				UPDATE daily_tasks
				SET completed = ?, status = ?
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setBoolean(1, completed);
			ps.setString(2, completed ? "COMPLETED" : "NOT_STARTED");
			ps.setInt(3, taskId);
			ps.executeUpdate();

		} catch (SQLException e) {
			throw new IllegalStateException("Could not update task status.", e);
		}
	}

	public void updateTaskState(int taskId, String status) {
		String sql = "UPDATE daily_tasks SET status = ? WHERE id = ?";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

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

				try (PreparedStatement ps = con.prepareStatement(deleteTasksSql)) {
					ps.setInt(1, roadmapId);
					ps.executeUpdate();
				}

				String deletePlanSql = "DELETE FROM study_plan WHERE roadmap_id = ?";

				try (PreparedStatement ps = con.prepareStatement(deletePlanSql)) {
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

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setBoolean(1, completed);
			ps.setString(2, completed ? "COMPLETED" : "NOT_STARTED");
			ps.setInt(3, subtopicId);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void addNewSubtopicToSchedule(int roadmapId, int subtopicId, int weight) {

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

	public int replacePlanAndTasks(StudyPlan plan, List<DailyTask> tasks) {
		if (plan == null || tasks == null) {
			throw new IllegalArgumentException("Plan and tasks cannot be null.");
		}

		if (plan.getUserId() <= 0 || plan.getRoadmapId() <= 0 || plan.getTotalDays() <= 0
				|| plan.getDailyMinutes() <= 0) {
			throw new IllegalArgumentException("Invalid study plan details.");
		}

		String completedSql = """
				SELECT DISTINCT dt.subtopic_id
				FROM daily_tasks dt
				JOIN study_plan sp ON sp.id = dt.plan_id
				WHERE sp.roadmap_id = ?
				  AND (dt.completed = TRUE OR dt.status = 'COMPLETED')
				""";

		String deleteTasksSql = """
				DELETE FROM daily_tasks
				WHERE plan_id IN (
				    SELECT id FROM study_plan WHERE roadmap_id = ?
				)
				""";

		String deletePlansSql = "DELETE FROM study_plan WHERE roadmap_id = ?";

		String insertPlanSql = """
				INSERT INTO study_plan
				(user_id, roadmap_id, total_days, start_date, daily_minutes)
				VALUES (?, ?, ?, ?, ?)
				""";

		String insertTaskSql = """
				INSERT INTO daily_tasks
				(plan_id, subtopic_id, day_number, task_order, weight,
				 schedule_date, planned_minutes, completed, status,
				 start_time, end_time)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection con = dataSource.getConnection()) {
			boolean originalAutoCommit = con.getAutoCommit();

			try {
				con.setAutoCommit(false);

				// Preserve completion before removing the old schedule.
				Set<Integer> completedSubtopics = new HashSet<>();

				try (PreparedStatement ps = con.prepareStatement(completedSql)) {
					ps.setInt(1, plan.getRoadmapId());

					try (ResultSet rs = ps.executeQuery()) {
						while (rs.next()) {
							completedSubtopics.add(rs.getInt("subtopic_id"));
						}
					}
				}

				// Remove old tasks before old plans because there is no cascade.
				try (PreparedStatement ps = con.prepareStatement(deleteTasksSql)) {
					ps.setInt(1, plan.getRoadmapId());
					ps.executeUpdate();
				}

				try (PreparedStatement ps = con.prepareStatement(deletePlansSql)) {
					ps.setInt(1, plan.getRoadmapId());
					ps.executeUpdate();
				}

				int planId;

				try (PreparedStatement ps = con.prepareStatement(insertPlanSql, Statement.RETURN_GENERATED_KEYS)) {

					ps.setInt(1, plan.getUserId());
					ps.setInt(2, plan.getRoadmapId());
					ps.setInt(3, plan.getTotalDays());

					if (plan.getStartDate() != null) {
						ps.setDate(4, plan.getStartDate());
					} else {
						ps.setNull(4, java.sql.Types.DATE);
					}

					ps.setInt(5, plan.getDailyMinutes());

					if (ps.executeUpdate() != 1) {
						throw new SQLException("Failed to create study plan.");
					}

					try (ResultSet keys = ps.getGeneratedKeys()) {
						if (keys.next()) {
							planId = keys.getInt(1);
						} else {
							// SQLite fallback: get the inserted ID on this connection.
							try (Statement stmt = con.createStatement();
									ResultSet rs = stmt.executeQuery("SELECT last_insert_rowid()")) {
								if (!rs.next()) {
									throw new SQLException("Could not retrieve the new plan ID.");
								}
								planId = rs.getInt(1);
							}
						}
					}
				}

				try (PreparedStatement ps = con.prepareStatement(insertTaskSql)) {
					for (DailyTask task : tasks) {
						if (task == null || task.getSubtopicId() <= 0 || task.getDayNumber() <= 0
								|| task.getTaskOrder() <= 0 || task.getPlannedMinutes() < 0) {
							throw new IllegalArgumentException("Invalid task in generated schedule.");
						}

						boolean completed = task.isCompleted() || completedSubtopics.contains(task.getSubtopicId());

						task.setPlanId(planId);
						task.setCompleted(completed);
						task.setStatus(completed ? "COMPLETED" : "NOT_STARTED");

						ps.setInt(1, planId);
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
						ps.setBoolean(8, completed);
						ps.setString(9, completed ? "COMPLETED" : "NOT_STARTED");
						ps.setString(10, task.getStartTime());
						ps.setString(11, task.getEndTime());

						ps.executeUpdate();
					}
				}

				con.commit();
				return planId;

			} catch (Exception e) {
				try {
					con.rollback();
				} catch (SQLException rollbackError) {
					e.addSuppressed(rollbackError);
				}

				if (e instanceof RuntimeException runtimeException) {
					throw runtimeException;
				}
				throw new IllegalStateException("Failed to regenerate study plan; changes were rolled back.", e);

			} finally {
				try {
					con.setAutoCommit(originalAutoCommit);
				} catch (SQLException ignored) {
					// Closing the connection below releases this connection.
				}
			}

		} catch (SQLException e) {
			throw new IllegalStateException("Database error while regenerating study plan.", e);
		}
	}

	public boolean taskBelongsToRoadmap(int taskId, int roadmapId) {
		String sql = """
				SELECT 1
				FROM daily_tasks dt
				JOIN study_plan sp ON sp.id = dt.plan_id
				WHERE dt.id = ?
				  AND sp.roadmap_id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, taskId);
			ps.setInt(2, roadmapId);

			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}

		} catch (SQLException e) {
			throw new IllegalStateException("Could not verify task ownership.", e);
		}
	}

}