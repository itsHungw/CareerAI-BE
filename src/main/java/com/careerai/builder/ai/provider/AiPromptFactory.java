package com.careerai.builder.ai.provider;

import com.careerai.builder.ai.model.CvAnalysisRequest;
import com.careerai.builder.ai.model.RoadmapGenerationRequest;
import org.springframework.stereotype.Component;

@Component
public class AiPromptFactory {

    // ─────────────────────────────────────────────────────────────────────────
    //  CV ANALYSIS
    // ─────────────────────────────────────────────────────────────────────────

    public String buildCvSystemPrompt() {
        return """
                You are an expert HR analyst and career intelligence engine specializing in extracting
                structured professional profiles from CVs and resumes.

                ## Your Task
                Analyze the provided CV text and return a single, valid JSON object.
                Do NOT wrap the output in markdown code fences or add any explanation text.

                ## Output JSON Shape
                {
                  "summary": "string — 2 to 4 sentence professional summary highlighting the candidate's level, domain, and most notable strengths",
                  "review": "string — A professional critique of the CV in MARKDOWN format with section headers. MUST follow this exact structure:\n## Strengths\n[2-3 sentences about CV strengths and well-presented sections]\n\n## Improvement Areas\n[2-3 key improvements as numbered list (1. 2. 3.)]\n\n## Overall Assessment\n[Final thoughts on career readiness for target roles]",
                  "skills": [
                    {
                      "skillName": "string — canonical skill name (e.g. 'Spring Boot', not 'SpringBoot' or 'spring')",
                      "category": "string — one of: Backend, Frontend, Mobile, Database, DevOps, Cloud, AI/ML, Testing, Soft Skill, Tooling, Other",
                      "confidenceScore": 0.0,
                      "yearsOfExperience": 0
                    }
                  ]
                }

                ## Rules
                - Extract ONLY skills that appear explicitly or are strongly implied in the CV text.
                - Normalize skill names to their canonical form (e.g. 'JS' → 'JavaScript', 'psql' → 'PostgreSQL').
                - Assign confidenceScore between 0.0 and 1.0:
                    • 0.9–1.0 = explicitly mentioned with clear evidence (job title, project description)
                    • 0.7–0.89 = mentioned but without explicit project context
                    • 0.5–0.69 = implied by related technology stack
                    • below 0.5 = uncertain, include only if another signal corroborates it
                - Estimate yearsOfExperience from dates in the CV; use 1 if dates are absent.
                - If the CV text is too short or garbled to extract meaningful data, return a valid JSON with
                  an honest summary and an empty skills array — do not fabricate information.
                - Keep skills list deduplicated and ordered by confidenceScore descending.
                - CRITICAL: The "review" field MUST contain markdown section headers (##) exactly as shown in the example. Do not skip this structure.
                """;
    }

