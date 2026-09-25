package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistBlockedTimeRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserProfileScheduleRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.user.dto.AvailabilityStatus;
import com.smart.therapy.flow.user.dto.BlockType;
import com.smart.therapy.flow.user.dto.LicenseStatus;
import com.smart.therapy.flow.user.entity.TherapistBlockedTime;
import com.smart.therapy.flow.user.entity.UserContact;
import com.smart.therapy.flow.user.entity.UserProfile;
import com.smart.therapy.flow.user.entity.UserProfileAgeGroup;
import com.smart.therapy.flow.user.entity.UserProfileAward;
import com.smart.therapy.flow.user.entity.UserProfileCertification;
import com.smart.therapy.flow.user.entity.UserProfileContinuingEducation;
import com.smart.therapy.flow.user.entity.UserProfileEducation;
import com.smart.therapy.flow.user.entity.UserProfileLanguage;
import com.smart.therapy.flow.user.entity.UserProfileMembership;
import com.smart.therapy.flow.user.entity.UserProfilePhysicalRoom;
import com.smart.therapy.flow.user.entity.UserProfilePreviousPosition;
import com.smart.therapy.flow.user.entity.UserProfilePublication;
import com.smart.therapy.flow.user.entity.UserProfileReference;
import com.smart.therapy.flow.user.entity.UserProfileSpecialization;
import com.smart.therapy.flow.user.entity.UserProfileTreatmentApproach;
import com.smart.therapy.flow.user.entity.UserProfileWorkingHours;
import com.smart.therapy.flow.user.repository.TherapistBlockedTimeRepository;
import com.smart.therapy.flow.user.repository.UserContactRepository;
import com.smart.therapy.flow.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class ClientHubTherapistScheduleExecuteService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final TherapistBlockedTimeRepository blockedTimeRepository;
    private final RoomRepository roomRepository;
    private final UserContactRepository userContactRepository;

    public TherapistScheduleExecuteResult execute(
            List<SourceUserProfileScheduleRecord> profiles,
            List<SourceTherapistBlockedTimeRecord> blockedTimes,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required for therapist schedule execution");
        }
        return tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(),
                () -> executeInTenant(profiles, blockedTimes, target));
    }

    private TherapistScheduleExecuteResult executeInTenant(
            List<SourceUserProfileScheduleRecord> profiles,
            List<SourceTherapistBlockedTimeRecord> blockedTimes,
            TargetInventory target) {
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");
        Map<String, Long> roomMappings = loadMappings(target.organisationId(), "rooms");
        Map<String, Long> profileMappings = loadMappings(target.organisationId(), "user_profiles");
        Map<String, Long> blockedMappings = loadMappings(target.organisationId(), "therapist_blocked_times");

        int profilesCreated = 0;
        int profilesUpdated = 0;
        int profileMappedReruns = 0;
        int workingHoursUpserted = 0;
        int professionalChildrenUpserted = 0;
        int emergencyContactsUpserted = 0;
        int physicalRoomsLinked = 0;
        int physicalRoomsSkipped = 0;
        int virtualRoomsSkipped = 0;

        for (SourceUserProfileScheduleRecord source : profiles) {
            Long targetUserId = requiredMapping(userMappings, source.userLegacyId(), "users");
            Optional<Long> mappedProfileId = Optional.ofNullable(profileMappings.get(source.legacyProfilePk()));
            UserProfile profile = mappedProfileId.flatMap(userProfileRepository::findById)
                    .or(() -> userProfileRepository.findByUserId(targetUserId))
                    .orElseGet(() -> UserProfile.builder()
                            .user(userRepository.getReferenceById(targetUserId))
                            .build());
            boolean existing = profile.getId() != null;

            applyProfileSchedulingFields(profile, source, roomMappings);
            applyProfessionalScalarFields(profile, source);
            if (source.virtualRoomLegacyId() != null && !source.virtualRoomLegacyId().isBlank()
                    && profile.getVirtualRoom() == null) {
                virtualRoomsSkipped++;
            }

            List<ClientHubWorkingHoursParser.ParsedShift> shifts = resolveShifts(source);
            profile.getWorkingHours().clear();
            for (ClientHubWorkingHoursParser.ParsedShift shift : shifts) {
                profile.getWorkingHours().add(UserProfileWorkingHours.builder()
                        .userProfile(profile)
                        .day(shift.day())
                        .startTime(shift.startTime())
                        .endTime(shift.endTime())
                        .sessionMode(shift.sessionMode())
                        .build());
                workingHoursUpserted++;
            }

            professionalChildrenUpserted += replaceProfessionalChildren(profile, source);

            profile.getAvailablePhysicalRooms().clear();
            int order = 0;
            for (String legacyRoomId : source.physicalRoomLegacyIds()) {
                Long targetRoomId = roomMappings.get(legacyRoomId);
                if (targetRoomId == null) {
                    physicalRoomsSkipped++;
                    continue;
                }
                Room room = roomRepository.getReferenceById(targetRoomId);
                profile.getAvailablePhysicalRooms().add(UserProfilePhysicalRoom.builder()
                        .userProfile(profile)
                        .room(room)
                        .isPrimary(order == 0)
                        .displayOrder(order++)
                        .build());
                physicalRoomsLinked++;
            }

            profile.setIsDeleted(false);
            profile.setDeletedAt(null);
            profile.setCreatedBy(0L);
            profile.setUpdatedBy(0L);
            UserProfile saved = userProfileRepository.save(profile);
            profileMappings.put(source.legacyProfilePk(), saved.getId());
            upsertLegacyMapping(target, "user_profiles", "user_profiles",
                    source.legacyProfilePk(), saved.getId(), profileChecksum(source));

            if (upsertEmergencyContact(saved, source)) {
                emergencyContactsUpserted++;
            }

            if (existing) {
                profilesUpdated++;
                if (mappedProfileId.isPresent()) {
                    profileMappedReruns++;
                }
            } else {
                profilesCreated++;
            }
        }

        int blockedCreated = 0;
        int blockedUpdated = 0;
        int blockedMappedReruns = 0;
        for (SourceTherapistBlockedTimeRecord source : blockedTimes) {
            Long targetTherapistId = requiredMapping(userMappings, source.therapistLegacyId(), "users");
            Optional<Long> mappedBlockedId = Optional.ofNullable(blockedMappings.get(source.legacyBlockedPk()));
            TherapistBlockedTime blocked = mappedBlockedId.flatMap(blockedTimeRepository::findById)
                    .orElseGet(TherapistBlockedTime::new);
            boolean existing = blocked.getId() != null;

            User therapist = userRepository.getReferenceById(targetTherapistId);
            blocked.setTherapist(therapist);
            blocked.setStartTime(source.startTime());
            blocked.setEndTime(source.endTime());
            blocked.setAllDay(source.allDay());
            blocked.setBlockType(mapBlockType(source.blockType()));
            blocked.setReason(trim(source.reason()));
            blocked.setIsRecurring(source.recurring());
            blocked.setRecurrencePattern(trim(source.recurrencePattern()));
            blocked.setIsActive(source.active());
            blocked.setIsDeleted(false);
            blocked.setDeletedAt(null);
            blocked.setCreatedBy(0L);
            blocked.setUpdatedBy(0L);

            TherapistBlockedTime saved = blockedTimeRepository.save(blocked);
            blockedMappings.put(source.legacyBlockedPk(), saved.getId());
            upsertLegacyMapping(target, "therapist_blocked_times", "therapist_blocked_times",
                    source.legacyBlockedPk(), saved.getId(), blockedChecksum(source));

            if (existing) {
                blockedUpdated++;
                if (mappedBlockedId.isPresent()) {
                    blockedMappedReruns++;
                }
            } else {
                blockedCreated++;
            }
        }

        return new TherapistScheduleExecuteResult(
                profiles.size(),
                profilesCreated,
                profilesUpdated,
                profileMappedReruns,
                workingHoursUpserted,
                professionalChildrenUpserted,
                emergencyContactsUpserted,
                physicalRoomsLinked,
                physicalRoomsSkipped,
                virtualRoomsSkipped,
                blockedTimes.size(),
                blockedCreated,
                blockedUpdated,
                blockedMappedReruns);
    }

    private void applyProfileSchedulingFields(
            UserProfile profile,
            SourceUserProfileScheduleRecord source,
            Map<String, Long> roomMappings) {
        profile.setMaxClientsPerDay(source.maxClientsPerDay());
        profile.setSessionDuration(source.sessionDuration() != null ? source.sessionDuration() : 50);
        profile.setAvailabilityStatus(mapAvailabilityStatus(source.availabilityStatus()));

        if (source.virtualRoomLegacyId() != null && !source.virtualRoomLegacyId().isBlank()) {
            Long targetRoomId = roomMappings.get(source.virtualRoomLegacyId());
            if (targetRoomId != null) {
                profile.setVirtualRoom(roomRepository.getReferenceById(targetRoomId));
            }
        } else {
            profile.setVirtualRoom(null);
        }
    }

    private void applyProfessionalScalarFields(UserProfile profile, SourceUserProfileScheduleRecord source) {
        profile.setLicenseNumber(trimTo(source.licenseNumber(), 50));
        profile.setLicenseType(trimTo(source.licenseType(), 100));
        profile.setLicenseState(trimTo(source.licenseState(), 50));
        profile.setLicenseExpiry(source.licenseExpiry());
        profile.setLicenseStatus(mapLicenseStatus(source.licenseStatus()));
        profile.setYearsOfExperience(source.yearsOfExperience());
        profile.setClinicalExperience(trim(source.clinicalExperience()));
        profile.setResearchBackground(trim(source.researchBackground()));
        profile.setSupervisoryExperience(trim(source.supervisoryExperience()));
        profile.setCareerObjectives(trim(source.careerObjectives()));
    }

    private int replaceProfessionalChildren(UserProfile profile, SourceUserProfileScheduleRecord source) {
        int count = 0;
        profile.getSpecializations().clear();
        int order = 0;
        for (String value : nonBlank(source.specializations())) {
            profile.getSpecializations().add(UserProfileSpecialization.builder()
                    .userProfile(profile)
                    .specialization(trimTo(value, 255))
                    .expertiseLevel(UserProfileSpecialization.ExpertiseLevel.INTERMEDIATE)
                    .isPrimary(order == 0)
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getTreatmentApproaches().clear();
        order = 0;
        for (String value : nonBlank(source.treatmentApproaches())) {
            profile.getTreatmentApproaches().add(UserProfileTreatmentApproach.builder()
                    .userProfile(profile)
                    .approach(trimTo(value, 255))
                    .proficiencyLevel(UserProfileTreatmentApproach.ProficiencyLevel.COMPETENT)
                    .isPrimary(order == 0)
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getAgeGroups().clear();
        order = 0;
        for (String value : nonBlank(source.ageGroups())) {
            profile.getAgeGroups().add(UserProfileAgeGroup.builder()
                    .userProfile(profile)
                    .ageGroup(trimTo(value, 100))
                    .isPrimary(order == 0)
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getLanguages().clear();
        order = 0;
        for (String value : nonBlank(source.languages())) {
            profile.getLanguages().add(UserProfileLanguage.builder()
                    .userProfile(profile)
                    .language(trimTo(value, 100))
                    .proficiencyLevel(UserProfileLanguage.ProficiencyLevel.CONVERSATIONAL)
                    .isPrimary(order == 0)
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getCertifications().clear();
        order = 0;
        for (String value : nonBlank(source.certifications())) {
            profile.getCertifications().add(UserProfileCertification.builder()
                    .userProfile(profile)
                    .certificationName(trimTo(value, 255))
                    .status(UserProfileCertification.CertificationStatus.PENDING)
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getEducation().clear();
        order = 0;
        for (String value : nonBlank(source.education())) {
            profile.getEducation().add(UserProfileEducation.builder()
                    .userProfile(profile)
                    .institution(trimTo(value, 255))
                    .notes(trim(value))
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getPreviousPositions().clear();
        order = 0;
        for (String value : nonBlank(source.previousPositions())) {
            profile.getPreviousPositions().add(UserProfilePreviousPosition.builder()
                    .userProfile(profile)
                    .jobTitle(trimTo(value, 255))
                    .responsibilities(trim(value))
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getPublications().clear();
        order = 0;
        for (String value : nonBlank(source.publications())) {
            profile.getPublications().add(UserProfilePublication.builder()
                    .userProfile(profile)
                    .title(trim(value))
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getProfessionalMemberships().clear();
        order = 0;
        for (String value : nonBlank(source.professionalMemberships())) {
            profile.getProfessionalMemberships().add(UserProfileMembership.builder()
                    .userProfile(profile)
                    .organizationName(trimTo(value, 255))
                    .status(UserProfileMembership.MembershipStatus.ACTIVE)
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getContinuingEducation().clear();
        order = 0;
        for (String value : nonBlank(source.continuingEducation())) {
            profile.getContinuingEducation().add(UserProfileContinuingEducation.builder()
                    .userProfile(profile)
                    .courseName(trimTo(value, 255))
                    .description(trim(value))
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getAwardRecognitions().clear();
        order = 0;
        for (String value : nonBlank(source.awardRecognitions())) {
            profile.getAwardRecognitions().add(UserProfileAward.builder()
                    .userProfile(profile)
                    .awardName(trimTo(value, 255))
                    .description(trim(value))
                    .displayOrder(order++)
                    .build());
            count++;
        }

        profile.getProfessionalReferences().clear();
        order = 0;
        for (String value : nonBlank(source.professionalReferences())) {
            profile.getProfessionalReferences().add(UserProfileReference.builder()
                    .userProfile(profile)
                    .referenceName(trimTo(value, 255))
                    .notes(trim(value))
                    .displayOrder(order++)
                    .build());
            count++;
        }

        return count;
    }

    private boolean upsertEmergencyContact(UserProfile profile, SourceUserProfileScheduleRecord source) {
        String name = trim(source.emergencyContactName());
        String phone = trimTo(source.emergencyContactPhone(), 20);
        String relationship = trimTo(source.emergencyContactRelationship(), 100);
        if (name == null && phone == null && relationship == null) {
            userContactRepository.findByUserProfileAndType(profile, "emergency")
                    .ifPresent(userContactRepository::delete);
            return false;
        }

        UserContact contact = userContactRepository.findByUserProfileAndType(profile, "emergency")
                .orElseGet(() -> UserContact.builder()
                        .userProfile(profile)
                        .type("emergency")
                        .build());
        contact.setName(name != null ? name : "Emergency contact");
        contact.setPhone(phone);
        contact.setRelationship(relationship);
        contact.setIsDeleted(false);
        contact.setDeletedAt(null);
        contact.setCreatedBy(0L);
        contact.setUpdatedBy(0L);
        userContactRepository.save(contact);
        return true;
    }

    private List<ClientHubWorkingHoursParser.ParsedShift> resolveShifts(SourceUserProfileScheduleRecord source) {
        try {
            List<ClientHubWorkingHoursParser.ParsedShift> parsed =
                    ClientHubWorkingHoursParser.parse(source.workingHoursJson());
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (IllegalArgumentException ignored) {
            // Fall through to working_days default hours.
        }
        return ClientHubWorkingHoursParser.fromWorkingDaysFallback(source.workingDays());
    }

    static AvailabilityStatus mapAvailabilityStatus(String value) {
        if (value == null || value.isBlank()) {
            return AvailabilityStatus.AVAILABLE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "available" -> AvailabilityStatus.AVAILABLE;
            case "busy" -> AvailabilityStatus.BUSY;
            case "unavailable", "on_leave", "limited", "away" -> AvailabilityStatus.UNAVAILABLE;
            default -> AvailabilityStatus.AVAILABLE;
        };
    }

    static LicenseStatus mapLicenseStatus(String value) {
        if (value == null || value.isBlank()) {
            return LicenseStatus.ACTIVE;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "inactive", "expired", "revoked", "suspended" -> LicenseStatus.INACTIVE;
            default -> LicenseStatus.ACTIVE;
        };
    }

    static BlockType mapBlockType(String value) {
        if (value == null || value.isBlank()) {
            return BlockType.OTHER;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "vacation" -> BlockType.VACATION;
            case "meeting" -> BlockType.MEETING;
            case "sick_leave", "sickleave", "sick" -> BlockType.SICK_LEAVE;
            case "personal" -> BlockType.PERSONAL;
            case "training" -> BlockType.TRAINING;
            case "admin" -> BlockType.ADMIN;
            case "holiday" -> BlockType.HOLIDAY;
            default -> BlockType.OTHER;
        };
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(
            TargetInventory target,
            String entityName,
            String targetTable,
            String sourceId,
            Long targetId,
            String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                entityName,
                sourceId,
                target.schemaName(),
                targetTable,
                targetId,
                checksum);
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private String profileChecksum(SourceUserProfileScheduleRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyProfilePk(),
                source.userLegacyId(),
                nullToEmpty(source.licenseNumber()),
                nullToEmpty(source.licenseType()),
                nullToEmpty(source.licenseState()),
                String.valueOf(source.licenseExpiry()),
                nullToEmpty(source.licenseStatus()),
                String.valueOf(source.specializations()),
                String.valueOf(source.treatmentApproaches()),
                String.valueOf(source.ageGroups()),
                String.valueOf(source.languages()),
                String.valueOf(source.certifications()),
                String.valueOf(source.education()),
                String.valueOf(source.yearsOfExperience()),
                nullToEmpty(source.workingHoursJson()),
                String.valueOf(source.workingDays()),
                String.valueOf(source.maxClientsPerDay()),
                String.valueOf(source.sessionDuration()),
                nullToEmpty(source.availabilityStatus()),
                nullToEmpty(source.virtualRoomLegacyId()),
                String.valueOf(source.physicalRoomLegacyIds()),
                nullToEmpty(source.emergencyContactName()),
                nullToEmpty(source.emergencyContactPhone()),
                nullToEmpty(source.emergencyContactRelationship()),
                String.valueOf(source.previousPositions()),
                nullToEmpty(source.clinicalExperience()),
                nullToEmpty(source.researchBackground()),
                String.valueOf(source.publications()),
                String.valueOf(source.professionalMemberships()),
                String.valueOf(source.continuingEducation()),
                nullToEmpty(source.supervisoryExperience()),
                String.valueOf(source.awardRecognitions()),
                String.valueOf(source.professionalReferences()),
                nullToEmpty(source.careerObjectives())));
    }

    private String blockedChecksum(SourceTherapistBlockedTimeRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyBlockedPk(),
                source.therapistLegacyId(),
                String.valueOf(source.startTime()),
                String.valueOf(source.endTime()),
                Boolean.toString(source.allDay()),
                nullToEmpty(source.blockType()),
                nullToEmpty(source.reason()),
                Boolean.toString(source.recurring()),
                nullToEmpty(source.recurrencePattern()),
                Boolean.toString(source.active())));
    }

    private static List<String> nonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimTo(String value, int maxLength) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    record TherapistScheduleExecuteResult(
            int sourceProfiles,
            int profilesCreated,
            int profilesUpdated,
            int profileMappedReruns,
            int workingHoursUpserted,
            int professionalChildrenUpserted,
            int emergencyContactsUpserted,
            int physicalRoomsLinked,
            int physicalRoomsSkipped,
            int virtualRoomsSkipped,
            int sourceBlockedTimes,
            int blockedCreated,
            int blockedUpdated,
            int blockedMappedReruns) {
    }
}
