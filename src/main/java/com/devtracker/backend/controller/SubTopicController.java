package com.devtracker.backend.controller;

import com.devtracker.backend.model.User;
import com.devtracker.backend.service.SubTopicService;

import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/topics/{topicId}/subtopics")
public class SubTopicController {

	private final SubTopicService subTopicService;

	public SubTopicController(SubTopicService subTopicService) {
		this.subTopicService = subTopicService;
	}

	@PostMapping
	public ResponseEntity<?> addSubTopic(@PathVariable int topicId, @RequestBody Map<String, Object> request,
			HttpSession session) {

		Object sessionUser = session.getAttribute("user");

		if (!(sessionUser instanceof User)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Please log in first"));
		}

		User user = (User) sessionUser;

		String name = request.get("name") instanceof String ? (String) request.get("name") : null;

		String difficulty = request.get("difficulty") instanceof String ? (String) request.get("difficulty") : "Medium";

		int estimatedMinutes = 60;

		Object minutesValue = request.get("minutes");

		if (minutesValue instanceof Number) {
			estimatedMinutes = ((Number) minutesValue).intValue();
		} else if (minutesValue instanceof String) {
			try {
				estimatedMinutes = Integer.parseInt((String) minutesValue);
			} catch (NumberFormatException e) {
				return ResponseEntity.badRequest().body(Map.of("message", "Study time must be a valid number"));
			}
		}

		try {
			int subTopicId = subTopicService.addSubTopic(user.getId(), topicId, name, difficulty, estimatedMinutes);

			return ResponseEntity.status(HttpStatus.CREATED)
					.body(Map.of("message", "Lesson created successfully", "id", subTopicId, "topicId", topicId, "name",
							name.trim(), "difficulty", difficulty, "estimatedMinutes", estimatedMinutes));

		} catch (IllegalArgumentException e) {
			return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));

		} catch (IllegalStateException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("message", "Could not create lesson"));
		}
	}
}