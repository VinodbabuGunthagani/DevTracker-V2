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

    // Save a remember-me token
    public boolean saveToken(int userId, String tokenHash, Timestamp expiresAt) {

        String sql = """
                INSERT INTO remember_tokens(user_id, token_hash, expires_at)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);
            statement.setString(2, tokenHash);
            statement.setTimestamp(3, expiresAt);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }


    // Find the user using a remember token
    public User findUserByToken(String tokenHash) {

        String sql = """
                SELECT u.*
                FROM remember_tokens rt
                JOIN users u ON rt.user_id = u.id
                WHERE rt.token_hash = ?
                AND rt.expires_at > CURRENT_TIMESTAMP
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);

            try (ResultSet rs = statement.executeQuery()) {

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


    // Delete one token
    public boolean deleteToken(String tokenHash) {

        String sql =
                "DELETE FROM remember_tokens WHERE token_hash = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, tokenHash);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }


    // Delete all remember-me sessions of a user
    public boolean deleteUserTokens(int userId) {

        String sql =
                "DELETE FROM remember_tokens WHERE user_id = ?";

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, userId);

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }
}