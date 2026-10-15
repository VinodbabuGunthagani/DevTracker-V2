
package com.devtracker.backend.service;

import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.SubjectDAO;

import org.springframework.stereotype.Service;

@Service
public class SubjectService {

	private final RoadmapDAO roadmapDAO;
	private final SubjectDAO subjectDAO;

	public SubjectService(RoadmapDAO roadmapDAO, SubjectDAO subjectDAO) {
		this.roadmapDAO = roadmapDAO;
		this.subjectDAO = subjectDAO;
	}

	public int addSubject(int userId, int roadmapId, String name) {

		if (name == null || name.isBlank()) {
			throw new IllegalArgumentException("Subject name cannot be empty");
		}

		name = name.trim();

		if (name.length() > 150) {
			throw new IllegalArgumentException("Subject name cannot exceed 150 characters");
		}

		if (!roadmapDAO.roadmapBelongsToUser(roadmapId, userId)) {
			throw new IllegalArgumentException("Roadmap not found or access denied");
		}

		int subjectId = subjectDAO.addSubjectReturnId(roadmapId, name);

		if (subjectId <= 0) {
			throw new IllegalStateException("Could not create subject");
		}

		return subjectId;
	}
}
