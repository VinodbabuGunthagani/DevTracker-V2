package com.devtracker.backend.model;

public class AIStudyPlan {

	private int id;

	private int userId;

	private int roadmapId;

	private int totalDays;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public int getRoadmapId() {
		return roadmapId;
	}

	public void setRoadmapId(int roadmapId) {
		this.roadmapId = roadmapId;
	}

	public int getTotalDays() {
		return totalDays;
	}

	public void setTotalDays(int totalDays) {
		this.totalDays = totalDays;
	}

}