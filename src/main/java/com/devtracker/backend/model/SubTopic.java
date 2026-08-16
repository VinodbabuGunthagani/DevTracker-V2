package com.devtracker.backend.model;

public class SubTopic {

    private int id;
    private int topicId;

    private String name;

    private boolean completed;

    private String difficulty;

    private int estimatedMinutes;

    private int weight;

    private String aiNotes;
    private String aiLearning;
    private String aiKeywords;

    private String subjectName;
    private String topicName;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
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

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public String getAiNotes() {
        return aiNotes;
    }

    public void setAiNotes(String aiNotes) {
        this.aiNotes = aiNotes;
    }

    public String getAiLearning() {
        return aiLearning;
    }

    public void setAiLearning(String aiLearning) {
        this.aiLearning = aiLearning;
    }

    public String getAiKeywords() {
        return aiKeywords;
    }

    public void setAiKeywords(String aiKeywords) {
        this.aiKeywords = aiKeywords;
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
}