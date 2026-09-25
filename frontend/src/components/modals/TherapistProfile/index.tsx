import React, { useCallback, useEffect, useState } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import ModalSidebar from "./ModalSidebar";
import BasicInfoSection from "./BasicInfoSection";
import LicenseSection from "./LicenseSection";
import SpecializationsSection from "./SpecializationsSection";
import BackgroundSection from "./BackgroundSection";
import ScheduleSection from "./ScheduleSection";
import ConsultationScheduleSection from "./ConsultationScheduleSection";
import ZoomIntegrationSection from "./ZoomIntegrationSection";
import PasswordSection from "./PasswordSection";
import Toast from "@/components/shared/Toast";
import {
  useGetMyProfileQuery,
  useGetMyTimezoneQuery,
  useUpdateMyProfileMutation,
  type UpdateMyProfilePayload,
} from "@/store/api/userProfile.api";
import type {
  BackgroundFormValues,
  LicenseFormValues,
  SpecializationsFormValues,
  TherapistBasicInfoFormValues,
} from "@/schemas/general-profile-modal-schemas";
import { formatDateOnly } from "@/utils/transformer/dates.transformer";
import {
  areEducationEntriesEqual,
  mapEducationFormToPayload,
} from "@/utils/userProfileEducation";
import { getApiErrorMessage } from "@/utils/apiError";

interface TherapistProfileModalProps {
  isOpen: boolean;
  /** Tab to land on. The content only mounts while open, so this applies on every open. */
  initialSection?: string;
  onClose: () => void;
}

