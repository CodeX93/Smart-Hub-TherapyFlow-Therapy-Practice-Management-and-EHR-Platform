
import { ContentLoader } from "@/components/shared/ContentLoader";
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { X } from "lucide-react";
import LicenseSection from "./LicenseSection";
import SpecializationsSection from "./SpecializationsSection";
import ScheduleSection from "./ScheduleSection";
import ConsultationScheduleSection from "./ConsultationScheduleSection";
import ModalSidebar from "./ModalSidebar";
import EmergencyContact from "./EmergencyContact";
import BackgroundSection from "./BackgroundSection";
import {
  useCreateAdminUserProfileMutation,
  useLazyGetAdminUserProfileQuery,
  useUpdateAdminUserProfileMutation,
  type AdminUserProfessionalProfile,
  type UpdateAdminUserProfessionalProfilePayload,
} from "@/store/api/admin/users.api";
import type {
  BackgroundFormValues,
  ConsultationScheduleFormValues,
  EmergencyContactFormValues,
  LicenseFormValues,
  ScheduleFormValues,
  SpecializationsFormValues,
} from "@/schemas/user-access-profiles.schema";
import { getApiErrorMessage } from "@/utils/apiError";
import { formatDateOnly } from "@/utils/transformer/dates.transformer";
import { format12hToApiTime } from "@/utils/therapistTimezone";
import {
  areEducationEntriesEqual,
  mapEducationFormToPayload,
} from "@/utils/userProfileEducation";
import Toast from "../shared/Toast";
import { useGetAdminRoomsQuery } from "@/store/api/admin/rooms.api";
import type {
  ProfileSectionHandle,
  ScheduleSectionHandle,
} from "./profileSectionHandle";
import { Button } from "../ui/button";

type RoomMultiSelectOption = { id: string; label: string; roomType: string };

type ProfileSectionId =
  | "license"
  | "specializations"
  | "background"
  | "schedule"
  | "consultationSchedule"
  | "emergencyContact";

interface AdminAddUserProps {
  isOpen: boolean;
  onClose: () => void;
  userId?: number | null;
  userName?: string;
}

function buildLicensePatch(
  values: LicenseFormValues,
  profileData: AdminUserProfessionalProfile | null,
): UpdateAdminUserProfessionalProfilePayload {
  const patch: UpdateAdminUserProfessionalProfilePayload = {};
  if ((profileData?.licenseNumber || "") !== values.licenseNumber) {
    patch.licenseNumber = values.licenseNumber;
  }
  if ((profileData?.licenseType || "") !== values.licenseType) {
    patch.licenseType = values.licenseType;
  }
  if ((profileData?.licenseState || "") !== values.licenseState) {
    patch.licenseState = values.licenseState;
  }
  const nextDate = values.licenseExpiration
    ? formatDateOnly(values.licenseExpiration)
    : "";
  if ((profileData?.licenseExpiry || "") !== nextDate) {
    patch.licenseExpiry = nextDate;
  }
  return patch;
}

function buildSpecializationsPatch(
  values: SpecializationsFormValues,
  profileData: AdminUserProfessionalProfile | null,
): UpdateAdminUserProfessionalProfilePayload | { error: string } {
  const patch: UpdateAdminUserProfessionalProfilePayload = {};
  const yearsRaw = values.yearsOfExperience?.trim() || "";
  const nextYears = yearsRaw === "" ? 0 : Number.parseInt(yearsRaw, 10);

  if (!Number.isFinite(nextYears) || Number.isNaN(nextYears) || nextYears < 0 || nextYears > 2147483647) {
    return {
      error: "Years of experience must be a valid integer between 0 and 2147483647.",
    };
  }

  if ((profileData?.yearsOfExperience || 0) !== nextYears) {
    patch.yearsOfExperience = nextYears;
  }
  if ((profileData?.clinicalExperience || "") !== values.clinicalSummary) {
    patch.clinicalExperience = values.clinicalSummary;
  }
  const nextSpecializations = values.specializations.map((item) => item.trim()).filter(Boolean);
  const prevSpecializations = profileData?.specializations || [];
  if (JSON.stringify(prevSpecializations) !== JSON.stringify(nextSpecializations)) {
    patch.specializations = nextSpecializations;
  }
  const nextLanguages = values.languages.map((item) => item.trim()).filter(Boolean);
  const prevLanguages = profileData?.languages || [];
  if (JSON.stringify(prevLanguages) !== JSON.stringify(nextLanguages)) {
    patch.languages = nextLanguages;
  }
  return patch;
}

