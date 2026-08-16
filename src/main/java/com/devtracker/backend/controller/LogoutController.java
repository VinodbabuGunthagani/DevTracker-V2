package com.devtracker.backend.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.devtracker.backend.dao.RememberTokenDAO;
import com.devtracker.backend.model.User;
import com.devtracker.backend.util.RememberMeUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class LogoutController {

    private final RememberTokenDAO rememberTokenDAO;

    public LogoutController(RememberTokenDAO rememberTokenDAO) {
        this.rememberTokenDAO = rememberTokenDAO;
    }

    @GetMapping("/logout")
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        HttpSession session = request.getSession(false);

        if (session != null) {

            User user = (User) session.getAttribute("user");

            if (user != null) {
                rememberTokenDAO.deleteUserTokens(user.getId());
            }

            session.invalidate();
        }

        RememberMeUtil.deleteCookie(response);

        response.sendRedirect("/login.html");
    }
}