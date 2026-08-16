package com.devtracker.backend.model;

import java.util.List;

public class Subject {

    private int id;
    private int roadmapId;
    private String name;
    private List<Topic> topics;
    private int progress;

    public List<Topic> getTopics() {
        return topics;
    }

    public void setTopics(List<Topic> topics) {
        this.topics = topics;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRoadmapId() {
        return roadmapId;
    }

    public void setRoadmapId(int roadmapId) {
        this.roadmapId = roadmapId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}