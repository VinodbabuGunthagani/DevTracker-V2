package com.devtracker.backend.model;

public class AIDailyTask {

	private int id;

	private int planId;

	private int subtopicId;

	private int dayNumber;

	private int taskOrder;

	private boolean completed;

	private String subtopicName;

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

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public String getSubtopicName() {
		return subtopicName;
	}

	public void setSubtopicName(String subtopicName) {
		this.subtopicName = subtopicName;
	}

}