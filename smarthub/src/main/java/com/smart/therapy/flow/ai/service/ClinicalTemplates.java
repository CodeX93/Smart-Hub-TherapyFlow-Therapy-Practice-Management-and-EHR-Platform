package com.smart.therapy.flow.ai.service;

import java.util.HashMap;
import java.util.Map;

/**
 * Clinical Content Library - Connected templates for intelligent field suggestions
 * Mirrors the structure from ClientHubAI/server/ai/openai.ts
 */
public class ClinicalTemplates {

    public static Map<String, Object> getAllTemplates() {
        Map<String, Object> templates = new HashMap<>();
        
        // Cognitive Behavioral Therapy template
        templates.put("cognitive_behavioral", createCbtTemplate());
        
        // Trauma-Focused Therapy template
        templates.put("trauma_focused", createTraumaFocusedTemplate());
        
        // Mindfulness-Based Therapy template
        templates.put("mindfulness_based", createMindfulnessBasedTemplate());
        
        return templates;
    }

    private static Map<String, Object> createCbtTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("name", "Cognitive Behavioral Therapy (CBT)");
        template.put("description", "Focus on thought patterns, cognitive restructuring, and behavioral interventions");
        
        // Session Focus Options
        Map<String, Object> sessionFocusOptions = new HashMap<>();
        sessionFocusOptions.put("anxiety_management", createOption(
            "Anxiety Management",
            "Explored cognitive patterns related to anxiety. Identified negative thought cycles and worked on cognitive restructuring techniques for anxiety reduction.",
            Map.of("symptoms", "anxiety_symptoms", "intervention", "cbt_anxiety_interventions", 
                   "progress", "anxiety_progress", "recommendations", "anxiety_recommendations")
        ));
        sessionFocusOptions.put("depression_treatment", createOption(
            "Depression Treatment",
            "Addressed depressive thought patterns and behavioral activation. Focused on cognitive restructuring for negative self-talk and mood improvement.",
            Map.of("symptoms", "depression_symptoms", "intervention", "cbt_depression_interventions",
                   "progress", "depression_progress", "recommendations", "depression_recommendations")
        ));
        sessionFocusOptions.put("trauma_processing", createOption(
            "Trauma Processing",
            "Explored trauma-related cognitive distortions and safety mechanisms. Worked on processing traumatic memories using CBT techniques.",
            Map.of("symptoms", "trauma_symptoms", "intervention", "cbt_trauma_interventions",
                   "progress", "trauma_progress", "recommendations", "trauma_recommendations")
        ));
        template.put("sessionFocusOptions", sessionFocusOptions);
        
        // Symptoms Options
        Map<String, Object> symptomsOptions = new HashMap<>();
        symptomsOptions.put("anxiety_symptoms", createSimpleOption(
            "Anxiety Symptoms",
            "Client presented with elevated anxiety including physical symptoms (racing heart, sweating), cognitive symptoms (catastrophic thinking, worry), and behavioral avoidance patterns."
        ));
        symptomsOptions.put("depression_symptoms", createSimpleOption(
            "Depression Symptoms",
            "Client reported depressive symptoms including low mood, decreased energy, negative self-talk, sleep disturbances, and reduced interest in activities."
        ));
        symptomsOptions.put("trauma_symptoms", createSimpleOption(
            "Trauma Symptoms",
            "Trauma symptoms included hypervigilance, intrusive thoughts, emotional numbing, dissociative episodes, and avoidance of trauma-related triggers."
        ));
        template.put("symptomsOptions", symptomsOptions);
        
        // Intervention Options
        Map<String, Object> interventionOptions = new HashMap<>();
        interventionOptions.put("cbt_anxiety_interventions", createSimpleOption(
            "CBT Anxiety Interventions",
            "Applied cognitive restructuring for catastrophic thoughts, taught breathing techniques, practiced thought challenging worksheets, and implemented gradual exposure planning."
        ));
        interventionOptions.put("cbt_depression_interventions", createSimpleOption(
            "CBT Depression Interventions",
            "Used behavioral activation techniques, challenged negative self-statements, implemented activity scheduling, and practiced cognitive reframing exercises."
        ));
        interventionOptions.put("cbt_trauma_interventions", createSimpleOption(
            "CBT Trauma Interventions",
            "Utilized cognitive processing techniques, implemented grounding exercises, practiced trauma-focused cognitive restructuring, and worked on safety planning."
        ));
        template.put("interventionOptions", interventionOptions);
        
