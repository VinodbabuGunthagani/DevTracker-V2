package com.devtracker.backend.model;

public class LearningResource {

	private int id;

	private int subtopicId;

	private String videoId;

	private String title;

	private String channelName;

	private String thumbnail;

	private int rankNo;

	private String reason;

	public int getId() {

		return id;

	}

	public void setId(int id) {

		this.id = id;

	}

	public int getSubtopicId() {

		return subtopicId;

	}

	public void setSubtopicId(int subtopicId) {

		this.subtopicId = subtopicId;

	}

	public String getVideoId() {

		return videoId;

	}

	public void setVideoId(String videoId) {

		this.videoId = videoId;

	}

	public String getTitle() {

		return title;

	}

	public void setTitle(String title) {

		this.title = title;

	}

	public String getChannelName() {

		return channelName;

	}

	public void setChannelName(String channelName) {

		this.channelName = channelName;

	}

	public String getThumbnail() {

		return thumbnail;

	}

	public void setThumbnail(String thumbnail) {

		this.thumbnail = thumbnail;

	}

	public int getRankNo() {

		return rankNo;

	}

	public void setRankNo(int rankNo) {

		this.rankNo = rankNo;

	}

	public String getReason() {

		return reason;

	}

	public void setReason(String reason) {

		this.reason = reason;

	}

}