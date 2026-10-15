
package com.devtracker.backend.controller;

import com.devtracker.backend.model.Roadmap;
import com.devtracker.backend.service.DashboardService;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/roadmaps")
public class RoadmapController {

	private final DashboardService dashboardService;

	public RoadmapController(DashboardService dashboardService) {
		this.dashboardService = dashboardService;
	}

	@PostMapping
	public ResponseEntity<?> createRoadmap(@RequestBody Roadmap roadmap, HttpSession session) {

		Object sessionUser = session.getAttribute("user");

		if (!(sessionUser instanceof com.devtracker.backend.model.User)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in first"));
		}

		com.devtracker.backend.model.User user = (com.devtracker.backend.model.User) sessionUser;

		try {
			int roadmapId = dashboardService.createRoadmap(user.getId(), roadmap);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "Roadmap created successfully", "id", roadmapId));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Could not create roadmap"));
		}
	}
}
