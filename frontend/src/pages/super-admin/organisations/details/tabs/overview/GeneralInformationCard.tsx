import { useState } from "react";
import { Check, PencilLine } from "lucide-react";
import { cn } from "@/lib/utils";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import Toast from "@/components/shared/Toast";
import { getApiErrorMessage } from "@/utils/apiError";
import type { OrganisationRow } from "../../../organisations.data";
import InfoRow from "./InfoRow";
import {
  GENERAL_INFO_LIMITS,
  sanitizeOrganisationName,
  sanitizeSupportEmail,
  validateGeneralInformation,
} from "./generalInformation.utils";

interface GeneralInformationCardProps {
  org: OrganisationRow;
  className?: string;
  isSaving: boolean;
  onSave(values: {
    organisationName: string;
    supportEmail: string;
    slug: string;
    region: string;
    dataResidency: string;
    timezone: string;
  }): Promise<void>;
}

function renderSupportEmail(org: OrganisationRow) {
  return org.supportEmail?.trim() || "-";
}

function renderCustomDomain(org: OrganisationRow) {
  return (
    <div className="flex items-center gap-1.5">
      <span>{org.customDomain}</span>
      <span className="grid h-4 w-4 place-items-center rounded-full bg-(--bg-success-light) text-(--status-paid)">
        <Check size={10} aria-hidden="true" />
      </span>
    </div>
  );
}

interface GeneralInfoFormValues {
  organisationName: string;
  supportEmail: string;
  slug: string;
}

function GeneralInformationCard(props: GeneralInformationCardProps) {
  const [isEditing, setIsEditing] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("info");
  const [values, setValues] = useState<GeneralInfoFormValues>({
    organisationName: props.org.name,
    supportEmail: props.org.supportEmail ?? "",
    slug: props.org.slug,
  });

  const currentRecordInput = JSON.stringify([isEditing, props.org.name, props.org.supportEmail ?? "", props.org.slug]);
  const [previousRecordInput, setPreviousRecordInput] = useState(currentRecordInput);
  if (currentRecordInput !== previousRecordInput) {
    setPreviousRecordInput(currentRecordInput);
    if (!isEditing) setValues({ organisationName: props.org.name, supportEmail: props.org.supportEmail ?? "", slug: props.org.slug });
  }

  function setField<K extends keyof GeneralInfoFormValues>(
    key: K,
    value: GeneralInfoFormValues[K]
  ) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  const hasChanges =
    values.organisationName.trim() !== props.org.name.trim() ||
    values.supportEmail.trim() !== (props.org.supportEmail ?? "").trim();

  async function handleSave() {
    const validationError = validateGeneralInformation(values);
    if (validationError) {
      setToastType("error");
      setToastMessage(validationError);
      return;
    }

    try {
      await props.onSave({
        ...values,
        region: props.org.region,
        dataResidency: props.org.dataResidency,
        timezone: props.org.timezone,
      });
      setIsEditing(false);
      setToastType("success");
      setToastMessage("General information updated successfully.");
    } catch (error) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(error));
    }
  }

  function handleCancel() {
    setIsEditing(false);
  }

  return (
    <div
      className={cn(
        "w-full rounded-[1rem] border border-(--neutral-100) bg-(--surface-white) px-5 py-5 shadow-[0_1px_2px_0px_var(--shadow)]",
        "flex flex-col",
        props.className
      )}
    >
      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}

      <div className="flex items-start justify-between gap-4">
        <div className="text-(--text-gray-900) text-[1rem] font-semibold leading-6">
          General Information
        </div>
        {!isEditing ? (
          <button
            type="button"
            onClick={() => setIsEditing(true)}
            className="inline-flex items-center gap-2 text-(--text-primary-500) text-sm font-medium leading-5 hover:opacity-90"
          >
            <PencilLine size={15} aria-hidden="true" />
            Edit Profile
          </button>
        ) : (
          <div className="flex items-center gap-2">
            <Button
              type="button"
              variant="outline"
              className="h-8 rounded-full border-[#d4dde6] px-4 text-xs font-medium"
              onClick={handleCancel}
              disabled={props.isSaving}
            >
              Cancel
            </Button>
            <Button
              type="button"
              className="h-8 rounded-full bg-[#435564] px-4 text-xs font-semibold text-white hover:bg-[#394957]"
              onClick={handleSave}
              disabled={props.isSaving || !hasChanges}
              loading={props.isSaving}
              loadingLabel="Saving..."
            >
              Save
            </Button>
          </div>
        )}
      </div>

      <div className="mt-5 grid grid-cols-1 gap-x-12 gap-y-5 sm:grid-cols-2">
        <InfoRow
          label="Organization Name"
          value={
            isEditing ? (
              <Input
                value={values.organisationName}
                onChange={(event) =>
                  setField("organisationName", sanitizeOrganisationName(event.target.value))
                }
                className="h-9 border-[#dce5ee] bg-white"
                placeholder="Enter organization name"
                maxLength={GENERAL_INFO_LIMITS.organisationName}
              />
            ) : (
              <span
                className="block overflow-hidden text-ellipsis break-words"
                style={{
                  display: "-webkit-box",
                  WebkitLineClamp: 2,
                  WebkitBoxOrient: "vertical",
                }}
                title={props.org.name}
              >
                {props.org.name}
              </span>
            )
          }
        />
        <InfoRow
          label="Primary Admin Email"
          value={props.org.primaryAdmin.email || "-"}
        />
        <InfoRow
          label="Support Email"
          value={
            isEditing ? (
              <Input
                value={values.supportEmail}
                onChange={(event) =>
                  setField("supportEmail", sanitizeSupportEmail(event.target.value))
                }
                className="h-9 border-[#dce5ee] bg-white"
                placeholder="support@organization.com"
                type="email"
                inputMode="email"
                autoComplete="email"
                maxLength={GENERAL_INFO_LIMITS.supportEmail}
              />
            ) : (
              renderSupportEmail(props.org)
            )
          }
        />
        <InfoRow
          label="Organization Slug"
          value={
            isEditing ? (
              <Input
                value={values.slug}
                onChange={(event) => setField("slug", event.target.value)}
                className="h-9 border-[#dce5ee] bg-white"
                disabled
              />
            ) : (
              props.org.slug
            )
          }
        />
        <InfoRow
          label="Created At"
          value={
            props.org.createdAt && props.org.createdAt !== "-"
              ? new Date(props.org.createdAt).toLocaleDateString(undefined, {
                  year: "numeric",
                  month: "short",
                  day: "numeric",
                })
              : "-"
          }
        />
        <InfoRow label="Custom Domain" value={renderCustomDomain(props.org)} />
        <InfoRow label="Region" value={props.org.region} />
        <InfoRow label="Data Residency" value={props.org.dataResidency} />
        <InfoRow label="Timezone" value={props.org.timezone} />
      </div>
    </div>
  );
}

export default GeneralInformationCard;