        // Progress Options
        Map<String, Object> progressOptions = new HashMap<>();
        progressOptions.put("anxiety_progress", createSimpleOption(
            "Anxiety Progress",
            "Client demonstrated improved ability to identify and challenge anxious thoughts. Reduced avoidance behaviors and increased use of coping strategies in anxiety-provoking situations."
        ));
        progressOptions.put("depression_progress", createSimpleOption(
            "Depression Progress",
            "Client showed increased engagement in pleasurable activities, improved mood regulation, and better recognition of negative thought patterns with successful challenging."
        ));
        progressOptions.put("trauma_progress", createSimpleOption(
            "Trauma Progress",
            "Client exhibited decreased trauma reactivity, improved grounding skills, and increased capacity to discuss traumatic experiences without overwhelming distress."
        ));
        template.put("progressOptions", progressOptions);
        
        // Recommendations Options
        Map<String, Object> recommendationsOptions = new HashMap<>();
        recommendationsOptions.put("anxiety_recommendations", createSimpleOption(
            "Anxiety Recommendations",
            "Continue CBT anxiety protocol with daily thought records, practice exposure exercises between sessions, implement relaxation techniques, and monitor anxiety levels using rating scales."
        ));
        recommendationsOptions.put("depression_recommendations", createSimpleOption(
            "Depression Recommendations",
            "Maintain behavioral activation schedule, continue mood monitoring, practice cognitive restructuring techniques daily, and increase social engagement activities."
        ));
        recommendationsOptions.put("trauma_recommendations", createSimpleOption(
            "Trauma Recommendations",
            "Continue trauma-focused CBT sessions, practice grounding techniques daily, maintain safety planning, and gradually increase trauma processing work as tolerated."
        ));
        template.put("recommendationsOptions", recommendationsOptions);
        
