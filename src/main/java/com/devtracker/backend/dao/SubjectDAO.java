package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class SubjectDAO {

    private final DataSource dataSource;

    public SubjectDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int getRoadmapProgress(int roadmapId) {

        int progress = 0;

        String sql = """
                SELECT
                    COUNT(st.id) AS total,
                    SUM(st.completed) AS done
                FROM subjects s
                LEFT JOIN topics t
                    ON s.id = t.subject_id
                LEFT JOIN sub_topics st
                    ON t.id = st.topic_id
                WHERE s.roadmap_id = ?
                AND s.is_deleted = FALSE
                AND t.is_deleted = FALSE
                AND st.is_deleted = FALSE
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
}