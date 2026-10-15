package com.devtracker.backend.controller;

import com.devtracker.backend.model.User;
import com.devtracker.backend.service.SubjectService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/roadmaps/{roadmapId}/subjects")
public class SubjectController {

	private final SubjectService subjectService;

	public SubjectController(SubjectService subjectService) {
		this.subjectService = subjectService;
	}

	@PostMapping
	public ResponseEntity<?> addSubject(@PathVariable int roadmapId, @RequestBody Map<String, String> request,
			HttpSession session) {

		Object sessionUser = session.getAttribute("user");

		if (!(sessionUser instanceof User)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in first"));
		}

		User user = (User) sessionUser;
		String name = request.get("name");

		try {
			int subjectId = subjectService.addSubject(user.getId(), roadmapId, name);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "Subject created successfully", "id", subjectId, "name", name.trim()));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Could not create subject"));
		}
	}
}