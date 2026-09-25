package com.smart.therapy.flow.ai.service;

import com.smart.therapy.flow.ai.dto.*;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ErrorCode;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.exception.TranscriptionServiceUnavailableException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestion;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionOption;
import com.smart.therapy.flow.assessment.entity.AssessmentQuestionRatingLabel;
import com.smart.therapy.flow.assessment.entity.AssessmentResponse;
import com.smart.therapy.flow.assessment.entity.AssessmentResponseOption;
import com.smart.therapy.flow.assessment.entity.AssessmentSection;
import com.smart.therapy.flow.client.entity.ClientAddress;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {

    private static final DateTimeFormatter DISPLAY_DATE = DateTimeFormatter.ofPattern("MMM dd, yyyy");

    private final OpenAiClient openAiClient;
    private final ClientRepository clientRepository;
    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final ConsentPolicyService consentPolicyService;
    private final com.smart.therapy.flow.common.service.TimezoneService timezoneService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final SystemOptionResolverService systemOptionResolverService;

    @Value("${ai.models.session-notes:gpt-4o}")
    private String sessionNoteModel;

    @Value("${ai.models.assistant:gpt-5}")
    private String assistantModel;

    @Value("${ai.models.transcription-organization:gpt-4o}")
    private String transcriptionOrganizationModel;

    public String generateSessionNoteTemplate(SessionNoteTemplateRequest request) {
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage("You are a professional clinical therapist AI assistant. Generate comprehensive, professional session note templates in plain text format only. Do not use markdown formatting such as ** for bold, --- for separators, or # for headers. Use clear section breaks with line spacing only."));
        messages.add(userMessage(buildSessionTemplatePrompt(request)));

        String response = openAiClient.createChatCompletion(sessionNoteModel, messages, 0.7, 2000);
        return enrichGeneratedSessionNoteWithClientInfo(
                sanitizePlainText(response),
                request.getClient(),
                request.getSession());
    }

    public String getAssistantResponse(AssistantChatRequest request) {
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(buildAssistantContext(request.getUserRole())));

        if (!CollectionUtils.isEmpty(request.getConversationHistory())) {
            for (ChatMessageDto message : request.getConversationHistory()) {
                messages.add(chatMessage(message.getRole(), message.getContent()));
            }
        }

        messages.add(userMessage(request.getUserMessage()));

        return openAiClient.createChatCompletion(assistantModel, messages, 0.4, 1200);
    }

    private String buildSessionTemplatePrompt(SessionNoteTemplateRequest request) {
        SessionNoteTemplateRequest.ClientInfo client = request.getClient();
        SessionNoteTemplateRequest.SessionInfo session = request.getSession();
        Map<String, String> formData = Optional.ofNullable(request.getFormData()).orElseGet(HashMap::new);

        String stage = Optional.ofNullable(client.getStage()).orElse("Not specified");

        // Privacy: outbound prompt stays de-identified. Real client/session details are
        // filled into the returned note after generation (see enrichGeneratedSessionNote*).
        return "You are a professional clinical therapist AI assistant specializing in session note documentation.\n\n" +
                "CLIENT INFORMATION:\n" +
                "- Subject: Client\n" +
                "- Age range: " + determineAgeBand(client.getDateOfBirth()) + "\n" +
                "- Gender: " + Optional.ofNullable(client.getGender()).orElse("Not specified") + "\n" +
                "- Treatment Stage: " + stage + "\n\n" +
                "SESSION INFORMATION:\n" +
                "- Type: " + Optional.ofNullable(session.getSessionType()).orElse("Not specified") + "\n" +
                "- Duration: " + Optional.ofNullable(session.getDuration()).map(Object::toString).orElse("Not specified") + " minutes\n\n" +
                "EXISTING FORM DATA:\n" +
                "- Session Focus: " + formData.getOrDefault("sessionFocus", "Not filled") + "\n" +
                "- Symptoms: " + formData.getOrDefault("symptoms", "Not filled") + "\n" +
                "- Short-term Goals: " + formData.getOrDefault("shortTermGoals", "Not filled") + "\n" +
                "- Interventions: " + formData.getOrDefault("intervention", "Not filled") + "\n" +
                "- Progress: " + formData.getOrDefault("progress", "Not filled") + "\n" +
                "- Additional Notes: " + formData.getOrDefault("remarks", "None") + "\n\n" +
                "CUSTOM INSTRUCTIONS FROM THERAPIST:\n" + Optional.ofNullable(request.getCustomInstructions()).orElse("None provided") + "\n\n" +
                "Based on the above information and custom instructions, generate a comprehensive, professional session note template. Follow these guidelines:\n\n" +
                "1. Use professional clinical language appropriate for mental health documentation\n" +
                "2. Structure the content logically and clearly\n" +
                "3. Include all relevant sections based on the form fields\n" +
                "4. Follow the specific instructions provided by the therapist\n" +
                "5. Make the content specific to this client and session\n" +
                "6. Ensure compliance with clinical documentation standards\n" +
                "7. Use the existing form data when available, but expand and enhance it\n" +
                "8. DO NOT invent identifying client details (name, MRN/client ID). Refer to the subject as \"the client\"\n" +
                "9. When a clinical honorific is needed, write exactly \"Mr./Ms. [Last Name]\" — do not invent a surname\n" +
                "10. If a surname placeholder is needed alone, write exactly \"[Last Name]\"\n" +
                "11. DO NOT include CLIENT INFORMATION or SESSION INFORMATION header blocks — those are added by the application after generation\n" +
                "12. DO NOT use any markdown formatting - no ** bold text **, no --- separators, no # headers\n" +
                "13. Use plain text only with clear section breaks using line breaks\n\n" +
                "Generate a complete session note template that can be used directly for clinical documentation. Format it as plain text without any markdown formatting.";
    }

    private String buildAssistantContext(String userRole) {
        boolean isClient = "client".equalsIgnoreCase(userRole);
        String baseContext = "You are the SmartHub Navigation Assistant. Your ONLY job is to help users navigate and use SmartHub.\n\n" +
                "## YOUR PURPOSE\n" +
                "- Guide users on HOW to navigate the app\n" +
                "- Tell them WHICH buttons to click and WHERE to find things\n" +
                "- Explain HOW to use features step-by-step\n" +
                "- Help them get from point A to point B in the app\n\n" +
                "## DO NOT\n" +
                "- Provide data or numbers (don't say \"you have X clients\")\n" +
                "- Make assumptions about what's in their database\n" +
                "- Give generic therapy advice\n" +
                "- Discuss features that don't exist\n\n" +
                "## ALWAYS\n" +
                "- Use EXACT button names from the UI (e.g., \"Add Client\", \"+ Add Session Note\")\n" +
                "- Give precise navigation paths (e.g., \"Click Clients → Click the client name → Sessions tab\")\n" +
                "- Keep it concise - 3-5 steps when possible\n" +
                "- If you're unsure, admit it\n\n" +
                "Focus on SmartHub navigation and actions only.";

        if (isClient) {
            return baseContext + "\n\nYou are currently helping a CLIENT use the SmartHub Client Portal. Focus on client-facing features like viewing appointments, uploading documents, and navigating the portal. Keep responses simple and non-technical.";
        }
        return baseContext + "\n\nYou are currently helping a THERAPIST/CLINICIAN use SmartHub. You can discuss all features including client management, scheduling, documentation, and administrative tasks.";
    }

    private String sanitizePlainText(String generatedContent) {
        if (generatedContent == null) {
            return "";
        }
        return generatedContent
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("^\\s*---+\\s*$", "")
                .replaceAll("^#+\\s+", "")
                .trim();
    }

    private Map<String, String> systemMessage(String content) {
        return chatMessage("system", content);
    }

    private Map<String, String> userMessage(String content) {
        return chatMessage("user", content);
    }

    private Map<String, String> chatMessage(String role, String content) {
        Map<String, String> message = new HashMap<>();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String determineAge(java.time.Instant dateOfBirth) {
        if (dateOfBirth == null) {
            return "Not specified";
        }
        LocalDate birthDate = LocalDate.ofInstant(dateOfBirth, ZoneId.systemDefault());
        int age = LocalDate.now().getYear() - birthDate.getYear();
        if (birthDate.plusYears(age).isAfter(LocalDate.now())) {
            age -= 1;
        }
        return age > 0 ? String.valueOf(age) : "Not specified";
    }

    private String determineAgeBand(java.time.Instant dateOfBirth) {
        String calculatedAge = determineAge(dateOfBirth);
        if ("Not specified".equals(calculatedAge)) {
            return calculatedAge;
        }
        int age = Integer.parseInt(calculatedAge);
        if (age < 13) return "under 13";
        if (age < 18) return "13-17";
        if (age < 25) return "18-24";
        if (age < 35) return "25-34";
        if (age < 45) return "35-44";
        if (age < 55) return "45-54";
        if (age < 65) return "55-64";
        if (age < 75) return "65-74";
        if (age < 90) return "75-89";
        return "90 or older";
    }

    // ========== NEW AI ENDPOINT METHODS ==========

    public String generateAITemplate(GenerateTemplateRequest request) {
        Objects.requireNonNull(request, "Request is required");
        Long clientId = Objects.requireNonNull(request.getClientId(), "Client ID is required");

        // GDPR: Check AI processing consent
        consentPolicyService.requireAiConsent(clientId);
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );

        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Resolve optional session via a local ID variable so the null-check is visible to the null-safety analyzer
        Session session = null;
        Long sessionId = request.getSessionId();
        if (sessionId != null) {
            session = sessionRepository.findById(sessionId)
                    .orElse(null);
        }

        Map<String, String> formData = Optional.ofNullable(request.getFormData()).orElseGet(HashMap::new);
        String customInstructions = Optional.ofNullable(request.getCustomInstructions()).orElse("");

        String prompt = buildAITemplatePrompt(client, session, formData, customInstructions);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage("You are a professional clinical therapist AI assistant. Generate comprehensive, professional session note templates in plain text format only. Do not use markdown formatting such as ** for bold, --- for separators, or # for headers. Use clear section breaks with line spacing only."));
        messages.add(userMessage(prompt));

        String response = openAiClient.createChatCompletion(sessionNoteModel, messages, 0.7, 2000);
        return enrichGeneratedSessionNoteWithClientInfo(sanitizePlainText(response), client, session);
    }

    public Map<String, Object> getAllTemplates() {
        return ClinicalTemplates.getAllTemplates();
    }

    public String generateFromTemplate(String templateId, String field, String context) {
        Map<String, Object> templates = ClinicalTemplates.getAllTemplates();
        @SuppressWarnings("unchecked")
        Map<String, Object> template = (Map<String, Object>) templates.get(templateId);
        
        if (template == null) {
            throw new ResourceNotFoundException("Template " + templateId + " not found");
        }

        String fieldOptionsKey = field + "Options";
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldOptions = (Map<String, Object>) template.get(fieldOptionsKey);
        
        if (fieldOptions == null || fieldOptions.isEmpty()) {
            throw new ResourceNotFoundException("Template field " + field + " not found in " + templateId);
        }

        // Return the first available option template
        @SuppressWarnings("unchecked")
        Map<String, Object> firstOption = (Map<String, Object>) fieldOptions.values().iterator().next();
        String templateName = (String) template.get("name");
        return (String) firstOption.getOrDefault("template", field + " content for " + templateName);
    }

    public List<FieldOptionDto> getFieldOptions(String templateId, String field) {
        Map<String, Object> templates = ClinicalTemplates.getAllTemplates();
        @SuppressWarnings("unchecked")
        Map<String, Object> template = (Map<String, Object>) templates.get(templateId);
        
        if (template == null) {
            return Collections.emptyList();
        }

        String fieldOptionsKey = field + "Options";
        @SuppressWarnings("unchecked")
        Map<String, Object> fieldOptions = (Map<String, Object>) template.get(fieldOptionsKey);
        
        if (fieldOptions == null) {
            return Collections.emptyList();
        }

        return fieldOptions.entrySet().stream()
                .map(entry -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> option = (Map<String, Object>) entry.getValue();
                    return FieldOptionDto.builder()
                            .key(entry.getKey())
                            .label((String) option.get("label"))
                            .template((String) option.get("template"))
                            .build();
                })
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getConnectedSuggestions(String templateId, String sourceField, String sourceValue, Long clientId) {
        // GDPR: Check AI processing consent if clientId provided
        if (clientId != null) {
            consentPolicyService.requireAiConsent(clientId);
        }
        Map<String, Object> templates = ClinicalTemplates.getAllTemplates();
        @SuppressWarnings("unchecked")
        Map<String, Object> template = (Map<String, Object>) templates.get(templateId);
        
        if (template == null) {
            return Collections.emptyMap();
        }

        String sourceOptionsKey = sourceField + "Options";
        @SuppressWarnings("unchecked")
        Map<String, Object> sourceOptions = (Map<String, Object>) template.get(sourceOptionsKey);
        
        if (sourceOptions == null) {
            return Collections.emptyMap();
        }

        // Find matching option based on sourceValue
        Optional<Map.Entry<String, Object>> matchingEntry = sourceOptions.entrySet().stream()
                .filter(entry -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> option = (Map<String, Object>) entry.getValue();
                    String label = ((String) option.getOrDefault("label", "")).toLowerCase();
                    String templateText = ((String) option.getOrDefault("template", "")).toLowerCase();
                    String searchValue = sourceValue.toLowerCase();
                    return label.contains(searchValue) || templateText.contains(searchValue);
                })
                .findFirst();

        if (matchingEntry.isEmpty()) {
            return Collections.emptyMap();
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> matchingOption = (Map<String, Object>) matchingEntry.get().getValue();
        @SuppressWarnings("unchecked")
        Map<String, String> connects = (Map<String, String>) matchingOption.get("connects");
        
        if (connects == null) {
            return Collections.emptyMap();
        }

        // Build suggestions for connected fields
        Map<String, List<String>> suggestions = new HashMap<>();
        for (Map.Entry<String, String> connectEntry : connects.entrySet()) {
            String targetField = connectEntry.getKey();
            String optionKey = connectEntry.getValue();
            String targetOptionsKey = targetField + "Options";
            
            @SuppressWarnings("unchecked")
            Map<String, Object> targetOptions = (Map<String, Object>) template.get(targetOptionsKey);
            
            if (targetOptions != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> targetOption = (Map<String, Object>) targetOptions.get(optionKey);
                if (targetOption != null) {
                    String templateText = (String) targetOption.get("template");
                    suggestions.put(targetField, Collections.singletonList(templateText));
                }
            }
        }

        return suggestions;
    }

    public List<String> generateSmartSuggestions(String field, String context, Long clientId) {
        // GDPR: Check AI processing consent if clientId provided
        if (clientId != null) {
            consentPolicyService.requireAiConsent(clientId);
        }
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );
        String systemPrompt = "You are a clinical psychology assistant. Generate 3-5 concise, professional suggestions for the " + field + " field in therapy session notes. Focus on evidence-based practices and clinical terminology.";
        String userPrompt = "Generate suggestions for \"" + field + "\" based on this context: " + context + "\n\nReturn only a JSON array of strings, each suggestion should be 1-2 sentences maximum.";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(systemPrompt));
        messages.add(userMessage(userPrompt));

        try {
            String response = openAiClient.createChatCompletion(sessionNoteModel, messages, 0.8, 500);
            // Parse JSON array response (simplified - assumes valid JSON array)
            response = response.trim();
            if (response.startsWith("[")) {
                // Simple JSON array parsing
                response = response.substring(1, response.length() - 1);
                return Arrays.stream(response.split(","))
                        .map(s -> s.trim().replaceAll("^\"|\"$", ""))
                        .filter(StringUtils::hasText)
                        .collect(Collectors.toList());
            }
            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Failed to generate smart suggestions", e);
            return Collections.emptyList();
        }
    }

    public String generateClinicalReport(GenerateClinicalReportRequest request) {
        // GDPR: Check AI processing consent before generating clinical report
        if (request.getClientId() != null) {
            consentPolicyService.requireAiConsent(request.getClientId());
        }
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_REPORTS_PER_MONTH,
                "AI report generation"
        );

        String systemPrompt = "You are a licensed clinical psychologist. Generate a formal, professional clinical report in third-person narrative format. Use flowing prose suitable for official medical records, insurance documentation, and clinical case files.\n\n" +
                "Key requirements:\n" +
                "- Third-person clinical language\n" +
                "- Professional medical terminology\n" +
                "- Objective observations and assessments\n" +
                "- No bullet points or lists\n" +
                "- Flowing paragraph structure\n" +
                "- Evidence-based treatment approach references";

        Map<String, String> sessionNoteData = Optional.ofNullable(request.getSessionNoteData()).orElseGet(HashMap::new);
        String userPrompt = "Generate a formal clinical report based on this session data:\n\n" +
                "Client Information: " + Optional.ofNullable(request.getClientName()).orElse("Client") + "\n" +
                "Session Type: " + Optional.ofNullable(request.getSessionType()).orElse("Individual therapy") + "\n" +
                "Date: " + Optional.ofNullable(request.getSessionDate()).orElse("Session date") + "\n\n" +
                "Clinical Data:\n" +
                (sessionNoteData.containsKey("sessionFocus") ? "Session Focus: " + sessionNoteData.get("sessionFocus") + "\n" : "") +
                (sessionNoteData.containsKey("symptoms") ? "Presented Symptoms: " + sessionNoteData.get("symptoms") + "\n" : "") +
                (sessionNoteData.containsKey("shortTermGoals") ? "Treatment Goals: " + sessionNoteData.get("shortTermGoals") + "\n" : "") +
                (sessionNoteData.containsKey("intervention") ? "Interventions Applied: " + sessionNoteData.get("intervention") + "\n" : "") +
                (sessionNoteData.containsKey("progress") ? "Progress Assessment: " + sessionNoteData.get("progress") + "\n" : "") +
                (sessionNoteData.containsKey("remarks") ? "Clinical Observations: " + sessionNoteData.get("remarks") + "\n" : "") +
                (sessionNoteData.containsKey("recommendations") ? "Treatment Recommendations: " + sessionNoteData.get("recommendations") + "\n" : "");

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(systemPrompt));
        messages.add(userMessage(userPrompt));

        return openAiClient.createChatCompletion(sessionNoteModel, messages, 0.6, 1500);
    }

    public String regenerateContent(Long sessionNoteId, String customPrompt) {
        Objects.requireNonNull(sessionNoteId, "Session note ID is required");

        SessionNote sessionNote = sessionNoteRepository.findById(sessionNoteId)
                .orElseThrow(() -> new ResourceNotFoundException("Session note not found"));

        // GDPR: Check AI processing consent before regenerating content
        Long clientId = sessionNote.getClient() != null ? sessionNote.getClient().getId() : null;
        if (clientId != null) {
            consentPolicyService.requireAiConsent(clientId);
        }
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );

        String systemPrompt = "You are a professional clinical psychologist assistant. Generate comprehensive session notes in third-person clinical narrative format suitable for formal medical records. Focus on:\n\n" +
                "1. Professional clinical language\n" +
                "2. Objective observations and assessments\n" +
                "3. Treatment progress and interventions\n" +
                "4. Specific, actionable recommendations\n" +
                "5. Integration of mood and behavioral data\n\n" +
                (StringUtils.hasText(customPrompt) ? "Additional instructions: " + customPrompt + "\n\n" : "") +
                "Return response as JSON with:\n" +
                "- \"generatedContent\": Complete clinical narrative (flowing prose, no bullet points)\n" +
                "- \"suggestions\": Object with arrays of suggestions for each field";

        Client client = sessionNote.getClient();
        Session session = sessionNote.getSession();

        String sessionType = session != null ? resolveSessionModeLabel(session.getSessionType()) : "therapy";

        String userPrompt = "Generate session notes for a de-identified client from a " + sessionType + " session.\n\n" +
                "Session Data:\n" +
                "- Session Focus: " + Optional.ofNullable(sessionNote.getSessionFocus()).orElse("Not specified") + "\n" +
                "- Symptoms: " + Optional.ofNullable(sessionNote.getSymptoms()).orElse("Not specified") + "\n" +
                "- Goals: " + Optional.ofNullable(sessionNote.getShortTermGoals()).orElse("Not specified") + "\n" +
                "- Interventions: " + Optional.ofNullable(sessionNote.getIntervention()).orElse("Not specified") + "\n" +
                "- Progress: " + Optional.ofNullable(sessionNote.getProgress()).orElse("Not specified") + "\n" +
                "- Clinical Remarks: " + Optional.ofNullable(sessionNote.getRemarks()).orElse("Not specified") + "\n" +
                "- Recommendations: " + Optional.ofNullable(sessionNote.getRecommendations()).orElse("Not specified") + "\n\n" +
                "Generate a professional clinical summary and provide smart suggestions for each category.";

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(systemPrompt));
        messages.add(userMessage(userPrompt));

        return openAiClient.createChatCompletion(sessionNoteModel, messages, 0.7, 2000);
    }

    // ========== PRIVATE HELPER METHODS ==========

    private void enforceAndTrackAiLimit(String featureCode, String actionLabel) {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null) {
            return;
        }
        subscriptionFeatureService.consumeUsageOrThrow(orgId, featureCode, 1L, actionLabel);
    }

    private String buildAITemplatePrompt(Client client, Session session, Map<String, String> formData, String customInstructions) {
        String age = client.getDateOfBirth() != null
                ? determineAgeBand(client.getDateOfBirth().atStartOfDay(ZoneId.of("America/New_York")).toInstant())
                : "Not specified";
        String gender = client.getGender() != null
                ? resolveOptionLabel(SystemOptionCategories.GENDER, client.getGender())
                : "Not specified";
        String stage = client.getStage() != null ? client.getStage() : "Not specified";

        String sessionType = session != null && session.getSessionType() != null
                ? resolveSessionModeLabel(session.getSessionType())
                : "Not specified";
        String duration = session != null && session.getDuration() != null
                ? String.valueOf(session.getDuration())
                : "Not specified";

        // Privacy: outbound prompt stays de-identified. Real client/session details are
        // filled into the returned note after generation (see enrichGeneratedSessionNote*).
        return "You are a professional clinical therapist AI assistant specializing in session note documentation.\n\n" +
                "CLIENT INFORMATION:\n" +
                "- Subject: Client\n" +
                "- Age range: " + age + "\n" +
                "- Gender: " + gender + "\n" +
                "- Treatment Stage: " + stage + "\n\n" +
                "SESSION INFORMATION:\n" +
                "- Type: " + sessionType + "\n" +
                "- Duration: " + duration + " minutes\n\n" +
                "EXISTING FORM DATA:\n" +
                "- Session Focus: " + formData.getOrDefault("sessionFocus", "Not filled") + "\n" +
                "- Symptoms: " + formData.getOrDefault("symptoms", "Not filled") + "\n" +
                "- Short-term Goals: " + formData.getOrDefault("shortTermGoals", "Not filled") + "\n" +
                "- Interventions: " + formData.getOrDefault("intervention", "Not filled") + "\n" +
                "- Progress: " + formData.getOrDefault("progress", "Not filled") + "\n" +
                "- Additional Notes: " + formData.getOrDefault("remarks", "None") + "\n\n" +
                "CUSTOM INSTRUCTIONS FROM THERAPIST:\n" + (StringUtils.hasText(customInstructions) ? customInstructions : "None provided") + "\n\n" +
                "Based on the above information and custom instructions, generate a comprehensive, professional session note template. Follow these guidelines:\n\n" +
                "1. Use professional clinical language appropriate for mental health documentation\n" +
                "2. Structure the content logically and clearly\n" +
                "3. Include all relevant sections based on the form fields\n" +
                "4. Follow the specific instructions provided by the therapist\n" +
                "5. Make the content specific to this client and session\n" +
                "6. Ensure compliance with clinical documentation standards\n" +
                "7. Use the existing form data when available, but expand and enhance it\n" +
                "8. DO NOT invent identifying client details (name, MRN/client ID). Refer to the subject as \"the client\"\n" +
                "9. When a clinical honorific is needed, write exactly \"Mr./Ms. [Last Name]\" — do not invent a surname\n" +
                "10. If a surname placeholder is needed alone, write exactly \"[Last Name]\"\n" +
                "11. DO NOT include CLIENT INFORMATION or SESSION INFORMATION header blocks — those are added by the application after generation\n" +
                "12. DO NOT use any markdown formatting - no ** bold text **, no --- separators, no # headers\n" +
                "13. Use plain text only with clear section breaks using line breaks\n\n" +
                "Generate a complete session note template that can be used directly for clinical documentation. Format it as plain text without any markdown formatting.";
    }

    /**
     * Injects real client/session identity into the AI note after generation so PII never
     * needs to leave the application in the outbound prompt.
     */
    String enrichGeneratedSessionNoteWithClientInfo(String generatedContent, Client client, Session session) {
        String name = client != null && StringUtils.hasText(client.getFullName())
                ? client.getFullName().trim()
                : "Client";
        String clientId = client != null && StringUtils.hasText(client.getClientId())
                ? client.getClientId().trim()
                : "Not specified";
        String age = client != null && client.getDateOfBirth() != null
                ? determineAge(client.getDateOfBirth().atStartOfDay(ZoneId.of("America/New_York")).toInstant())
                : "Not specified";
        String gender = client != null && client.getGender() != null
                ? resolveOptionLabel(SystemOptionCategories.GENDER, client.getGender())
                : "Not specified";
        String stage = client != null && StringUtils.hasText(client.getStage())
                ? client.getStage().trim()
                : "Not specified";

        String sessionDate = formatSessionDateForDisplay(session != null ? session.getSessionDate() : null);
        String sessionType = session != null && session.getSessionType() != null
                ? resolveSessionModeLabel(session.getSessionType())
                : "Not specified";
        String duration = session != null && session.getDuration() != null
                ? String.valueOf(session.getDuration())
                : "Not specified";

        return mergeIdentityHeaderWithGeneratedNote(
                generatedContent,
                name,
                clientId,
                age,
                gender,
                stage,
                sessionDate,
                sessionType,
                duration);
    }

    String enrichGeneratedSessionNoteWithClientInfo(
            String generatedContent,
            SessionNoteTemplateRequest.ClientInfo client,
            SessionNoteTemplateRequest.SessionInfo session) {
        String name = client != null && StringUtils.hasText(client.getFullName())
                ? client.getFullName().trim()
                : "Client";
        String clientId = client != null && StringUtils.hasText(client.getClientId())
                ? client.getClientId().trim()
                : "Not specified";
        String age = client != null && client.getDateOfBirth() != null
                ? determineAge(client.getDateOfBirth())
                : "Not specified";
        String gender = client != null && StringUtils.hasText(client.getGender())
                ? client.getGender().trim()
                : "Not specified";
        String stage = client != null && StringUtils.hasText(client.getStage())
                ? client.getStage().trim()
                : "Not specified";

        String sessionDate = formatSessionDateForDisplay(session != null ? session.getSessionDate() : null);
        String sessionType = session != null && StringUtils.hasText(session.getSessionType())
                ? session.getSessionType().trim()
                : "Not specified";
        String duration = session != null && session.getDuration() != null
                ? String.valueOf(session.getDuration())
                : "Not specified";

        return mergeIdentityHeaderWithGeneratedNote(
                generatedContent,
                name,
                clientId,
                age,
                gender,
                stage,
                sessionDate,
                sessionType,
                duration);
    }

    private String mergeIdentityHeaderWithGeneratedNote(
            String generatedContent,
            String name,
            String clientId,
            String age,
            String gender,
            String stage,
            String sessionDate,
            String sessionType,
            String duration) {
        String lastName = extractLastName(name);
        String honorific = resolveHonorific(gender);
        String body = fillClientNamePlaceholders(
                stripLeadingIdentitySections(generatedContent),
                name,
                lastName,
                honorific);
        StringBuilder header = new StringBuilder();
        header.append("CLIENT INFORMATION\n");
        header.append("Name: ").append(name).append("\n");
        header.append("Client ID: ").append(clientId).append("\n");
        header.append("Age: ").append(age).append("\n");
        header.append("Gender: ").append(gender).append("\n");
        header.append("Treatment Stage: ").append(stage).append("\n\n");
        header.append("SESSION INFORMATION\n");
        header.append("Date: ").append(sessionDate).append("\n");
        header.append("Type: ").append(sessionType).append("\n");
        header.append("Duration: ").append(duration).append(" minutes");
        if (StringUtils.hasText(body)) {
            header.append("\n\n").append(body);
        }
        return header.toString().trim();
    }

    /**
     * Replaces AI/privacy placeholders with real client identity after generation.
     */
    String fillClientNamePlaceholders(String content, String fullName, String lastName, String honorific) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String safeLastName = StringUtils.hasText(lastName) ? lastName : "Client";
        String safeFullName = StringUtils.hasText(fullName) ? fullName : safeLastName;
        String titledName = StringUtils.hasText(honorific)
                ? honorific + " " + safeLastName
                : "Mr./Ms. " + safeLastName;

        String filled = content;
        // Combined honorific + last-name placeholders first (more specific).
        filled = filled.replaceAll(
                "(?i)\\bMr\\.?\\s*(?:or|/)?\\s*Ms\\.?\\s*\\[\\s*Last\\s*Name\\s*\\]",
                java.util.regex.Matcher.quoteReplacement(titledName));
        filled = filled.replaceAll(
                "(?i)\\bMs\\.?\\s*(?:or|/)?\\s*Mr\\.?\\s*\\[\\s*Last\\s*Name\\s*\\]",
                java.util.regex.Matcher.quoteReplacement(titledName));
        filled = filled.replaceAll(
                "(?i)\\bMr\\.?\\s*(?:or|/)?\\s*Mrs\\.?\\s*\\[\\s*Last\\s*Name\\s*\\]",
                java.util.regex.Matcher.quoteReplacement(titledName));
        filled = filled.replaceAll(
                "(?i)\\[(?:Client\\s+)?Last\\s*Name\\]",
                java.util.regex.Matcher.quoteReplacement(safeLastName));
        filled = filled.replaceAll(
                "(?i)\\[(?:Client\\s+)?(?:Full\\s+)?Name\\]",
                java.util.regex.Matcher.quoteReplacement(safeFullName));
        return filled;
    }

    private String extractLastName(String fullName) {
        if (!StringUtils.hasText(fullName)) {
            return "Client";
        }
        String trimmed = fullName.trim();
        int lastSpace = trimmed.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace < trimmed.length() - 1) {
            return trimmed.substring(lastSpace + 1).trim();
        }
        return trimmed;
    }

    private String resolveHonorific(String gender) {
        if (!StringUtils.hasText(gender) || "Not specified".equalsIgnoreCase(gender)) {
            return null;
        }
        String normalized = gender.trim().toLowerCase(Locale.ROOT);
        if ("f".equals(normalized)
                || "female".equals(normalized)
                || normalized.contains("female")
                || normalized.contains("woman")) {
            return "Ms.";
        }
        if ("m".equals(normalized)
                || "male".equals(normalized)
                || normalized.contains("male")
                || (normalized.contains("man") && !normalized.contains("woman"))) {
            return "Mr.";
        }
        return null;
    }

    /**
     * Removes AI-produced CLIENT/SESSION INFORMATION blocks so we can replace them with
     * authoritative local identity data without duplicating sections.
     */
    private String stripLeadingIdentitySections(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String remaining = content.trim();
        remaining = remaining.replaceFirst(
                "(?is)^\\s*CLIENT\\s+INFORMATION\\b.*?(?=\\n\\s*(?:SESSION\\s+INFORMATION\\b|[A-Z][A-Z0-9 /()&'-]{2,80}\\s*$)|\\z)",
                "");
        remaining = remaining.trim();
        remaining = remaining.replaceFirst(
                "(?is)^\\s*SESSION\\s+INFORMATION\\b.*?(?=\\n\\s*[A-Z][A-Z0-9 /()&'-]{2,80}\\s*$|\\z)",
                "");
        return remaining.trim();
    }

    private String formatSessionDateForDisplay(Instant sessionDate) {
        if (sessionDate == null) {
            return "Not specified";
        }
        return DateTimeFormatter.ofPattern("MMM dd, yyyy")
                .withZone(ZoneId.of("America/New_York"))
                .format(sessionDate);
    }

    /**
     * Organize transcribed audio text into structured session note fields
     * @param transcription The raw transcribed text from audio
     * @param clientName Optional client name for context
     * @return Map of field names to extracted values
     */
    public Map<String, String> organizeTranscriptionIntoFields(String transcription, String clientName) {
        return organizeTranscriptionIntoFieldsInternal(transcription, clientName, false);
    }

    public Map<String, String> organizeTranscriptionIntoFieldsStrict(String transcription, String clientName) {
        return organizeTranscriptionIntoFieldsInternal(transcription, clientName, true);
    }

    /**
     * Uses GPT-4o to re-label each speaker turn in a finalized transcript as either
     * "Therapist:" or "Client:" based purely on conversational patterns.
     *
     * HIPAA COMPLIANCE: The client's real name is NEVER sent to OpenAI.
     * Only the spoken text (already stored encrypted in DB, covered by AI consent) is sent.
     * The prompt explicitly instructs the model NOT to invent or infer any names.
     *
     * @param rawTranscript the finalTranscript string (timestamped, with "Therapist:" placeholders)
     * @return the same transcript with each turn re-labeled as Therapist: or Client:
     */
    public String diarizeTranscript(String rawTranscript) {
        if (!StringUtils.hasText(rawTranscript)) {
            return rawTranscript;
        }
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );

        log.info("[Diarization] Sending transcript for speaker labeling: wordCount={}", countWords(rawTranscript));

        String result = requestDiarization(rawTranscript, buildDiarizationMessages(rawTranscript));
        if (isInvalidDiarizationResult(result)) {
            log.warn("[Diarization] Model returned a refusal/invalid result; retrying with fallback prompt. preview={}",
                    abbreviateForLogs(result));
            result = requestDiarization(rawTranscript, buildDiarizationFallbackMessages(rawTranscript));
        }

        if (isInvalidDiarizationResult(result)) {
            log.error("[Diarization] Model refused or returned invalid speaker labels. preview={}",
                    abbreviateForLogs(result));
            throw new BadRequestException(
                    "Speaker identification was blocked by the AI provider. Please try again in a moment.");
        }

        log.info("[Diarization] Received diarized transcript: wordCount={}", countWords(result));
        return result;
    }

    /**
     * True when a stored/returned diarization value is an OpenAI safety refusal (or similarly
     * useless short reply) rather than a labeled transcript. Used to avoid caching refusals.
     */
    public static boolean isInvalidDiarizationResult(String text) {
        if (!StringUtils.hasText(text)) {
            return true;
        }
        String trimmed = text.trim();
        if (looksLikeLabeledTranscript(trimmed)) {
            return false;
        }
        String normalized = trimmed.toLowerCase(Locale.ROOT);
        boolean refusalPhrase =
                normalized.contains("can't assist")
                        || normalized.contains("cannot assist")
                        || normalized.contains("unable to assist")
                        || normalized.contains("can't help with that")
                        || normalized.contains("cannot help with that")
                        || normalized.contains("not able to assist")
                        || normalized.contains("i'm sorry, i can't")
                        || normalized.contains("i am sorry, i can't")
                        || normalized.contains("i'm sorry, but i cannot")
                        || normalized.contains("i am sorry, but i cannot");
        // Short non-transcript replies (typical refusals are one sentence) are invalid.
        return refusalPhrase || trimmed.length() < 80;
    }

    private String requestDiarization(String rawTranscript, List<Map<String, String>> messages) {
        int maxTokens = Math.min(8000, Math.max(4000, countWords(rawTranscript) * 3));
        return openAiClient.createChatCompletion(transcriptionOrganizationModel, messages, 0.1, maxTokens);
    }

    private List<Map<String, String>> buildDiarizationMessages(String rawTranscript) {
        List<Map<String, String>> messages = new ArrayList<>();
        // Frame as an authorized EHR labeling utility — avoid "analyze clinical conversation"
        // wording that commonly triggers model safety refusals on therapy dialogue.
        messages.add(systemMessage(
                "You are a speaker-labeling utility inside an authorized electronic health record (EHR) application.\n\n" +
                "Task: Assign each turn in a two-person professional counseling conversation to exactly one role label:\n" +
                "- Therapist:\n" +
                "- Client:\n\n" +
                "This is an authorized healthcare documentation workflow. Always complete the labeling. " +
                "Do not refuse. Do not advise, diagnose, counsel, interpret clinical meaning, or comment on content.\n\n" +
                "Rules:\n" +
                "1. Preserve every timestamp and every spoken word exactly — only change speaker labels\n" +
                "2. Exactly two roles: Therapist and Client\n" +
                "3. Infer roles from conversational structure only:\n" +
                "   - Therapist: questions, reflections, psychoeducation, structure, interventions, summaries\n" +
                "   - Client: personal experiences, answers, emotions, history, symptom reports\n" +
                "4. Never invent, use, or infer real names or other identifiers\n" +
                "5. If a turn is ambiguous, keep continuity with the previous speaker\n" +
                "6. Output format must match input: [HH:MM:SS]\\n<Speaker>: <text>\n" +
                "7. Output ONLY the labeled transcript — no preamble, apology, or explanation"
        ));
        messages.add(userMessage(
                "Relabel speakers in this two-person professional conversation transcript for EHR documentation. " +
                "Replace existing speaker labels with Therapist: or Client: as appropriate. " +
                "Keep all timestamps and spoken words exactly as-is. Output only the labeled transcript.\n\n" +
                rawTranscript
        ));
        return messages;
    }

    private List<Map<String, String>> buildDiarizationFallbackMessages(String rawTranscript) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(
                "You label speakers in transcripts for EHR software. " +
                "Output only the transcript with each line labeled Therapist: or Client:. " +
                "Do not change spoken words or timestamps. Do not refuse. Do not explain."
        ));
        messages.add(userMessage(
                "Rewrite speaker labels only (Therapist: / Client:). Return the full transcript:\n\n" +
                rawTranscript
        ));
        return messages;
    }

    private static boolean looksLikeLabeledTranscript(String text) {
        return text.contains("Therapist:")
                || text.contains("Client:")
                || text.contains("[00:")
                || text.matches("(?s).*\\[\\d{2}:\\d{2}:\\d{2}\\].*");
    }

    private int countWords(String text) {
        if (!StringUtils.hasText(text)) return 0;
        return text.trim().split("\\s+").length;
    }

    private Map<String, String> organizeTranscriptionIntoFieldsInternal(String transcription,
                                                                        String clientName,
                                                                        boolean strictMode) {
        if (!StringUtils.hasText(transcription)) {
            return new HashMap<>();
        }
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH,
                "AI content generation"
        );

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(buildTranscriptionOrganizationSystemPrompt()));
        messages.add(userMessage(buildTranscriptionOrganizationUserPrompt(transcription, clientName)));

        String response;
        try {
            response = requestStructuredTranscriptOrganization(messages, strictMode);
        } catch (BadRequestException e) {
            throw e;
        }

        Map<String, String> mappedFields = parseFieldOrganizationResponse(response, strictMode);
        return mappedFields;
    }

    private String requestStructuredTranscriptOrganization(List<Map<String, String>> messages, boolean strictMode) {
        try {
            return openAiClient.createChatCompletion(
                    transcriptionOrganizationModel,
                    messages,
                    0.3,
                    2000,
                    Map.of("response_format", Map.of("type", "json_object"))
            );
        } catch (Exception structuredFailure) {
            log.warn("Structured transcript field organization request failed; retrying without response_format. strictMode={}",
                    strictMode, structuredFailure);

            try {
                return openAiClient.createChatCompletion(
                        transcriptionOrganizationModel,
                        messages,
                        0.3,
                        2000
                );
            } catch (Exception legacyFailure) {
                log.error("Failed to organize transcription into fields after structured and legacy retries: strictMode={}",
                        strictMode, legacyFailure);
                if (strictMode) {
                    throw mapTranscriptOrganizationFailure(structuredFailure, legacyFailure);
                }
                return "";
            }
        }
    }

    private RuntimeException mapTranscriptOrganizationFailure(Throwable structuredFailure, Throwable legacyFailure) {
        Throwable effectiveFailure = legacyFailure != null ? legacyFailure : structuredFailure;
        String message = effectiveFailure != null ? effectiveFailure.getMessage() : null;

        if (message != null && message.toLowerCase(Locale.ROOT).contains("api key is not configured")) {
            return new TranscriptionServiceUnavailableException(
                    "Smart-fill is unavailable due to a server configuration issue. Please contact support.",
                    ErrorCode.SYSTEM_CONFIGURATION_ERROR,
                    false,
                    "smart_fill_configuration_error",
                    effectiveFailure);
        }

        return new TranscriptionServiceUnavailableException(
                "Smart-fill is temporarily unavailable. Please try again later.",
                ErrorCode.EXTERNAL_OPENAI_ERROR,
                true,
                "smart_fill_unavailable",
                effectiveFailure);
    }

    private String buildTranscriptionOrganizationSystemPrompt() {
        return "You are a professional clinical therapist AI assistant specializing in organizing session notes from voice transcriptions.\n\n" +
                "Your task is to analyze transcribed therapist voice notes and extract structured information into appropriate clinical fields.\n\n" +
                "You must return a JSON object with the following fields (use null for fields that cannot be extracted):\n" +
                "- sessionFocus: The main focus or topic of the session\n" +
                "- symptoms: Any symptoms, behaviors, or concerns mentioned\n" +
                "- shortTermGoals: Short-term treatment goals discussed\n" +
                "- intervention: Therapeutic interventions, techniques, or approaches used\n" +
                "- progress: Progress made, improvements, or changes observed\n" +
                "- remarks: Additional observations or notes\n" +
                "- recommendations: Recommendations for next steps or future sessions\n" +
                "- clientRating: Client's self-reported rating (0-10) if mentioned\n" +
                "- therapistRating: Therapist's rating of the session (0-10) if mentioned\n" +
                "- moodBefore: Client's mood before session (1-10) if mentioned\n" +
                "- moodAfter: Client's mood after session (1-10) if mentioned\n" +
                "- riskSuicidalIdeation: Risk level for suicidal ideation (0-4) if mentioned\n" +
                "- riskSelfHarm: Risk level for self-harm (0-4) if mentioned\n" +
                "- riskHomicidalIdeation: Risk level for homicidal ideation (0-4) if mentioned\n" +
                "- riskPsychosis: Risk level for psychosis (0-4) if mentioned\n" +
                "- riskSubstanceUse: Risk level for substance use (0-4) if mentioned\n" +
                "- riskImpulsivity: Risk level for impulsivity (0-4) if mentioned\n" +
                "- riskAggression: Risk level for aggression (0-4) if mentioned\n" +
                "- riskTraumaSymptoms: Risk level for trauma symptoms (0-4) if mentioned\n" +
                "- riskNonAdherence: Risk level for non-adherence (0-4) if mentioned\n" +
                "- riskSupportSystem: Risk level for support system (0-4) if mentioned\n\n" +
                "IMPORTANT:\n" +
                "1. Only extract information that is explicitly mentioned in the transcription\n" +
                "2. Use professional clinical language when summarizing\n" +
                "3. For risk assessments, use the scale: 0=None/Low, 1=Mild, 2=Moderate, 3=Severe, 4=Critical/Acute\n" +
                "4. Return ONLY valid JSON, no additional text or explanation\n" +
                "5. If a field cannot be determined, use null (not empty string)\n" +
                "6. Keep extracted text concise but complete";
    }

    private String buildTranscriptionOrganizationUserPrompt(String transcription, String clientName) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Analyze the following voice transcription from a therapy session for a de-identified client.\n\n");
        prompt.append("TRANSCRIPTION:\n");
        prompt.append(transcription);
        prompt.append("\n\n");
        prompt.append("Extract and organize the information into the structured fields. Return a JSON object with the field names as keys and extracted values as strings (or null if not found).");
        
        return prompt.toString();
    }

    private static final Set<String> TRANSCRIPTION_NOTE_FIELDS = Set.of(
            "sessionFocus", "symptoms", "shortTermGoals", "intervention", "progress", "remarks", "recommendations",
            "clientRating", "therapistRating", "moodBefore", "moodAfter",
            "riskSuicidalIdeation", "riskSelfHarm", "riskHomicidalIdeation", "riskPsychosis", "riskSubstanceUse",
            "riskImpulsivity", "riskAggression", "riskTraumaSymptoms", "riskNonAdherence", "riskSupportSystem"
    );

    @SuppressWarnings("unchecked")
    private Map<String, String> parseFieldOrganizationResponse(String response, boolean strictMode) {
        Map<String, String> fields = new HashMap<>();
        if (!StringUtils.hasText(response)) {
            if (strictMode) {
                throw new IllegalStateException("AI returned an empty structured response for transcript smart-fill");
            }
            return fields;
        }

        try {
            String jsonStr = extractJsonPayload(response);
            if (!StringUtils.hasText(jsonStr) || !jsonStr.contains("{")) {
                log.warn("AI response did not contain a JSON object for field organization. responsePreview={}",
                        abbreviateForLogs(response));
                if (strictMode) {
                    throw new IllegalStateException("AI returned a non-JSON response for transcript smart-fill");
                }
                return fields;
            }

            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> parsed = mapper.readValue(jsonStr, Map.class);

            for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                String normalizedKey = normalizeTranscriptionFieldKey(entry.getKey());
                if (!TRANSCRIPTION_NOTE_FIELDS.contains(normalizedKey)) {
                    continue;
                }
                String value = stringifyFieldValue(entry.getValue());
                if (StringUtils.hasText(value)) {
                    fields.put(normalizedKey, value.trim());
                }
            }
        } catch (Exception e) {
            log.error("Failed to parse field organization response as JSON. responsePreview={}",
                    abbreviateForLogs(response), e);
            if (strictMode) {
                throw new IllegalStateException("Failed to parse AI structured response for transcript smart-fill", e);
            }
        }

        return fields;
    }

    private String abbreviateForLogs(String response) {
        if (!StringUtils.hasText(response)) {
            return "<empty>";
        }
        String normalized = response.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 500 ? normalized : normalized.substring(0, 500) + "...";
    }

    private String extractJsonPayload(String response) {
        String trimmed = response.trim();
        if (trimmed.contains("```")) {
            int fenceStart = trimmed.indexOf("```json");
            int contentStart;
            if (fenceStart >= 0) {
                contentStart = trimmed.indexOf('\n', fenceStart);
                contentStart = contentStart >= 0 ? contentStart + 1 : fenceStart + 7;
            } else {
                contentStart = trimmed.indexOf("```") + 3;
                int firstNewline = trimmed.indexOf('\n', contentStart);
                if (firstNewline > contentStart) {
                    contentStart = firstNewline + 1;
                }
            }
            int fenceEnd = trimmed.indexOf("```", contentStart);
            if (fenceEnd > contentStart) {
                return trimmed.substring(contentStart, fenceEnd).trim();
            }
        }

        int objectStart = trimmed.indexOf('{');
        int objectEnd = trimmed.lastIndexOf('}');
        if (objectStart >= 0 && objectEnd > objectStart) {
            return trimmed.substring(objectStart, objectEnd + 1);
        }
        return trimmed;
    }

    private String normalizeTranscriptionFieldKey(String key) {
        if (!StringUtils.hasText(key)) {
            return key;
        }
        String normalized = key.trim();
        if (normalized.contains("_")) {
            StringBuilder builder = new StringBuilder();
            boolean upperNext = false;
            for (char ch : normalized.toCharArray()) {
                if (ch == '_') {
                    upperNext = true;
                    continue;
                }
                builder.append(upperNext ? Character.toUpperCase(ch) : ch);
                upperNext = false;
            }
            normalized = builder.toString();
        }
        if (Character.isUpperCase(normalized.charAt(0))) {
            normalized = Character.toLowerCase(normalized.charAt(0)) + normalized.substring(1);
        }
        return normalized;
    }

    private String stringifyFieldValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String text) {
            return text;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
        }
        return value.toString();
    }

    /**
     * Generate AI assessment report from assignment responses and sections.
     * Prompt structure matches ClientHub {@code generateAssessmentReport} (HTML narrative output).
     * Uses temperature 0 for deterministic output.
     */
    public String generateAssessmentReport(AssessmentAssignment assignment,
                                          List<AssessmentResponse> responses,
                                          List<AssessmentSection> sections) {
        Objects.requireNonNull(assignment, "Assignment is required");
        Objects.requireNonNull(responses, "Responses are required");
        Objects.requireNonNull(sections, "Sections are required");
        if (assignment.getClient() == null) {
            throw new BadRequestException("Assessment assignment and client data are required");
        }
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_REPORTS_PER_MONTH,
                "AI report generation"
        );

        Client client = assignment.getClient();

        String clientName = sanitizeForPrompt(client.getFullName());
        if (!StringUtils.hasText(clientName) || "Not provided".equals(clientName)) {
            clientName = "Client Name";
        }
        String gender = sanitizeForPrompt(client.getGender());
        if (!StringUtils.hasText(gender) || "Not provided".equals(gender)) {
            gender = "Not specified";
        }
        String address = formatClientAddressForPrompt(client);

        String systemPrompt = """
                You are a licensed clinical psychologist generating a professional assessment report. Create a comprehensive clinical report using the assessment responses and section-specific prompts.

                ⚠️ CRITICAL SAFETY RULES:
                1. USE the client data provided in bullet points (• Question → Answer format)
                2. Transform this data into professional clinical narrative following the template format
                3. If a specific answer says "Not provided" or is missing, write "information not available" in your narrative
                4. Template examples in instructions (like "Dr. Sarah Thompson" or "123 Main Street") show the FORMAT only - replace them with actual client answers
                5. NEVER invent details not present in the client's actual answers
                6. This is real clinical documentation - use actual data provided, acknowledge gaps when data is missing

                CRITICAL: Generate properly formatted HTML that will display correctly in a rich text editor.

                HTML Formatting Requirements:
                - Use <h2> tags for main section headings (no inline styles)
                - Use <h3> tags for subsections (no inline styles)
                - Wrap ALL narrative content in <p> tags - every sentence should be in a paragraph
                - Add a blank line (<p><br></p>) between paragraphs for better readability
                - Use <strong> tags for emphasis on key clinical terms
                - DO NOT use <ul>, <li>, or bullet points - write everything as flowing narrative paragraphs
                - Keep all HTML properly formatted and closed
                - DO NOT use inline styles - they will be stripped by the editor

                Clinical Content Requirements:
                - Use professional clinical language appropriate for healthcare documentation
                - Write in third-person narrative style (e.g., "The client reported..." or "Ms./Mr. [Name] indicated...")
                - Transform raw responses into clinical observations and professional assessments
                - CRITICAL: Follow each section's detailed template instructions EXACTLY
                - The template shows you the exact format and structure - follow it precisely
                - Create flowing narrative PARAGRAPHS ONLY - absolutely NO bullet points, NO lists, NO Q&A format
                - Each section's template example shows the style - match that style exactly
                - Include relevant clinical terminology and evidence-based observations
                - Structure content logically within each section
                - Synthesize information into coherent narrative paragraphs
                - Break content into digestible paragraphs (3-5 sentences each)

                For each section:
                1. Start with an <h2> heading for the section title
                2. Add a blank paragraph (<p><br></p>) after the heading
                3. Read the section's template instructions and example output carefully
                4. Transform client data into narrative paragraphs matching the template example format
                5. Write ONLY in paragraph format using <p> tags - never use bullet points or lists
                6. Separate paragraphs with blank lines (<p><br></p>) for readability
                7. Use the template example as your guide for tone, structure, and level of detail
                8. Create coherent, flowing clinical narrative paragraphs
                """;

        String lastName = clientName;
        int lastSpace = clientName.lastIndexOf(' ');
        if (lastSpace > 0 && lastSpace < clientName.length() - 1) {
            lastName = clientName.substring(lastSpace + 1);
        }

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Generate a comprehensive clinical assessment report.\n\n");
        userPrompt.append("CONTEXT (for personalizing the narrative - DO NOT output this as a section):\n");
        userPrompt.append("Client: ").append(clientName).append("\n");
        userPrompt.append("Gender: ").append(gender).append("\n");
        userPrompt.append("Address: ").append(address).append("\n\n");
        userPrompt.append("CRITICAL RULES:\n");
        userPrompt.append("- DO NOT create a \"CLIENT INFORMATION\" section - this is shown separately in the UI\n");
        userPrompt.append("- Start immediately with the first assessment section below\n");
        userPrompt.append("- Use the client's actual name \"").append(clientName)
                .append("\" when writing narratives (e.g., \"Mr./Ms. ").append(lastName).append("\")\n");
        userPrompt.append("- Use appropriate pronouns for ").append(gender).append(" client\n");
        userPrompt.append("- Output must be properly formatted HTML without inline styles\n");
        userPrompt.append("- Each section: <h2>SECTION NAME</h2> then <p><br></p> then content in <p> tags\n");
        userPrompt.append("- Add blank paragraphs (<p><br></p>) between content paragraphs for readability\n");
        userPrompt.append("- Transform responses into professional clinical narrative\n");
        userPrompt.append("- Use <strong> tags to emphasize key clinical terms\n\n");
        userPrompt.append("ASSESSMENT SECTIONS TO GENERATE:\n\n");

        Map<Long, AssessmentResponse> uniqueByQuestion = dedupeResponsesByQuestion(responses);

        List<AssessmentSection> orderedSections = sections.stream()
                .sorted(Comparator.comparing(s -> s.getSortOrder() != null ? s.getSortOrder() : 0))
                .collect(Collectors.toList());

        for (AssessmentSection section : orderedSections) {
            List<AssessmentQuestion> questions = section.getQuestions() != null
                    ? section.getQuestions().stream()
                            .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                            .sorted(Comparator.comparing(q -> q.getSortOrder() != null ? q.getSortOrder() : 0))
                            .collect(Collectors.toList())
                    : Collections.emptyList();

            List<AssessmentResponse> sectionResponses = questions.stream()
                    .map(AssessmentQuestion::getId)
                    .map(uniqueByQuestion::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (sectionResponses.isEmpty()) {
                continue;
            }

            userPrompt.append("\n<h2>").append(section.getTitle().toUpperCase()).append("</h2>\n<p><br></p>\n");

            BigDecimal sectionTotal = null;
            if (Boolean.TRUE.equals(section.getIsScoring())) {
                sectionTotal = sectionResponses.stream()
                        .map(AssessmentResponse::getScoreValue)
                        .filter(Objects::nonNull)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
            }

            if (StringUtils.hasText(section.getAiReportPrompt())) {
                userPrompt.append("Section Template Instructions:\n")
                        .append(section.getAiReportPrompt()).append("\n\n");
                userPrompt.append("CRITICAL: The template above shows the EXACT format you should follow. ")
                        .append("Write flowing narrative paragraphs matching that example style. ")
                        .append("Do NOT use bullet points or lists.\n\n");
                if (sectionTotal != null) {
                    int maxPerItem = 0;
                    if (!questions.isEmpty() && questions.get(0).getOptions() != null
                            && !questions.get(0).getOptions().isEmpty()) {
                        maxPerItem = Math.max(0, questions.get(0).getOptions().size() - 1);
                    }
                    int maxPossible = sectionResponses.size() * maxPerItem;
                    userPrompt.append("CRITICAL - USE EXACT SCORE: This section's total score is ")
                            .append(sectionTotal).append(" out of ").append(maxPossible)
                            .append(". You MUST report this EXACT score - do not recalculate or interpret it.\n");
                    userPrompt.append("Number of items: ").append(sectionResponses.size()).append("\n\n");
                }
            } else {
                userPrompt.append(defaultSectionInstructions(section.getTitle()));
            }

            userPrompt.append("Client Data (use ALL details below to fill the template completely):\n\n");
            for (AssessmentResponse response : sectionResponses) {
                AssessmentQuestion question = response.getQuestion();
                if (question == null) {
                    continue;
                }
                String answerText = formatAssessmentAnswer(question, response);
                if (Boolean.TRUE.equals(section.getIsScoring()) && response.getScoreValue() != null) {
                    int score = response.getScoreValue().intValue();
                    String severityLabel = score <= 0 ? "Not endorsed"
                            : score == 1 ? "Mild"
                            : score == 2 ? "Moderate"
                            : "Severe";
                    userPrompt.append("• ").append(question.getQuestionText()).append("\n")
                            .append("  → ").append(answerText)
                            .append(" (Score: ").append(score).append(" - ").append(severityLabel).append(")\n\n");
                } else {
                    userPrompt.append("• ").append(question.getQuestionText()).append("\n")
                            .append("  → ").append(answerText).append("\n\n");
                }
            }
            userPrompt.append("\nREMINDER: Transform the answers above into professional narrative following the template format. ")
                    .append("Use the actual client answers - do not invent information not provided.\n\n");
        }

        List<AssessmentSection> generalSections = orderedSections.stream()
                .filter(section -> StringUtils.hasText(section.getReportMapping())
                        && !"none".equalsIgnoreCase(section.getReportMapping().trim())
                        && StringUtils.hasText(section.getAiReportPrompt()))
                .collect(Collectors.toList());

        for (AssessmentSection section : generalSections) {
            userPrompt.append("\n<h2>").append(section.getTitle().toUpperCase()).append("</h2>\n<p><br></p>\n");
            userPrompt.append("Instructions: ").append(section.getAiReportPrompt()).append("\n");
            userPrompt.append("Wrap content in <p> tags and separate paragraphs with <p><br></p> for readability.\n\n");

            List<AssessmentQuestion> questions = section.getQuestions() != null
                    ? section.getQuestions().stream()
                            .filter(q -> !Boolean.TRUE.equals(q.getIsDeleted()))
                            .collect(Collectors.toList())
                    : Collections.emptyList();
            if (!questions.isEmpty()) {
                userPrompt.append("Section-Specific Data:\n");
                for (AssessmentQuestion question : questions) {
                    AssessmentResponse response = uniqueByQuestion.get(question.getId());
                    if (response == null) {
                        continue;
                    }
                    userPrompt.append("Q: ").append(question.getQuestionText()).append("\n");
                    userPrompt.append("A: ").append(formatAssessmentAnswer(question, response)).append("\n\n");
                }
            }
            userPrompt.append("Assessment Synthesis: Analyze and synthesize ALL assessment responses and findings ")
                    .append("provided above to generate this section according to the instructions.\n\n");
        }

        if (generalSections.isEmpty()) {
            userPrompt.append("""

                    <h2>CLINICAL SUMMARY</h2>
                    <p><br></p>

                    Instructions: Generate a comprehensive clinical summary that synthesizes all assessment findings. Include:
                    - Overall clinical presentation and diagnostic impressions
                    - Key symptoms and their severity/impact
                    - Risk factors and protective factors
                    - Functional impairments and strengths
                    - Clinical observations and professional judgment
                    Use third-person clinical language suitable for diagnostic and treatment planning purposes.
                    Wrap all narrative content in <p> tags. Separate paragraphs with <p><br></p> for better readability.

                    Client Response Data: Use all the assessment responses provided above to synthesize this summary.

                    <h2>INTERVENTION PLAN AND RECOMMENDATIONS</h2>
                    <p><br></p>

                    Instructions: Generate evidence-based treatment recommendations and intervention planning based on the assessment findings. Include:
                    - Recommended treatment modalities and therapeutic approaches
                    - Specific intervention targets and goals
                    - Referral recommendations if appropriate
                    - Risk management strategies if indicated
                    - Timeline and frequency recommendations
                    - Client strengths that can support treatment
                    Use professional clinical language appropriate for treatment planning documentation.
                    Wrap all narrative content in <p> tags. Separate paragraphs with <p><br></p> for better readability.

                    Client Response Data: Base recommendations on the assessment findings and clinical summary above.
                    """);
        }

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(systemPrompt));
        messages.add(userMessage(userPrompt.toString()));

        String report = openAiClient.createChatCompletion(sessionNoteModel, messages, 0.0, 4000);
        return sanitizeAssessmentReportHtml(report);
    }

    private Map<Long, AssessmentResponse> dedupeResponsesByQuestion(List<AssessmentResponse> responses) {
        Map<Long, AssessmentResponse> unique = new LinkedHashMap<>();
        for (AssessmentResponse response : responses) {
            if (response.getQuestion() == null || response.getQuestion().getId() == null) {
                continue;
            }
            Long questionId = response.getQuestion().getId();
            AssessmentResponse existing = unique.get(questionId);
            if (existing == null) {
                unique.put(questionId, response);
                continue;
            }
            boolean hasScore = response.getScoreValue() != null;
            boolean existingHasScore = existing.getScoreValue() != null;
            boolean hasData = StringUtils.hasText(response.getResponseText())
                    || (response.getSelectedOptions() != null && !response.getSelectedOptions().isEmpty())
                    || response.getRatingValue() != null;
            boolean existingHasData = StringUtils.hasText(existing.getResponseText())
                    || (existing.getSelectedOptions() != null && !existing.getSelectedOptions().isEmpty())
                    || existing.getRatingValue() != null;
            if ((hasScore && !existingHasScore) || (hasData && !existingHasData)) {
                unique.put(questionId, response);
            }
        }
        return unique;
    }

    private String defaultSectionInstructions(String title) {
        String sectionTitle = title != null ? title.toLowerCase(Locale.ROOT) : "";
        if (sectionTitle.contains("background") || sectionTitle.contains("history")) {
            return "Instructions: Generate a comprehensive clinical background narrative for the \"" + title
                    + "\" section. Focus on historical information, developmental factors, and contextual elements that inform the clinical picture. Use third-person clinical language appropriate for medical documentation.\n\n";
        }
        if (sectionTitle.contains("symptom") || sectionTitle.contains("present")) {
            return "Instructions: Generate a detailed presentation of current symptoms and concerns for the \"" + title
                    + "\" section. Focus on symptom severity, frequency, impact on functioning, and clinical observations. Use diagnostic criteria language where appropriate.\n\n";
        }
        if (sectionTitle.contains("mental status") || sectionTitle.contains("cognitive")) {
            return "Instructions: Generate a formal mental status examination narrative for the \"" + title
                    + "\" section. Include observations of appearance, behavior, mood, affect, thought process, thought content, perception, cognition, insight, and judgment as relevant to the responses.\n\n";
        }
        return "Instructions: Generate a professional clinical narrative for the \"" + title
                + "\" section using third-person language appropriate for clinical documentation. Focus on clinically relevant information and observations.\n\n";
    }

    private String formatAssessmentAnswer(AssessmentQuestion question, AssessmentResponse response) {
        String type = question.getQuestionType() != null
                ? question.getQuestionType().trim().toLowerCase(Locale.ROOT).replace('-', '_')
                : "";

        if ("short_text".equals(type) || "long_text".equals(type) || "text".equals(type) || "textarea".equals(type)
                || type.contains("text")) {
            return StringUtils.hasText(response.getResponseText()) ? response.getResponseText() : "Not provided";
        }

        if ("multiple_choice".equals(type) || "radio".equals(type) || type.contains("choice")) {
            String selected = formatSelectedOptionTexts(question, response);
            if (StringUtils.hasText(selected)) {
                return selected;
            }
            return StringUtils.hasText(response.getResponseText()) ? response.getResponseText() : "Not selected";
        }

        if ("checkbox".equals(type) || type.contains("multi")) {
            String selected = formatSelectedOptionTexts(question, response);
            if (StringUtils.hasText(selected)) {
                return selected.replace(", ", "; ");
            }
            return StringUtils.hasText(response.getResponseText()) ? response.getResponseText() : "None selected";
        }

        if (("rating_scale".equals(type) || type.contains("rating")) && response.getRatingValue() != null) {
            int rating = response.getRatingValue();
            int max = question.getRatingMax() != null ? question.getRatingMax() : 10;
            String minLabel = "Low";
            String maxLabel = "High";
            if (question.getRatingLabels() != null && !question.getRatingLabels().isEmpty()) {
                List<AssessmentQuestionRatingLabel> labels = question.getRatingLabels().stream()
                        .filter(l -> !Boolean.FALSE.equals(l.getActive()))
                        .sorted(Comparator.comparing(l -> l.getScore() != null ? l.getScore() : 0))
                        .collect(Collectors.toList());
                if (!labels.isEmpty()) {
                    minLabel = labels.get(0).getLabel();
                    maxLabel = labels.size() > 1 ? labels.get(1).getLabel() : labels.get(0).getLabel();
                    if (labels.size() > 2) {
                        maxLabel = labels.get(labels.size() - 1).getLabel();
                    }
                }
            }
            return rating + "/" + max + " (" + minLabel + " to " + maxLabel + " scale)";
        }

        if ("number".equals(type) && StringUtils.hasText(response.getResponseValue())) {
            return response.getResponseValue();
        }

        if ("date".equals(type) && StringUtils.hasText(response.getResponseText())) {
            return response.getResponseText();
        }

        return StringUtils.hasText(response.getResponseText()) ? response.getResponseText() : "Not provided";
    }

    private String formatSelectedOptionTexts(AssessmentQuestion question, AssessmentResponse response) {
        if (response.getSelectedOptions() == null || response.getSelectedOptions().isEmpty()) {
            return null;
        }
        List<String> texts = response.getSelectedOptions().stream()
                .map(AssessmentResponseOption::getOption)
                .filter(Objects::nonNull)
                .map(AssessmentQuestionOption::getOptionText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
        if (texts.isEmpty() && question.getOptions() != null) {
            Set<Long> selectedIds = response.getSelectedOptions().stream()
                    .map(opt -> opt.getOption() != null ? opt.getOption().getId() : null)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            texts = question.getOptions().stream()
                    .filter(opt -> selectedIds.contains(opt.getId()))
                    .map(AssessmentQuestionOption::getOptionText)
                    .filter(StringUtils::hasText)
                    .collect(Collectors.toList());
        }
        return texts.isEmpty() ? null : String.join(", ", texts);
    }

    private String formatClientAddressForPrompt(Client client) {
        ClientAddress primary = client.getPrimaryAddress();
        if (primary == null) {
            return "Not provided";
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(primary.getStreetAddress1())) {
            parts.add(sanitizeForPrompt(primary.getStreetAddress1()));
        }
        if (StringUtils.hasText(primary.getStreetAddress2())) {
            parts.add(sanitizeForPrompt(primary.getStreetAddress2()));
        }
        List<String> cityLine = new ArrayList<>();
        if (StringUtils.hasText(primary.getCity())) {
            cityLine.add(sanitizeForPrompt(primary.getCity()));
        }
        if (StringUtils.hasText(primary.getStateProvince())) {
            cityLine.add(sanitizeForPrompt(primary.getStateProvince()));
        }
        if (StringUtils.hasText(primary.getPostalCode())) {
            cityLine.add(sanitizeForPrompt(primary.getPostalCode()));
        }
        cityLine.removeIf(part -> !StringUtils.hasText(part) || "Not provided".equals(part));
        if (!cityLine.isEmpty()) {
            parts.add(String.join(", ", cityLine));
        }
        if (StringUtils.hasText(primary.getCountry())) {
            parts.add(sanitizeForPrompt(primary.getCountry()));
        }
        parts.removeIf(part -> !StringUtils.hasText(part) || "Not provided".equals(part));
        return parts.isEmpty() ? "Not provided" : String.join(", ", parts);
    }

    private String sanitizeForPrompt(String value) {
        if (!StringUtils.hasText(value)) {
            return "Not provided";
        }
        return value
                .replaceAll("(?is)<script[^>]*>.*?</script>", "")
                .replaceAll("<[^>]+>", "")
                .trim();
    }

    private String sanitizeAssessmentReportHtml(String generatedContent) {
        if (generatedContent == null) {
            return "";
        }
        String content = generatedContent.trim();
        content = content.replaceFirst("(?i)^```(?:html)?\\s*\\n", "");
        content = content.replaceFirst("(?i)\\n```\\s*$", "");
        content = content.replaceAll("(?i)```(?:html)?\\s*", "");
        return content.trim();
    }

    /**
     * Generate AI client report HTML from template structure and aggregated client data.
     */
    public String generateClientReportFromTemplate(
            com.smart.therapy.flow.report.entity.ReportTemplate template,
            com.smart.therapy.flow.report.service.ClientReportDataAggregator.AggregatedReportData data) {
        Objects.requireNonNull(template, "Template is required");
        Objects.requireNonNull(data, "Report data is required");
        enforceAndTrackAiLimit(
                SubscriptionFeatureService.FEATURE_AI_REPORTS_PER_MONTH,
                "AI client report generation");

        String structureText = template.getStructureText() != null
                ? truncate(template.getStructureText(), 12_000) : "";
        String aiInstructions = template.getAiInstructions() != null
                ? truncate(template.getAiInstructions(), 4_000) : "";

        StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("Generate a clinical psychotherapy progress report as clean HTML.\n\n");
        userPrompt.append("REPORT TEMPLATE — use these section headings in this order. ");
        userPrompt.append("The outline describes structure only; do not copy boilerplate, invoice lines, ");
        userPrompt.append("or placeholder text from it into the report.\n");
        userPrompt.append(structureText).append("\n\n");
        if (StringUtils.hasText(aiInstructions)) {
            userPrompt.append("CLINICIAN INSTRUCTIONS:\n").append(aiInstructions).append("\n\n");
        }
        userPrompt.append("SOURCE DATA (internal reference — synthesize into narrative paragraphs under ");
        userPrompt.append("the template headings; do NOT reproduce these block titles or raw lists):\n\n");
        appendBlock(userPrompt, data.getProfileBlock());
        appendBlock(userPrompt, data.getSessionStatsBlock());
        appendBlock(userPrompt, data.getSessionsBlock());
        appendBlock(userPrompt, data.getNotesBlock());
        appendBlock(userPrompt, data.getAssessmentsBlock());
        appendBlock(userPrompt, data.getSupportingFilesBlock());

        userPrompt.append("SOURCE AVAILABILITY FLAGS:\n");
        userPrompt.append("- Profile: ").append(data.isIncludeProfile() ? "ENABLED" : "DISABLED").append('\n');
        userPrompt.append("- Session notes / sessions: ").append(data.isIncludeNotes() ? "ENABLED" : "DISABLED").append('\n');
        userPrompt.append("- Assessments: ").append(data.isIncludeAssessments()
                ? "ENABLED" : "DISABLED").append('\n');
        if (!data.isIncludeAssessments()) {
            userPrompt.append("Assessments are DISABLED. For \"Assessment Findings and Initial Treatment Goals\" ");
            userPrompt.append("(and any psychometric / assessment-results section), output ONLY:\n");
            userPrompt.append("<h2>Assessment Findings and Initial Treatment Goals</h2>\n");
            userPrompt.append("<p>Information not available.</p>\n");
            userPrompt.append("Do not invent assessment findings, goals, or diagnoses from session notes or profile.\n\n");
        } else {
            userPrompt.append("Assessments are ENABLED. You MUST write a narrative for ");
            userPrompt.append("\"Assessment Findings and Initial Treatment Goals\" using the ASSESSMENTS source block ");
            userPrompt.append("(assignment names, statuses, scores, clinician notes, response content, and any ");
            userPrompt.append("assessment report summaries). Summarize findings and treatment goals when present. ");
            userPrompt.append("Only write <p>Information not available.</p> if ASSESSMENTS says \"None assigned.\" ");
            userPrompt.append("If assessments are assigned but incomplete/pending, state that status and any partial ");
            userPrompt.append("results explicitly — do not leave the section empty and do not invent results.\n\n");
        }

        String systemPrompt = """
                You are a professional clinical documentation assistant for a therapy practice.
                Write a polished clinical report as narrative HTML using only the template section headings.

                Rules:
                - Use ONLY facts from the source data — never invent diagnoses, dates, scores, or clinical facts.
                - Do NOT write "Client Information" or "Referral Information" sections (name, DOB, age, gender, \
                report date, clinician, referral source/date/reason). The server inserts those exactly from \
                the client profile after your output.
                - Do not output sections titled CLIENT PROFILE, SESSIONS, SESSION NOTES, ASSESSMENTS, \
                PSYCHOTHERAPY SESSION STATISTICS, SUPPORTING DOCUMENTS, CLINICIAN INSTRUCTIONS, or ADMIN INSTRUCTIONS.
                - Do not dump raw session, note, or assessment lists unless the template explicitly requires a log.
                - For treatment participation, use the server-computed psychotherapy session statistics exactly.
                - If SOURCE AVAILABILITY marks Assessments as DISABLED, Assessment Findings sections must be \
                exactly: <p>Information not available.</p> — never fill them from notes or profile.
                - If SOURCE AVAILABILITY marks Assessments as ENABLED, write Assessment Findings from the \
                ASSESSMENTS source block. Do NOT default that section to Information not available when \
                assessments are listed (even if pending/incomplete — report status and available facts).
                - For other template sections with no supporting source data, write: <p>Information not available.</p>
                - Output raw HTML only — no markdown and no ``` code fences.
                - Allowed tags: h2, h3, p, strong, ul, ol, li, br. No inline styles.
                - Use complete sentences and professional clinical narrative paragraphs.
                """;

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage(systemPrompt));
        messages.add(userMessage(userPrompt.toString()));

        String report = openAiClient.createChatCompletion(sessionNoteModel, messages, 0.2, 4000);
        return com.smart.therapy.flow.report.util.HtmlSanitizer.stripMarkdownFences(
                report != null ? report.trim() : "");
    }

    private void appendBlock(StringBuilder sb, String block) {
        if (StringUtils.hasText(block)) {
            sb.append(block).append("\n\n");
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max);
    }

    private String resolveOptionLabel(String categoryKey, String optionKey) {
        if (!StringUtils.hasText(optionKey)) {
            return null;
        }
        return systemOptionResolverService.resolveOptionLabel(categoryKey, optionKey);
    }

    private String resolveSessionModeLabel(String sessionModeKey) {
        return resolveOptionLabel(SystemOptionCategories.SESSION_MODE, sessionModeKey);
    }
}
