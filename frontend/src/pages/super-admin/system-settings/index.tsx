import { useRef, useState, type ReactNode } from "react";
import {
  Bold,
  Eraser,
  Italic,
  List,
  ListOrdered,
  Underline,
} from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Switch } from "@/components/ui/switch";
import { Checkbox } from "@/components/ui/checkbox";
import { Textarea } from "@/components/ui/textarea";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import { cn } from "@/lib/utils";
import {
  useGetSuperAdminEmailTemplateByKeyQuery,
  useGetSuperAdminEmailTemplatesQuery,
  useGetSuperAdminSecuritySettingsQuery,
  useBootstrapStaffProfilesBulkMutation,
  useUpdateSuperAdminEmailTemplateByKeyMutation,
  useUpdateSuperAdminSecuritySettingsMutation,
  useGetTenantRoutingSettingsQuery,
  useUpdateTenantRoutingSettingsMutation,
  useGetImpersonationPolicyQuery,
  useUpdateImpersonationPolicyMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

type SettingsTabKey =
  | "email-templates"
  | "tenant-routing"
  | "impersonation-policy"
  | "security-defaults"
  | "provision";

type AllowedRoleKey =
  | "admin"
  | "supervisor"
  | "therapist"
  | "client"
  | "billing-specialist";

const SETTINGS_TABS: Array<{ key: SettingsTabKey; label: string }> = [
  { key: "email-templates", label: "Email Templates" },
  { key: "tenant-routing", label: "Tenant Routing" },
  { key: "impersonation-policy", label: "Impersonation Policy" },
  { key: "security-defaults", label: "Security Defaults" },
  { key: "provision", label: "Provision" },
];

const ROLE_OPTIONS: Array<{ key: AllowedRoleKey; label: string }> = [
  { key: "admin", label: "Admin" },
  { key: "supervisor", label: "Supervisor" },
  { key: "therapist", label: "Therapist" },
  { key: "client", label: "Client" },
  { key: "billing-specialist", label: "Billing Specialist" },
];

const EMAIL_SUBJECT_MAX_LENGTH = 250;
const EMAIL_BODY_MAX_LENGTH = 20000;
const IMPERSONATION_DURATION_MIN = 1;
const IMPERSONATION_DURATION_MAX = 480;
const IMPERSONATION_DURATION_MAX_DIGITS = String(IMPERSONATION_DURATION_MAX).length;

function sanitizeImpersonationDurationMinutes(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, IMPERSONATION_DURATION_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > IMPERSONATION_DURATION_MAX) {
    return String(IMPERSONATION_DURATION_MAX);
  }

  return digitsOnly;
}

function clampImpersonationDurationMinutes(value: number): number {
  if (!Number.isFinite(value)) return 60;
  return Math.min(IMPERSONATION_DURATION_MAX, Math.max(IMPERSONATION_DURATION_MIN, value));
}

const PASSWORD_POLICY_MIN_LENGTH = 6;
const PASSWORD_POLICY_MAX_LENGTH = 128;
const PASSWORD_POLICY_LENGTH_MAX_DIGITS = String(PASSWORD_POLICY_MAX_LENGTH).length;

function sanitizePasswordPolicyMinLength(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, PASSWORD_POLICY_LENGTH_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > PASSWORD_POLICY_MAX_LENGTH) {
    return String(PASSWORD_POLICY_MAX_LENGTH);
  }

  return digitsOnly;
}

function clampPasswordPolicyMinLength(value: number): number {
  if (!Number.isFinite(value)) return 12;
  return Math.min(PASSWORD_POLICY_MAX_LENGTH, Math.max(PASSWORD_POLICY_MIN_LENGTH, value));
}

const BULK_PROVISION_LIMIT_MIN = 1;
const BULK_PROVISION_LIMIT_MAX = 999_999;
const BULK_PROVISION_LIMIT_MAX_DIGITS = String(BULK_PROVISION_LIMIT_MAX).length;
const BULK_PROVISION_LIMIT_DEFAULT = 200;

function sanitizeBulkProvisionLimit(value: string): string {
  const digitsOnly = value.replace(/\D/g, "").slice(0, BULK_PROVISION_LIMIT_MAX_DIGITS);
  if (!digitsOnly) return "";

  const parsed = Number.parseInt(digitsOnly, 10);
  if (!Number.isFinite(parsed)) return "";
  if (parsed > BULK_PROVISION_LIMIT_MAX) {
    return String(BULK_PROVISION_LIMIT_MAX);
  }

  return digitsOnly;
}

function clampBulkProvisionLimit(value: number): number {
  if (!Number.isFinite(value)) return BULK_PROVISION_LIMIT_DEFAULT;
  return Math.min(BULK_PROVISION_LIMIT_MAX, Math.max(BULK_PROVISION_LIMIT_MIN, value));
}

function getTemplateFieldShellClassName(): string {
  return "relative flex h-[3.75rem] w-full min-w-0 overflow-hidden rounded-[1rem] border border-[#d8e0e7] bg-white";
}

function getTemplateSelectTriggerClassName(): string {
  return cn(
    "h-full !h-full flex-1 data-[size=default]:!h-full !w-full max-w-full min-w-0 overflow-hidden rounded-[1rem] border-0 bg-transparent px-4 py-0 shadow-none",
    "!whitespace-normal items-center text-[0.875rem] font-normal text-[#394653] [&_svg]:shrink-0 [&_svg]:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0",
    "[&_[data-slot=select-value]]:block [&_[data-slot=select-value]]:min-w-0 [&_[data-slot=select-value]]:max-w-full [&_[data-slot=select-value]]:truncate",
  );
}

function getTemplateSelectContentClassName(): string {
  return cn(
    "z-[10050] max-h-[min(15rem,var(--radix-select-content-available-height))]",
    "w-[var(--radix-select-trigger-width)] max-w-[var(--radix-select-trigger-width)]",
    "overflow-hidden border-[#e6ebf0] bg-white p-1 shadow-[0px_12px_24px_rgba(15,23,42,0.08)]",
  );
}

function getTemplateSelectFieldContentClassName(): string {
  return "flex h-full w-full min-w-0 max-w-full flex-col justify-center gap-0.5 overflow-hidden pr-6 text-left";
}

function getTemplateSelectFieldLabelClassName(): string {
  return "text-[0.6875rem] font-medium leading-4 text-[#a5afb9]";
}

function getRoutingSelectTriggerClassName(widthClassName: string): string {
  return cn(
    "h-10 rounded-full border border-[#e3eaf1] bg-white px-4 shadow-[0_1px_2px_rgba(15,23,42,0.04)]",
    "text-[0.875rem] font-medium leading-[1.375rem] text-[#3e4955]",
    "focus-visible:border-[#e3eaf1] focus-visible:ring-0 [&_svg]:text-[#8c98a4]",
    widthClassName,
  );
}

