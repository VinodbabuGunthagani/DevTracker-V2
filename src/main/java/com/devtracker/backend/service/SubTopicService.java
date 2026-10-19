package com.devtracker.backend.service;

import org.springframework.stereotype.Service;

import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.SubjectDAO;
import com.devtracker.backend.dao.TopicDAO;
import com.devtracker.backend.dao.SubTopicDAO;
import com.devtracker.backend.model.Subject;
import com.devtracker.backend.model.SubTopic;
import com.devtracker.backend.model.Topic;

@Service
public class SubTopicService {

	private final SubTopicDAO subTopicDAO;
	private final TopicDAO topicDAO;
	private final SubjectDAO subjectDAO;
	private final RoadmapDAO roadmapDAO;

	public SubTopicService(SubTopicDAO subTopicDAO, TopicDAO topicDAO, SubjectDAO subjectDAO, RoadmapDAO roadmapDAO) {
		this.subTopicDAO = subTopicDAO;
		this.topicDAO = topicDAO;
		this.subjectDAO = subjectDAO;
		this.roadmapDAO = roadmapDAO;
	}

	public int addSubTopic(int userId, int topicId, String name, String difficulty, int estimatedMinutes) {

		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Lesson name is required");
		}

		String lessonName = name.trim();

		if (lessonName.length() > 200) {
			throw new IllegalArgumentException("Lesson name cannot exceed 200 characters");
		}

		if (estimatedMinutes < 1 || estimatedMinutes > 1440) {
			throw new IllegalArgumentException("Study time must be between 1 and 1440 minutes");
		}

		if (difficulty == null) {
			difficulty = "Medium";
		}

		difficulty = difficulty.trim();

		if (!difficulty.equalsIgnoreCase("Easy") && !difficulty.equalsIgnoreCase("Medium")
				&& !difficulty.equalsIgnoreCase("Hard")) {
			throw new IllegalArgumentException("Difficulty must be Easy, Medium, or Hard");
		}

		Topic topic = topicDAO.getTopicById(topicId);

		if (topic == null || topic.getSubjectId() <= 0) {
			throw new IllegalArgumentException("Topic not found");
		}

		Subject subject = subjectDAO.getSubjectById(topic.getSubjectId());

		if (subject == null || subject.getRoadmapId() <= 0) {
			throw new IllegalArgumentException("Subject not found");
		}

		int roadmapId = subject.getRoadmapId();

		if (!roadmapDAO.roadmapBelongsToUser(roadmapId, userId)) {
			throw new IllegalArgumentException("You do not have permission to modify this lesson");
		}

		SubTopic subTopic = new SubTopic();
		subTopic.setTopicId(topicId);
		subTopic.setName(lessonName);
		subTopic.setDifficulty(difficulty.substring(0, 1).toUpperCase() + difficulty.substring(1).toLowerCase());
		subTopic.setEstimatedMinutes(estimatedMinutes);
		subTopic.setWeight(1);

		int subTopicId = subTopicDAO.addSubTopicReturnId(subTopic);

		if (subTopicId <= 0) {
			throw new IllegalStateException("Could not create lesson");
		}

		return subTopicId;
	}
}