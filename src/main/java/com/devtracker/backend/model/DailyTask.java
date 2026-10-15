package com.devtracker.backend.model;

import java.sql.Date;

public class DailyTask {

	private int id;

	private int planId;

	private int subtopicId;

	private int dayNumber;

	private int taskOrder;

	private int weight;

	private Date scheduleDate;

	private int plannedMinutes;

	// temporary keep old system
	private boolean completed;

	// hierarchy info

	private String subjectName;

	private String topicName;

	private String subtopicName;

	// learning info

	private String difficulty;

	private int estimatedMinutes;

	// new task state

	private String status;

	// schedule time

	private String startTime;

	private String endTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getPlanId() {
		return planId;
	}

	public void setPlanId(int planId) {
		this.planId = planId;
	}

	public int getSubtopicId() {
		return subtopicId;
	}

	public void setSubtopicId(int subtopicId) {
		this.subtopicId = subtopicId;
	}

	public int getDayNumber() {
		return dayNumber;
	}

	public void setDayNumber(int dayNumber) {
		this.dayNumber = dayNumber;
	}

	public int getTaskOrder() {
		return taskOrder;
	}

	public void setTaskOrder(int taskOrder) {
		this.taskOrder = taskOrder;
	}

	public int getWeight() {
		return weight;
	}

	public void setWeight(int weight) {
		this.weight = weight;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getSubjectName() {
		return subjectName;
	}

	public void setSubjectName(String subjectName) {
		this.subjectName = subjectName;
	}

	public String getTopicName() {
		return topicName;
	}

	public void setTopicName(String topicName) {
		this.topicName = topicName;
	}

	public String getSubtopicName() {
		return subtopicName;
	}

	public void setSubtopicName(String subtopicName) {
		this.subtopicName = subtopicName;
	}

	public String getDifficulty() {
		return difficulty;
	}

	public void setDifficulty(String difficulty) {
		this.difficulty = difficulty;
	}

	public int getEstimatedMinutes() {
		return estimatedMinutes;
	}

	public void setEstimatedMinutes(int estimatedMinutes) {
		this.estimatedMinutes = estimatedMinutes;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getStartTime() {
		return startTime;
	}

	public void setStartTime(String startTime) {
		this.startTime = startTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public Date getScheduleDate() {

		return scheduleDate;

	}

	public void setScheduleDate(Date scheduleDate) {

		this.scheduleDate = scheduleDate;

	}

	public int getPlannedMinutes() {

		return plannedMinutes;

	}

	public void setPlannedMinutes(int plannedMinutes) {

		this.plannedMinutes = plannedMinutes;

	}
}