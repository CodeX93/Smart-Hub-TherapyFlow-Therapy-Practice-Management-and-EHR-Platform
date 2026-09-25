package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Types of consent required for HIPAA compliance and therapy services
 */
public enum ConsentType {

    // Core treatment consents
    TREATMENT("Consent for Treatment", "General consent to receive mental health treatment"),
    TELEHEALTH("Telehealth Consent", "Consent for teletherapy/remote services"),

    // Privacy and data consents (HIPAA)
    HIPAA_PRIVACY("HIPAA Privacy Notice", "Acknowledgment of HIPAA privacy practices"),
    HIPAA_AUTHORIZATION("HIPAA Authorization", "Authorization to use/disclose PHI"),

    // Technology and AI
    AI_PROCESSING("AI Processing Consent", "Consent for AI-assisted therapy notes and analysis"),
    ELECTRONIC_RECORDS("Electronic Health Records", "Consent to maintain electronic health records"),

    // Financial and insurance
    INSURANCE_SHARING("Insurance Information Sharing", "Authorization to share information with insurance"),
    PAYMENT_AUTHORIZATION("Payment Authorization", "Authorization for payment processing"),

    // Research and education
    RESEARCH("Research Participation", "Consent to participate in research studies"),
    TRAINING("Training/Supervision", "Consent for sessions to be used in training/supervision"),
    
    // Data sharing and marketing
    DATA_SHARING("Data Sharing", "Consent to share data with third parties"),
    MARKETING("Marketing Communications", "Consent to receive marketing communications"),

    // Recording and documentation
    PHOTOGRAPHY("Photography Consent", "Consent for photographs"),
    AUDIO_RECORDING("Audio Recording", "Consent for audio recording of sessions"),
    VIDEO_RECORDING("Video Recording", "Consent for video recording of sessions"),

    // Communication
    EMAIL_COMMUNICATION("Email Communication", "Consent to communicate via email"),
    SMS_COMMUNICATION("SMS Communication", "Consent to receive SMS messages"),

    // Minors
    PARENTAL_CONSENT("Parental/Guardian Consent", "Parental consent for minor's treatment"),

    // Emergency
    EMERGENCY_CONTACT("Emergency Contact Authorization", "Authorization to contact emergency contacts"),

    // Custom
    OTHER("Other", "Other type of consent");

    private final String displayName;
    private final String description;

    ConsentType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ConsentType fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Invalid consent type: " + value);
        }
        String trimmed = value.trim();
        for (ConsentType type : ConsentType.values()) {
            if (type.name().equalsIgnoreCase(trimmed)
                    || type.displayName.equalsIgnoreCase(trimmed)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid consent type: " + value);
    }

    /* ---------- Domain Logic ---------- */

    public boolean isHIPAARequired() {
        return this == HIPAA_PRIVACY || this == HIPAA_AUTHORIZATION;
    }

    public boolean isRecordingConsent() {
        return this == AUDIO_RECORDING || this == VIDEO_RECORDING || this == PHOTOGRAPHY;
    }
}