        return template;
    }

    private static Map<String, Object> createTraumaFocusedTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("name", "Trauma-Focused Therapy");
        template.put("description", "Specialized approach for trauma processing and PTSD treatment");
        
        // Session Focus Options
        Map<String, Object> sessionFocusOptions = new HashMap<>();
        sessionFocusOptions.put("trauma_stabilization", createOption(
            "Trauma Stabilization",
            "Focused on safety, stabilization, and developing coping resources. Worked on building distress tolerance and emotional regulation skills.",
            Map.of("symptoms", "ptsd_symptoms", "intervention", "stabilization_interventions",
                   "progress", "stabilization_progress", "recommendations", "stabilization_recommendations")
        ));
        sessionFocusOptions.put("trauma_processing", createOption(
            "Trauma Processing",
            "Engaged in direct trauma processing work. Focused on integrating traumatic memories and reducing trauma-related distress.",
            Map.of("symptoms", "processing_symptoms", "intervention", "processing_interventions",
                   "progress", "processing_progress", "recommendations", "processing_recommendations")
        ));
        template.put("sessionFocusOptions", sessionFocusOptions);
        
        // Symptoms, Interventions, Progress, Recommendations (simplified for brevity)
        template.put("symptomsOptions", Map.of(
            "ptsd_symptoms", createSimpleOption("PTSD Symptoms", 
                "Client presented with PTSD symptoms including intrusive memories, nightmares, hypervigilance, emotional numbing, and avoidance of trauma reminders."),
            "processing_symptoms", createSimpleOption("Processing Symptoms",
                "During processing work, client experienced manageable activation including emotional flooding, dissociative episodes, and somatic trauma responses.")
        ));
        
        template.put("interventionOptions", Map.of(
            "stabilization_interventions", createSimpleOption("Stabilization Interventions",
                "Implemented grounding techniques, taught emotional regulation skills, practiced breathing exercises, and established safety planning protocols."),
            "processing_interventions", createSimpleOption("Processing Interventions",
                "Utilized EMDR bilateral stimulation, implemented CPT cognitive processing techniques, and guided trauma narrative development with titrated exposure.")
        ));
        
        template.put("progressOptions", Map.of(
            "stabilization_progress", createSimpleOption("Stabilization Progress",
                "Client demonstrated improved emotional regulation, decreased dissociative episodes, and increased capacity to use grounding techniques effectively."),
            "processing_progress", createSimpleOption("Processing Progress",
                "Client showed reduced emotional charge around traumatic memories, improved narrative coherence, and decreased avoidance of trauma-related triggers.")
        ));
        
        template.put("recommendationsOptions", Map.of(
            "stabilization_recommendations", createSimpleOption("Stabilization Recommendations",
                "Continue stabilization phase work, practice grounding techniques daily, maintain safety planning, and monitor dissociative symptoms closely."),
            "processing_recommendations", createSimpleOption("Processing Recommendations",
                "Continue trauma processing sessions, practice self-care between sessions, monitor trauma symptoms, and prepare for integration phase work.")
        ));
        
        return template;
    }

    private static Map<String, Object> createMindfulnessBasedTemplate() {
        Map<String, Object> template = new HashMap<>();
        template.put("name", "Mindfulness-Based Therapy");
        template.put("description", "Integration of mindfulness practices with therapeutic interventions");
        
        // Session Focus Options
        Map<String, Object> sessionFocusOptions = new HashMap<>();
        sessionFocusOptions.put("mindfulness_training", createOption(
            "Mindfulness Training",
            "Practiced core mindfulness techniques including breath awareness, body scanning, and present-moment attention skills.",
            Map.of("symptoms", "mindfulness_symptoms", "intervention", "mindfulness_interventions",
                   "progress", "mindfulness_progress", "recommendations", "mindfulness_recommendations")
        ));
        sessionFocusOptions.put("emotional_regulation", createOption(
            "Emotional Regulation",
            "Focused on using mindfulness for emotional regulation, distress tolerance, and reducing emotional reactivity.",
            Map.of("symptoms", "emotional_symptoms", "intervention", "regulation_interventions",
                   "progress", "regulation_progress", "recommendations", "regulation_recommendations")
        ));
        template.put("sessionFocusOptions", sessionFocusOptions);
        
        // Simplified options for other fields
        template.put("symptomsOptions", Map.of(
            "mindfulness_symptoms", createSimpleOption("Mindfulness-Related Symptoms",
                "Client reported difficulty with present-moment awareness, mind wandering, rumination patterns, and challenges with acceptance of current experiences."),
            "emotional_symptoms", createSimpleOption("Emotional Dysregulation",
                "Client experienced emotional overwhelm, difficulty managing intense emotions, reactive responses to triggers, and challenges with distress tolerance.")
        ));
        
        template.put("interventionOptions", Map.of(
            "mindfulness_interventions", createSimpleOption("Mindfulness Interventions",
                "Guided breathing meditation, body scan exercises, mindful movement practices, and awareness of thoughts and emotions without judgment."),
            "regulation_interventions", createSimpleOption("Emotional Regulation Interventions",
                "Taught STOP technique, implemented loving-kindness meditation, practiced distress tolerance skills, and used mindful self-compassion exercises.")
        ));
        
        template.put("progressOptions", Map.of(
            "mindfulness_progress", createSimpleOption("Mindfulness Progress",
                "Client demonstrated increased present-moment awareness, improved ability to observe thoughts without attachment, and reduced rumination patterns."),
            "regulation_progress", createSimpleOption("Emotional Regulation Progress",
                "Client showed improved emotional regulation skills, increased distress tolerance, and better ability to respond rather than react to triggers.")
        ));
        
        template.put("recommendationsOptions", Map.of(
            "mindfulness_recommendations", createSimpleOption("Mindfulness Recommendations",
                "Continue daily mindfulness practice, use mindfulness apps for guided sessions, integrate mindful moments throughout the day, and maintain meditation journal."),
            "regulation_recommendations", createSimpleOption("Emotional Regulation Recommendations",
                "Practice emotional regulation techniques daily, use mindfulness during triggering situations, continue self-compassion exercises, and monitor emotional patterns.")
        ));
        
        return template;
    }

    private static Map<String, Object> createOption(String label, String template, Map<String, String> connects) {
        Map<String, Object> option = new HashMap<>();
        option.put("label", label);
        option.put("template", template);
        option.put("connects", connects);
        return option;
    }

    private static Map<String, Object> createSimpleOption(String label, String template) {
        Map<String, Object> option = new HashMap<>();
        option.put("label", label);
        option.put("template", template);
        return option;
    }
}

