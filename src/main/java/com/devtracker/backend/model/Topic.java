package com.devtracker.backend.model;

import java.util.List;

public class Topic {

    private int id;
    private int subjectId;
    private String name;
    private List<SubTopic> subtopics;
    private int progress;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(int subjectId) {
        this.subjectId = subjectId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<SubTopic> getSubtopics() {
        return subtopics;
    }

    public void setSubtopics(List<SubTopic> subtopics) {
        this.subtopics = subtopics;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }
}