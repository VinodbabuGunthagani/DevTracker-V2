package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

@Repository
public class AITimetableDAO {

    private final DataSource dataSource;

    public AITimetableDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public int getAIProgress(int roadmapId) {

        int progress = 0;

        String sql = """
                SELECT
                    COUNT(*) AS total,
                    SUM(completed) AS done
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
}