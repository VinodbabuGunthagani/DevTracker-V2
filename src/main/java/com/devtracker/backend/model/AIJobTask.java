package com.devtracker.backend.model;

import java.time.LocalDateTime;

public class AIJobTask {

	private int id;
	private int jobId;
	private int subtopicId;

	private String status;
	private int attempts;
	private String errorMessage;

	private LocalDateTime createdAt;
	private LocalDateTime startedAt;
	private LocalDateTime completedAt;
	private String subjectName;
	private String topicName;
	private String subtopicName;

	public AIJobTask() {
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getJobId() {
		return jobId;
	}

	public void setJobId(int jobId) {
		this.jobId = jobId;
	}

	public int getSubtopicId() {
		return subtopicId;
	}

	public void setSubtopicId(int subtopicId) {
		this.subtopicId = subtopicId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getAttempts() {
		return attempts;
	}

	public void setAttempts(int attempts) {
		this.attempts = attempts;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
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

	public LocalDateTime getCompletedAt() {
		return completedAt;
	}

	public void setCompletedAt(LocalDateTime completedAt) {
		this.completedAt = completedAt;
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
}