package com.devtracker.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devtracker.backend.model.DashboardData;
import com.devtracker.backend.model.User;
import com.devtracker.backend.service.DashboardService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService) {

        this.dashboardService = dashboardService;
    }

    @GetMapping
    public ResponseEntity<?> getDashboard(
            HttpSession session) {

        User user =
                (User) session.getAttribute("user");

        if (user == null) {

            return ResponseEntity
                    .status(401)
                    .body("Not authenticated");
        }

        DashboardData dashboard =
                dashboardService.getDashboard(
                        user.getId(),
                        user.getName()
                );

        return ResponseEntity.ok(dashboard);
    }
}