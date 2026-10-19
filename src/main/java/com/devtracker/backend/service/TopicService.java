package com.devtracker.backend.service;

import org.springframework.stereotype.Service;

import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.SubjectDAO;
import com.devtracker.backend.dao.TopicDAO;
import com.devtracker.backend.model.Subject;

@Service
public class TopicService {

	private final TopicDAO topicDAO;
	private final SubjectDAO subjectDAO;
	private final RoadmapDAO roadmapDAO;

	public TopicService(TopicDAO topicDAO, SubjectDAO subjectDAO, RoadmapDAO roadmapDAO) {
		this.topicDAO = topicDAO;
		this.subjectDAO = subjectDAO;
		this.roadmapDAO = roadmapDAO;
	}

	public int addTopic(int userId, int subjectId, String name) {

		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Topic name is required");
		}

		String topicName = name.trim();

		if (topicName.length() > 200) {
			throw new IllegalArgumentException("Topic name cannot exceed 200 characters");
		}

		Subject subject = subjectDAO.getSubjectById(subjectId);

		if (subject == null || subject.getRoadmapId() <= 0) {
			throw new IllegalArgumentException("Subject not found");
		}

		int roadmapId = subject.getRoadmapId();

		if (!roadmapDAO.roadmapBelongsToUser(roadmapId, userId)) {
			throw new IllegalArgumentException("You do not have permission to modify this subject");
		}

		int topicId = topicDAO.addTopicReturnId(subjectId, topicName);

		if (topicId <= 0) {
			throw new IllegalStateException("Could not create topic");
		}

		return topicId;
	}
}