package com.devtracker.backend.dao;

import com.devtracker.backend.model.Topic;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TopicDAO {

	private final DataSource dataSource;

	public TopicDAO(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public boolean addTopic(Topic topic) {
		String sql = "INSERT INTO topics (subject_id, name) VALUES (?, ?)";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, topic.getSubjectId());
			ps.setString(2, topic.getName());

			return ps.executeUpdate() > 0;

		} catch (Exception e) {
			e.printStackTrace();
		}

		return false;
	}

	public List<Topic> getTopics(int subjectId) {
		return getTopicsBySubject(subjectId);
	}

	public List<Topic> getTopicsBySubject(int subjectId) {
		List<Topic> list = new ArrayList<>();

		String sql = """
				SELECT *
				FROM topics
				WHERE subject_id = ?
				  AND is_deleted = FALSE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, subjectId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapTopic(rs));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public int getSubjectProgress(int subjectId) {
		String sql = """
				SELECT
				    COUNT(st.id) AS total,
				    COALESCE(SUM(
				        CASE WHEN st.completed = TRUE THEN 1 ELSE 0 END
				    ), 0) AS done
				FROM topics t
				LEFT JOIN sub_topics st
				    ON t.id = st.topic_id
				    AND st.is_deleted = FALSE
				WHERE t.subject_id = ?
				  AND t.is_deleted = FALSE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, subjectId);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					int total = rs.getInt("total");
					int done = rs.getInt("done");

					if (total > 0) {
						return (done * 100) / total;
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	public void deleteTopic(int id) {
		String[] statements = { """
				UPDATE sub_topics
				SET is_deleted = TRUE
				WHERE topic_id = ?
				""", """
				UPDATE topics
				SET is_deleted = TRUE,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""" };

		try (Connection con = dataSource.getConnection()) {
			con.setAutoCommit(false);

			try {
				for (String sql : statements) {
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

	public void updateName(int id, String name) {
		String sql = """
				UPDATE topics
				SET name = ?, updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setString(1, name);
			ps.setInt(2, id);
			ps.executeUpdate();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public List<Topic> getDeletedTopics(int subjectId) {
		List<Topic> list = new ArrayList<>();

		String sql = """
				SELECT *
				FROM topics
				WHERE subject_id = ?
				  AND is_deleted = TRUE
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, subjectId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapTopic(rs));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public void restoreTopic(int id) {
		String[] statements = { """
				UPDATE topics
				SET is_deleted = FALSE,
				    updated_at = CURRENT_TIMESTAMP
				WHERE id = ?
				""", """
				UPDATE sub_topics
				SET is_deleted = FALSE
				WHERE topic_id = ?
				""" };

		try (Connection con = dataSource.getConnection()) {
			con.setAutoCommit(false);

			try {
				for (String sql : statements) {
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

	public int addTopicReturnId(int subjectId, String name) {
		String sql = "INSERT INTO topics (subject_id, name) VALUES (?, ?)";

		try (Connection con = dataSource.getConnection();
				PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

			ps.setInt(1, subjectId);
			ps.setString(2, name);

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

	public List<Topic> getDeletedTopicsByUser(int userId) {
		List<Topic> list = new ArrayList<>();

		String sql = """
				SELECT t.*
				FROM topics t
				JOIN subjects s ON t.subject_id = s.id
				JOIN roadmaps r ON s.roadmap_id = r.id
				WHERE r.user_id = ?
				  AND r.is_deleted = FALSE
				  AND t.is_deleted = TRUE
				ORDER BY t.id
				""";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, userId);

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					list.add(mapTopic(rs));
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return list;
	}

	public Topic getTopicById(int id) {
		String sql = "SELECT * FROM topics WHERE id = ?";

		try (Connection con = dataSource.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {

			ps.setInt(1, id);

			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return mapTopic(rs);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private Topic mapTopic(ResultSet rs) throws java.sql.SQLException {
		Topic topic = new Topic();
		topic.setId(rs.getInt("id"));
		topic.setSubjectId(rs.getInt("subject_id"));
		topic.setName(rs.getString("name"));
		return topic;
	}
}