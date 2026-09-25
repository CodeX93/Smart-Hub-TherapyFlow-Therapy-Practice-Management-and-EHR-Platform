import { ContentLoader } from "@/components/shared/ContentLoader";
import { CalendarIcon } from "@/components/icons/commonIcons";
import { useEffect, type ReactNode } from "react";
import { Award, BriefcaseBusiness, GraduationCap, Mail, Phone, Shield, X } from "lucide-react";
import { useLazyGetAdminUserProfileQuery } from "@/store/api/admin/users.api";
import { getApiErrorMessage } from "@/utils/apiError";
import { formatApiTimeTo12h } from "@/utils/therapistTimezone";
import type { UserProfileEducationEntry } from "@/types/user-profile-education.type";

interface ProfessionalProfilePreviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  userId: number | null;
  userName?: string;
  userEmail?: string;
}

interface WorkingHourEntry {
  day: string;
  start?: string;
  end?: string;
  mode?: string;
  enabled?: boolean;
}

function formatLabel(value?: string | null): string {
  if (!value) return "—";
  return value
    .replace(/[_-]+/g, " ")
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function displayValue(value?: string | number | null): string {
  if (value === undefined || value === null || value === "") return "—";
  return String(value);
}

function formatTimeRange(start?: string, end?: string): string {
  const startLabel = start ? formatApiTimeTo12h(start) : "";
  const endLabel = end ? formatApiTimeTo12h(end) : "";
  if (startLabel && endLabel) return `${startLabel} – ${endLabel}`;
  return startLabel || endLabel || "Available";
}

function parseWorkingHours(value?: string): WorkingHourEntry[] {
  if (!value) return [];
  try {
    const parsed: unknown = JSON.parse(value);
    if (!Array.isArray(parsed)) return [];
    return parsed.filter(
      (entry): entry is WorkingHourEntry =>
        typeof entry === "object" &&
        entry !== null &&
        "day" in entry &&
        typeof entry.day === "string",
    );
  } catch {
    return [];
  }
}

function ChipList({ items }: { items?: string[] }) {
  if (!items?.length) {
    return <span className="text-sm text-(--text-neutral-500)">—</span>;
  }

  return (
    <div className="flex flex-wrap gap-2">
      {items.map((item) => (
        <span
          key={item}
          className="rounded-full border border-(--neutral-100) bg-white px-3 py-1 text-xs font-medium text-(--text-primary-dark)"
        >
          {item}
        </span>
      ))}
    </div>
  );
}

const Detail = ({
  label,
  value,
  className = "",
}: {
  label: string;
  value?: string | number | null;
  className?: string;
}) => (
  <div className={`min-w-0 ${className}`}>
    <p className="text-xs font-medium text-(--text-neutral-500)">{label}</p>
    <p className="mt-1 break-words text-sm font-medium leading-5 text-(--text-primary-dark)">
      {displayValue(value)}
    </p>
  </div>
);

const SectionCard = ({
  icon,
  title,
  children,
}: {
  icon: ReactNode;
  title: string;
  children: ReactNode;
}) => (
  <section className="rounded-2xl border border-(--neutral-100) bg-white p-4 shadow-xs sm:p-5">
    <div className="mb-4 flex items-center gap-2.5">
      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-(--bg-primary-50) text-(--text-neutral-600)">
        {icon}
      </div>
      <h3 className="text-base font-semibold text-(--text-primary-dark)">{title}</h3>
    </div>
    {children}
  </section>
);

function EducationCard({ entry }: { entry: UserProfileEducationEntry }) {
  const title =
    [formatLabel(entry.degreeType), entry.fieldOfStudy]
      .filter(Boolean)
      .join(" · ") || "Education";

  return (
    <div className="rounded-xl border border-(--neutral-100) bg-(--neutral-50)/50 px-4 py-3.5">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div className="min-w-0">
          <p className="text-sm font-semibold text-(--text-primary-dark)">{title}</p>
          <p className="mt-0.5 text-sm text-(--text-neutral-600)">
            {displayValue(entry.institution)}
          </p>
        </div>
        {entry.isAccredited ? (
          <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-[0.6875rem] font-semibold text-emerald-700">
            Accredited
          </span>
        ) : null}
      </div>

      <div className="mt-3 grid gap-3 sm:grid-cols-2">
        <Detail
          label="Graduation date"
          value={entry.graduationDate ?? entry.graduationYear}
        />
        <Detail label="Accreditation body" value={entry.accreditationBody} />
      </div>

      {entry.notes ? (
        <p className="mt-3 text-sm leading-5 text-(--text-neutral-600)">
          {entry.notes}
        </p>
      ) : null}
    </div>
  );
}

const ProfessionalProfilePreviewModal = ({
  isOpen,
  onClose,
  userId,
  userName,
  userEmail,
}: ProfessionalProfilePreviewModalProps) => {
  const [loadProfile, { data: profile, isFetching, error }] =
    useLazyGetAdminUserProfileQuery();

  useEffect(() => {
    if (!isOpen || !userId) return;
    void loadProfile(userId);
  }, [isOpen, loadProfile, userId]);

  useEffect(() => {
    if (!isOpen) return;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKeyDown);

    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", onKeyDown);
    };
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const workingHours = parseWorkingHours(profile?.workingHours).filter(
    (entry) => entry.enabled !== false,
  );
  const education = profile?.education ?? [];
  const licenseStatus = formatLabel(profile?.licenseStatus);

  return (
    <div
      className="fixed inset-0 z-9999 flex items-center justify-center bg-black/45 p-3 backdrop-blur-[0.0625rem] sm:p-4"
      onClick={onClose}
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label={`Professional profile for ${userName || "therapist"}`}
        className="flex max-h-[92dvh] w-full max-w-3xl flex-col overflow-hidden rounded-2xl bg-[#f8fafc] shadow-2xl"
        onClick={(event) => event.stopPropagation()}
      >
        <div className="shrink-0 border-b border-(--neutral-100) bg-white px-5 py-4 sm:px-6 sm:py-5">
          <div className="flex items-start justify-between gap-4">
            <div className="min-w-0">
              <p className="text-xs font-semibold uppercase tracking-wide text-(--text-neutral-500)">
                Professional Profile Preview
              </p>
              <h2 className="mt-1 truncate text-xl font-semibold text-(--text-primary-dark)">
                {profile?.fullName || userName || "Therapist"}
              </h2>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <span className="inline-flex items-center gap-1.5 rounded-full bg-(--neutral-50) px-2.5 py-1 text-xs text-(--text-neutral-600)">
                  <Mail size={12} />
                  <span className="max-w-56 truncate sm:max-w-none">
                    {profile?.email || userEmail || "—"}
                  </span>
                </span>
                {profile?.licenseStatus ? (
                  <span className="rounded-full bg-emerald-50 px-2.5 py-1 text-xs font-semibold text-emerald-700">
                    License {licenseStatus}
                  </span>
                ) : null}
                {profile?.availabilityStatus ? (
                  <span className="rounded-full bg-sky-50 px-2.5 py-1 text-xs font-semibold text-sky-700">
                    {formatLabel(profile.availabilityStatus)}
                  </span>
                ) : null}
              </div>
            </div>
            <button
              type="button"
              onClick={onClose}
              className="cursor-pointer rounded-full p-2 text-(--text-neutral-500) hover:bg-(--neutral-50)"
              aria-label="Close professional profile preview"
            >
              <X size={20} />
            </button>
          </div>
        </div>

        <div className="custom-scrollbar min-h-0 flex-1 overflow-y-auto px-4 py-4 sm:px-6 sm:py-5">
          {isFetching ? (
            <ContentLoader size="md" className="gap-2 text-sm text-(--text-neutral-600)" />
          ) : error ? (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm text-red-700">
              {getApiErrorMessage(error)}
            </div>
          ) : profile ? (
            <div className="space-y-4">
              <SectionCard icon={<Award size={16} />} title="License">
                <div className="grid gap-4 sm:grid-cols-2">
                  <Detail label="License number" value={profile.licenseNumber} />
                  <Detail label="License type" value={profile.licenseType} />
                  <Detail label="License state" value={profile.licenseState} />
                  <Detail label="License expiry" value={profile.licenseExpiry} />
                </div>
              </SectionCard>

              <SectionCard
                icon={<BriefcaseBusiness size={16} />}
                title="Specializations & Languages"
              >
                <div className="grid gap-4 sm:grid-cols-2">
                  <div>
                    <p className="mb-2 text-xs font-medium text-(--text-neutral-500)">
                      Specializations
                    </p>
                    <ChipList items={profile.specializations} />
                  </div>
                  <div>
                    <p className="mb-2 text-xs font-medium text-(--text-neutral-500)">
                      Languages
                    </p>
                    <ChipList items={profile.languages} />
                  </div>
                  <Detail
                    label="Years of experience"
                    value={
                      profile.yearsOfExperience !== undefined &&
                      profile.yearsOfExperience !== null
                        ? `${profile.yearsOfExperience} years`
                        : undefined
                    }
                  />
                </div>
              </SectionCard>

              <SectionCard
                icon={<GraduationCap size={16} />}
                title="Education"
              >
                {education.length ? (
                  <div className="space-y-3">
                    {education.map((entry, index) => (
                      <EducationCard
                        key={entry.id ?? `${entry.institution}-${index}`}
                        entry={entry}
                      />
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-(--text-neutral-500)">
                    No education details added.
                  </p>
                )}
              </SectionCard>

              <SectionCard
                icon={<BriefcaseBusiness size={16} />}
                title="Professional Background"
              >
                <div className="space-y-4">
                  <Detail
                    label="Clinical experience"
                    value={profile.clinicalExperience}
                  />
                  <Detail
                    label="Research background"
                    value={profile.researchBackground}
                  />
                  <Detail
                    label="Supervisory experience"
                    value={profile.supervisoryExperience}
                  />
                  <Detail
                    label="Career objectives"
                    value={profile.careerObjectives}
                  />
                </div>
              </SectionCard>

              <SectionCard icon={<CalendarIcon size={16} />} title="Schedule">
                <div className="mb-4 grid gap-4 sm:grid-cols-3">
                  <Detail label="Time zone" value={profile.timezone} />
                  <Detail
                    label="Max clients / day"
                    value={profile.maxClientsPerDay}
                  />
                  <Detail
                    label="Session duration"
                    value={
                      profile.sessionDuration
                        ? `${profile.sessionDuration} minutes`
                        : undefined
                    }
                  />
                </div>
                {workingHours.length ? (
                  <div className="overflow-hidden rounded-xl border border-(--neutral-100) bg-(--neutral-50)/40">
                    {workingHours.map((entry, index) => (
                      <div
                        key={`${entry.day}-${index}`}
                        className="flex flex-col gap-1 border-b border-(--neutral-100) px-4 py-3 text-sm last:border-b-0 sm:flex-row sm:items-center sm:justify-between sm:gap-4"
                      >
                        <span className="font-medium text-(--text-primary-dark)">
                          {formatLabel(entry.day)}
                        </span>
                        <div className="flex flex-wrap items-center gap-2 text-(--text-neutral-600)">
                          <span>{formatTimeRange(entry.start, entry.end)}</span>
                          {entry.mode ? (
                            <span className="rounded-full bg-white px-2.5 py-0.5 text-xs font-medium text-(--text-primary-dark)">
                              {formatLabel(entry.mode)}
                            </span>
                          ) : null}
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <p className="text-sm text-(--text-neutral-500)">
                    No working hours configured.
                  </p>
                )}
              </SectionCard>

              <SectionCard icon={<Shield size={16} />} title="Emergency Contact">
                <div className="grid gap-4 sm:grid-cols-2">
                  <Detail label="Name" value={profile.emergencyContactName} />
                  <Detail
                    label="Relationship"
                    value={profile.emergencyContactRelationship}
                  />
                  <div className="min-w-0">
                    <p className="text-xs font-medium text-(--text-neutral-500)">
                      Phone
                    </p>
                    <p className="mt-1 flex items-center gap-1.5 text-sm font-medium text-(--text-primary-dark)">
                      <Phone size={14} className="shrink-0 text-(--text-neutral-500)" />
                      {displayValue(profile.emergencyContactPhone)}
                    </p>
                  </div>
                  <div className="min-w-0">
                    <p className="text-xs font-medium text-(--text-neutral-500)">
                      Email
                    </p>
                    <p className="mt-1 flex items-center gap-1.5 text-sm font-medium text-(--text-primary-dark)">
                      <Mail size={14} className="shrink-0 text-(--text-neutral-500)" />
                      <span className="truncate">
                        {displayValue(profile.emergencyContactEmail)}
                      </span>
                    </p>
                  </div>
                </div>
              </SectionCard>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
};

export default ProfessionalProfilePreviewModal;
