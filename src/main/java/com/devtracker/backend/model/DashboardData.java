package com.devtracker.backend.model;

import java.util.List;

public class DashboardData {

    private String userName;
    private String initials;

    private int total;
    private int completed;
    private int avgProgress;
    private int aiCount;

    private List<RoadmapDashboardData> roadmaps;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getInitials() {
        return initials;
    }

    public void setInitials(String initials) {
        this.initials = initials;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getCompleted() {
        return completed;
    }

    public void setCompleted(int completed) {
        this.completed = completed;
    }

    public int getAvgProgress() {
        return avgProgress;
    }

    public void setAvgProgress(int avgProgress) {
        this.avgProgress = avgProgress;
    }

    public int getAiCount() {
        return aiCount;
    }

    public void setAiCount(int aiCount) {
        this.aiCount = aiCount;
    }

    public List<RoadmapDashboardData> getRoadmaps() {
        return roadmaps;
    }

    public void setRoadmaps(List<RoadmapDashboardData> roadmaps) {
        this.roadmaps = roadmaps;
    }
}