package com.devtracker.backend.dao;

import com.devtracker.backend.model.Subject;

import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class SubjectDAO {

    private final DataSource dataSource;

    public SubjectDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean addSubject(Subject subject) {
        String sql = """
                INSERT INTO subjects (roadmap_id, name)
                VALUES (?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subject.getRoadmapId());
            ps.setString(2, subject.getName());

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<Subject> getSubjects(int roadmapId) {
        return getSubjectsByRoadmap(roadmapId);
    }

    public int getRoadmapProgress(int roadmapId) {
        String sql = """
                SELECT
                    COUNT(st.id) AS total,
                    COALESCE(SUM(
                        CASE WHEN st.completed = TRUE THEN 1 ELSE 0 END
                    ), 0) AS done
                FROM subjects s
                LEFT JOIN topics t
                    ON s.id = t.subject_id
                    AND t.is_deleted = FALSE
                LEFT JOIN sub_topics st
                    ON t.id = st.topic_id
                    AND st.is_deleted = FALSE
                WHERE s.roadmap_id = ?
                  AND s.is_deleted = FALSE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

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

    public void deleteSubject(int id) {
        String[] statements = {
            """
            UPDATE sub_topics
            SET is_deleted = TRUE
            WHERE topic_id IN (
                SELECT id FROM topics WHERE subject_id = ?
            )
            """,
            "UPDATE topics SET is_deleted = TRUE WHERE subject_id = ?",
            "UPDATE subjects SET is_deleted = TRUE WHERE id = ?"
        };

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
                UPDATE subjects
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

    public List<Subject> getDeletedSubjects(int roadmapId) {
        List<Subject> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM subjects
                WHERE roadmap_id = ?
                  AND is_deleted = TRUE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubject(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public void restoreSubject(int id) {
        String[] statements = {
            "UPDATE subjects SET is_deleted = FALSE WHERE id = ?",
            "UPDATE topics SET is_deleted = FALSE WHERE subject_id = ?",
            """
            UPDATE sub_topics
            SET is_deleted = FALSE
            WHERE topic_id IN (
                SELECT id FROM topics WHERE subject_id = ?
            )
            """
        };

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

    public int addSubjectReturnId(int roadmapId, String name) {
        String sql = """
                INSERT INTO subjects (roadmap_id, name)
                VALUES (?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setInt(1, roadmapId);
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

    public List<Subject> getSubjectsByRoadmap(int roadmapId) {
        List<Subject> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM subjects
                WHERE roadmap_id = ?
                  AND is_deleted = FALSE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, roadmapId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubject(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    public Subject getSubjectById(int id) {
        String sql = "SELECT * FROM subjects WHERE id = ?";

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapSubject(rs);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<Subject> getDeletedSubjectsByUser(int userId) {
        List<Subject> list = new ArrayList<>();

        String sql = """
                SELECT s.*
                FROM subjects s
                JOIN roadmaps r ON s.roadmap_id = r.id
                WHERE r.user_id = ?
                  AND r.is_deleted = FALSE
                  AND s.is_deleted = TRUE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapSubject(rs));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    private Subject mapSubject(ResultSet rs) throws java.sql.SQLException {
        Subject subject = new Subject();
        subject.setId(rs.getInt("id"));
        subject.setRoadmapId(rs.getInt("roadmap_id"));
        subject.setName(rs.getString("name"));
        return subject;
    }
}