package com.devtracker.backend.controller;

import com.devtracker.backend.dao.RoadmapDAO;
import com.devtracker.backend.dao.SubjectDAO;
import com.devtracker.backend.dao.TopicDAO;
import com.devtracker.backend.dao.SubTopicDAO;

import com.devtracker.backend.model.Roadmap;
import com.devtracker.backend.model.Subject;
import com.devtracker.backend.model.Topic;
import com.devtracker.backend.model.SubTopic;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class OpenAIRoadmapController {

    private final RoadmapDAO roadmapDAO;
    private final SubjectDAO subjectDAO;
    private final TopicDAO topicDAO;
    private final SubTopicDAO subTopicDAO;

    public OpenAIRoadmapController(
            RoadmapDAO roadmapDAO,
            SubjectDAO subjectDAO,
            TopicDAO topicDAO,
            SubTopicDAO subTopicDAO) {

        this.roadmapDAO = roadmapDAO;
        this.subjectDAO = subjectDAO;
        this.topicDAO = topicDAO;
        this.subTopicDAO = subTopicDAO;
    }

    @GetMapping("/openAIRoadmap")
    public String openAIRoadmap(
            @RequestParam("id") int roadmapId,
            Model model) {

        Roadmap roadmap = roadmapDAO.getRoadmapById(roadmapId);

        if (roadmap == null) {
            return "redirect:/dashboard";
        }

        List<Subject> subjects =
                subjectDAO.getSubjectsByRoadmap(roadmapId);

        for (Subject subject : subjects) {

            subject.setProgress(
                    topicDAO.getSubjectProgress(subject.getId()));

            List<Topic> topics =
                    topicDAO.getTopicsBySubject(subject.getId());

            for (Topic topic : topics) {

                topic.setProgress(
                        subTopicDAO.getProgress(topic.getId()));

                List<SubTopic> subTopics =
                        subTopicDAO.getSubTopicsByTopic(topic.getId());

                topic.setSubtopics(subTopics);
            }

            subject.setTopics(topics);
        }

        roadmap.setSubjects(subjects);

        model.addAttribute("roadmap", roadmap);

        return "ai-roadmap-view";
    }
}