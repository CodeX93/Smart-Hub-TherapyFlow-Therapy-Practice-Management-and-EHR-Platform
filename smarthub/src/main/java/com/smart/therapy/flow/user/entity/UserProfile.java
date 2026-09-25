package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.user.dto.AvailabilityStatus;
import com.smart.therapy.flow.user.dto.LicenseStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_profiles", indexes = {
        @Index(name = "idx_user_profile_user_id", columnList = "user_id"),
        @Index(name = "idx_user_profile_timezone", columnList = "timezone"),
        @Index(name = "idx_user_profile_availability", columnList = "availability_status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = { "user" })
@ToString(callSuper = true, exclude = {
        "user",
        "specializations",
        "treatmentApproaches",
        "ageGroups",
        "languages",
        "certifications",
        "education",
        "workingHours",
        "virtualRoom",
        "availablePhysicalRooms",
        "previousPositions",
        "publications",
        "professionalMemberships",
        "continuingEducation",
        "awardRecognitions",
        "professionalReferences"
})
public class UserProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Professional Information
    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_type", length = 100)
    private String licenseType; // LMFT, LCSW, etc.

    @Column(name = "license_state", length = 50)
    private String licenseState;

    @Column(name = "license_expiry")
    private LocalDate licenseExpiry;

    @Column(name = "license_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LicenseStatus licenseStatus = LicenseStatus.ACTIVE;

    // Clinical Specializations
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileSpecialization> specializations = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileTreatmentApproach> treatmentApproaches = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileAgeGroup> ageGroups = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileLanguage> languages = new ArrayList<>();

    // Professional Development
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileCertification> certifications = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileEducation> education = new ArrayList<>();

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience;

    // Availability & Scheduling
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileWorkingHours> workingHours = new ArrayList<>();

    @Column(name = "max_clients_per_day")
    private Integer maxClientsPerDay;

    @Column(name = "session_duration")
    @Builder.Default
    private Integer sessionDuration = 50; // Minutes

    @Column(name = "availability_status", length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AvailabilityStatus availabilityStatus = AvailabilityStatus.AVAILABLE;

    @Column(name = "timezone", length = 50)
    private String timezone; // IANA timezone ID (e.g., "America/New_York", "America/Los_Angeles", "Europe/London")

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "virtual_room_id")
    private Room virtualRoom;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfilePhysicalRoom> availablePhysicalRooms = new ArrayList<>();

    // Professional Background & History
    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfilePreviousPosition> previousPositions = new ArrayList<>();

    @Column(name = "clinical_experience", columnDefinition = "TEXT")
    private String clinicalExperience;

    @Column(name = "research_background", columnDefinition = "TEXT")
    private String researchBackground;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfilePublication> publications = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileMembership> professionalMemberships = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileContinuingEducation> continuingEducation = new ArrayList<>();

    @Column(name = "supervisory_experience", columnDefinition = "TEXT")
    private String supervisoryExperience;

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileAward> awardRecognitions = new ArrayList<>();

    @OneToMany(mappedBy = "userProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserProfileReference> professionalReferences = new ArrayList<>();

    @Column(name = "career_objectives", columnDefinition = "TEXT")
    private String careerObjectives;
}
