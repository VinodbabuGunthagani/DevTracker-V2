
package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.devtracker.backend.model.AIJob;

@Repository
public class AIJobDAO {

	private final DataSource dataSource;

	public AIJobDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	// CREATE AI JOB
	public int createJob(AIJob job) {

		String sql = """
				INSERT INTO ai_jobs
				    (user_id, roadmap_id, job_type, status,
				     total_tasks, completed_tasks, failed_tasks)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""";

		try (Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setInt(1, job.getUserId());
			ps.setInt(2, job.getRoadmapId());
			ps.setString(3, job.getJobType());
			ps.setString(4, job.getStatus());
			ps.setInt(5, job.getTotalTasks());
			ps.setInt(6, job.getCompletedTasks());
			ps.setInt(7, job.getFailedTasks());

			int affectedRows = ps.executeUpdate();

			if (affectedRows == 0) {
				return -1;
			}

			try (ResultSet rs = ps.getGeneratedKeys()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return -1;
	}

	// UPDATE TOTAL TASK COUNT
	public void updateTotalTasks(int jobId, int totalTasks) {

		String sql = """
				UPDATE ai_jobs
				SET total_tasks = ?
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, totalTasks);
			ps.setInt(2, jobId);

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// INCREMENT COMPLETED TASK COUNT
	public void incrementCompletedTasks(int jobId) {

		String sql = """
				UPDATE ai_jobs
				SET completed_tasks = completed_tasks + 1
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, jobId);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// INCREMENT FAILED TASK COUNT
	public void incrementFailedTasks(int jobId) {

		String sql = """
				UPDATE ai_jobs
				SET failed_tasks = failed_tasks + 1
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, jobId);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// MARK JOB COMPLETED
	public void markCompleted(int jobId) {

		String sql = """
				UPDATE ai_jobs
				SET
				    status = 'COMPLETED',
				    finished_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, jobId);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// GET JOB BY ID
	public AIJob getJob(int jobId) {

		String sql = """
				SELECT *
				FROM ai_jobs
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, jobId);

			try (ResultSet rs = ps.executeQuery()) {

				if (rs.next()) {

					AIJob job = new AIJob();

					job.setId(rs.getInt("id"));
					job.setUserId(rs.getInt("user_id"));
					job.setRoadmapId(rs.getInt("roadmap_id"));

					int currentSubtopicId = rs.getInt("current_subtopic_id");

					job.setCurrentSubtopicId(rs.wasNull() ? null : currentSubtopicId);

					job.setJobType(rs.getString("job_type"));
					job.setStatus(rs.getString("status"));
					job.setTotalTasks(rs.getInt("total_tasks"));
					job.setCompletedTasks(rs.getInt("completed_tasks"));
					job.setFailedTasks(rs.getInt("failed_tasks"));

					Timestamp createdAt = rs.getTimestamp("created_at");
					Timestamp startedAt = rs.getTimestamp("started_at");
					Timestamp finishedAt = rs.getTimestamp("finished_at");

					// Timestamp fields are available in the database.
					// Map them if the AIJob model defines corresponding setters.

					return job;
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	// CHECK WHETHER JOB TASKS ARE FINISHED
	public boolean isFinished(int jobId) {

		AIJob job = getJob(jobId);

		if (job == null) {
			return true;
		}

		return job.getCompletedTasks() + job.getFailedTasks() >= job.getTotalTasks();
	}
}