function getSecuritySelectTriggerClassName(): string {
  return cn(
    "h-full !h-full flex-1 data-[size=default]:!h-full w-full min-w-0 overflow-hidden rounded-[1rem] border-0 bg-transparent px-4 py-0 shadow-none",
    "!whitespace-normal items-center text-[0.875rem] font-normal text-[#2b3946] [&_svg]:shrink-0 [&_svg]:text-[#97a4b0]",
    "focus-visible:border-0 focus-visible:ring-0",
    "[&_[data-slot=select-value]]:block [&_[data-slot=select-value]]:min-w-0 [&_[data-slot=select-value]]:max-w-full [&_[data-slot=select-value]]:truncate",
  );
}

function getSecuritySelectFieldShellClassName(): string {
  return "relative flex h-[3.75rem] w-full min-w-0 overflow-hidden rounded-[1rem] border border-[#dce5ee] bg-white";
}

function getSecuritySelectFieldContentClassName(): string {
  return "flex h-full w-full min-w-0 flex-col justify-center gap-0.5 overflow-hidden pr-6 text-left";
}

function getSecuritySelectFieldLabelClassName(): string {
  return "text-[0.6875rem] font-medium leading-4 text-[#8a96a3]";
}

function getSettingsCardClassName(): string {
  return "rounded-[1rem] border border-[#edf1f4] bg-white shadow-[0_1px_3px_rgba(15,23,42,0.04)]";
}

function SettingRow(props: {
  title: string;
  description: string;
  className?: string;
  controlClassName?: string;
  titleClassName?: string;
  descriptionClassName?: string;
  children: ReactNode;
}) {
  return (
    <div
      className={cn(
        "flex flex-col gap-4 py-5 md:flex-row md:items-start md:justify-between md:gap-8",
        props.className,
      )}
    >
      <div className="max-w-[40.625rem]">
        <h3
          className={cn(
            "text-[0.875rem] font-semibold leading-5 text-[#2a3642]",
            props.titleClassName,
          )}
        >
          {props.title}
        </h3>
        <p
          className={cn(
            "mt-2 text-[0.75rem] leading-[1.55] text-[#a2adb8]",
            props.descriptionClassName,
          )}
        >
          {props.description}
        </p>
      </div>

      <div className={cn("w-full md:w-auto", props.controlClassName)}>
        {props.children}
      </div>
    </div>
  );
}

function RoleCheckbox(props: {
  id: string;
  label: string;
  checked: boolean;
  onCheckedChange: (checked: boolean) => void;
}) {
  return (
    <label
      htmlFor={props.id}
      className="inline-flex cursor-pointer items-center gap-[0.625rem] text-[0.875rem] font-medium leading-[1.375rem] text-[#313b45]"
    >
      <Checkbox
        id={props.id}
        checked={props.checked}
        onCheckedChange={props.onCheckedChange}
        className="h-[1.125rem] w-[1.125rem] rounded-[0.3125rem] border-[#cfd7df] checked:border-[#445461] checked:bg-[#445461]"
      />
      <span>{props.label}</span>
    </label>
  );
}

