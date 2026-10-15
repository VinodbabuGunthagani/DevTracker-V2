package com.devtracker.backend.model;

import java.sql.Date;

public class StudyPlan {

	private int id;

	private int userId;

	private int roadmapId;

	private int totalDays;

	private Date startDate;

	private int dailyMinutes;

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

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public int getDailyMinutes() {

		return dailyMinutes;

	}

	public void setDailyMinutes(int dailyMinutes) {

		this.dailyMinutes = dailyMinutes;

	}

}