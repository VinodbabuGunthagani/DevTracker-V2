
package com.devtracker.backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.StudyPlanDAO;
import com.devtracker.backend.model.User;
import com.devtracker.backend.service.AuthService;
import com.devtracker.backend.service.ScheduleGenerator;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

	private final ScheduleGenerator scheduleGenerator;
	private final AuthService authService;
	private final StudyPlanDAO studyPlanDAO;
	private final RoadmapDAO roadmapDAO;

	public ScheduleController(ScheduleGenerator scheduleGenerator, AuthService authService, StudyPlanDAO studyPlanDAO,
			RoadmapDAO roadmapDAO) {

		this.scheduleGenerator = scheduleGenerator;
		this.authService = authService;
		this.studyPlanDAO = studyPlanDAO;
		this.roadmapDAO = roadmapDAO;
	}

	@PostMapping("/generate")
	public ResponseEntity<?> generateSchedule(@RequestParam int roadmapId, HttpServletRequest request,
			HttpServletResponse response) {

		User user = authService.checkSession(request, response);

		if (user == null) {
			return ResponseEntity.status(401)
					.body(Map.of("success", false, "message", "Please log in to generate a schedule."));
		}

		try {
			int planId = scheduleGenerator.generate(user.getId(), roadmapId);

			return ResponseEntity
					.ok(Map.of("success", true, "message", "Study schedule generated successfully.", "planId", planId));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));

		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "message", "Failed to generate the study schedule."));
		}
	}

	@GetMapping("/tasks")
	public ResponseEntity<?> getTasks(@RequestParam int roadmapId, HttpServletRequest request,
			HttpServletResponse response) {

		User user = authService.checkSession(request, response);

		if (user == null) {
			return ResponseEntity.status(401)
					.body(Map.of("success", false, "message", "Please log in to view your schedule."));
		}

		try {
			if (!roadmapDAO.roadmapBelongsToUser(roadmapId, user.getId())) {
				return ResponseEntity.status(404).body(Map.of("success", false, "message", "Roadmap not found."));
			}

			return ResponseEntity.ok(studyPlanDAO.getTasks(roadmapId));

		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "message", "Failed to load the study schedule."));
		}
	}

	@PatchMapping("/tasks/{taskId}")
	public ResponseEntity<?> updateTaskStatus(@PathVariable int taskId, @RequestParam int roadmapId,
			@RequestParam boolean completed, HttpServletRequest request, HttpServletResponse response) {

		User user = authService.checkSession(request, response);

		if (user == null) {
			return ResponseEntity.status(401)
					.body(Map.of("success", false, "message", "Please log in to update your schedule."));
		}

		try {
			if (!roadmapDAO.roadmapBelongsToUser(roadmapId, user.getId())) {
				return ResponseEntity.status(404).body(Map.of("success", false, "message", "Roadmap not found."));
			}

			if (!studyPlanDAO.taskBelongsToRoadmap(taskId, roadmapId)) {
				return ResponseEntity.status(404)
						.body(Map.of("success", false, "message", "Task not found in this roadmap."));
			}

			studyPlanDAO.updateTaskStatus(taskId, completed);

			return ResponseEntity.ok(
					Map.of("success", true, "message", "Task status updated successfully.", "completed", completed));

		} catch (Exception e) {
			return ResponseEntity.internalServerError()
					.body(Map.of("success", false, "message", "Failed to update task status."));
		}
	}
}