function SystemSettings() {
  const emailBodyRef = useRef<HTMLTextAreaElement | null>(null);
  const [activeTab, setActiveTab] = useState<SettingsTabKey>("email-templates");
  const [templateKey, setTemplateKey] = useState("");
  const [emailSubject, setEmailSubject] = useState("");
  const [emailBody, setEmailBody] = useState("");
  const [emailTemplateActive, setEmailTemplateActive] = useState(true);
  const [emailAutoRouting, setEmailAutoRouting] = useState(false);
  const [pathBasedRouting, setPathBasedRouting] = useState(false);
  const [pathPrefix, setPathPrefix] = useState("/org");
  const [identifierType, setIdentifierType] = useState("Organization Slug");
  const [sessionDuration, setSessionDuration] = useState("60");
  const [requireJustification, setRequireJustification] = useState(false);
  const [minPasswordLength, setMinPasswordLength] = useState("12");
  const [requireUppercase, setRequireUppercase] = useState(false);
  const [requireNumbers, setRequireNumbers] = useState(false);
  const [requireSymbols, setRequireSymbols] = useState(false);
  const [mfaRequirement, setMfaRequirement] = useState("Required for admins");
  const [sessionTimeout, setSessionTimeout] = useState("1 Hour");
  const [lockoutPolicy, setLockoutPolicy] = useState("5 Attempts");
  const [allowedRoles, setAllowedRoles] = useState<
    Record<AllowedRoleKey, boolean>
  >({
    admin: true,
    supervisor: true,
    therapist: true,
    client: true,
    "billing-specialist": true,
  });

  const isEmailTemplatesTab = activeTab === "email-templates";
  const {
    data: emailTemplates = [],
    isLoading: isLoadingEmailTemplates,
    isError: isEmailTemplatesError,
    error: emailTemplatesError,
  } = useGetSuperAdminEmailTemplatesQuery(undefined, {
    skip: !isEmailTemplatesTab,
  });
  const {
    currentData: selectedEmailTemplate,
    isLoading: isLoadingSelectedEmailTemplate,
    isError: isSelectedEmailTemplateError,
    error: selectedEmailTemplateError,
  } = useGetSuperAdminEmailTemplateByKeyQuery(templateKey, {
    skip: !isEmailTemplatesTab || !templateKey,
    refetchOnMountOrArgChange: true,
  });
  const [updateEmailTemplate, { isLoading: isSavingEmailTemplate }] =
    useUpdateSuperAdminEmailTemplateByKeyMutation();
  const [emailTemplateSaveError, setEmailTemplateSaveError] = useState<string | null>(
    null,
  );
  const [emailTemplateSaveSuccess, setEmailTemplateSaveSuccess] = useState(false);
  const isSecurityDefaultsTab = activeTab === "security-defaults";
  const {
    data: securityDefaultsData,
    isLoading: isLoadingSecurityDefaults,
  } = useGetSuperAdminSecuritySettingsQuery(undefined, {
    skip: !isSecurityDefaultsTab,
  });
  const [updateSecurityDefaults, { isLoading: isSavingSecurityDefaults }] =
    useUpdateSuperAdminSecuritySettingsMutation();
  const [securitySaveError, setSecuritySaveError] = useState<string | null>(null);
  const [securitySaveSuccess, setSecuritySaveSuccess] = useState(false);
  const [bulkProvisionLimit, setBulkProvisionLimit] = useState("200");
  const [bulkProvisionError, setBulkProvisionError] = useState<string | null>(null);
  const [bulkProvisionSuccess, setBulkProvisionSuccess] = useState(false);
  const [bootstrapStaffProfilesBulk, { isLoading: isBootstrappingBulk }] =
    useBootstrapStaffProfilesBulkMutation();

  function parseSessionTimeoutToMinutes(value: string): number {
    const normalized = value.trim().toLowerCase();
    if (normalized.includes("30")) return 30;
    if (normalized.includes("2 hour")) return 120;
    if (normalized.includes("1 hour")) return 60;
    const parsed = Number.parseInt(value, 10);
    return Number.isNaN(parsed) ? 60 : parsed;
  }

  const isTenantRoutingTab = activeTab === "tenant-routing";

  const { data: tenantRoutingData, isLoading: isLoadingTenantRouting } =
    useGetTenantRoutingSettingsQuery(undefined, {
      skip: !isTenantRoutingTab,
    });

  const [updateTenantRouting, { isLoading: isSavingRouting }] =
    useUpdateTenantRoutingSettingsMutation();

  const [routingSaveError, setRoutingSaveError] = useState<string | null>(null);
  const [routingSaveSuccess, setRoutingSaveSuccess] = useState(false);

  const [routingInitialized, setRoutingInitialized] = useState(false);
  if (tenantRoutingData && !routingInitialized) {
    setRoutingInitialized(true);
    setEmailAutoRouting(tenantRoutingData.emailAutoRouting);
    setPathBasedRouting(tenantRoutingData.pathBasedRouting);
    setPathPrefix(tenantRoutingData.pathPrefix);
    setIdentifierType(tenantRoutingData.orgIdentifier === "id" ? "Organization ID" : "Organization Slug");
  }

  async function handleSaveRouting() {
    setRoutingSaveError(null);
    setRoutingSaveSuccess(false);
    try {
      await updateTenantRouting({
        emailAutoRouting,
        pathBasedRouting,
        pathPrefix,
        orgIdentifier: identifierType === "Organization Slug" ? "slug" : "id",
      }).unwrap();
      setRoutingSaveSuccess(true);
      setTimeout(() => setRoutingSaveSuccess(false), 3000);
    } catch (err) {
      setRoutingSaveError(
        getApiErrorMessage(err)
      );
    }
  }

  if (isEmailTemplatesTab && !templateKey && emailTemplates.length > 0) setTemplateKey(emailTemplates[0].templateKey);

  const [emailDraftKey, setEmailDraftKey] = useState(templateKey);
  const [emailInitialized, setEmailInitialized] = useState(false);
  if (emailDraftKey !== templateKey) {
    setEmailDraftKey(templateKey);
    setEmailInitialized(false);
    setEmailSubject("");
    setEmailBody("");
    setEmailTemplateActive(true);
  } else if (selectedEmailTemplate && !emailInitialized) {
    setEmailInitialized(true);
    setEmailSubject(selectedEmailTemplate.subjectTemplate);
    setEmailBody(selectedEmailTemplate.bodyTemplate);
    setEmailTemplateActive(selectedEmailTemplate.isActive);
  }

  function getEmailSubjectError(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) return "Email subject is required.";
    if (value.length > EMAIL_SUBJECT_MAX_LENGTH) {
      return `Subject must be ${EMAIL_SUBJECT_MAX_LENGTH} characters or less.`;
    }
    return null;
  }

  function getEmailBodyError(value: string): string | null {
    const trimmed = value.trim();
    if (!trimmed) return "Email body is required.";
    if (value.length > EMAIL_BODY_MAX_LENGTH) {
      return `Email body must be ${EMAIL_BODY_MAX_LENGTH} characters or less.`;
    }
    return null;
  }

  async function handleSaveEmailTemplate() {
    if (!templateKey) return;
    const emailSubjectError = getEmailSubjectError(emailSubject);
    const emailBodyError = getEmailBodyError(emailBody);
    if (emailSubjectError || emailBodyError) {
      setEmailTemplateSaveError(emailSubjectError ?? emailBodyError);
      setEmailTemplateSaveSuccess(false);
      return;
    }
    setEmailTemplateSaveError(null);
    setEmailTemplateSaveSuccess(false);
    try {
      const updatedTemplate = await updateEmailTemplate({
        templateKey,
        body: {
          subjectTemplate: emailSubject,
          bodyTemplate: emailBody,
          active: emailTemplateActive,
        },
      }).unwrap();
      setEmailSubject(updatedTemplate.subjectTemplate);
      setEmailBody(updatedTemplate.bodyTemplate);
      setEmailTemplateActive(updatedTemplate.isActive);
      setEmailTemplateSaveSuccess(true);
      setTimeout(() => setEmailTemplateSaveSuccess(false), 3000);
    } catch (error) {
      setEmailTemplateSaveError(getApiErrorMessage(error));
    }
  }

  const [securityInitialized, setSecurityInitialized] = useState(false);
  if (securityDefaultsData && !securityInitialized) {
    setSecurityInitialized(true);
    setMinPasswordLength(String(clampPasswordPolicyMinLength(securityDefaultsData.minReasonLength || 10)));
    setRequireUppercase(securityDefaultsData.requireReason);
    setRequireNumbers(securityDefaultsData.allowCrossOrganisation);
    setRequireSymbols(securityDefaultsData.enabled);
    setSessionTimeout(`${securityDefaultsData.maxDurationMinutes || 60} Minutes`);
  }

  function replaceEmailBodySelection(
    getReplacement: (selectedText: string) => string,
    options?: { applyToWholeWhenEmpty?: boolean },
  ) {
    const textarea = emailBodyRef.current;
    if (!textarea) return;

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    if (start === end && options?.applyToWholeWhenEmpty) {
      const replacement = getReplacement(emailBody);
      setEmailBody(replacement);
      requestAnimationFrame(() => {
        textarea.focus();
        textarea.setSelectionRange(replacement.length, replacement.length);
      });
      return;
    }

    const selectedText = emailBody.slice(start, end);
    const replacement = getReplacement(selectedText);
    const nextValue = `${emailBody.slice(0, start)}${replacement}${emailBody.slice(end)}`;
    const nextCursor = start + replacement.length;

    setEmailBody(nextValue);
    requestAnimationFrame(() => {
      textarea.focus();
      textarea.setSelectionRange(nextCursor, nextCursor);
    });
  }

  function handleInlineFormat(tagName: "strong" | "em" | "u") {
    replaceEmailBodySelection((selectedText) => {
      const content = selectedText || "Text";
      return `<${tagName}>${content}</${tagName}>`;
    });
  }

  function handleListFormat(type: "ol" | "ul") {
    replaceEmailBodySelection((selectedText) => {
      const lines = (selectedText || "List item")
        .split("\n")
        .map((line) => line.trim())
        .filter(Boolean);
      const items = lines.map((line) => `  <li>${line}</li>`).join("\n");
      return `<${type}>\n${items}\n</${type}>`;
    });
  }

  function handleClearFormatting() {
    replaceEmailBodySelection(
      (selectedText) => selectedText.replace(/<[^>]+>/g, ""),
      { applyToWholeWhenEmpty: true },
    );
  }

  async function handleSaveSecurityDefaults() {
    setSecuritySaveError(null);
    setSecuritySaveSuccess(false);
    try {
      const payload = {
        enabled: requireSymbols,
        requireReason: requireUppercase,
        minReasonLength: clampPasswordPolicyMinLength(
          Number.parseInt(minPasswordLength, 10) || 10,
        ),
        maxDurationMinutes: parseSessionTimeoutToMinutes(sessionTimeout),
        allowCrossOrganisation: requireNumbers,
        allowedRoles: securityDefaultsData?.allowedRoles ?? ["ADMIN"],
        deniedRoles: securityDefaultsData?.deniedRoles ?? [],
        allowedOrgIds: securityDefaultsData?.allowedOrgIds ?? [],
        deniedOrgIds: securityDefaultsData?.deniedOrgIds ?? [],
      };
      const updated = await updateSecurityDefaults(payload).unwrap();
      setMinPasswordLength(String(clampPasswordPolicyMinLength(updated.minReasonLength || payload.minReasonLength)));
      setRequireUppercase(updated.requireReason);
      setRequireNumbers(updated.allowCrossOrganisation);
      setRequireSymbols(updated.enabled);
      setSessionTimeout(`${updated.maxDurationMinutes || payload.maxDurationMinutes} Minutes`);
      setSecuritySaveSuccess(true);
      setTimeout(() => setSecuritySaveSuccess(false), 3000);
    } catch (error) {
      setSecuritySaveError(getApiErrorMessage(error));
    }
  }

  async function handleBulkBootstrapProvision() {
    setBulkProvisionError(null);
    setBulkProvisionSuccess(false);
    try {
      const parsedLimit = clampBulkProvisionLimit(
        Number.parseInt(bulkProvisionLimit, 10) || BULK_PROVISION_LIMIT_DEFAULT,
      );
      await bootstrapStaffProfilesBulk(parsedLimit).unwrap();
      setBulkProvisionSuccess(true);
      setTimeout(() => setBulkProvisionSuccess(false), 3000);
    } catch (error) {
      setBulkProvisionError(getApiErrorMessage(error));
    }
  }

  function handleRoleToggle(role: AllowedRoleKey, checked: boolean) {
    setAllowedRoles((current) => ({
      ...current,
      [role]: checked,
    }));
  }

  const exampleUrl =
    identifierType === "Organization Slug"
      ? `https://therapyflow.pro${pathPrefix}/harbor/login`
      : `https://therapyflow.pro${pathPrefix}/tenant-4829/login`;

  const isImpersonationTab = activeTab === "impersonation-policy";

  // --- Impersonation policy API ---
  const { data: impersonationData, isLoading: isLoadingImpersonation } =
    useGetImpersonationPolicyQuery(undefined, { skip: !isImpersonationTab });

  const [updateImpersonationPolicy, { isLoading: isSavingPolicy }] =
    useUpdateImpersonationPolicyMutation();

  const [policySaveError, setPolicySaveError] = useState<string | null>(null);
  const [policySaveSuccess, setPolicySaveSuccess] = useState(false);

  const [policyInitialized, setPolicyInitialized] = useState(false);
  if (impersonationData && !policyInitialized) {
    setPolicyInitialized(true);
    setSessionDuration(String(clampImpersonationDurationMinutes(impersonationData.maxDurationMinutes)));
    setRequireJustification(impersonationData.requireReason);
    const apiRoles = impersonationData.allowedRoles.map(role => role.toLowerCase());
    setAllowedRoles({ admin: apiRoles.includes("admin"), supervisor: apiRoles.includes("supervisor"), therapist: apiRoles.includes("therapist"), client: apiRoles.includes("client"), "billing-specialist": apiRoles.includes("billing-specialist") || apiRoles.includes("billing_specialist") });
  }

  async function handleSavePolicy() {
    setPolicySaveError(null);
    setPolicySaveSuccess(false);
    try {
      const enabledRoles = (Object.keys(allowedRoles) as Array<keyof typeof allowedRoles>)
        .filter((key) => allowedRoles[key]);
      await updateImpersonationPolicy({
        enabled: true,
        requireReason: requireJustification,
        minReasonLength: impersonationData?.minReasonLength ?? 10,
        maxDurationMinutes: clampImpersonationDurationMinutes(
          Number.parseInt(sessionDuration, 10) || 60,
        ),
        allowCrossOrganisation: impersonationData?.allowCrossOrganisation ?? false,
        allowedRoles: enabledRoles,
        deniedRoles: impersonationData?.deniedRoles ?? [],
        allowedOrgIds: impersonationData?.allowedOrgIds ?? [],
        deniedOrgIds: impersonationData?.deniedOrgIds ?? [],
      }).unwrap();
      setPolicySaveSuccess(true);
      setTimeout(() => setPolicySaveSuccess(false), 3000);
    } catch (err) {
      setPolicySaveError(getApiErrorMessage(err));
    }
  }


    const emailSubjectError = getEmailSubjectError(emailSubject);
    const emailBodyError = getEmailBodyError(emailBody);
    const hasEmailTemplateValidationError = Boolean(emailSubjectError || emailBodyError);
    const toolbarButtons: Array<
      | {
          label: string;
          divider: true;
        }
      | {
          label: string;
          icon: typeof Bold;
          action: "strong" | "em" | "u" | "ol" | "ul" | "clear";
        }
    > = [
      { icon: Bold, label: "Bold", action: "strong" },
      { icon: Italic, label: "Italic", action: "em" },
      { icon: Underline, label: "Underline", action: "u" },
      { divider: true, label: "divider-1" },
      {
        icon: ListOrdered,
        label: "Numbered list",
        action: "ol",
      },
      { icon: List, label: "Bulleted list", action: "ul" },
      { divider: true, label: "divider-2" },
      { icon: Eraser, label: "Clear formatting", action: "clear" },
    ];


  function renderTenantRoutingPanel() {
    return (
      <>
        <div className="border-b border-[#eef2f5] px-[1.875rem] py-7">
          <h2 className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
            Tenant Routing (Single Domain)
          </h2>
          <p className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
            Configure how users authenticate and access their specific tenant
            environment on a shared domain.
          </p>
        </div>

        {isLoadingTenantRouting ? (
          <div className="px-[1.875rem] py-10 text-[0.875rem] text-[#8f9aa6]">
            Loading settings...
          </div>
        ) : (
          <div className="px-[1.875rem] pb-[1.875rem] pt-[1.125rem]">
            <SettingRow
              title="Email Auto-Routing"
            description="Automatically resolve the user's organization based on their email domain or explicit directory lookup before requiring a password."
            className="py-[1.125rem]"
            controlClassName="md:pt-[0.125rem]"
            titleClassName="text-[1rem] leading-6 text-[#1e282e]"
            descriptionClassName="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]"
          >
            <Switch
              checked={emailAutoRouting}
              onCheckedChange={setEmailAutoRouting}
              className="h-[1.375rem] w-[2.375rem]"
              onClassName="bg-[#445461]"
              offClassName="bg-[#d7e0e8]"
            />
          </SettingRow>

          <SettingRow
            title="Path-Based Routing"
            description="Allows users to log in directly via a dedicated URL path, skipping the email resolution step. Useful for organizations using SSO."
            className="py-[1.125rem]"
            controlClassName="md:pt-[0.125rem]"
            titleClassName="text-[1rem] leading-6 text-[#1e282e]"
            descriptionClassName="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]"
          >
            <Switch
              checked={pathBasedRouting}
              onCheckedChange={setPathBasedRouting}
              className="h-[1.375rem] w-[2.375rem]"
              onClassName="bg-[#445461]"
              offClassName="bg-[#d7e0e8]"
            />
          </SettingRow>

          <SettingRow
            title="Path Prefix"
            description="The routing prefix used for path-based login URLs."
            className="py-[1.125rem]"
            controlClassName="md:pt-[0.125rem]"
            titleClassName="text-[1rem] leading-6 text-[#1e282e]"
            descriptionClassName="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]"
          >
            <Select value={pathPrefix} onValueChange={setPathPrefix}>
              <SelectTrigger
                className={getRoutingSelectTriggerClassName(
                  "w-full sm:w-[8.625rem]",
                )}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="border-[#e6ebf0] bg-white shadow-[0px_12px_24px_rgba(15,23,42,0.08)]">
                <SelectItem value="/org">/org</SelectItem>
                <SelectItem value="/tenant">/tenant</SelectItem>
                <SelectItem value="/workspace">/workspace</SelectItem>
              </SelectContent>
            </Select>
          </SettingRow>

          <SettingRow
            title="Organization Identifier"
            description="The attribute used in the URL path to identify the specific tenant."
            className="py-[1.125rem]"
            controlClassName="md:pt-[0.125rem]"
            titleClassName="text-[1rem] leading-6 text-[#1e282e]"
            descriptionClassName="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]"
          >
            <Select value={identifierType} onValueChange={setIdentifierType}>
              <SelectTrigger
                className={getRoutingSelectTriggerClassName(
                  "w-full sm:w-[12.625rem]",
                )}
              >
                <SelectValue />
              </SelectTrigger>
              <SelectContent className="border-[#e6ebf0] bg-white shadow-[0px_12px_24px_rgba(15,23,42,0.08)]">
                <SelectItem value="Organization Slug">
                  Organization Slug
                </SelectItem>
                <SelectItem value="Organization ID">Organization ID</SelectItem>
              </SelectContent>
            </Select>
          </SettingRow>

          <div className="pb-1 pt-[0.375rem]">
            <div className="w-full rounded-[0.875rem] bg-[#f4f7fa] px-[0.875rem] py-[0.6875rem]">
              <div className="text-[0.875rem] font-semibold leading-[1.375rem] text-[#33414d]">
                Example Login URL
              </div>
              <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#a0aab5]">
                {exampleUrl}
              </div>
            </div>
          </div>

          {routingSaveError && (
            <div className="mx-[1.875rem] mb-3 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
              {routingSaveError}
            </div>
          )}
          {routingSaveSuccess && (
            <div className="mx-[1.875rem] mb-3 rounded-[0.75rem] border border-[#d1fae5] bg-[#f0fdf4] px-4 py-3 text-[0.8125rem] text-[#065f46]">
              Routing settings saved successfully.
            </div>
          )}
          <div className="flex justify-end pt-7">
            <Button
              variant="primary"
              size="md"
              onClick={handleSaveRouting}
              disabled={isSavingRouting}
              className="min-w-[9.4375rem]"
              loading={isSavingRouting}
              loadingLabel="Saving..."
            >
              Save Routing
            </Button>
          </div>
        </div>
        )}
      </>
    );
  }

  function renderImpersonationPanel() {
    return (
      <>
        <div className="border-b border-[#eef2f5] px-[1.875rem] py-7">
          <h2 className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
            Impersonation Controls
          </h2>
          <p className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
            Define the boundaries and limitations for administrative support
            impersonation sessions.
          </p>
        </div>

        {isLoadingImpersonation ? (
          <div className="px-[1.875rem] py-10 text-[0.875rem] text-[#8f9aa6]">
            Loading policy...
          </div>
        ) : (
        <div className="px-[1.875rem] pb-[1.875rem] pt-[1.125rem]">
          <SettingRow
            title="Maximum Session Duration"
            description="Set the maximum number of minutes an impersonation session can last before automatically expiring."
            className="border-b border-[#f1f4f6] py-[1.125rem]"
            controlClassName="md:pt-[0.375rem]"
            titleClassName="text-[1rem] leading-6 text-[#1e282e]"
            descriptionClassName="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]"
          >
            <div className="flex h-10 w-full overflow-hidden rounded-full border border-[#e3eaf1] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)] md:w-[11.25rem]">
              <div className="flex items-center border-r border-[#edf1f4] px-4 text-[0.875rem] font-medium leading-[1.375rem] text-[#9aa5b1]">
                Minutes
              </div>
              <Input
                type="text"
                inputMode="numeric"
                value={sessionDuration}
                maxLength={IMPERSONATION_DURATION_MAX_DIGITS}
                onChange={(event) =>
                  setSessionDuration(sanitizeImpersonationDurationMinutes(event.target.value))
                }
                className="h-full border-0 bg-transparent px-4 text-right text-[0.875rem] font-medium leading-[1.375rem] text-[#3a4551] shadow-none focus-visible:ring-0"
              />
            </div>
          </SettingRow>

          <SettingRow
            title="Require Justification"
            description="Mandate that support staff enter a reason (e.g. Zendesk ticket number) before starting an impersonation session."
            className="py-[1.125rem]"
            controlClassName="md:pt-[0.25rem]"
            titleClassName="text-[1rem] leading-6 text-[#1e282e]"
            descriptionClassName="mt-1 max-w-[45rem] text-[0.875rem] leading-[1.375rem] text-[#9aa4af]"
          >
            <Switch
              checked={requireJustification}
              onCheckedChange={setRequireJustification}
              className="h-[1.375rem] w-[2.375rem]"
              onClassName="bg-[#5e7d8d]"
              offClassName="bg-[#d7e0e8]"
            />
          </SettingRow>

          <div className="pb-[0.625rem] pt-[1.375rem]">
            <div className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
              Target Role Allowlist
            </div>
            <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]">
              Select which roles Super Admins are permitted to impersonate
              within tenant organizations.
            </div>

            <div className="mt-4 grid max-w-[22.5rem] grid-cols-1 gap-x-[2.75rem] gap-y-[0.625rem] sm:grid-cols-2">
              {ROLE_OPTIONS.map((role) => (
                <RoleCheckbox
                  key={role.key}
                  id={`impersonation-role-${role.key}`}
                  label={role.label}
                  checked={allowedRoles[role.key]}
                  onCheckedChange={(checked) =>
                    handleRoleToggle(role.key, checked)
                  }
                />
              ))}
            </div>
          </div>

          {policySaveError && (
            <div className="mt-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
              {policySaveError}
            </div>
          )}
          {policySaveSuccess && (
            <div className="mt-4 rounded-[0.75rem] border border-[#d1fae5] bg-[#f0fdf4] px-4 py-3 text-[0.8125rem] text-[#065f46]">
              Policy saved successfully.
            </div>
          )}

          <div className="flex justify-end pt-[1.75rem]">
            <Button
              variant="primary"
              size="md"
              onClick={handleSavePolicy}
              disabled={isSavingPolicy}
              className="min-w-[9.5rem]"
              loading={isSavingPolicy}
              loadingLabel="Saving..."
            >
              Save Policy
            </Button>
          </div>
        </div>
        )}
      </>
    );
  }

  function renderSecurityDefaultsPanel() {
    if (isLoadingSecurityDefaults) {
      return (
        <section className={getSettingsCardClassName()}>
          <div className="px-[1.375rem] py-8 text-[0.875rem] text-[#8f9aa6]">
            Loading security defaults...
          </div>
        </section>
      );
    }

    return (
      <div className="flex flex-col gap-5">
        <section className={getSettingsCardClassName()}>
          <div className="border-b border-[#eef2f5] px-[1.375rem] py-[1.375rem]">
            <h2 className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
              Global Security Defaults
            </h2>
            <p className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
              Configure baseline security and authentication policies. These
              apply to all organizations unless specifically overridden.
            </p>
          </div>

          <div className="px-[1.375rem] py-[1.375rem]">
            <div className="max-w-[42.5rem]">
              <h3 className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
                Password Policy
              </h3>
              <p className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]">
                Define complexity requirement for all user passwords across the
                platform
              </p>
            </div>

            <div className="mt-5 flex h-[2.5rem] w-full max-w-[11rem] overflow-hidden rounded-[0.75rem] border border-[#e6ebf0] bg-white shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
              <div className="flex items-center border-r border-[#edf1f4] px-4 text-[0.875rem] leading-[1.375rem] text-[#9aa5b1] whitespace-nowrap">
                Mini Length:
              </div>
              <Input
                type="text"
                inputMode="numeric"
                value={minPasswordLength}
                maxLength={PASSWORD_POLICY_LENGTH_MAX_DIGITS}
                onChange={(event) =>
                  setMinPasswordLength(sanitizePasswordPolicyMinLength(event.target.value))
                }
                className="h-full w-[3.75rem] border-0 bg-transparent px-3 text-center text-[0.875rem] font-medium leading-[1.375rem] text-[#3a4551] shadow-none focus-visible:ring-0"
              />
            </div>

            <div className="mt-5 flex max-w-[11.25rem] flex-col gap-4">
              <div className="flex items-center gap-4">
                <span className="min-w-[8.25rem] text-[0.875rem] font-semibold leading-[1.375rem] text-[#313b45]">
                  Require Uppercase
                </span>
                <Switch
                  checked={requireUppercase}
                  onCheckedChange={setRequireUppercase}
                  className="h-[1.375rem] w-[2.375rem]"
                  onClassName="bg-[#445461]"
                  offClassName="bg-[#d7e0e8]"
                />
              </div>

              <div className="flex items-center gap-4">
                <span className="min-w-[8.25rem] text-[0.875rem] font-semibold leading-[1.375rem] text-[#313b45]">
                  Require Numbers
                </span>
                <Switch
                  checked={requireNumbers}
                  onCheckedChange={setRequireNumbers}
                  className="h-[1.375rem] w-[2.375rem]"
                  onClassName="bg-[#445461]"
                  offClassName="bg-[#d7e0e8]"
                />
              </div>

              <div className="flex items-center gap-4">
                <span className="min-w-[8.25rem] text-[0.875rem] font-semibold leading-[1.375rem] text-[#313b45]">
                  Require Symbols
                </span>
                <Switch
                  checked={requireSymbols}
                  onCheckedChange={setRequireSymbols}
                  className="h-[1.375rem] w-[2.375rem]"
                  onClassName="bg-[#445461]"
                  offClassName="bg-[#d7e0e8]"
                />
              </div>
            </div>
          </div>
        </section>

        <section className={getSettingsCardClassName()}>
          <div className="px-[1.375rem] pt-0">
            <div className="grid grid-cols-1 gap-0 lg:grid-cols-[minmax(0,1fr)_28.75rem]">
              <div className="border-b border-[#eef2f5] py-[1.625rem] pr-8">
                <div className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
                  Multi-Factor Authentication (MFA)
                </div>
                <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]">
                  Set the global requirement level for two-step verification.
                </div>
              </div>
              <div className="border-b border-[#eef2f5] py-[1.125rem] lg:flex lg:items-center lg:justify-center">
                <Select
                  value={mfaRequirement}
                  onValueChange={setMfaRequirement}
                >
                  <div
                    className={cn(
                      getSecuritySelectFieldShellClassName(),
                      "w-full lg:w-[25.75rem]",
                    )}
                  >
                    <SelectTrigger
                      className={getSecuritySelectTriggerClassName()}
                    >
                      <div className={getSecuritySelectFieldContentClassName()}>
                        <span className={getSecuritySelectFieldLabelClassName()}>
                          MFA Requirement
                        </span>
                        <SelectValue
                          placeholder="Select MFA requirement"
                          className="block w-full min-w-0 truncate text-[0.875rem] font-normal leading-5 text-[#2b3946] data-[placeholder]:text-[#97a4b0]"
                        />
                      </div>
                    </SelectTrigger>
                  </div>
                  <SelectContent className="border-[#e6ebf0] bg-white shadow-[0px_12px_24px_rgba(15,23,42,0.08)]">
                    <SelectItem value="Required for admins">
                      Required for admins
                    </SelectItem>
                    <SelectItem value="Required for all users">
                      Required for all users
                    </SelectItem>
                    <SelectItem value="Optional">Optional</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="border-b border-[#eef2f5] py-[1.625rem] pr-8">
                <div className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
                  Session Timeout
                </div>
                <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]">
                  Automatically log users out after a period of inactivity.
                </div>
              </div>
              <div className="border-b border-[#eef2f5] py-[1.125rem] lg:flex lg:items-center lg:justify-center">
                <Select
                  value={sessionTimeout}
                  onValueChange={setSessionTimeout}
                >
                  <div
                    className={cn(
                      getSecuritySelectFieldShellClassName(),
                      "w-full lg:w-[25.75rem]",
                    )}
                  >
                    <SelectTrigger
                      className={getSecuritySelectTriggerClassName()}
                    >
                      <div className={getSecuritySelectFieldContentClassName()}>
                        <span className={getSecuritySelectFieldLabelClassName()}>
                          Session Timeout
                        </span>
                        <SelectValue
                          placeholder="Select session timeout"
                          className="block w-full min-w-0 truncate text-[0.875rem] font-normal leading-5 text-[#2b3946] data-[placeholder]:text-[#97a4b0]"
                        />
                      </div>
                    </SelectTrigger>
                  </div>
                  <SelectContent className="border-[#e6ebf0] bg-white shadow-[0px_12px_24px_rgba(15,23,42,0.08)]">
                    <SelectItem value="30 Minutes">30 Minutes</SelectItem>
                    <SelectItem value="1 Hour">1 Hour</SelectItem>
                    <SelectItem value="2 Hours">2 Hours</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div className="py-[1.625rem] pr-8">
                <div className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
                  Account Lockout Policy
                </div>
                <div className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#9aa4af]">
                  Number of consecutive failed login attempts before temporarily
                  locking an account.
                </div>
              </div>
              <div className="py-[1.125rem] lg:flex lg:items-center lg:justify-center">
                <Select value={lockoutPolicy} onValueChange={setLockoutPolicy}>
                  <div
                    className={cn(
                      getSecuritySelectFieldShellClassName(),
                      "w-full lg:w-[25.75rem]",
                    )}
                  >
                    <SelectTrigger
                      className={getSecuritySelectTriggerClassName()}
                    >
                      <div className={getSecuritySelectFieldContentClassName()}>
                        <span className={getSecuritySelectFieldLabelClassName()}>
                          Account Lockout
                        </span>
                        <SelectValue
                          placeholder="Select lockout policy"
                          className="block w-full min-w-0 truncate text-[0.875rem] font-normal leading-5 text-[#2b3946] data-[placeholder]:text-[#97a4b0]"
                        />
                      </div>
                    </SelectTrigger>
                  </div>
                  <SelectContent className="border-[#e6ebf0] bg-white shadow-[0px_12px_24px_rgba(15,23,42,0.08)]">
                    <SelectItem value="3 Attempts">3 Attempts</SelectItem>
                    <SelectItem value="5 Attempts">5 Attempts</SelectItem>
                    <SelectItem value="10 Attempts">10 Attempts</SelectItem>
                  </SelectContent>
                </Select>
              </div>
            </div>

            {securitySaveError ? (
              <div className="mt-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
                {securitySaveError}
              </div>
            ) : null}
            {securitySaveSuccess ? (
              <div className="mt-4 rounded-[0.75rem] border border-[#d1fae5] bg-[#f0fdf4] px-4 py-3 text-[0.8125rem] text-[#065f46]">
                Security defaults saved successfully.
              </div>
            ) : null}

            <div className="flex justify-end pb-[1.375rem] pt-[0.375rem]">
              <Button
                variant="primary"
                size="md"
                onClick={handleSaveSecurityDefaults}
                disabled={isSavingSecurityDefaults}
                loading={isSavingSecurityDefaults}
                loadingLabel="Saving..."
              >
                Save Security Defaults
              </Button>
            </div>
          </div>
        </section>
      </div>
    );
  }

  function renderProvisionPanel() {
    return (
      <div className={getSettingsCardClassName()}>
        <div className="border-b border-[#eef2f5] px-[1.375rem] py-[1.375rem]">
          <h2 className="text-[1rem] font-semibold leading-6 text-[#1e282e]">
            Bulk Staff Profiles Bootstrap
          </h2>
          <p className="mt-1 text-[0.875rem] leading-[1.375rem] text-[#8f9aa6]">
            Repair missing tenant users records across organisations.
          </p>
        </div>
        <div className="px-[1.375rem] py-[1.375rem]">
          <div className="max-w-[17.5rem]">
            <div className="mb-2 text-[0.8125rem] font-medium text-[#334155]">Limit</div>
            <Input
              type="text"
              value={bulkProvisionLimit}
              inputMode="numeric"
              maxLength={BULK_PROVISION_LIMIT_MAX_DIGITS}
              onChange={(event) =>
                setBulkProvisionLimit(sanitizeBulkProvisionLimit(event.target.value))
              }
              className="h-10 rounded-[0.75rem] border-[#dce5ee]"
              placeholder="200"
            />
          </div>

          {bulkProvisionError ? (
            <div className="mt-4 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
              {bulkProvisionError}
            </div>
          ) : null}
          {bulkProvisionSuccess ? (
            <div className="mt-4 rounded-[0.75rem] border border-[#d1fae5] bg-[#f0fdf4] px-4 py-3 text-[0.8125rem] text-[#065f46]">
              Bulk bootstrap queued successfully.
            </div>
          ) : null}

          <div className="mt-5 flex justify-end">
            <Button
              variant="primary"
              size="md"
              onClick={handleBulkBootstrapProvision}
              disabled={isBootstrappingBulk}
              loading={isBootstrappingBulk}
              loadingLabel="Bootstrapping..."
            >
              Run Bulk Bootstrap
            </Button>
          </div>
        </div>
      </div>
    );
  }

  function renderPlaceholderPanel(title: string, description: string) {
    return (
      <>
        <div className="border-b border-[#eef2f5] px-6 py-6">
          <h2 className="text-[1.375rem] font-semibold leading-7 text-[#25323e]">
            {title}
          </h2>
          <p className="mt-1 text-[0.75rem] leading-5 text-[#9ca7b2]">
            {description}
          </p>
        </div>

        <div className="px-6 py-10">
          <div className="rounded-[0.75rem] border border-dashed border-[#d9e1e8] bg-[#fafcfd] px-5 py-8 text-[0.8125rem] text-[#8b96a2]">
            Configuration content for this tab can be added next.
          </div>
        </div>
      </>
    );
  }

  return (
    <SuperAdminPageShell
      title="System Settings"
      description="Manage global configurations, communication templates, and security policies."
    >
      <div className="w-full overflow-x-auto pb-1">
        <div className="flex min-w-max flex-nowrap items-center gap-1 rounded-full bg-[#eceff3] p-1">
          {SETTINGS_TABS.map((tab) => (
            <button
              key={tab.key}
              type="button"
              onClick={() => setActiveTab(tab.key)}
              className={cn(
                "whitespace-nowrap rounded-full px-[1.125rem] py-[0.4375rem] text-[0.875rem] font-medium leading-[1.375rem] transition-colors",
                activeTab === tab.key
                  ? "bg-white text-[#2f3945] shadow-[0_1px_2px_rgba(15,23,42,0.05)]"
                  : "text-[#697584] hover:text-[#2f3945]",
              )}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {activeTab === "security-defaults" ? (
        renderSecurityDefaultsPanel()
      ) : activeTab === "provision" ? (
        renderProvisionPanel()
      ) : (
        <section className="rounded-[1rem] border border-[#edf1f4] bg-white shadow-[0_1px_3px_rgba(15,23,42,0.04)]">
          {activeTab === "email-templates"
            ? (
      <div className="px-5 pb-4 pt-[1.125rem]">
        <h2 className="text-[1rem] font-semibold leading-6 text-[#25323e]">
          Configure Global Email Templates
        </h2>

        <div className="mt-4 grid min-w-0 grid-cols-1 gap-x-[2.125rem] gap-y-3 lg:grid-cols-2">
          <div className="min-w-0 w-full">
            <Select
              value={templateKey}
              onValueChange={(next) => {
                setTemplateKey(next);
                setEmailTemplateSaveError(null);
                setEmailTemplateSaveSuccess(false);
              }}
            >
              <div className={getTemplateFieldShellClassName()}>
                <SelectTrigger className={getTemplateSelectTriggerClassName()}>
                  <div className={getTemplateSelectFieldContentClassName()}>
                    <span className={getTemplateSelectFieldLabelClassName()}>
                      Template Type <span className="text-[#ef6b6b]">*</span>
                    </span>
                    <SelectValue asChild>
                      <span
                        className={cn(
                          "block w-full max-w-full min-w-0 truncate text-[0.875rem] font-normal leading-5",
                          templateKey ? "text-[#394653]" : "text-[#9ca7b2]",
                        )}
                        title={templateKey || undefined}
                      >
                        {templateKey || "Select template type"}
                      </span>
                    </SelectValue>
                  </div>
                </SelectTrigger>
              </div>
              <SelectContent
                position="popper"
                side="bottom"
                align="start"
                collisionPadding={12}
                className={getTemplateSelectContentClassName()}
              >
                {emailTemplates.map((template) => (
                  <SelectItem
                    key={template.templateKey}
                    value={template.templateKey}
                    title={template.templateKey}
                    className="min-w-0 [&_div]:min-w-0 [&_div]:truncate"
                  >
                    {template.templateKey}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            <p className="mt-2 text-[0.75rem] leading-5 text-[#8f9aa6]">
              Select which global email template you want to edit.
            </p>
          </div>

          <div className="min-w-0 w-full">
            <Input
              value={emailSubject}
              onChange={(event) => setEmailSubject(event.target.value)}
              placeholder="Email Subject"
              maxLength={EMAIL_SUBJECT_MAX_LENGTH}
              className="h-[3.75rem] w-full rounded-[1rem] border-[#d8e0e7] bg-white px-4 text-[0.875rem] text-[#394653] shadow-none focus-visible:border-[#d8e0e7] focus-visible:ring-0 placeholder:text-[0.875rem] placeholder:text-[#9ca7b2]"
            />
            <p
              className={cn(
                "mt-2 text-[0.75rem] leading-5",
                emailSubjectError ? "text-[#b42318]" : "text-[#8f9aa6]",
              )}
            >
              {emailSubjectError ??
                `Max ${EMAIL_SUBJECT_MAX_LENGTH} characters (${emailSubject.length}/${EMAIL_SUBJECT_MAX_LENGTH}).`}
            </p>
          </div>
        </div>

        {isLoadingEmailTemplates || isLoadingSelectedEmailTemplate ? (
          <div className="mt-3 text-[0.75rem] leading-5 text-[#8f9aa6]">
            Loading email templates...
          </div>
        ) : null}
        {isEmailTemplatesError ? (
          <div className="mt-3 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
            {getApiErrorMessage(emailTemplatesError)}
          </div>
        ) : null}
        {isSelectedEmailTemplateError ? (
          <div className="mt-3 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
            {getApiErrorMessage(selectedEmailTemplateError)}
          </div>
        ) : null}

        <div className="mt-5 overflow-hidden rounded-[1rem] border border-[#d8e0e7] bg-white">
          <div className="flex h-[2.75rem] flex-wrap items-center gap-0.5 border-b border-[#e9eff4] px-3.5">
            {toolbarButtons.map((button) => {
              if ("divider" in button && button.divider) {
                return (
                  <span
                    key={button.label}
                    aria-hidden="true"
                    className="mx-1 h-4 w-px bg-[#e6ebf0]"
                  />
                );
              }

              if (!("icon" in button)) {
                return null;
              }

              const Icon = button.icon;

              return (
                <button
                  key={button.label}
                  type="button"
                  aria-label={button.label}
                  onClick={() => {
                    if (button.action === "clear") handleClearFormatting();
                    else if (button.action === "ol" || button.action === "ul") handleListFormat(button.action);
                    else handleInlineFormat(button.action);
                  }}
                  className="flex h-8 w-8 items-center justify-center rounded-[0.5rem] text-[#56616c] transition-colors hover:bg-[#f3f6f8]"
                >
                  <Icon size={16} strokeWidth={1.75} />
                </button>
              );
            })}
          </div>

          <Textarea
            ref={emailBodyRef}
            value={emailBody}
            onChange={(event) => setEmailBody(event.target.value)}
            maxLength={EMAIL_BODY_MAX_LENGTH}
            className="min-h-[6rem] resize-none border-0 px-4 py-3 text-[0.8125rem] leading-[1.6] text-[#4c5662] shadow-none focus-visible:ring-0"
          />
        </div>

        <p
          className={cn(
            "mt-2 text-[0.75rem] leading-5",
            emailBodyError ? "text-[#b42318]" : "text-[#8f9aa6]",
          )}
        >
          {emailBodyError ??
            `Max ${EMAIL_BODY_MAX_LENGTH} characters (${emailBody.length}/${EMAIL_BODY_MAX_LENGTH}).`}
        </p>

        <p className="mt-2 text-[0.75rem] leading-5 text-[#98a3ae]">
          Available variables: {"{{user.firstName}}"}, {"{{user.lastName}}"},{" "}
          {"{{user.email}}"}, {"{{organization.name}}"},{" "}
          {"{{verificationLink}}"}
        </p>

        <div className="mt-3 flex flex-wrap items-center justify-end gap-3">
          <span className="mr-auto text-[0.8125rem] font-medium leading-5 text-[#4c5662]">
            Active Template
          </span>
          <Switch
            checked={emailTemplateActive}
            onCheckedChange={setEmailTemplateActive}
            className="h-[1.375rem] w-[2.375rem]"
            onClassName="bg-[#445461]"
            offClassName="bg-[#d7e0e8]"
          />
          <Button
            variant="primary"
            size="md"
            onClick={handleSaveEmailTemplate}
            disabled={!templateKey || isSavingEmailTemplate || hasEmailTemplateValidationError}
            className="min-w-[8.875rem]"
            loading={isSavingEmailTemplate}
            loadingLabel="Saving..."
          >
            Save Template
          </Button>
        </div>

        {emailTemplateSaveError ? (
          <div className="mt-3 rounded-[0.75rem] border border-[#f3d4d4] bg-[#fff5f5] px-4 py-3 text-[0.8125rem] text-[#b42318]">
            {emailTemplateSaveError}
          </div>
        ) : null}
        {emailTemplateSaveSuccess ? (
          <div className="mt-3 rounded-[0.75rem] border border-[#d1fae5] bg-[#f0fdf4] px-4 py-3 text-[0.8125rem] text-[#065f46]">
            Template saved successfully.
          </div>
        ) : null}
      </div>
)
            : isTenantRoutingTab
              ? renderTenantRoutingPanel()
              : isImpersonationTab
                ? renderImpersonationPanel()
                : renderPlaceholderPanel(
                    activeTab === "email-templates"
                      ? "Email Templates"
                      : "Security Defaults",
                    activeTab === "email-templates"
                      ? "Manage reusable communication templates shared across the platform."
                      : "Control default platform-wide security settings and baseline protections.",
                  )}
        </section>
      )}
    </SuperAdminPageShell>
  );
}

export default SystemSettings;
