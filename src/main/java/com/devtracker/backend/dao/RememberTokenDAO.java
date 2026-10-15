package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.devtracker.backend.model.User;

@Repository
public class RememberTokenDAO {

    private final DataSource dataSource;

    public RememberTokenDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // SAVE A REMEMBER-ME TOKEN
    public boolean saveToken(
            int userId,
            String tokenHash,
            Timestamp expiresAt) {

        String sql = """
                INSERT INTO remember_tokens
                    (user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, expiresAt);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // FIND USER USING A REMEMBER TOKEN
    public User findUserByToken(String tokenHash) {

        String sql = """
                SELECT u.*
                FROM remember_tokens rt
                JOIN users u ON rt.user_id = u.id
                WHERE rt.token_hash = ?
                AND rt.expires_at > CURRENT_TIMESTAMP
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tokenHash);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    User user = new User();

                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    user.setPassword(rs.getString("password"));

                    return user;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    // DELETE ONE TOKEN
    public boolean deleteToken(String tokenHash) {

        String sql = """
                DELETE FROM remember_tokens
                WHERE token_hash = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, tokenHash);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    // DELETE ALL REMEMBER-ME TOKENS FOR A USER
    public boolean deleteUserTokens(int userId) {

        String sql = """
                DELETE FROM remember_tokens
                WHERE user_id = ?
                """;

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, userId);

            return ps.executeUpdate() > 0;

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}