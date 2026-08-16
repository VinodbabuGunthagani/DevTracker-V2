package com.devtracker.backend.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.devtracker.backend.model.User;

@Repository
public class UserDAO {

    private final DataSource dataSource;

    public UserDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public boolean register(User user) {

        String sql = """
                INSERT INTO users(name, email, password)
                VALUES (?, ?, ?)
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPassword());

            return statement.executeUpdate() > 0;

        } catch (Exception e) {

            e.printStackTrace();
            return false;
        }
    }
    public User getUserByEmail(String email) {

    String sql = "SELECT * FROM users WHERE email = ?";

    try (Connection connection = dataSource.getConnection();
         PreparedStatement statement = connection.prepareStatement(sql)) {

        statement.setString(1, email);

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
}