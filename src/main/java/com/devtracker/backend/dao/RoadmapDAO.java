package com.devtracker.backend.dao;

import com.devtracker.backend.model.Roadmap;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

@Repository
public class RoadmapDAO {

    private final DataSource dataSource;

    public RoadmapDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<Roadmap> getRoadmaps(int userId) {

        List<Roadmap> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM roadmaps
                WHERE user_id = ?
                AND is_deleted = FALSE
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    Roadmap r = new Roadmap();

                    r.setId(rs.getInt("id"));
                    r.setUserId(rs.getInt("user_id"));
                    r.setTitle(rs.getString("title"));
                    r.setDescription(rs.getString("description"));
                    r.setStartDate(rs.getDate("start_date"));
                    r.setTargetDate(rs.getDate("target_date"));
                    r.setAi(rs.getBoolean("is_ai"));

                    list.add(r);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }
}