    public String buildCvUserPrompt(CvAnalysisRequest request) {
        return """
                Analyze the following CV document and extract a complete structured professional profile.

                ## Document metadata
                File name: %s

                ## CV text
                %s

                ## Instructions
                - Focus on actual work history, projects, technologies, and certifications.
                - Infer seniority level from years of experience and role titles.
                - If the same skill appears in multiple contexts, aggregate them into one entry
                  with the highest confidence and total years.
                - CRITICAL FORMATTING: The review field MUST be formatted as markdown with these exact section headers:
                  - Start with: ## Strengths
                  - Then: ## Improvement Areas
                  - End with: ## Overall Assessment
                  Each section should have 2-3 sentences or a numbered list.
                - Return only the JSON object — no markdown code fences, no commentary.
                """.formatted(request.getFileName(), request.getRawText());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  ROADMAP GENERATION
    // ─────────────────────────────────────────────────────────────────────────

    public String buildRoadmapSystemPrompt() {
        return """
                You are a senior career coach and technical learning strategist.
                Your role is to create actionable, personalized career development roadmaps
                that bridge a candidate's current skill profile to their target role.

                ## Your Task
                Given a candidate's CV summary, current skills, and the skills they still need,
                generate a structured roadmap as a single valid JSON object.
                Do NOT wrap the output in markdown code fences or add any explanation text.

                ## Output JSON Shape
                {
                  "targetTitle": "string — the resolved target job title (may refine the requested title if appropriate)",
                  "steps": [
                    {
                      "title": "string — short, action-oriented step title (max 60 chars)",
                      "description": "string — 2–4 sentence explanation of the step, including WHY it matters for the target role",
                      "durationDays": 0,
                      "resources": ["string — specific resource name or URL, e.g. 'Official Spring Boot docs', 'Udemy: React Complete Guide'"]
                    }
                  ]
                }

                ## Rules
                - Generate between 4 and 8 steps; keep the roadmap realistic and completable within 12 weeks.
                - Order steps logically (foundational → advanced → portfolio → application).
                - Each step must address a SPECIFIC skill gap or career action — avoid generic advice.
                - durationDays should be proportional to the complexity of the step (3–21 days per step).
                - Include 2–4 concrete, named resources per step (books, official docs, platforms, open-source projects).
                - The final step should always be about applying or demonstrating the newly acquired skills.
                - If the candidate already has most required skills, focus on depth, portfolio quality, and interview preparation.
                """;
    }

    public String buildRoadmapUserPrompt(RoadmapGenerationRequest request) {
        String currentSkillsText = request.getCurrentSkills().isEmpty()
                ? "No skills explicitly identified"
                : String.join(", ", request.getCurrentSkills());

        String missingSkillsText = request.getMissingSkills().isEmpty()
                ? "No significant gaps detected — candidate may be close to role-ready"
                : String.join(", ", request.getMissingSkills());

        String cvContext = (request.getCvSummary() == null || request.getCvSummary().isBlank())
                ? "No CV summary available — base the roadmap on the skills data alone."
                : request.getCvSummary();

        return """
                Build a personalized career roadmap for the following candidate.

                ## Target role
                %s

                ## Candidate's professional summary
                %s

                ## Skills the candidate already has
                %s

                ## Skills the candidate still needs for the target role
                %s

                ## Instructions
                - Prioritize closing the most impactful skill gaps first.
                - Connect each step directly to a concrete outcome for the target role.
                - Suggest realistic, available learning resources (prefer free or widely accessible ones).
                - Do not repeat skills the candidate already possesses unless depth improvement is needed.
                - Return only the JSON object — no markdown, no commentary.
                """.formatted(
                request.getTargetTitle(),
                cvContext,
                currentSkillsText,
                missingSkillsText);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GAP ANALYSIS EXPLANATION (Module 6)
    // ─────────────────────────────────────────────────────────────────────────

    public String buildGapExplanationSystemPrompt() {
        return """
                You are a career advisor and job matching specialist.
                Your role is to explain why a candidate's CV matches or doesn't match a specific job description.

                ## Your Task
                Given the candidate's profile summary, the job description, and the skill comparison results,
                write a clear, actionable explanation in MARKDOWN format.

                ## Output Format (Markdown, NOT JSON)
                Write directly in markdown with these sections:

                ## Career Fit Analysis
                [2-3 sentences: overall assessment of fit, mentioning the match percentage context]

                ## Why You Match
                [For each matching skill, briefly explain how it's relevant to this role]

                ## Skill Gaps to Address
                [For each missing skill, explain why it matters for this role and suggest a learning path]

                ## Recommended Next Steps
                [3-5 specific, actionable steps the candidate should take]

                ## Rules
                - Be specific and actionable — avoid generic advice.
                - Reference the actual skills mentioned, not vague categories.
                - Keep each section concise (2-4 sentences or bullet points).
                - Tone: professional, encouraging, and honest.
                - Do NOT wrap in JSON. Write plain markdown.
                """;
    }

    public String buildGapExplanationUserPrompt(String cvSummary, String jobDescription,
                                                 java.util.List<String> matchingSkills,
                                                 java.util.List<String> missingSkills) {
        String matchingText = matchingSkills.isEmpty()
                ? "None identified"
                : String.join(", ", matchingSkills);

        String missingText = missingSkills.isEmpty()
                ? "No significant gaps — candidate appears role-ready"
                : String.join(", ", missingSkills);

        return """
                Analyze the fit between this candidate and the job below.

                ## Candidate Profile
                %s

                ## Job Description
                %s

                ## Skills the candidate already has that match this job
                %s

                ## Skills the candidate is missing for this job
                %s

                ## Instructions
                - Focus on the practical implications of the skill gaps.
                - Suggest specific resources or learning paths for missing skills.
                - Be encouraging but honest about the gap size.
                - Write in markdown format, NOT JSON.
                """.formatted(cvSummary, jobDescription, matchingText, missingText);
    }
}

