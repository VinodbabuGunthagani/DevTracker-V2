package com.devtracker.backend.dao;

import com.devtracker.backend.model.SubTopic;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SubTopicDAO {

    private final DataSource dataSource;

    public SubTopicDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int addSubTopic(SubTopic subTopic) {
        return insertSubTopic(subTopic);
    }

    public int addSubTopicReturnId(SubTopic sub) {
        return insertSubTopic(sub);
    }

    private int insertSubTopic(SubTopic subTopic) {
        String sql = """
                INSERT INTO sub_topics
                (topic_id, name, difficulty, estimated_minutes, weight)
                VALUES (?, ?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, subTopic.getTopicId());
            ps.setString(2, subTopic.getName());
            ps.setString(3, subTopic.getDifficulty());
            ps.setInt(4, subTopic.getEstimatedMinutes());
            ps.setInt(5, subTopic.getWeight());

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

    public List<SubTopic> getSubTopics(int topicId) {
        return getSubTopicsByTopic(topicId);
    }

    public List<SubTopic> getSubTopicsByTopic(int topicId) {
        List<SubTopic> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM sub_topics
                WHERE topic_id = ?
                  AND is_deleted = FALSE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, topicId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubTopic(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void updateStatus(int id, boolean completed) {
        String sql = """
                UPDATE sub_topics
                SET completed = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setBoolean(1, completed);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getProgress(int topicId) {
        String sql = """
                SELECT
                    COUNT(*) AS total,
                    COALESCE(SUM(
                        CASE WHEN completed = TRUE THEN 1 ELSE 0 END
                    ), 0) AS done
                FROM sub_topics
                WHERE topic_id = ?
                  AND is_deleted = FALSE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, topicId);

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

    public void deleteSubTopic(int id) {
        String sql = """
                UPDATE sub_topics
                SET is_deleted = TRUE, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateName(int id, String name) {
        String sql = """
                UPDATE sub_topics
                SET name = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SubTopic> getDeletedSubTopics(int topicId) {
        List<SubTopic> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM sub_topics
                WHERE topic_id = ?
                  AND is_deleted = TRUE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, topicId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubTopic(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void restoreSubTopic(int id) {
        String sql = """
                UPDATE sub_topics
                SET is_deleted = FALSE, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateAIAnalysis(
            int id, String difficulty, int minutes, int weight) {

        String sql = """
                UPDATE sub_topics
                SET difficulty = ?,
                    estimated_minutes = ?,
                    weight = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, difficulty);
            ps.setInt(2, minutes);
            ps.setInt(3, weight);
            ps.setInt(4, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<SubTopic> getDeletedSubTopicsByUser(int userId) {
        List<SubTopic> list = new ArrayList<>();

        String sql = """
                SELECT st.*
                FROM sub_topics st
                JOIN topics t ON st.topic_id = t.id
                JOIN subjects s ON t.subject_id = s.id
                JOIN roadmaps r ON s.roadmap_id = r.id
                WHERE r.user_id = ?
                  AND st.is_deleted = TRUE
                ORDER BY st.id
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubTopic(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public SubTopic getSubTopicById(int id) {
        String sql = "SELECT * FROM sub_topics WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSubTopic(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public void updateAINotes(int id, String notes) {
        updateAIField("ai_notes", id, notes);
    }

    public void updateAILearning(int id, String learning) {
        updateAIField("ai_learning", id, learning);
    }

    public void updateAIKeywords(int id, String keywords) {
        updateAIField("ai_keywords", id, keywords);
    }

    private void updateAIField(String column, int id, String value) {
        // The column names are fixed by the methods above.
        String sql = "UPDATE sub_topics SET " + column
                + " = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, value);
            ps.setInt(2, id);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLearningContext(int subtopicId) {
        String sql = """
                SELECT
                    r.title AS roadmap,
                    r.description AS roadmap_description,
                    s.name AS subject,
                    t.name AS topic,
                    st.name AS subtopic
                FROM sub_topics st
                JOIN topics t ON st.topic_id = t.id
                JOIN subjects s ON t.subject_id = s.id
                JOIN roadmaps r ON s.roadmap_id = r.id
                WHERE st.id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subtopicId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return """
                            LEARNING CONTEXT

                            Roadmap:
                            %s

                            Roadmap Goal:
                            %s

                            Subject:
                            %s

                            Topic:
                            %s

                            Target Subtopic:
                            %s
                            """.formatted(
                            rs.getString("roadmap"),
                            rs.getString("roadmap_description"),
                            rs.getString("subject"),
                            rs.getString("topic"),
                            rs.getString("subtopic"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public List<SubTopic> getRoadmapLessons(int roadmapId) {
        List<SubTopic> list = new ArrayList<>();

        String sql = """
                SELECT
                    st.*,
                    s.name AS subject_name,
                    t.name AS topic_name
                FROM sub_topics st
                JOIN topics t ON st.topic_id = t.id
                JOIN subjects s ON t.subject_id = s.id
                WHERE s.roadmap_id = ?
                  AND s.is_deleted = FALSE
                  AND t.is_deleted = FALSE
                  AND st.is_deleted = FALSE
                ORDER BY s.id, t.id, st.weight DESC
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SubTopic st = mapSubTopic(rs);
                    st.setSubjectName(rs.getString("subject_name"));
                    st.setTopicName(rs.getString("topic_name"));
                    list.add(st);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private SubTopic mapSubTopic(ResultSet rs)
            throws java.sql.SQLException {

        SubTopic st = new SubTopic();

        st.setId(rs.getInt("id"));
        st.setTopicId(rs.getInt("topic_id"));
        st.setName(rs.getString("name"));
        st.setCompleted(rs.getBoolean("completed"));
        st.setDifficulty(rs.getString("difficulty"));
        st.setEstimatedMinutes(rs.getInt("estimated_minutes"));
        st.setWeight(rs.getInt("weight"));
        st.setAiNotes(rs.getString("ai_notes"));
        st.setAiLearning(rs.getString("ai_learning"));
        st.setAiKeywords(rs.getString("ai_keywords"));

        return st;
    }
}