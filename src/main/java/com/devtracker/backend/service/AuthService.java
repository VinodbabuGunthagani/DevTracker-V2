package com.devtracker.backend.service;

import java.sql.Timestamp;
import java.util.Collections;

import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.devtracker.backend.dao.RememberTokenDAO;
import com.devtracker.backend.dao.UserDAO;
import com.devtracker.backend.model.User;
import com.devtracker.backend.util.RememberMeUtil;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;


@Service
public class AuthService {

	private final UserDAO userDAO;
	private final RememberTokenDAO rememberTokenDAO;

	@Value("${google.client-id}")
	private String googleClientId;

	private GoogleIdTokenVerifier googleVerifier;

    public AuthService(
            UserDAO userDAO,
            RememberTokenDAO rememberTokenDAO) {

        this.userDAO = userDAO;
        this.rememberTokenDAO = rememberTokenDAO;
    }
    @PostConstruct
    public void initializeGoogleVerifier() {
        this.googleVerifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                )
                .setAudience(
                        Collections.singletonList(googleClientId)
                )
                .build();
    }


    // ============================================================
    // REGISTER
    // ============================================================

    public boolean register(
            String name,
            String email,
            String password) {

        String hashedPassword =
                BCrypt.hashpw(
                        password,
                        BCrypt.gensalt()
                );

        User user = new User();

        user.setName(name);
        user.setEmail(email);
        user.setPassword(hashedPassword);

        return userDAO.register(user);
    }


    // ============================================================
    // LOGIN
    // ============================================================

    public boolean login(
            String email,
            String password,
            boolean remember,
            HttpServletRequest request,
            HttpServletResponse response) {

        User user =
                userDAO.getUserByEmail(email);

        // User doesn't exist
        if (user == null) {
            return false;
        }

        // Google-only account has no password
        if (user.getPassword() == null ||
            user.getPassword().isBlank()) {

            return false;
        }

        // Password doesn't match
        if (!BCrypt.checkpw(
                password,
                user.getPassword())) {

            return false;
        }


        // ========================================================
        // Destroy existing session
        // ========================================================

        HttpSession oldSession =
                request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }


        // ========================================================
        // Create fresh session
        // ========================================================

        createSession(request, user);


        // ========================================================
        // Remember Me
        // ========================================================

        if (remember) {

            createRememberMeToken(
                    user,
                    response
            );
        }

        return true;
    }


    // ============================================================
    // GOOGLE LOGIN
    // ============================================================

    public boolean googleLogin(
            String googleId,
            String name,
            String email,
            String profilePicture,
            HttpServletRequest request,
            HttpServletResponse response) {

        // --------------------------------------------------------
        // Check whether this Google account already exists
        // --------------------------------------------------------

        User user =
                userDAO.getUserByGoogleId(googleId);


        // --------------------------------------------------------
        // First Google login
        // --------------------------------------------------------

        if (user == null) {

            user =
                    userDAO.getUserByEmail(email);

            // Existing local account with same email
            if (user != null) {

                // Do not automatically convert a local
                // password account into a Google account.
                return false;
            }

            // Create new Google account
            user =
                    userDAO.createGoogleUser(
                            name,
                            email,
                            googleId,
                            profilePicture
                    );

            if (user == null) {
                return false;
            }
        }


        // --------------------------------------------------------
        // Destroy existing session
        // --------------------------------------------------------

        HttpSession oldSession =
                request.getSession(false);

        if (oldSession != null) {
            oldSession.invalidate();
        }


        // --------------------------------------------------------
        // Create fresh session
        // --------------------------------------------------------

        createSession(request, user);

        return true;
    }


    // ============================================================
    // CREATE SESSION
    // ============================================================

    private void createSession(
            HttpServletRequest request,
            User user) {

        HttpSession session =
                request.getSession(true);

        session.setAttribute("user", user);

        // 30 minutes inactivity
        session.setMaxInactiveInterval(
                30 * 60
        );
    }


    // ============================================================
    // REMEMBER ME
    // ============================================================

    private void createRememberMeToken(
            User user,
            HttpServletResponse response) {

        String token =
                RememberMeUtil.generateToken();

        String tokenHash =
                RememberMeUtil.hashToken(token);

        Timestamp expires =
                new Timestamp(
                        System.currentTimeMillis()
                        + (30L * 24 * 60 * 60 * 1000)
                );


        // Remove previous tokens
        rememberTokenDAO.deleteUserTokens(
                user.getId()
        );


        // Save new token
        rememberTokenDAO.saveToken(
                user.getId(),
                tokenHash,
                expires
        );


        // Send cookie
        RememberMeUtil.createCookie(
                response,
                token
        );
    }


    // ============================================================
    // CHECK SESSION
    // ============================================================

    public User checkSession(
            HttpServletRequest request,
            HttpServletResponse response) {

        // 1. Check existing session
        HttpSession session =
                request.getSession(false);

        if (session != null) {

            User user =
                    (User) session.getAttribute("user");

            if (user != null) {
                return user;
            }
        }


        // 2. No active session ->
        //    check remember-me cookie

        Cookie[] cookies =
                request.getCookies();

        if (cookies == null) {
            return null;
        }

        for (Cookie cookie : cookies) {

            if (!"remember_me".equals(
                    cookie.getName())) {

                continue;
            }

            String token =
                    cookie.getValue();

            if (token == null ||
                token.isBlank()) {

                return null;
            }


            // Hash the token exactly like
            // when it was saved
            String tokenHash =
                    RememberMeUtil.hashToken(token);


            // Look up token in SQLite
            User user =
                    rememberTokenDAO.findUserByToken(
                            tokenHash
                    );

            if (user == null) {

                // Invalid/expired token
                RememberMeUtil.deleteCookie(
                        response
                );

                return null;
            }


            // 3. Valid remember-me token
            createSession(request, user);

            return user;
        }

        return null;
    }


    // ============================================================
    // LOGOUT
    // ============================================================

    public void logout(
            HttpServletRequest request,
            HttpServletResponse response) {

        HttpSession session =
                request.getSession(false);

        if (session != null) {

            User user =
                    (User) session.getAttribute("user");

            if (user != null) {

                rememberTokenDAO.deleteUserTokens(
                        user.getId()
                );
            }

            session.invalidate();
        }

        // Delete browser remember-me cookie
        RememberMeUtil.deleteCookie(response);
    }
}