function buildBackgroundPatch(
  values: BackgroundFormValues,
  profileData: AdminUserProfessionalProfile | null,
): UpdateAdminUserProfessionalProfilePayload {
  const patch: UpdateAdminUserProfessionalProfilePayload = {};
  if ((profileData?.researchBackground || "") !== (values.researchBackground || "")) {
    patch.researchBackground = values.researchBackground || "";
  }
  if ((profileData?.supervisoryExperience || "") !== (values.supervisoryExperience || "")) {
    patch.supervisoryExperience = values.supervisoryExperience || "";
  }
  if ((profileData?.careerObjectives || "") !== (values.careerObjectives || "")) {
    patch.careerObjectives = values.careerObjectives || "";
  }
  const nextEducation = mapEducationFormToPayload(values.education);
  if (!areEducationEntriesEqual(profileData?.education, nextEducation)) {
    patch.education = nextEducation;
  }
  return patch;
}

function buildEmergencyPatch(
  values: EmergencyContactFormValues,
  profileData: AdminUserProfessionalProfile | null,
): UpdateAdminUserProfessionalProfilePayload {
  const patch: UpdateAdminUserProfessionalProfilePayload = {};
  if ((profileData?.emergencyContactName || "") !== values.emergencyContactName) {
    patch.emergencyContactName = values.emergencyContactName;
  }
  if ((profileData?.emergencyContactPhone || "") !== values.emergencyContactNumber) {
    patch.emergencyContactPhone = values.emergencyContactNumber;
  }
  if ((profileData?.emergencyContactEmail || "") !== values.emergencyContactEmail) {
    patch.emergencyContactEmail = values.emergencyContactEmail;
  }
  if ((profileData?.emergencyContactRelationship || "") !== values.emergencyContactRelation) {
    patch.emergencyContactRelationship = values.emergencyContactRelation;
  }
  return patch;
}

function buildSchedulePatch(
  values: ScheduleFormValues,
  profileData: AdminUserProfessionalProfile | null,
  rooms: Array<{ id: number; roomType?: string | null }>,
  timezoneWasChosen: boolean,
): UpdateAdminUserProfessionalProfilePayload {
  const patch: UpdateAdminUserProfessionalProfilePayload = {};
  // Send the timezone only when the admin actually picked one. The field always
  // displays an effective zone, so patching it on any other schedule edit would
  // freeze today's clinic zone onto the profile and cut it loose from the clinic.
  if (timezoneWasChosen && (profileData?.timezone || "") !== values.timezone) {
    patch.timezone = values.timezone;
  }
  const nextMax = Number(values.maxClientsPerDay || "0");
  if ((profileData?.maxClientsPerDay || 0) !== nextMax) {
    patch.maxClientsPerDay = nextMax;
  }
  const nextDuration = Number(values.sessionDuration || "0");
  if ((profileData?.sessionDuration || 0) !== nextDuration) {
    patch.sessionDuration = nextDuration;
  }
  const activeDays = values.workingHours.filter((day) => day.active);
  const workingDays = activeDays.map((day) => day.id.toLowerCase());
  if (JSON.stringify(profileData?.workingDays || []) !== JSON.stringify(workingDays)) {
    patch.workingDays = workingDays;
  }
  const workingHoursPayload = activeDays.flatMap((day) =>
    day.slots.map((slot) => ({
      day: day.id.toLowerCase(),
      enabled: true,
      start: format12hToApiTime(slot.startTime),
      end: format12hToApiTime(slot.endTime),
      mode: slot.type,
    })),
  );
  const nextWorkingHours = JSON.stringify(workingHoursPayload);
  if ((profileData?.workingHours || "") !== nextWorkingHours) {
    patch.workingHours = nextWorkingHours;
  }
  const selectedRoomIds = (values.physicalRoomIds || [])
    .map((id) => Number(id))
    .filter((id) => Number.isFinite(id) && id > 0);
  const selectedRooms = selectedRoomIds
    .map((id) => rooms.find((room) => room.id === id))
    .filter((room): room is (typeof rooms)[number] => Boolean(room));
  const physicalIds = selectedRooms
    .filter((room) => String(room.roomType || "").toUpperCase() !== "VIRTUAL")
    .map((room) => room.id);
  const virtualRoomId =
    selectedRooms.find((room) => String(room.roomType || "").toUpperCase() === "VIRTUAL")
      ?.id ?? undefined;
  if (
    JSON.stringify(profileData?.availablePhysicalRoomIds || []) !==
    JSON.stringify(physicalIds)
  ) {
    patch.availablePhysicalRoomIds = physicalIds;
  }
  if ((profileData?.virtualRoomId || undefined) !== virtualRoomId) {
    patch.virtualRoomId = virtualRoomId;
  }
  return patch;
}

