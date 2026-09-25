package com.smart.therapy.flow.report.service;

import com.smart.therapy.flow.assessment.entity.AssessmentAssignment;
import com.smart.therapy.flow.assessment.entity.AssessmentReport;
import com.smart.therapy.flow.assessment.entity.AssessmentResponse;
import com.smart.therapy.flow.assessment.repository.AssessmentAssignmentRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentReportRepository;
import com.smart.therapy.flow.assessment.repository.AssessmentResponseRepository;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientReferral;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientContactRepository;
import com.smart.therapy.flow.report.entity.ReportSupportingFile;
import com.smart.therapy.flow.report.entity.ReportTemplate;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientReportDataAggregator {

    private static final int MAX_SESSIONS = 40;
    private static final int MAX_NOTES = 20;
    private static final int MAX_ASSESSMENTS = 20;
    /** Display for missing profile / referral fields (exact, not AI). */
    private static final String MISSING = "_";
    private static final Pattern PSYCHOTHERAPY_CODE = Pattern.compile(
            "^(psy|ifh-[0-9]+|fam|cou|ink)", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter DISPLAY_DATE =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);
    private static final Pattern AI_CLIENT_INFO_SECTION = Pattern.compile(
            "(?is)<h[23][^>]*>\\s*Client\\s+Information\\s*</h[23]>.*?(?=<h[23][^>]*>|\\z)");
    private static final Pattern AI_REFERRAL_SECTION = Pattern.compile(
            "(?is)<h[23][^>]*>\\s*Referral\\s+Information\\s*</h[23]>.*?(?=<h[23][^>]*>|\\z)");
    /** Template section filled only when assessment source data was included. */
    private static final Pattern ASSESSMENT_FINDINGS_SECTION = Pattern.compile(
            "(?is)<h[23][^>]*>\\s*Assessment\\s+Findings(?:\\s+and\\s+Initial\\s+Treatment\\s+Goals)?\\s*</h[23]>.*?(?=<h[23][^>]*>|\\z)");
    private static final String ASSESSMENT_FINDINGS_UNAVAILABLE =
            "<h2>Assessment Findings and Initial Treatment Goals</h2>"
                    + "<p>Information not available.</p>";

    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final AssessmentAssignmentRepository assessmentAssignmentRepository;
    private final AssessmentResponseRepository assessmentResponseRepository;
    private final AssessmentReportRepository assessmentReportRepository;
    private final ClientContactRepository clientContactRepository;
    private final UserProfileRepository userProfileRepository;
    private final SystemOptionResolverService systemOptionResolverService;

    @Data
    @Builder
    public static class AggregatedReportData {
        private String profileBlock;
        private String sessionStatsBlock;
        private String sessionsBlock;
        private String notesBlock;
        private String assessmentsBlock;
        private String supportingFilesBlock;
        /** Request/template source flags — used to force excluded sections unavailable. */
        private boolean includeProfile;
        private boolean includeNotes;
        private boolean includeAssessments;

        /** True when any data block was supplied for the AI (profile, notes, assessments, files). */
        public boolean hasUsableSourceData() {
            return StringUtils.hasText(profileBlock)
                    || StringUtils.hasText(sessionStatsBlock)
                    || StringUtils.hasText(sessionsBlock)
                    || StringUtils.hasText(notesBlock)
                    || StringUtils.hasText(assessmentsBlock)
                    || StringUtils.hasText(supportingFilesBlock);
        }
    }

    public AggregatedReportData aggregate(
            Client client,
            ReportTemplate template,
            boolean includeProfile,
            boolean includeNotes,
            boolean includeAssessments,
            List<ReportSupportingFile> supportingFiles) {

        return AggregatedReportData.builder()
                .profileBlock(includeProfile ? buildProfileBlock(client) : null)
                .sessionStatsBlock(includeNotes ? buildSessionStats(client.getId()) : null)
                .sessionsBlock(includeNotes ? buildSessionsBlock(client.getId()) : null)
                .notesBlock(includeNotes ? buildNotesBlock(client.getId()) : null)
                .assessmentsBlock(includeAssessments ? buildAssessmentsBlock(client.getId()) : null)
                .supportingFilesBlock(buildSupportingFilesBlock(supportingFiles))
                .includeProfile(includeProfile)
                .includeNotes(includeNotes)
                .includeAssessments(includeAssessments)
                .build();
    }

    /**
     * Deterministic narrative HTML when no sources were selected — skips AI and inventing content.
     * Client Information / Referral Information are omitted (server inject adds them).
     */
    public String buildUnavailableNarrativeHtml(ReportTemplate template) {
        List<String> headings = extractNarrativeSectionHeadings(
                template != null ? template.getStructureText() : null);
        StringBuilder out = new StringBuilder();
        for (String heading : headings) {
            out.append("<h2>").append(escapeHtml(heading)).append("</h2>");
            out.append("<p>Information not available.</p>");
        }
        return out.toString();
    }

    /**
     * Overwrites Client Information and Referral Information with exact profile data.
     * Missing values render as {@code _}. All other AI narrative sections are preserved.
     */
    public String injectExactProfileAndReferralSections(String aiHtml, Client client) {
        String rest = aiHtml != null ? aiHtml : "";
        rest = AI_CLIENT_INFO_SECTION.matcher(rest).replaceAll("");
        rest = AI_REFERRAL_SECTION.matcher(rest).replaceAll("");
        rest = rest.trim();

        StringBuilder out = new StringBuilder();
        out.append(buildClientInformationHtml(client));
        out.append(buildReferralInformationHtml(client));
        if (StringUtils.hasText(rest)) {
            out.append(rest);
        }
        return out.toString().trim();
    }

    /**
     * Honors assessment source flag after AI generation:
     * <ul>
     *   <li>flag false → always "Information not available" for Assessment Findings</li>
     *   <li>flag true + real assessment records → if the model left the section empty / N/A,
     *       replace with a factual fallback built from assessment assignments</li>
     * </ul>
     */
    public String enforceExcludedSourceSections(String html, AggregatedReportData data) {
        if (!StringUtils.hasText(html) || data == null) {
            return html != null ? html : "";
        }
        String result = html;
        if (!data.isIncludeAssessments()) {
            // Assessments disabled: never infer findings from notes/profile
            result = ASSESSMENT_FINDINGS_SECTION.matcher(result)
                    .replaceAll(Matcher.quoteReplacement(ASSESSMENT_FINDINGS_UNAVAILABLE));
            return result.trim();
        }

        // Assessments enabled: if the model defaulted to N/A but we have assignment data,
        // inject a deterministic factual summary so the section is not empty.
        if (hasAssignedAssessments(data.getAssessmentsBlock())) {
            Matcher sectionMatcher = ASSESSMENT_FINDINGS_SECTION.matcher(result);
            if (sectionMatcher.find()) {
                if (isUnavailableAssessmentSection(sectionMatcher.group())) {
                    String fallback = buildAssessmentFindingsFallbackHtml(data.getAssessmentsBlock());
                    if (StringUtils.hasText(fallback)) {
                        result = sectionMatcher.replaceFirst(Matcher.quoteReplacement(fallback));
                    }
                }
            } else if (StringUtils.hasText(data.getAssessmentsBlock())) {
                String fallback = buildAssessmentFindingsFallbackHtml(data.getAssessmentsBlock());
                if (StringUtils.hasText(fallback)) {
                    result = result + fallback;
                }
            }
        }
        return result.trim();
    }

    private static boolean hasAssignedAssessments(String assessmentsBlock) {
        return StringUtils.hasText(assessmentsBlock)
                && !assessmentsBlock.toLowerCase(Locale.ROOT).contains("none assigned");
    }

    private static boolean isUnavailableAssessmentSection(String sectionHtml) {
        if (!StringUtils.hasText(sectionHtml)) {
            return true;
        }
        String body = sectionHtml
                .replaceAll("(?is)<h[23][^>]*>.*?</h[23]>", " ")
                .replaceAll("(?is)<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.ROOT);
        return !StringUtils.hasText(body) || body.contains("information not available");
    }

    /**
     * Factual Assessment Findings HTML from the ASSESSMENTS source block (no clinical invention).
     */
    private String buildAssessmentFindingsFallbackHtml(String assessmentsBlock) {
        if (!hasAssignedAssessments(assessmentsBlock)) {
            return ASSESSMENT_FINDINGS_UNAVAILABLE;
        }
        // Convert the human-readable assessments block into short narrative paragraphs.
        String plain = assessmentsBlock
                .replace("ASSESSMENTS (use for Assessment Findings and Initial Treatment Goals):", "")
                .replace("ASSESSMENTS:", "")
                .trim();
        if (!StringUtils.hasText(plain)) {
            return ASSESSMENT_FINDINGS_UNAVAILABLE;
        }

        StringBuilder out = new StringBuilder();
        out.append("<h2>Assessment Findings and Initial Treatment Goals</h2>");
        out.append("<p>The following assessment information was available for this client:</p>");
        // Split on assignment separators written by buildAssessmentsBlock
        String[] parts = plain.split("(?m)^--- Assessment:");
        int written = 0;
        for (String part : parts) {
            if (!StringUtils.hasText(part) || written >= MAX_ASSESSMENTS) {
                continue;
            }
            String chunk = part.trim();
            if (chunk.isEmpty()) {
                continue;
            }
            // First line is name --- rest is metadata; keep readable sentences.
            String[] lines = chunk.split("\\R");
            String name = lines[0].replaceAll("^\\s*|\\s*---\\s*$", "").trim();
            if (name.endsWith("---")) {
                name = name.substring(0, name.length() - 3).trim();
            }
            StringBuilder details = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i].trim();
                if (!StringUtils.hasText(line) || line.startsWith("…") || line.startsWith("(")) {
                    continue;
                }
                if (line.startsWith("- ")) {
                    line = line.substring(2).trim();
                }
                if (line.startsWith("• ")) {
                    line = line.substring(2).trim();
                }
                // Skip raw response dumps in fallback when long — prefer status/score/notes/report
                if (line.toLowerCase(Locale.ROOT).startsWith("responses")) {
                    if (line.toLowerCase(Locale.ROOT).contains("none")
                            || line.toLowerCase(Locale.ROOT).contains("unavailable")) {
                        details.append(line).append(". ");
                    }
                    break;
                }
                if (line.toLowerCase(Locale.ROOT).startsWith("assessment report")) {
                    // Include a short plain-text excerpt if present on following lines
                    details.append("An assessment report is on file. ");
                    StringBuilder reportExcerpt = new StringBuilder();
                    for (int j = i + 1; j < lines.length && reportExcerpt.length() < 600; j++) {
                        String rl = lines[j].trim();
                        if (!StringUtils.hasText(rl) || rl.startsWith("---") || rl.startsWith("- ")) {
                            break;
                        }
                        reportExcerpt.append(rl).append(' ');
                    }
                    if (reportExcerpt.length() > 0) {
                        details.append(truncate(reportExcerpt.toString().trim(), 500)).append(' ');
                    }
                    break;
                }
                details.append(line);
                if (!line.endsWith(".")) {
                    details.append('.');
                }
                details.append(' ');
            }
            out.append("<p><strong>")
                    .append(escapeHtml(StringUtils.hasText(name) ? name : "Assessment"))
                    .append("</strong>. ")
                    .append(escapeHtml(details.toString().trim()))
                    .append("</p>");
            written++;
        }
        if (written == 0) {
            return ASSESSMENT_FINDINGS_UNAVAILABLE;
        }
        return out.toString();
    }

    private static final List<String> FALLBACK_NARRATIVE_HEADINGS = List.of(
            "Brief Background Summary",
            "Clinical Presentation",
            "Assessment Findings and Initial Treatment Goals",
            "Treatment Summary and Interventions",
            "Overall Clinical Progress",
            "Recommendations and Future Treatment Focus");

    private static final Set<String> SKIP_STRUCTURE_HEADINGS = Set.of(
            "client information",
            "referral information",
            "purpose",
            "session count instructions",
            "progress evaluation",
            "report generation");

    /**
     * Pulls section titles from the template structure outline for empty-source reports.
     */
    List<String> extractNarrativeSectionHeadings(String structureText) {
        if (!StringUtils.hasText(structureText)) {
            return new ArrayList<>(FALLBACK_NARRATIVE_HEADINGS);
        }

        List<String> headings = new ArrayList<>();
        for (String rawLine : structureText.split("\\R")) {
            String line = rawLine != null ? rawLine.trim() : "";
            if (!isCandidateStructureHeading(line)) {
                continue;
            }
            String key = line.toLowerCase(Locale.ROOT);
            if (SKIP_STRUCTURE_HEADINGS.contains(key) || key.startsWith("purpose")) {
                continue;
            }
            if (key.contains("template") && key.contains("guide")) {
                continue;
            }
            if (headings.stream().anyMatch(h -> h.equalsIgnoreCase(line))) {
                continue;
            }
            headings.add(line);
        }

        if (headings.isEmpty()) {
            return new ArrayList<>(FALLBACK_NARRATIVE_HEADINGS);
        }
        return headings;
    }

    private boolean isCandidateStructureHeading(String line) {
        if (!StringUtils.hasText(line) || line.length() < 4 || line.length() > 120) {
            return false;
        }
        // Field lists / sentences / bullets are not section titles
        if (line.startsWith("•") || line.startsWith("-") || line.startsWith("*")) {
            return false;
        }
        if (line.contains(":")) {
            return false;
        }
        long commaCount = line.chars().filter(c -> c == ',').count();
        if (commaCount >= 2) {
            return false;
        }
        // Prose usually ends with sentence punctuation
        if (line.endsWith(".") || line.endsWith(";") || line.endsWith("!")) {
            return false;
        }
        // Prefer Title Case / short phrase style (e.g. "Brief Background Summary")
        int letters = 0;
        int lower = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (Character.isLetter(c)) {
                letters++;
                if (Character.isLowerCase(c)) {
                    lower++;
                }
            }
        }
        if (letters < 4) {
            return false;
        }
        // Too much lowercase continuous prose
        if (lower > letters * 0.55 && line.split("\\s+").length > 8) {
            return false;
        }
        return Character.isLetter(line.charAt(0));
    }

    private String buildClientInformationHtml(Client client) {
        String name = valueOrMissing(client != null ? client.getFullName() : null);
        String dob = client != null && client.getDateOfBirth() != null
                ? DISPLAY_DATE.format(client.getDateOfBirth())
                : MISSING;
        String age = formatAge(client);
        String gender = resolveGender(client != null ? client.getGender() : null);
        String reportDate = DISPLAY_DATE.format(LocalDate.now());
        String clinician = formatClinician(client);

        return "<h2>Client Information</h2>"
                + line("Client Name", name)
                + line("Date of Birth", dob)
                + line("Age", age)
                + line("Gender", gender)
                + line("Report Date", reportDate)
                + line("Clinician Name and Credentials", clinician);
    }

    private String buildReferralInformationHtml(Client client) {
        ClientReferral referral = null;
        try {
            referral = client != null ? client.getReferral() : null;
        } catch (Exception ignored) {
            referral = null;
        }

        String source = MISSING;
        String referralDate = MISSING;
        String reason = MISSING;

        if (referral != null) {
            String rawSource = firstText(
                    referral.getReferralSource(),
                    referral.getReferrerOrganization(),
                    referral.getReferrerName());
            source = valueOrMissing(resolveReferralSourceLabel(rawSource));
            if (referral.getReferralDate() != null) {
                referralDate = DISPLAY_DATE.format(referral.getReferralDate());
            }
            reason = valueOrMissing(firstText(referral.getIntakeSummary(), referral.getReferralNotes()));
        }

        return "<h2>Referral Information</h2>"
                + line("Referral Source", source)
                + line("Referral Date", referralDate)
                + line("Reason for Referral", reason);
    }

    private String buildProfileBlock(Client client) {
        StringBuilder sb = new StringBuilder();
        sb.append("CLIENT PROFILE (factual identity — narrative sections only elsewhere):\n");
        sb.append("- Name: ").append(nullSafe(client.getFullName())).append('\n');
        sb.append("- Client ID: ").append(nullSafe(client.getClientId())).append('\n');
        sb.append("- Date of Birth: ")
                .append(client.getDateOfBirth() != null ? client.getDateOfBirth() : "N/A").append('\n');
        sb.append("- Age: ").append(formatAge(client)).append('\n');
        sb.append("- Gender: ").append(resolveGender(client.getGender())).append('\n');
        sb.append("- Status: ").append(client.getStatus() != null ? client.getStatus() : "N/A").append('\n');
        clientContactRepository.findPrimaryEmailByClientId(client.getId(), ContactType.EMAIL).ifPresent(contact -> {
            if (StringUtils.hasText(contact.getContactValue())) {
                sb.append("- Email: ").append(contact.getContactValue()).append('\n');
            }
        });
        return sb.toString();
    }

    private String buildSessionStats(Long clientId) {
        List<Session> sessions = sessionRepository.findByClientIdWithRelations(clientId);
        Instant now = Instant.now();
        int psychotherapyCount = 0;
        Instant firstDate = null;
        Instant lastDate = null;

        for (Session session : sessions) {
            if (!isPsychotherapySession(session, now)) {
                continue;
            }
            psychotherapyCount++;
            if (firstDate == null || session.getSessionDate().isBefore(firstDate)) {
                firstDate = session.getSessionDate();
            }
            if (lastDate == null || session.getSessionDate().isAfter(lastDate)) {
                lastDate = session.getSessionDate();
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PSYCHOTHERAPY SESSION STATISTICS (server-computed, use exactly):\n");
        sb.append("- Completed psychotherapy sessions: ").append(psychotherapyCount).append('\n');
        sb.append("- First psychotherapy session date: ")
                .append(firstDate != null ? DATE_FMT.format(firstDate) : "N/A").append('\n');
        sb.append("- Most recent psychotherapy session date: ")
                .append(lastDate != null ? DATE_FMT.format(lastDate) : "N/A").append('\n');
        return sb.toString();
    }

    private String buildSessionsBlock(Long clientId) {
        List<Session> sessions = sessionRepository.findByClientIdWithRelations(clientId).stream()
                .sorted(Comparator.comparing(Session::getSessionDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_SESSIONS)
                .collect(Collectors.toList());

        if (sessions.isEmpty()) {
            return "SESSIONS: None recorded.\n";
        }

        StringBuilder sb = new StringBuilder("SESSIONS:\n");
        for (Session session : sessions) {
            String serviceCode = session.getService() != null ? session.getService().getServiceCode() : "N/A";
            sb.append("- Date: ").append(session.getSessionDate() != null ? DATE_FMT.format(session.getSessionDate()) : "N/A");
            sb.append(", Status: ").append(session.getStatus() != null ? session.getStatus() : "N/A");
            sb.append(", Service: ").append(serviceCode);
            sb.append(", Type: ").append(nullSafe(session.getClinicalSessionType())).append('\n');
        }
        return sb.toString();
    }

    private String buildNotesBlock(Long clientId) {
        List<SessionNote> notes = sessionNoteRepository.findByClientIdWithRelations(clientId).stream()
                .sorted(Comparator.comparing(SessionNote::getDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_NOTES)
                .collect(Collectors.toList());

        if (notes.isEmpty()) {
            return "SESSION NOTES: None recorded.\n";
        }

        StringBuilder sb = new StringBuilder("SESSION NOTES:\n");
        for (SessionNote note : notes) {
            sb.append("--- Note dated ").append(note.getDate() != null ? DATE_FMT.format(note.getDate()) : "N/A").append(" ---\n");
            String content = resolveNoteContent(note);
            sb.append(StringUtils.hasText(content) ? content : "No note content available.").append("\n\n");
        }
        return sb.toString();
    }

    private String buildAssessmentsBlock(Long clientId) {
        List<AssessmentAssignment> assignments = assessmentAssignmentRepository
                .findByClientIdWithRelations(clientId).stream()
                .filter(a -> a.getIsDeleted() == null || Boolean.FALSE.equals(a.getIsDeleted()))
                .sorted(Comparator.comparing(
                        AssessmentAssignment::getAssignedDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(MAX_ASSESSMENTS)
                .collect(Collectors.toList());

        if (assignments.isEmpty()) {
            return "ASSESSMENTS: None assigned.\n";
        }

        StringBuilder sb = new StringBuilder("ASSESSMENTS (use for Assessment Findings and Initial Treatment Goals):\n");
        for (AssessmentAssignment assignment : assignments) {
            String templateName = assignment.getTemplate() != null
                    ? assignment.getTemplate().getName() : "N/A";
            sb.append("--- Assessment: ").append(templateName).append(" ---\n");
            sb.append("- Status: ").append(nullSafe(assignment.getStatus())).append('\n');
            sb.append("- Assigned: ")
                    .append(assignment.getAssignedDate() != null
                            ? DATE_FMT.format(assignment.getAssignedDate()) : "N/A")
                    .append('\n');
            sb.append("- Completed: ")
                    .append(assignment.getCompletedAt() != null
                            ? DATE_FMT.format(assignment.getCompletedAt()) : "N/A")
                    .append('\n');
            sb.append("- Total score: ")
                    .append(assignment.getTotalScore() != null ? assignment.getTotalScore() : "N/A")
                    .append('\n');
            if (StringUtils.hasText(assignment.getNotes())) {
                sb.append("- Clinician notes: ").append(truncate(assignment.getNotes(), 1500)).append('\n');
            }

            try {
                Optional<AssessmentReport> reportOpt =
                        assessmentReportRepository.findByAssignmentId(assignment.getId());
                if (reportOpt.isPresent()) {
                    AssessmentReport report = reportOpt.get();
                    String reportText = firstText(
                            plainText(report.getFinalContent()),
                            plainText(report.getDraftContent()),
                            plainText(report.getGeneratedContent()),
                            plainText(report.getReportData()));
                    if (StringUtils.hasText(reportText)) {
                        sb.append("- Assessment report summary:\n")
                                .append(truncate(reportText, 3000)).append("\n");
                    }
                }
            } catch (Exception ignored) {
                // optional
            }

            try {
                List<AssessmentResponse> responses =
                        assessmentResponseRepository.findByAssignmentIdOrderByQuestionSortOrder(assignment.getId());
                if (responses != null && !responses.isEmpty()) {
                    sb.append("- Responses (question → answer):\n");
                    int count = 0;
                    for (AssessmentResponse response : responses) {
                        if (count >= 40) {
                            sb.append("  … additional responses truncated\n");
                            break;
                        }
                        String q = response.getQuestion() != null
                                ? nullSafe(response.getQuestion().getQuestionText())
                                : "Question";
                        String answer = formatResponseAnswer(response);
                        if (!StringUtils.hasText(answer)) {
                            continue;
                        }
                        sb.append("  • ").append(truncate(q, 200)).append(": ")
                                .append(truncate(answer, 400));
                        if (response.getScore() != null) {
                            sb.append(" (score ").append(response.getScore()).append(')');
                        }
                        sb.append('\n');
                        count++;
                    }
                    if (count == 0) {
                        sb.append("  (no answered responses recorded yet)\n");
                    }
                } else {
                    sb.append("- Responses: none recorded yet\n");
                }
            } catch (Exception ex) {
                sb.append("- Responses: unavailable\n");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String formatResponseAnswer(AssessmentResponse response) {
        if (response == null) {
            return null;
        }
        String direct = firstText(response.getResponseText(), response.getResponseValue());
        if (StringUtils.hasText(direct)) {
            return direct;
        }
        if (response.getSelectedOptions() != null && !response.getSelectedOptions().isEmpty()) {
            String options = response.getSelectedOptions().stream()
                    .map(sel -> {
                        if (sel.getOption() == null) {
                            return firstText(sel.getNotes());
                        }
                        return firstText(
                                sel.getOption().getOptionText(),
                                sel.getOption().getOptionValue(),
                                sel.getNotes());
                    })
                    .filter(StringUtils::hasText)
                    .collect(Collectors.joining("; "));
            if (StringUtils.hasText(options)) {
                return options;
            }
        }
        return response.getScore() != null ? "score " + response.getScore() : null;
    }

    private static String plainText(String htmlOrText) {
        if (!StringUtils.hasText(htmlOrText)) {
            return null;
        }
        String text = Jsoup.parse(htmlOrText).text();
        return StringUtils.hasText(text) ? text.trim() : null;
    }

    private String buildSupportingFilesBlock(List<ReportSupportingFile> files) {
        if (files == null || files.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("SUPPORTING DOCUMENTS:\n");
        for (ReportSupportingFile file : files) {
            sb.append("--- ").append(StringUtils.hasText(file.getDocumentType())
                    ? file.getDocumentType() : file.getOriginalName()).append(" ---\n");
            sb.append(StringUtils.hasText(file.getExtractedText())
                    ? truncate(file.getExtractedText(), 8000) : "No extracted text.").append("\n\n");
        }
        return sb.toString();
    }

    private boolean isPsychotherapySession(Session session, Instant now) {
        if (session.getStatus() == null || !SystemOptionKeyMatcher.matchesAny(session.getStatus(), "completed")) {
            return false;
        }
        if (session.getSessionDate() != null && session.getSessionDate().isAfter(now)) {
            return false;
        }
        String clinicalType = session.getClinicalSessionType();
        if (StringUtils.hasText(clinicalType)) {
            String normalizedType = clinicalType.toLowerCase(Locale.ROOT);
            if (normalizedType.contains("psychotherapy")) {
                return true;
            }
            if (normalizedType.contains("assessment")
                    || normalizedType.contains("intake")
                    || normalizedType.contains("consultation")
                    || normalizedType.contains("administrative")) {
                return false;
            }
        }
        if (session.getService() == null || !StringUtils.hasText(session.getService().getServiceCode())) {
            return false;
        }
        String code = session.getService().getServiceCode().toLowerCase(Locale.ROOT);
        if ("mva".equals(code)) {
            return true;
        }
        return PSYCHOTHERAPY_CODE.matcher(code).find();
    }

    private String resolveNoteContent(SessionNote note) {
        if (Boolean.TRUE.equals(note.getIsFinalized()) && StringUtils.hasText(note.getFinalContent())) {
            return note.getFinalContent();
        }
        if (StringUtils.hasText(note.getDraftContent())) {
            return note.getDraftContent();
        }
        if (StringUtils.hasText(note.getGeneratedContent())) {
            return note.getGeneratedContent();
        }
        List<String> parts = new ArrayList<>();
        appendIfPresent(parts, "Session Focus", note.getSessionFocus());
        appendIfPresent(parts, "Symptoms", note.getSymptoms());
        appendIfPresent(parts, "Intervention", note.getIntervention());
        appendIfPresent(parts, "Progress", note.getProgress());
        appendIfPresent(parts, "Recommendations", note.getRecommendations());
        appendIfPresent(parts, "Remarks", note.getRemarks());
        return String.join("\n", parts);
    }

    private void appendIfPresent(List<String> parts, String label, String value) {
        if (StringUtils.hasText(value)) {
            parts.add(label + ": " + value);
        }
    }

    private String formatAge(Client client) {
        if (client == null || client.getDateOfBirth() == null) {
            return MISSING;
        }
        Integer age = client.getAge();
        if (age == null) {
            age = Period.between(client.getDateOfBirth(), LocalDate.now()).getYears();
        }
        return String.valueOf(age);
    }

    private String resolveGender(String genderKeyOrLabel) {
        if (!StringUtils.hasText(genderKeyOrLabel)) {
            return MISSING;
        }
        try {
            String label = systemOptionResolverService.resolveOptionLabel(
                    SystemOptionCategories.GENDER, genderKeyOrLabel.trim());
            if (StringUtils.hasText(label)) {
                return label;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return genderKeyOrLabel.trim();
    }

    private String resolveReferralSourceLabel(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        try {
            String label = systemOptionResolverService.resolveOptionLabel(
                    SystemOptionCategories.REFERRAL_SOURCES, raw.trim());
            if (StringUtils.hasText(label)) {
                return label;
            }
        } catch (Exception ignored) {
            // fall through
        }
        return raw.trim();
    }

    private String formatClinician(Client client) {
        User therapist = null;
        try {
            therapist = client != null ? client.getAssignedTherapist() : null;
        } catch (Exception ignored) {
            therapist = null;
        }
        if (therapist == null) {
            return MISSING;
        }

        StringBuilder name = new StringBuilder();
        if (StringUtils.hasText(therapist.getFullName())) {
            name.append(therapist.getFullName().trim());
        }
        if (StringUtils.hasText(therapist.getTitle())) {
            if (name.length() > 0) {
                name.append(", ");
            }
            name.append(therapist.getTitle().trim());
        }

        UserProfile profile = userProfileRepository.findByUserId(therapist.getId()).orElse(null);
        if (profile != null && StringUtils.hasText(profile.getLicenseType())) {
            if (name.length() > 0) {
                name.append(", ");
            }
            name.append(profile.getLicenseType().trim());
            if (StringUtils.hasText(profile.getLicenseNumber())) {
                name.append(" #").append(profile.getLicenseNumber().trim());
            }
        }

        return name.length() > 0 ? name.toString() : MISSING;
    }

    private static String line(String label, String value) {
        return "<p>" + escapeHtml(label) + ": " + escapeHtml(value) + "</p>";
    }

    private static String valueOrMissing(String value) {
        return StringUtils.hasText(value) ? value.trim() : MISSING;
    }

    private static String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        if (text.length() <= max) {
            return text;
        }
        return text.substring(0, max);
    }

    private String nullSafe(Object value) {
        return value != null ? value.toString() : "N/A";
    }
}
