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

	// ============================================================
	// REGISTER LOCAL USER
	// ============================================================

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

	// ============================================================
	// FIND USER BY EMAIL
	// ============================================================

	public User getUserByEmail(String email) {

		String sql = "SELECT * FROM users WHERE email = ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, email);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {
					return mapUser(rs);
				}
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	// ============================================================
	// FIND USER BY GOOGLE ID
	// ============================================================

	public User getUserByGoogleId(String googleId) {

		String sql = "SELECT * FROM users WHERE google_id = ?";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql)) {

			statement.setString(1, googleId);

			try (ResultSet rs = statement.executeQuery()) {

				if (rs.next()) {
					return mapUser(rs);
				}
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	// ============================================================
	// CREATE GOOGLE USER
	// ============================================================

	public User createGoogleUser(String name, String email, String googleId, String profilePicture) {

		String sql = """
				INSERT INTO users(
				    name,
				    email,
				    password,
				    google_id,
				    auth_provider,
				    profile_picture
				)
				VALUES (?, ?, NULL, ?, 'GOOGLE', ?)
				""";

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(sql,
						java.sql.Statement.RETURN_GENERATED_KEYS)) {

			statement.setString(1, name);
			statement.setString(2, email);
			statement.setString(3, googleId);
			statement.setString(4, profilePicture);

			int affectedRows = statement.executeUpdate();

			if (affectedRows == 0) {
				return null;
			}

			try (ResultSet keys = statement.getGeneratedKeys()) {

				if (keys.next()) {

					User user = new User();

					user.setId(keys.getInt(1));
					user.setName(name);
					user.setEmail(email);
					user.setPassword(null);
					user.setGoogleId(googleId);
					user.setAuthProvider("GOOGLE");
					user.setProfilePicture(profilePicture);

					return user;
				}
			}

		} catch (Exception e) {

			e.printStackTrace();
		}

		return null;
	}

	// ============================================================
	// MAP DATABASE ROW → USER
	// ============================================================

	private User mapUser(ResultSet rs) throws Exception {

		User user = new User();

		user.setId(rs.getInt("id"));
		user.setName(rs.getString("name"));
		user.setEmail(rs.getString("email"));
		user.setPassword(rs.getString("password"));

		user.setGoogleId(rs.getString("google_id"));
		user.setAuthProvider(rs.getString("auth_provider"));
		user.setProfilePicture(rs.getString("profile_picture"));

		return user;
	}
}