function buildConsultationSchedulePatch(
  values: ConsultationScheduleFormValues,
  profileData: AdminUserProfessionalProfile | null,
): UpdateAdminUserProfessionalProfilePayload {
  const patch: UpdateAdminUserProfessionalProfilePayload = {};
  const activeDays = values.workingHours.filter((day) => day.active);
  const workingHoursPayload = activeDays.flatMap((day) =>
    day.slots.map((slot) => ({
      day: day.id.toLowerCase(),
      enabled: true,
      start: format12hToApiTime(slot.startTime),
      end: format12hToApiTime(slot.endTime),
      mode: slot.type,
    })),
  );
  const next = JSON.stringify(workingHoursPayload);
  if ((profileData?.consultationWorkingHours || "") !== next) {
    patch.consultationWorkingHours = next;
  }
  return patch;
}

const AdminAddUser: React.FC<AdminAddUserProps> = ({
  isOpen,
  onClose,
  userId,
  userName,
}) => {
  const [activeSection, setActiveSection] = useState<ProfileSectionId>("license");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [isProfileMissing, setIsProfileMissing] = useState(false);
  const [profileData, setProfileData] = useState<AdminUserProfessionalProfile | null>(
    null,
  );
  const [isProfileLoading, setIsProfileLoading] = useState(false);
  const [triggerGetUserProfile] = useLazyGetAdminUserProfileQuery();
  const [updateUserProfile, { isLoading: isSavingProfile }] =
    useUpdateAdminUserProfileMutation();
  const [createUserProfile, { isLoading: isCreatingProfile }] =
    useCreateAdminUserProfileMutation();
  const { data: rooms = [] } = useGetAdminRoomsQuery();

  const licenseRef = useRef<ProfileSectionHandle<LicenseFormValues>>(null);
  const specializationsRef =
    useRef<ProfileSectionHandle<SpecializationsFormValues>>(null);
  const backgroundRef = useRef<ProfileSectionHandle<BackgroundFormValues>>(null);
  const scheduleRef = useRef<ScheduleSectionHandle<ScheduleFormValues>>(null);
  const consultationScheduleRef =
    useRef<ProfileSectionHandle<ConsultationScheduleFormValues>>(null);
  const emergencyRef =
    useRef<ProfileSectionHandle<EmergencyContactFormValues>>(null);

  const roomOptions = useMemo<RoomMultiSelectOption[]>(
    () =>
      rooms
        .filter((room) => room.isActive)
        .map((room) => ({
          id: String(room.id),
          label: [room.roomNumber, room.roomName].filter(Boolean).join(" - "),
          roomType: String(room.roomType || "").toUpperCase(),
        })),
    [rooms],
  );

  const fetchProfile = useCallback(async (options?: { soft?: boolean }) => {
    if (!userId) return;
    const soft = Boolean(options?.soft);
    if (!soft) {
      setIsProfileLoading(true);
    }
    try {
      const result = await triggerGetUserProfile(userId).unwrap();
      setProfileData(result);
      setIsProfileMissing(false);
    } catch (error) {
      const status =
        typeof error === "object" &&
        error !== null &&
        "status" in error &&
        typeof (error as { status?: unknown }).status === "number"
          ? (error as { status: number }).status
          : null;
      if (status === 404) {
        if (!soft) {
          setProfileData(null);
        }
        setIsProfileMissing(true);
        return;
      }
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    } finally {
      if (!soft) {
        setIsProfileLoading(false);
      }
    }
  }, [triggerGetUserProfile, userId]);

  useEffect(() => {
    if (!isOpen) {
      setProfileData(null);
      setIsProfileMissing(false);
      setIsProfileLoading(false);
      setToastMessage(null);
      setToastType("info");
      return;
    }
    if (!userId) return;
    setActiveSection("license");
    void fetchProfile();
  }, [fetchProfile, isOpen, userId]);

  const saveProfilePatch = async (patchBody: UpdateAdminUserProfessionalProfilePayload) => {
    if (!userId) return;
    if (Object.keys(patchBody).length === 0) {
      setToastType("info");
      setToastMessage("No changes to save.");
      return;
    }
    try {
      if (isProfileMissing) {
        await createUserProfile({ userId, body: patchBody }).unwrap();
        setIsProfileMissing(false);
        setToastType("success");
        setToastMessage("Profile created successfully.");
      } else {
        await updateUserProfile({ userId, body: patchBody }).unwrap();
        setToastType("success");
        setToastMessage("Profile updated successfully.");
      }
      await fetchProfile({ soft: true });
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
      throw error;
    }
  };

  const handleSaveAll = async () => {
    if (!userId) return;

    const sections: Array<{
      id: ProfileSectionId;
      handle:
        | ProfileSectionHandle<LicenseFormValues>
        | ProfileSectionHandle<SpecializationsFormValues>
        | ProfileSectionHandle<BackgroundFormValues>
        | ProfileSectionHandle<ScheduleFormValues>
        | ProfileSectionHandle<ConsultationScheduleFormValues>
        | ProfileSectionHandle<EmergencyContactFormValues>
        | null
        | undefined;
    }> = [
      { id: "license", handle: licenseRef.current },
      { id: "specializations", handle: specializationsRef.current },
      { id: "background", handle: backgroundRef.current },
      { id: "schedule", handle: scheduleRef.current },
      { id: "consultationSchedule", handle: consultationScheduleRef.current },
      { id: "emergencyContact", handle: emergencyRef.current },
    ];

    // Validate only tabs the user actually edited so untouched required
    // fields (e.g. schedule rooms) don't block saving other sections.
    for (const section of sections) {
      if (!section.handle?.isDirty()) continue;
      const valid = await section.handle.trigger();
      if (!valid) {
        setActiveSection(section.id);
        section.handle.focusError?.();
        setToastType("error");
        setToastMessage("Please fix the highlighted fields before saving.");
        return;
      }
    }

    const licenseValues = licenseRef.current!.getValues();
    const specializationsValues = specializationsRef.current!.getValues();
    const backgroundValues = backgroundRef.current!.getValues();
    const scheduleValues = scheduleRef.current!.getValues();
    const consultationScheduleValues =
      consultationScheduleRef.current!.getValues();
    const emergencyValues = emergencyRef.current!.getValues();

    const patch: UpdateAdminUserProfessionalProfilePayload = {};

    if (licenseRef.current?.isDirty()) {
      Object.assign(patch, buildLicensePatch(licenseValues, profileData));
    }

    if (specializationsRef.current?.isDirty()) {
      const specializationsPatch = buildSpecializationsPatch(
        specializationsValues,
        profileData,
      );
      if ("error" in specializationsPatch) {
        setActiveSection("specializations");
        setToastType("error");
        setToastMessage(specializationsPatch.error);
        return;
      }
      Object.assign(patch, specializationsPatch);
    }

    if (backgroundRef.current?.isDirty()) {
      Object.assign(patch, buildBackgroundPatch(backgroundValues, profileData));
    }

    if (scheduleRef.current?.isDirty()) {
      Object.assign(
        patch,
        buildSchedulePatch(
          scheduleValues,
          profileData,
          rooms,
          Boolean(scheduleRef.current?.isTimezoneExplicitlyChosen()),
        ),
      );
    }

    if (consultationScheduleRef.current?.isDirty()) {
      Object.assign(
        patch,
        buildConsultationSchedulePatch(
          consultationScheduleValues,
          profileData,
        ),
      );
    }

    if (emergencyRef.current?.isDirty()) {
      Object.assign(patch, buildEmergencyPatch(emergencyValues, profileData));
    }

    try {
      await saveProfilePatch(patch);
      if (licenseRef.current?.isDirty()) {
        licenseRef.current.reset(licenseValues);
      }
      if (specializationsRef.current?.isDirty()) {
        specializationsRef.current.reset(specializationsValues);
      }
      if (backgroundRef.current?.isDirty()) {
        backgroundRef.current.reset(backgroundValues);
      }
      if (scheduleRef.current?.isDirty()) {
        scheduleRef.current.reset(scheduleValues);
      }
      if (consultationScheduleRef.current?.isDirty()) {
        consultationScheduleRef.current.reset(consultationScheduleValues);
      }
      if (emergencyRef.current?.isDirty()) {
        emergencyRef.current.reset(emergencyValues);
      }
    } catch {
      // Toast already shown in saveProfilePatch.
    }
  };

  if (!isOpen) return null;

  const isProfileSectionLoading = isProfileLoading && !profileData;
  const isBusy = isSavingProfile || isCreatingProfile || isProfileSectionLoading;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-2 md:p-4">
      <div
        className="flex h-full max-h-[90vh] w-full max-w-3xl min-h-0 flex-col overflow-hidden rounded-[1.5rem] border border-(--neutral-100) bg-white shadow-xl"
        data-admin-profile-modal
      >
        <div className="flex shrink-0 items-center justify-between gap-3 border-b border-(--neutral-100) p-4">
          <h4 className="flex min-w-0 items-center gap-1 font-semibold text-(--text-primary-dark)">
            <span className="shrink-0">
              {isProfileMissing ? "Create" : "Edit"} Professional Profile for
            </span>
            <span
              className="max-w-[17.5rem] truncate md:max-w-[26.25rem]"
              title={userName || "User"}
            >
              {userName || "User"}
            </span>
          </h4>
          <button
            onClick={onClose}
            className="cursor-pointer rounded-full p-1 text-(--text-neutral-600) transition-colors hover:bg-(--bg-primary-50) hover:text-(--neutral-950)"
          >
            <X size={24} />
          </button>
        </div>

        <div className="flex min-h-0 flex-1 flex-col overflow-hidden md:flex-row">
          <ModalSidebar
            activeSection={activeSection}
            onSectionChange={(sectionId) =>
              setActiveSection(sectionId as ProfileSectionId)
            }
          />

          <div className="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
            <div className="min-h-0 flex-1 overflow-y-auto overflow-x-hidden overscroll-contain px-4 md:px-5">
              {isProfileSectionLoading ? (
                <ContentLoader className="min-h-48" />
              ) : (
                <>
                  <div
                    className={activeSection === "license" ? "block" : "hidden"}
                  >
                    <LicenseSection
                      ref={licenseRef}
                      profileData={profileData}
                    />
                  </div>
                  <div
                    className={
                      activeSection === "specializations" ? "block" : "hidden"
                    }
                  >
                    <SpecializationsSection
                      ref={specializationsRef}
                      profileData={profileData}
                    />
                  </div>
                  <div
                    className={
                      activeSection === "background" ? "block" : "hidden"
                    }
                  >
                    <BackgroundSection
                      ref={backgroundRef}
                      profileData={profileData}
                    />
                  </div>
                  <div
                    className={activeSection === "schedule" ? "block" : "hidden"}
                  >
                    <ScheduleSection
                      ref={scheduleRef}
                      profileData={profileData}
                      isProfileMissing={isProfileMissing}
                      roomOptions={roomOptions}
                    />
                  </div>
                  <div
                    className={
                      activeSection === "consultationSchedule"
                        ? "block"
                        : "hidden"
                    }
                  >
                    <ConsultationScheduleSection
                      ref={consultationScheduleRef}
                      profileData={profileData}
                    />
                  </div>
                  <div
                    className={
                      activeSection === "emergencyContact" ? "block" : "hidden"
                    }
                  >
                    <EmergencyContact
                      ref={emergencyRef}
                      profileData={profileData}
                    />
                  </div>
                </>
              )}
            </div>

            <div className="flex shrink-0 justify-end gap-4 border-t border-(--neutral-100) bg-white px-4 pt-4 pb-6 md:px-5">
              <Button
                variant="outline"
                className="h-11.5 cursor-pointer rounded-full px-8 text-base font-semibold hover:opacity-90"
                onClick={onClose}
                disabled={isBusy}
              >
                Cancel
              </Button>
              <Button
                className="h-11.5 cursor-pointer rounded-full bg-(--bg-primary-dark) px-8 text-base font-semibold text-white hover:opacity-90"
                disabled={isBusy}
                onClick={() => void handleSaveAll()}
              >
                Save Profile
              </Button>
            </div>
          </div>
        </div>
      </div>
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </div>
  );
};

export default AdminAddUser;
