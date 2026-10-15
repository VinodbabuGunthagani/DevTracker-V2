package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.devtracker.backend.model.LearningResource;

@Repository
public class LearningResourceDAO {

    private final DataSource dataSource;

    public LearningResourceDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ADD LEARNING RESOURCE
    public void addResource(LearningResource r) {

        String sql = """
                INSERT INTO learning_resources
                    (subtopic_id, video_id, title, channel_name,
                     thumbnail, rank_no, reason)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, r.getSubtopicId());
            ps.setString(2, r.getVideoId());
            ps.setString(3, r.getTitle());
            ps.setString(4, r.getChannelName());
            ps.setString(5, r.getThumbnail());
            ps.setInt(6, r.getRankNo());
            ps.setString(7, r.getReason());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // GET RESOURCES FOR A SUBTOPIC
    public List<LearningResource> getResources(int subtopicId) {

        List<LearningResource> list = new ArrayList<>();

        String sql = """
                SELECT *
                FROM learning_resources
                WHERE subtopic_id = ?
                ORDER BY rank_no
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subtopicId);

            try (ResultSet rs = ps.executeQuery()) {

                while (rs.next()) {

                    LearningResource r = new LearningResource();

                    r.setId(rs.getInt("id"));
                    r.setSubtopicId(rs.getInt("subtopic_id"));
                    r.setVideoId(rs.getString("video_id"));
                    r.setTitle(rs.getString("title"));
                    r.setChannelName(rs.getString("channel_name"));
                    r.setThumbnail(rs.getString("thumbnail"));
                    r.setRankNo(rs.getInt("rank_no"));
                    r.setReason(rs.getString("reason"));

                    list.add(r);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return list;
    }

    // CHECK WHETHER RESOURCES EXIST
    public boolean exists(int subtopicId) {

        String sql = """
                SELECT COUNT(*) AS total
                FROM learning_resources
                WHERE subtopic_id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, subtopicId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}