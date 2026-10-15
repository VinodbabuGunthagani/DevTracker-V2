package com.devtracker.backend.model;

import java.time.LocalDateTime;

public class AIJob {

	private int id;
	private int userId;
	private int roadmapId;
	private Integer currentSubtopicId;

	private String jobType;
	private String status;

	private int totalTasks;
	private int completedTasks;
	private int failedTasks;

	private LocalDateTime createdAt;
	private LocalDateTime startedAt;
	private LocalDateTime finishedAt;

	public AIJob() {
	}

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

	public Integer getCurrentSubtopicId() {
		return currentSubtopicId;
	}

	public void setCurrentSubtopicId(Integer currentSubtopicId) {
		this.currentSubtopicId = currentSubtopicId;
	}

	public String getJobType() {
		return jobType;
	}

	public void setJobType(String jobType) {
		this.jobType = jobType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getTotalTasks() {
		return totalTasks;
	}

	public void setTotalTasks(int totalTasks) {
		this.totalTasks = totalTasks;
	}

	public int getCompletedTasks() {
		return completedTasks;
	}

	public void setCompletedTasks(int completedTasks) {
		this.completedTasks = completedTasks;
	}

	public int getFailedTasks() {
		return failedTasks;
	}

	public void setFailedTasks(int failedTasks) {
		this.failedTasks = failedTasks;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}

	public LocalDateTime getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(LocalDateTime startedAt) {
		this.startedAt = startedAt;
	}

	public LocalDateTime getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(LocalDateTime finishedAt) {
		this.finishedAt = finishedAt;
	}

}