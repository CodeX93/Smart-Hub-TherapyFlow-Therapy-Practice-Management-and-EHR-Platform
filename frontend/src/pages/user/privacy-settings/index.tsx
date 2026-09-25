
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { ArrowLeft, ChevronDown, ChevronUp, Info } from "lucide-react";
import { Badge } from "../../../components/ui/badge";
import { Switch } from "../../../components/ui/switch";
import Toast from "@/components/shared/Toast";
import { PRIVACY_SETTINGS_STATIC_CONTENT } from "../user.static";
import {
  useGetPortalConsentsQuery,
  useTogglePortalConsentMutation,
} from "@/store/api/portalConsentsApi";
import { getApiErrorMessage } from "@/utils/apiError";
import { mapPortalConsentsToSettings } from "@/utils/portalConsentDisplay";
import type { ConsentSetting } from "../../../types/privacy-settings.type";

const PrivacySettings = () => {
  const navigate = useNavigate();

  const [expandedSections, setExpandedSections] = useState<Set<string>>(
    new Set(),
  );
  const [togglingConsentId, setTogglingConsentId] = useState<string | null>(
    null,
  );
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">(
    "error",
  );

  const {
    data: portalConsents = [],
    isLoading,
    isFetching,
    isError,
    error,
  } = useGetPortalConsentsQuery();

  const [togglePortalConsent] = useTogglePortalConsentMutation();

  const consentSettings = useMemo(
    () => mapPortalConsentsToSettings(portalConsents),
    [portalConsents],
  );

  const isInitialLoading =
    (isLoading || isFetching) && portalConsents.length === 0;

  useEffect(() => {
    if (!isError || !error) return;
    setToastType("error");
    setToastMessage(getApiErrorMessage(error));
  }, [isError, error]);

  const toggleSection = (id: string) => {
    const newExpanded = new Set(expandedSections);
    if (newExpanded.has(id)) {
      newExpanded.delete(id);
    } else {
      newExpanded.add(id);
    }
    setExpandedSections(newExpanded);
  };

  const handleToggle = async (setting: ConsentSetting, checked: boolean) => {
    if (togglingConsentId) return;

    setTogglingConsentId(setting.id);

    try {
      await togglePortalConsent({
        consentType: setting.consentType,
        granted: checked,
      }).unwrap();

      setToastType("success");
      setToastMessage(
        checked
          ? `${setting.title} consent granted.`
          : `${setting.title} consent withdrawn.`,
      );
    } catch (toggleError) {
      setToastType("error");
      setToastMessage(getApiErrorMessage(toggleError));
    } finally {
      setTogglingConsentId(null);
    }
  };

  const dataProtectionCards =
    PRIVACY_SETTINGS_STATIC_CONTENT.dataProtectionCards;

  return (
    <div className="min-h-screen bg-(--bg-primary-light)">
      <div className="sticky top-0 z-20 border-b border-transparent bg-(--bg-primary-light) px-4 pt-8 pb-4">
        <div className="mx-auto max-w-4xl">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="flex cursor-pointer items-center gap-2 text-gray-600 transition-colors hover:text-gray-900"
          >
            <ArrowLeft size={20} />
            <span className="text-sm font-medium">Back</span>
          </button>
        </div>
      </div>

      <div className="px-4 pb-8">
        <div className="mx-auto max-w-4xl space-y-6">
        {/* Privacy & Consent Settings Card */}
        <div className="bg-white rounded-xl shadow-sm p-8">
          <div className="flex items-center gap-3 mb-2">
            <img
              src="/settings/privacy.svg"
              alt="Privacy Settings"
              className="text-(--bg-primary-dark)"
            />
          </div>
          <h1 className="text-[1.25rem] leading-[1.75rem] font-semibold text-[#1B1C20]">
            Privacy & Consent Settings
          </h1>
          <p className="text-[1rem] leading-[1.5rem] font-normal text-[#33363D] mb-4">
            Manage how your personal health information is used. You can change
            these settings at any time.
          </p>
          <div className="border-b border-[#EDEEF1] mb-8"></div>

          {/* Consent Settings */}
          <div className="space-y-6">
            {isInitialLoading ? (
              <div className="flex items-center justify-center gap-2 py-12 text-sm text-[#5B616E]">
                <ContentLoader variant="inline" size="md" />
                <span>Loading consent settings...</span>
              </div>
            ) : consentSettings.length === 0 ? (
              <div className="py-12 text-center text-sm text-[#5B616E]">
                No consent records found.
              </div>
            ) : (
              consentSettings.map((setting) => {
                const isToggling = togglingConsentId === setting.id;

                return (
                  <div
                    key={setting.id}
                    className="border-b border-[#EDEEF1] pb-6 last:border-b-0 last:pb-0"
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="flex-1">
                        <h3 className="text-[1.125rem] leading-[1.625rem] font-semibold text-[#1B1C20] mb-2">
                          {setting.title}
                        </h3>
                        <p className="text-[0.875rem] leading-[1.375rem] font-normal text-[#5B616E] mb-3">
                          {setting.description}
                        </p>
                        <div className="flex items-center gap-2 mb-2">
                          <span className="text-[0.75rem] leading-[1.125rem] font-medium text-[#1B1C20]">
                            Status:
                          </span>
                          <Badge
                            variant={
                              setting.status === "granted"
                                ? "default"
                                : "outline"
                            }
                            className={
                              setting.status === "granted"
                                ? "bg-[#D0FBE3] text-[#007C54] border-[#D0FBE3]"
                                : "bg-[#EDEEF1] text-[#1B1C20] border-[#EDEEF1]"
                            }
                          >
                            {setting.status === "granted"
                              ? "Consent Granted"
                              : "Consent Withdrawn"}
                          </Badge>
                        </div>
                        {setting.details && (
                          <button
                            onClick={() => toggleSection(setting.id)}
                            className="inline-flex h-[2.25rem] items-center justify-between gap-2 rounded-full py-[0.4375rem] text-[0.875rem] leading-[1.375rem] font-semibold text-(--btn-link-text) hover:text-(--btn-link-text-hover) active:text-(--btn-link-text-pressed) transition-colors cursor-pointer"
                          >
                            <Info size={20} />
                            <span className="whitespace-nowrap">Show details</span>
                            {expandedSections.has(setting.id) ? (
                              <ChevronUp size={20} />
                            ) : (
                              <ChevronDown size={20} />
                            )}
                          </button>
                        )}
                        {setting.details && expandedSections.has(setting.id) && (
                          <ul className="mt-4 space-y-2 pl-6 list-disc">
                            {setting.details.map((detail, index) => (
                              <li
                                key={index}
                                className="text-sm text-(--text-neutral-600)"
                              >
                                {detail}
                              </li>
                            ))}
                          </ul>
                        )}
                      </div>
                      <div className="flex shrink-0 items-center gap-2 pt-1">
                        {isToggling ? (
                          <ContentLoader variant="inline" size="sm" />
                        ) : null}
                        <Switch
                          checked={setting.enabled}
                          disabled={isInitialLoading || isToggling}
                          onCheckedChange={(checked) =>
                            void handleToggle(setting, checked)
                          }
                        />
                      </div>
                    </div>
                  </div>
                );
              })
            )}
          </div>
        </div>

        {/* Data Protection Information Section */}
        <div className="rounded-xl">
          <h2 className="text-[1rem] leading-[1.5rem] font-semibold text-[#1B1C20] mb-2">
            Data Protection Information
          </h2>
          <p className="text-[0.875rem] leading-[1.375rem] font-normal text-[#33363D] mb-6">
            Manage how your personal health information is used. You can change
            these settings at any time.
          </p>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {dataProtectionCards.map((card, index) => (
              <div
                key={index}
                className="bg-white rounded-lg p-4 border border-[#EDEEF1]"
              >
                <img
                  src={card.iconSrc}
                  alt={card.title}
                  className="mb-3 h-12 w-12"
                />
                <h3 className="text-[1.125rem] leading-[1.625rem] font-semibold text-[#1B1C20] mb-2">
                  {card.title}
                </h3>
                <p className="text-[0.875rem] leading-[1.375rem] font-normal text-[#5B616E]">
                  {card.title === "Questions?" ? (
                    <>
                      Contact our Privacy Officer at{" "}
                      <a
                        href="mailto:privacy@smarthub.com"
                        className="font-semibold text-[#000000] underline underline-offset-[0.1875rem] cursor-pointer"
                      >
                        privacy@smarthub.com
                      </a>{" "}
                      or speak with your therapist.
                    </>
                  ) : (
                    card.description
                  )}
                </p>
              </div>
            ))}
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

export default PrivacySettings;