const TherapistProfileModalContent: React.FC<TherapistProfileModalProps> = ({
  isOpen,
  initialSection,
  onClose,
}) => {
  const [activeSection, setActiveSection] = useState(initialSection || "basic");
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error">("success");
  const { data: profileData, refetch: refetchProfile } = useGetMyProfileQuery(undefined, {
    skip: !isOpen,
  });
  const { refetch: refetchTimezone } = useGetMyTimezoneQuery(undefined, {
    skip: !isOpen,
  });
  const [updateMyProfile, { isLoading: isSavingProfile }] =
    useUpdateMyProfileMutation();

  const refreshProfileData = useCallback(async () => {
    await Promise.all([refetchProfile(), refetchTimezone()]);
  }, [refetchProfile, refetchTimezone]);

  const handleNotify = useCallback((message: string, type: "success" | "error") => {
    setToastType(type);
    setToastMessage(message);
  }, []);



  useEffect(() => {
    if (!isOpen) return;

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    return () => {
      document.body.style.overflow = previousOverflow;
    };
  }, [isOpen]);

  if (!isOpen) return null;

  const handleSectionChange = (section: string) => {
    setActiveSection(section);
    if (
      section === "license" ||
      section === "specializations" ||
      section === "background" ||
      section === "schedule" ||
      section === "consultation-schedule"
    ) {
      void refreshProfileData();
    }
  };

  const patchProfileIfChanged = async (partial: UpdateMyProfilePayload) => {
    if (Object.keys(partial).length === 0) return;

    try {
      await updateMyProfile(partial).unwrap();
      await refreshProfileData();
      setToastType("success");
      setToastMessage("Profile updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  };

  const handleSaveBasic = async (values: TherapistBasicInfoFormValues) => {
    const payload: UpdateMyProfilePayload = {};
    if ((profileData?.fullName || "") !== values.fullName) payload.fullName = values.fullName;
    if ((profileData?.email || "") !== values.email) payload.email = values.email;
    const prevYears = profileData?.yearsOfExperience ? String(profileData.yearsOfExperience) : "";
    if (prevYears !== values.experience) {
      payload.yearsOfExperience = values.experience ? Number(values.experience) : 0;
    }
    const prevMax = profileData?.maxClientsPerDay ? String(profileData.maxClientsPerDay) : "";
    if (prevMax !== values.maxClients) {
      payload.maxClientsPerDay = values.maxClients ? Number(values.maxClients) : 0;
    }
    const prevDuration = profileData?.sessionDuration ? String(profileData.sessionDuration) : "";
    if (prevDuration !== values.sessionDuration) {
      payload.sessionDuration = values.sessionDuration ? Number(values.sessionDuration) : 0;
    }
    if ((profileData?.emergencyContactName || "") !== values.emergencyName) {
      payload.emergencyContactName = values.emergencyName;
    }
    if ((profileData?.emergencyContactPhone || "") !== values.emergencyPhone) {
      payload.emergencyContactPhone = values.emergencyPhone;
    }
    if ((profileData?.emergencyContactRelationship || "") !== values.relationship) {
      payload.emergencyContactRelationship = values.relationship;
    }
    await patchProfileIfChanged(payload);
  };

  const handleSaveLicense = async (values: LicenseFormValues) => {
    const payload: UpdateMyProfilePayload = {};
    if ((profileData?.licenseNumber || "") !== values.licenseNumber) {
      payload.licenseNumber = values.licenseNumber;
    }
    if ((profileData?.licenseType || "") !== values.licenseType) {
      payload.licenseType = values.licenseType;
    }
    if ((profileData?.licenseState || "") !== values.licenseState) {
      payload.licenseState = values.licenseState;
    }
    const nextExpiry = values.licenseExpiration
      ? formatDateOnly(values.licenseExpiration)
      : "";
    const prevExpiry = profileData?.licenseExpiry || "";
    if (prevExpiry !== nextExpiry) {
      payload.licenseExpiry = nextExpiry;
    }
    await patchProfileIfChanged(payload);
  };

  const handleSaveSpecializations = async (values: SpecializationsFormValues) => {
    const payload: UpdateMyProfilePayload = {};
    if ((profileData?.clinicalExperience || "") !== values.clinicalSummary) {
      payload.clinicalExperience = values.clinicalSummary;
    }
    if ((profileData?.researchBackground || "") !== values.researchBackground) {
      payload.researchBackground = values.researchBackground;
    }
    if ((profileData?.supervisoryExperience || "") !== values.supervisoryExperience) {
      payload.supervisoryExperience = values.supervisoryExperience;
    }
    const nextSpecializations = values.specializations.map((item) => item.trim()).filter(Boolean);
    const prevSpecializations = profileData?.specializations || [];
    if (JSON.stringify(prevSpecializations) !== JSON.stringify(nextSpecializations)) {
      payload.specializations = nextSpecializations;
    }
    const nextLanguages = values.languages.map((item) => item.trim()).filter(Boolean);
    const prevLanguages = profileData?.languages || [];
    if (JSON.stringify(prevLanguages) !== JSON.stringify(nextLanguages)) {
      payload.languages = nextLanguages;
    }
    await patchProfileIfChanged(payload);
  };

  const handleSaveBackground = async (values: BackgroundFormValues) => {
    const payload: UpdateMyProfilePayload = {};
    if ((profileData?.careerObjectives || "") !== (values.careerObjectives || "")) {
      payload.careerObjectives = values.careerObjectives || "";
    }
    const nextEducation = mapEducationFormToPayload(values.education);
    if (!areEducationEntriesEqual(profileData?.education, nextEducation)) {
      payload.education = nextEducation;
    }
    await patchProfileIfChanged(payload);
  };

  const renderSection = () => {
    switch (activeSection) {
      case "basic":
        return (
          <BasicInfoSection
            profileData={profileData}
            onSave={handleSaveBasic}
            isSaving={isSavingProfile}
          />
        );
      case "license":
        return (
          <LicenseSection
            profileData={profileData}
            onSave={handleSaveLicense}
            isSaving={isSavingProfile}
          />
        );
      case "specializations":
        return (
          <SpecializationsSection
            profileData={profileData}
            onSave={handleSaveSpecializations}
            isSaving={isSavingProfile}
          />
        );
      case "background":
        return (
          <BackgroundSection
            profileData={profileData}
            onSave={handleSaveBackground}
            isSaving={isSavingProfile}
          />
        );
      case "schedule":
        return (
          <ScheduleSection
            onNotify={handleNotify}
            onProfileSaved={refreshProfileData}
          />
        );
      case "consultation-schedule":
        return (
          <ConsultationScheduleSection
            onNotify={handleNotify}
            onProfileSaved={refreshProfileData}
          />
        );
      case "zoom":
        return <ZoomIntegrationSection />;
      case "password":
        return (
          <PasswordSection onNotify={handleNotify} />
        );
      default:
        return (
          <div className="flex items-center justify-center h-full text-(--text-secondary-light)">
            Section coming soon...
          </div>
        );
    }
  };

  const getSectionTitle = () => {
    switch (activeSection) {
      case "basic":
        return "Basic Info";
      case "license":
        return "License";
      case "specializations":
        return "Specializations";
      case "background":
        return "Background";
      case "schedule":
        return "Schedule";
      case "consultation-schedule":
        return "Consultation Schedule";
      case "zoom":
        return "Zoom Integration";
      case "password":
        return "Password";
      default:
        return "Profile Section";
    }
  };

  return createPortal(
    <div className="fixed inset-0 z-[1000] flex items-center justify-center bg-black/50 p-2 md:p-0">
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
      <div className="w-3xl h-[90%] bg-white rounded-[1.5rem] border border-(--neutral-100) shadow-xl overflow-auto md:overflow-hidden flex flex-col md:flex-row" data-therapist-profile-modal>
        {/* Sidebar */}
        <ModalSidebar
          activeSection={activeSection}
          onSectionChange={handleSectionChange}
        />

        {/* Content Area */}
        <div className="flex min-w-0 flex-1 flex-col bg-white">
          {/* Header */}
          <div className="flex justify-between items-center p-4 md:px-5 md:py-4">
            <h2 className="text-(--neutral-950) text-[1.5rem] leading-8 font-semibold">
              {getSectionTitle()}
            </h2>
            <button
              onClick={onClose}
              className="text-(--text-neutral-600) hover:text-(--neutral-950) transition-colors cursor-pointer p-1 hover:bg-(--neutral-100) rounded-full"
            >
              <X size={24} />
            </button>
          </div>

          <div className="h-px bg-(--neutral-100) md:mx-5 mx-4" />

          {/* Form Content */}
          <div className="min-w-0 flex-1 overflow-y-auto overflow-x-hidden md:px-5 px-4">
            {renderSection()}
          </div>
        </div>
      </div>
    </div>,
    document.body,
  );
};

const TherapistProfileModal = (props: TherapistProfileModalProps) => props.isOpen ? <TherapistProfileModalContent {...props} /> : null;
export default TherapistProfileModal;
