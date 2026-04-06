package com.careerai.builder.ai.provider;

import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import org.springframework.stereotype.Component;

@Component
public class AiPromptFactory {

    public String buildCvSystemPrompt() {
        return """
                You are an AI career analysis engine.
                Return valid JSON only.
                Do not add markdown fences.
                Extract practical professional information from CV text.
                The JSON shape must be:
                {
                  "summary": "string",
                  "parsedContent": "string",
                  "skills": [
                    {
                      "skillName": "string",
                      "category": "string",
                      "confidenceScore": 0.0,
                      "yearsOfExperience": 0
                    }
                  ]
                }
                Keep skills normalized and concise.
                """;
    }

    public String buildCvUserPrompt(CvAnalysisRequest request) {
        return """
                Analyze this CV text and extract a structured professional profile.

                File name:
                %s

                CV text:
                %s
                """.formatted(request.getFileName(), request.getRawText());
    }

    public String buildRoadmapSystemPrompt() {
        return """
                You are an AI career roadmap planner.
                Return valid JSON only.
                Do not add markdown fences.
                Create a practical, realistic roadmap.
                The JSON shape must be:
                {
                  "targetTitle": "string",
                  "steps": [
                    {
                      "title": "string",
                      "description": "string",
                      "durationDays": 0,
                      "resources": ["string"]
                    }
                  ]
                }
                Steps should be concrete, ordered, and focused on skill gaps.
                """;
    }

    public String buildRoadmapUserPrompt(RoadmapGenerationRequest request) {
        return """
                Build a roadmap for this candidate.

                Target title:
                %s

                CV summary:
                %s

                Current skills:
                %s

                Missing skills:
                %s
                """.formatted(
                request.getTargetTitle(),
                request.getCvSummary(),
                String.join(", ", request.getCurrentSkills()),
                String.join(", ", request.getMissingSkills()));
    }
}
