package com.devtracker.backend.controller;

import com.devtracker.backend.model.User;
import com.devtracker.backend.service.TopicService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subjects/{subjectId}/topics")
public class TopicController {

	private final TopicService topicService;

	public TopicController(TopicService topicService) {
		this.topicService = topicService;
	}

	@PostMapping
	public ResponseEntity<?> addTopic(@PathVariable int subjectId, @RequestBody Map<String, String> request,
			HttpSession session) {

		Object sessionUser = session.getAttribute("user");

		if (!(sessionUser instanceof User)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in first"));
		}

		User user = (User) sessionUser;
		String name = request.get("name");

		try {
			int topicId = topicService.addTopic(user.getId(), subjectId, name);

			return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Topic created successfully", "id",
					topicId, "subjectId", subjectId, "name", name.trim()));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Could not create topic"));
		}
	}
}