package com.devtracker.backend.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.devtracker.backend.dao.AITimetableDAO;
import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.SubjectDAO;
import com.devtracker.backend.model.DashboardData;
import com.devtracker.backend.model.Roadmap;
import com.devtracker.backend.model.RoadmapDashboardData;

@Service
public class DashboardService {

	private final RoadmapDAO roadmapDAO;
	private final SubjectDAO subjectDAO;
	private final AITimetableDAO aiTimetableDAO;

	public DashboardService(RoadmapDAO roadmapDAO, SubjectDAO subjectDAO, AITimetableDAO aiTimetableDAO) {

		this.roadmapDAO = roadmapDAO;
		this.subjectDAO = subjectDAO;
		this.aiTimetableDAO = aiTimetableDAO;
	}

	public DashboardData getDashboard(int userId, String userName) {

		List<Roadmap> roadmaps = roadmapDAO.getRoadmaps(userId);

		int total = roadmaps.size();
		int completed = 0;
		int totalProgress = 0;
		int aiCount = 0;

		List<RoadmapDashboardData> roadmapData = new ArrayList<>();

		for (Roadmap roadmap : roadmaps) {

			int progress;

			if (roadmap.isAi()) {
				progress = aiTimetableDAO.getAIProgress(roadmap.getId());
				aiCount++;
			} else {
				progress = subjectDAO.getRoadmapProgress(roadmap.getId());
			}

			totalProgress += progress;

			if (progress >= 100) {
				completed++;
			}

			RoadmapDashboardData data = new RoadmapDashboardData();

			data.setId(roadmap.getId());
			data.setTitle(roadmap.getTitle());
			data.setDescription(roadmap.getDescription());
			data.setStartDate(roadmap.getStartDate());
			data.setTargetDate(roadmap.getTargetDate());
			data.setAi(roadmap.isAi());
			data.setProgress(progress);

			roadmapData.add(data);
		}

		int avgProgress = total > 0 ? totalProgress / total : 0;

		DashboardData dashboard = new DashboardData();

		dashboard.setUserName(userName);
		dashboard.setInitials(createInitials(userName));
		dashboard.setTotal(total);
		dashboard.setCompleted(completed);
		dashboard.setAvgProgress(avgProgress);
		dashboard.setAiCount(aiCount);
		dashboard.setRoadmaps(roadmapData);

		return dashboard;
	}

	private String createInitials(String name) {

		if (name == null || name.isBlank()) {
			return "";
		}

		String[] parts = name.trim().split("\\s+");

		StringBuilder initials = new StringBuilder();

		initials.append(Character.toUpperCase(parts[0].charAt(0)));

		if (parts.length > 1) {
			initials.append(Character.toUpperCase(parts[parts.length - 1].charAt(0)));
		}

		return initials.toString();
	}

	public int createRoadmap(int userId, Roadmap roadmap) {

		if (roadmap == null || roadmap.getTitle() == null || roadmap.getTitle().trim().isEmpty()) {
			throw new IllegalArgumentException("Roadmap title is required");
		}

		String title = roadmap.getTitle().trim();

		if (title.length() > 150) {
			throw new IllegalArgumentException("Roadmap title must be 150 characters or fewer");
		}

		java.sql.Date startDate = roadmap.getStartDate();
		java.sql.Date targetDate = roadmap.getTargetDate();

		if (startDate != null && targetDate != null && targetDate.before(startDate)) {
			throw new IllegalArgumentException("Target date cannot be before the start date");
		}

		roadmap.setUserId(userId);
		roadmap.setTitle(title);

		int roadmapId = roadmapDAO.createRoadmap(roadmap);

		if (roadmapId <= 0) {
			throw new IllegalStateException("Could not create roadmap");
		}

		return roadmapId;
	}

}