package com.devtracker.backend.dao;

import com.devtracker.backend.model.Roadmap;
import com.devtracker.backend.model.SubTopic;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Date;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoadmapDAO {

	private final DataSource dataSource;

	public RoadmapDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	// CREATE MANUAL ROADMAP
	// Retained for compatibility with the V1 method name.
	public boolean addRoadmap(Roadmap roadmap) {
		return createRoadmap(roadmap) > 0;
	}

	// GET USER ROADMAPS
	public List<Roadmap> getRoadmaps(int userId) {

		List<Roadmap> list = new ArrayList<>();

		String sql = """
				SELECT *
				FROM roadmaps
				WHERE user_id = ?
				  AND is_deleted = FALSE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Roadmap r = mapRoadmap(rs);
					r.setUserId(rs.getInt("user_id"));
					list.add(r);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// CREATE MANUAL ROADMAP — V2 METHOD
	public int createRoadmap(Roadmap roadmap) {

		String sql = """
				INSERT INTO roadmaps (
				    user_id, title, description,
				    start_date, target_date, is_ai
				)
				VALUES (?, ?, ?, ?, ?, FALSE)
				""";

		try (Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setInt(1, roadmap.getUserId());
			ps.setString(2, roadmap.getTitle());
			ps.setString(3, roadmap.getDescription());

			setDateOrNull(ps, 4, roadmap.getStartDate());
			setDateOrNull(ps, 5, roadmap.getTargetDate());

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

	// SOFT DELETE ROADMAP AND ITS SYLLABUS
	public void deleteRoadmap(int id) {

		String[] sqlStatements = { """
				UPDATE sub_topics
				SET is_deleted = TRUE
				WHERE topic_id IN (
				    SELECT t.id
				    FROM topics t
				    JOIN subjects s ON t.subject_id = s.id
				    WHERE s.roadmap_id = ?
				)
				""", """
				UPDATE topics
				SET is_deleted = TRUE
				WHERE subject_id IN (
				    SELECT id FROM subjects WHERE roadmap_id = ?
				)
				""", """
				UPDATE subjects
				SET is_deleted = TRUE
				WHERE roadmap_id = ?
				""", """
				UPDATE roadmaps
				SET is_deleted = TRUE,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""" };

		try (Connection con = dataSource.getConnection()) {

			con.setAutoCommit(false);

			try {
				for (String sql : sqlStatements) {
					try (PreparedStatement ps = con.prepareStatement(sql)) {
						ps.setInt(1, id);
						ps.executeUpdate();
					}
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

	// UPDATE ROADMAP TITLE AND DESCRIPTION
	public void updateName(int id, String title, String description) {

		String sql = """
				UPDATE roadmaps
				SET title = ?,
				    description = ?,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, title);
			ps.setString(2, description);
			ps.setInt(3, id);

			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// GET DELETED ROADMAPS
	public List<Roadmap> getDeletedRoadmaps(int userId) {

		List<Roadmap> list = new ArrayList<>();

		String sql = """
				SELECT *
				FROM roadmaps
				WHERE user_id = ?
				  AND is_deleted = TRUE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Roadmap r = mapRoadmap(rs);
					r.setUserId(rs.getInt("user_id"));
					list.add(r);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// RESTORE ROADMAP AND ITS SYLLABUS
	public void restoreRoadmap(int id) {

		String[] sqlStatements = { """
				UPDATE roadmaps
				SET is_deleted = FALSE,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", """
				UPDATE subjects
				SET is_deleted = FALSE
				WHERE roadmap_id = ?
				""", """
				UPDATE topics
				SET is_deleted = FALSE
				WHERE subject_id IN (
				    SELECT id FROM subjects WHERE roadmap_id = ?
				)
				""", """
				UPDATE sub_topics
				SET is_deleted = FALSE
				WHERE topic_id IN (
				    SELECT t.id
				    FROM topics t
				    JOIN subjects s ON t.subject_id = s.id
				    WHERE s.roadmap_id = ?
				)
				""" };

		try (Connection con = dataSource.getConnection()) {

			con.setAutoCommit(false);

			try {
				for (String sql : sqlStatements) {
					try (PreparedStatement ps = con.prepareStatement(sql)) {
						ps.setInt(1, id);
						ps.executeUpdate();
					}
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

	// GET ALL SUBTOPICS FOR A ROADMAP
	public List<SubTopic> getAllSubTopics(int roadmapId) {

		List<SubTopic> list = new ArrayList<>();

		String sql = """
				SELECT st.*
				FROM sub_topics st
				JOIN topics t ON st.topic_id = t.id
				JOIN subjects s ON t.subject_id = s.id
				WHERE s.roadmap_id = ?
				  AND s.is_deleted = FALSE
				  AND t.is_deleted = FALSE
				  AND st.is_deleted = FALSE
				ORDER BY st.weight DESC
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, roadmapId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					SubTopic st = new SubTopic();

					st.setId(rs.getInt("id"));
					st.setTopicId(rs.getInt("topic_id"));
					st.setName(rs.getString("name"));
					st.setCompleted(rs.getBoolean("completed"));
					st.setDifficulty(rs.getString("difficulty"));
					st.setEstimatedMinutes(rs.getInt("estimated_minutes"));
					st.setWeight(rs.getInt("weight"));
					st.setAiLearning(rs.getString("ai_learning"));

					list.add(st);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	// GET ROADMAP BY ID
	public Roadmap getRoadmapById(int id) {

		String sql = """
				SELECT *
				FROM roadmaps
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return mapRoadmap(rs);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	// CREATE AI ROADMAP AND RETURN GENERATED ID
	public int addRoadmapReturnId(Roadmap roadmap) {

		String sql = """
				INSERT INTO roadmaps (
				    user_id, title, description,
				    start_date, target_date, is_ai
				)
				VALUES (?, ?, ?, ?, ?, TRUE)
				""";

		try (Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setInt(1, roadmap.getUserId());
			ps.setString(2, roadmap.getTitle());
			ps.setString(3, roadmap.getDescription());

			setDateOrNull(ps, 4, roadmap.getStartDate());
			setDateOrNull(ps, 5, roadmap.getTargetDate());

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

	// GET AI ROADMAPS FOR A USER
	public List<Roadmap> getAIRoadmaps(int userId) {

		List<Roadmap> list = new ArrayList<>();

		String sql = """
				SELECT *
				FROM roadmaps
				WHERE user_id = ?
				  AND is_ai = TRUE
				  AND is_deleted = FALSE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					Roadmap r = mapRoadmap(rs);
					r.setUserId(rs.getInt("user_id"));
					list.add(r);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	private Roadmap mapRoadmap(ResultSet rs) throws java.sql.SQLException {

		Roadmap r = new Roadmap();

		r.setId(rs.getInt("id"));
		r.setTitle(rs.getString("title"));
		r.setDescription(rs.getString("description"));
		r.setStartDate(rs.getDate("start_date"));
		r.setTargetDate(rs.getDate("target_date"));
		r.setAi(rs.getBoolean("is_ai"));

		return r;
	}

	private void setDateOrNull(PreparedStatement ps, int index, Date date) throws java.sql.SQLException {

		if (date != null) {
			ps.setDate(index, date);
		} else {
			ps.setNull(index, Types.DATE);
		}
	}

	public boolean roadmapBelongsToUser(int roadmapId, int userId) {
		String sql = """
				SELECT 1
				FROM roadmaps
				WHERE id = ?
				  AND user_id = ?
				  AND is_deleted = FALSE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, roadmapId);
			ps.setInt(2, userId);

			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}

		} catch (Exception e) {
			throw new IllegalStateException("Could not verify roadmap ownership", e);
		}
	}
}