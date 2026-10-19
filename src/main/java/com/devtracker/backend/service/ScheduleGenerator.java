
package com.devtracker.backend.service;

import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.StudyPlanDAO;
import com.devtracker.backend.model.DailyTask;
import com.devtracker.backend.model.Roadmap;
import com.devtracker.backend.model.StudyPlan;
import com.devtracker.backend.model.SubTopic;

import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScheduleGenerator {

	private static final int DAILY_MINUTES = 120;

	private final RoadmapDAO roadmapDAO;
	private final StudyPlanDAO studyPlanDAO;

	public ScheduleGenerator(RoadmapDAO roadmapDAO, StudyPlanDAO studyPlanDAO) {
		this.roadmapDAO = roadmapDAO;
		this.studyPlanDAO = studyPlanDAO;
	}

	/**
	 * Generates or regenerates the standard timetable for a roadmap. Existing
	 * completed-task progress is preserved by StudyPlanDAO.
	 */
	public int generate(int userId, int roadmapId) {

		if (userId <= 0 || roadmapId <= 0) {
			throw new IllegalArgumentException("Valid user and roadmap IDs are required.");
		}

		// Verify that this active roadmap belongs to the current user.
		if (!roadmapDAO.roadmapBelongsToUser(roadmapId, userId)) {
			throw new IllegalArgumentException("Roadmap not found or access denied.");
		}

		Roadmap roadmap = roadmapDAO.getRoadmapById(roadmapId);

		if (roadmap == null) {
			throw new IllegalArgumentException("Roadmap could not be found.");
		}

		if (roadmap.getStartDate() == null || roadmap.getTargetDate() == null) {
			throw new IllegalArgumentException("Set the roadmap start date and target date first.");
		}

		LocalDate today = LocalDate.now();
		LocalDate roadmapStart = roadmap.getStartDate().toLocalDate();
		LocalDate targetDate = roadmap.getTargetDate().toLocalDate();

// Start today if the roadmap has already started.
// Respect a future start date if the roadmap hasn't started yet.
		LocalDate scheduleStart = roadmapStart.isAfter(today) ? roadmapStart : today;

// Keep the original deadline when it is still valid.
// For overdue roadmaps, allow the schedule to continue beyond it.
		LocalDate effectiveTargetDate = targetDate.isBefore(scheduleStart) ? scheduleStart : targetDate;

		int totalDays = Math.toIntExact(ChronoUnit.DAYS.between(scheduleStart, effectiveTargetDate) + 1);

		List<SubTopic> subtopics = roadmapDAO.getAllSubTopics(roadmapId);

		if (subtopics == null || subtopics.isEmpty()) {
			throw new IllegalArgumentException("Add at least one active subtopic before generating a timetable.");
		}

		StudyPlan plan = new StudyPlan();
		plan.setUserId(userId);
		plan.setRoadmapId(roadmapId);
		plan.setTotalDays(totalDays);
		plan.setStartDate(Date.valueOf(scheduleStart));
		plan.setDailyMinutes(DAILY_MINUTES);

		List<DailyTask> tasks = new ArrayList<>();

		int currentDay = 1;
		int currentMinutes = 0;
		int taskOrder = 1;

		// RoadmapDAO already returns subtopics in weight-descending order.
		for (SubTopic subtopic : subtopics) {

			if (subtopic.getEstimatedMinutes() < 0) {
				throw new IllegalArgumentException(
						"A subtopic has an invalid estimated duration: " + subtopic.getName());
			}

			int estimatedMinutes = subtopic.getEstimatedMinutes();

			// Preserve the original V1 day-allocation behavior.
			if (currentMinutes != 0 && currentMinutes + estimatedMinutes > DAILY_MINUTES && currentDay < totalDays) {

				currentDay++;
				currentMinutes = 0;
				taskOrder = 1;
			}

			DailyTask task = new DailyTask();
			task.setSubtopicId(subtopic.getId());
			task.setDayNumber(currentDay);
			task.setTaskOrder(taskOrder);
			task.setWeight(subtopic.getWeight());
			task.setScheduleDate(Date.valueOf(scheduleStart.plusDays(currentDay - 1L)));
			task.setPlannedMinutes(estimatedMinutes);

			boolean completed = subtopic.isCompleted();
			task.setCompleted(completed);
			task.setStatus(completed ? "COMPLETED" : "NOT_STARTED");

			// Retain the original V1 time-slot convention.
			if (taskOrder <= 2) {
				task.setStartTime("09:00");
				task.setEndTime("11:00");
			} else {
				task.setStartTime("16:00");
				task.setEndTime("18:00");
			}

			tasks.add(task);

			currentMinutes += estimatedMinutes;
			taskOrder++;
		}

		// The DAO replaces the plan and tasks atomically.
		// It assigns the new plan ID and preserves prior completions.
		return studyPlanDAO.replacePlanAndTasks(plan, tasks);
	}
}
