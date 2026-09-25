
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { ArrowLeft, ExternalLink } from "lucide-react";
import SuperAdminPageShell from "@/components/shared/SuperAdminPageShell";
import Toast from "@/components/shared/Toast";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Textarea } from "@/components/ui/textarea";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  type CmsBenefitCard,
  type CmsCircularCard,
  type CmsFaqItem,
  type CmsFeatureTab,
  type CmsGlobalSettingsContent,
  type CmsGridCard,
  type CmsSecurityCard,
  type CmsSocialLink,
  type LandingPageContent,
  useGetCmsGlobalSettingsQuery,
  useGetCmsLandingPageQuery,
  usePublishCmsLandingPageMutation,
  useSaveCmsGlobalSettingsMutation,
  useSaveCmsLandingPageDraftMutation,
} from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";
import CmsImageUpload from "./components/CmsImageUpload";
import CmsSectionNav, { type CmsSectionKey } from "./components/CmsSectionNav";
import LinkTableEditor from "./components/LinkTableEditor";
import RepeatableCardList from "./components/RepeatableCardList";

const LIVE_SITE_URL = "https://therapyflow.pro";

function Field({
  label,
  children,
  hint,
}: {
  label: string;
  children: React.ReactNode;
  hint?: string;
}) {
  return (
    <div className="space-y-1.5">
      <label className="text-sm font-medium text-[#2f3945]">{label}</label>
      {children}
      {hint ? <p className="text-xs text-[#97a3b1]">{hint}</p> : null}
    </div>
  );
}

const CmsLandingPageEditor = () => {
  const { data, isLoading, isError, error, refetch } = useGetCmsLandingPageQuery();
  const {
    data: settingsData,
    isLoading: settingsLoading,
  } = useGetCmsGlobalSettingsQuery();
  const [saveDraft, { isLoading: saving }] = useSaveCmsLandingPageDraftMutation();
  const [publish, { isLoading: publishing }] = usePublishCmsLandingPageMutation();
  const [saveSettings, { isLoading: savingSettings }] = useSaveCmsGlobalSettingsMutation();

  const [section, setSection] = useState<CmsSectionKey>("header");
  const [draft, setDraft] = useState<LandingPageContent>(data?.draftContent ?? {});
  const [settings, setSettings] = useState<CmsGlobalSettingsContent>(settingsData?.content ?? {});
  const [dirty, setDirty] = useState(false);
  const [settingsDirty, setSettingsDirty] = useState(false);
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<"success" | "error" | "info">("success");
  const [confirmPublish, setConfirmPublish] = useState(false);

  const currentRecordInput = data?.draftContent;
  const [previousRecordInput, setPreviousRecordInput] = useState(currentRecordInput);
  if (currentRecordInput !== previousRecordInput) {
    setPreviousRecordInput(currentRecordInput);
    if (data?.draftContent && !dirty) { setDraft(data.draftContent); }
  }

  const currentSettingsInput = settingsData?.content;
  const [previousSettingsInput, setPreviousSettingsInput] = useState(currentSettingsInput);
  if (currentSettingsInput !== previousSettingsInput) {
    setPreviousSettingsInput(currentSettingsInput);
    if (settingsData?.content && !settingsDirty) { setSettings(settingsData.content); }
  }

  useEffect(() => {
    const onBeforeUnload = (e: BeforeUnloadEvent) => {
      if (dirty || settingsDirty) {
        e.preventDefault();
        e.returnValue = "";
      }
    };
    window.addEventListener("beforeunload", onBeforeUnload);
    return () => window.removeEventListener("beforeunload", onBeforeUnload);
  }, [dirty, settingsDirty]);

  const patchDraft = (patch: Partial<LandingPageContent>) => {
    setDraft((prev) => ({ ...prev, ...patch }));
    setDirty(true);
  };

  const patchSettings = (patch: Partial<CmsGlobalSettingsContent>) => {
    setSettings((prev) => ({ ...prev, ...patch }));
    setSettingsDirty(true);
  };

  const showToast = (message: string, type: "success" | "error" | "info" = "success") => {
    setToastType(type);
    setToastMessage(message);
  };

  const handleSaveDraft = async () => {
    try {
      await saveDraft(draft).unwrap();
      setDirty(false);
      showToast("Draft saved.");
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
    }
  };

  const handleSaveSettings = async () => {
    try {
      await saveSettings(settings).unwrap();
      setSettingsDirty(false);
      showToast("Site settings saved.");
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
    }
  };

  const handlePublish = async () => {
    try {
      if (dirty) {
        await saveDraft(draft).unwrap();
        setDirty(false);
      }
      if (settingsDirty) {
        await saveSettings(settings).unwrap();
        setSettingsDirty(false);
      }
      await publish().unwrap();
      setConfirmPublish(false);
      showToast("Published. The live site will rebuild shortly.");
      void refetch();
    } catch (err) {
      showToast(getApiErrorMessage(err), "error");
    }
  };

  const statusLabel = useMemo(() => {
    if (dirty || settingsDirty) return "Unsaved changes";
    if (data?.hasUnpublishedChanges) return "Draft differs from live";
    if (data?.publishedAt) return `Published ${new Date(data.publishedAt).toLocaleString()}`;
    return "Not published yet";
  }, [data, dirty, settingsDirty]);

  if (isLoading || settingsLoading) {
    return (
      <SuperAdminPageShell title="Landing Page" description="Loading content…">
        <div className="flex items-center gap-2 text-sm text-[#697584]">
          <ContentLoader variant="inline" size="sm" />…
        </div>
      </SuperAdminPageShell>
    );
  }

  if (isError) {
    return (
      <SuperAdminPageShell title="Landing Page" description="Could not load CMS content.">
        <div className="rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-700">
          {getApiErrorMessage(error)}
          <Button type="button" className="mt-3" variant="outline" onClick={() => void refetch()}>
            Retry
          </Button>
        </div>
      </SuperAdminPageShell>
    );
  }

  return (
    <SuperAdminPageShell
      title="Landing Page"
      description="Edit website sections with simple forms. Save a draft anytime, then publish when ready."
      toolbar={
        <div className="flex flex-wrap items-center gap-2">
          <Button type="button" variant="ghost" size="sm" asChild>
            <Link to="/super-admin/cms">
              <ArrowLeft size={14} className="mr-1" />
              CMS
            </Link>
          </Button>
          <span className="rounded-full bg-[#eceff3] px-3 py-1 text-xs font-medium text-[#697584]">
            {statusLabel}
          </span>
          <Button type="button" variant="outline" size="sm" asChild>
            <a href={LIVE_SITE_URL} target="_blank" rel="noopener noreferrer">
              View live site
              <ExternalLink size={14} className="ml-1" />
            </a>
          </Button>
          {section === "settings" ? (
            <Button
              type="button"
              size="sm"
              onClick={() => void handleSaveSettings()}
              disabled={savingSettings || !settingsDirty}
            >
              {savingSettings ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
              Save settings
            </Button>
          ) : (
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => void handleSaveDraft()}
              disabled={saving || !dirty}
            >
              {saving ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
              Save draft
            </Button>
          )}
          <Button
            type="button"
            size="sm"
            onClick={() => setConfirmPublish(true)}
            disabled={publishing}
          >
            {publishing ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
            Publish
          </Button>
        </div>
      }
    >
      <CmsSectionNav active={section} onChange={setSection} />

      <div className="rounded-2xl border border-[#e6e9ee] bg-white p-5 shadow-[0_1px_2px_rgba(15,23,42,0.04)]">
        {section === "header" && (
          <div className="space-y-5">
            <CmsImageUpload
              label="Header logo"
              value={draft.Header_Logo}
              onChange={(media) => patchDraft({ Header_Logo: media })}
            />
            <Field label="CTA button text">
              <Input
                value={draft.Header_CTA_Text || ""}
                onChange={(e) => patchDraft({ Header_CTA_Text: e.target.value })}
              />
            </Field>
            <LinkTableEditor
              label="Menu links"
              value={draft.Header_Nav_Links || []}
              onChange={(links) => patchDraft({ Header_Nav_Links: links })}
            />
          </div>
        )}

        {section === "hero" && (
          <div className="space-y-5">
            <Field label="Headline">
              <Input
                value={draft.HeroTitle || ""}
                onChange={(e) => patchDraft({ HeroTitle: e.target.value })}
              />
            </Field>
            <Field label="Subtitle">
              <Textarea
                rows={3}
                value={draft.Hero_Subtitle || ""}
                onChange={(e) => patchDraft({ Hero_Subtitle: e.target.value })}
              />
            </Field>
            <Field label="Button text">
              <Input
                value={draft.Hero_Cta_Text || ""}
                onChange={(e) => patchDraft({ Hero_Cta_Text: e.target.value })}
              />
            </Field>
            <CmsImageUpload
              label="Hero image"
              value={draft.Hero_Image}
              onChange={(media) => patchDraft({ Hero_Image: media })}
            />
          </div>
        )}

        {section === "features" && (
          <div className="space-y-5">
            <Field label="Section heading">
              <Input
                value={draft.Features_Section_Heading || ""}
                onChange={(e) => patchDraft({ Features_Section_Heading: e.target.value })}
              />
            </Field>
            <RepeatableCardList<CmsFeatureTab>
              label="Feature tabs"
              max={7}
              items={draft.Features_Tabs_List || []}
              onChange={(items) => patchDraft({ Features_Tabs_List: items })}
              createItem={() => ({ Title: "", Description: "", Icon: "sparkles" })}
              addLabel="Add tab"
              renderItem={(item, _i, update) => (
                <div className="space-y-3">
                  <Field label="Title">
                    <Input value={item.Title || ""} onChange={(e) => update({ Title: e.target.value })} />
                  </Field>
                  <Field label="Description">
                    <Textarea
                      rows={3}
                      value={item.Description || ""}
                      onChange={(e) => update({ Description: e.target.value })}
                    />
                  </Field>
                  <Field label="Icon" hint="Icon name (e.g. sparkles) or pasted SVG from migration">
                    <Input
                      value={typeof item.Icon === "string" ? item.Icon : ""}
                      onChange={(e) => update({ Icon: e.target.value })}
                      placeholder="sparkles"
                    />
                  </Field>
                  <CmsImageUpload
                    label="Preview image"
                    value={item.Preview_Image}
                    onChange={(media) => update({ Preview_Image: media })}
                  />
                </div>
              )}
            />
          </div>
        )}

        {section === "customization" && (
          <div className="space-y-5">
            <Field label="Title">
              <Input
                value={draft.Customization_Title || ""}
                onChange={(e) => patchDraft({ Customization_Title: e.target.value })}
              />
            </Field>
            <Field label="Description">
              <Textarea
                rows={3}
                value={draft.Customization_Description || ""}
                onChange={(e) => patchDraft({ Customization_Description: e.target.value })}
              />
            </Field>
            <RepeatableCardList<CmsGridCard>
              label="Cards"
              max={6}
              items={draft.Customization_Cards || []}
              onChange={(items) => patchDraft({ Customization_Cards: items })}
              createItem={() => ({
                Title: "",
                Description: "",
                Layout_Type: "text-bottom",
              })}
              renderItem={(item, _i, update) => (
                <div className="space-y-3">
                  <Field label="Title">
                    <Input value={item.Title || ""} onChange={(e) => update({ Title: e.target.value })} />
                  </Field>
                  <Field label="Description">
                    <Textarea
                      rows={3}
                      value={item.Description || ""}
                      onChange={(e) => update({ Description: e.target.value })}
                    />
                  </Field>
                  <Field label="Layout">
                    <Select
                      value={item.Layout_Type || "text-bottom"}
                      onValueChange={(v) => update({ Layout_Type: v })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="text-bottom">Text bottom</SelectItem>
                        <SelectItem value="text-left">Text left</SelectItem>
                        <SelectItem value="text-right">Text right</SelectItem>
                        <SelectItem value="full-width">Full width</SelectItem>
                      </SelectContent>
                    </Select>
                  </Field>
                  <CmsImageUpload
                    label="Image"
                    value={item.Graphic_Image}
                    onChange={(media) => update({ Graphic_Image: media })}
                  />
                </div>
              )}
            />
          </div>
        )}

        {section === "communication" && (
          <div className="space-y-5">
            <Field label="Title">
              <Input
                value={draft.Communication_Title || ""}
                onChange={(e) => patchDraft({ Communication_Title: e.target.value })}
              />
            </Field>
            <Field label="Description">
              <Textarea
                rows={3}
                value={draft.Communication_Description || ""}
                onChange={(e) => patchDraft({ Communication_Description: e.target.value })}
              />
            </Field>
            <Field label="CTA text">
              <Input
                value={draft.Communication_Cta_Text || ""}
                onChange={(e) => patchDraft({ Communication_Cta_Text: e.target.value })}
              />
            </Field>
            <RepeatableCardList<CmsCircularCard>
              label="Step cards"
              max={4}
              items={draft.Communication_Cards || []}
              onChange={(items) => patchDraft({ Communication_Cards: items })}
              createItem={() => ({ Title: "", Description: "", Step_Number: String((draft.Communication_Cards?.length || 0) + 1) })}
              renderItem={(item, _i, update) => (
                <div className="space-y-3">
                  <Field label="Step number">
                    <Input
                      value={item.Step_Number || ""}
                      onChange={(e) => update({ Step_Number: e.target.value })}
                    />
                  </Field>
                  <Field label="Title">
                    <Input value={item.Title || ""} onChange={(e) => update({ Title: e.target.value })} />
                  </Field>
                  <Field label="Description">
                    <Textarea
                      rows={3}
                      value={item.Description || ""}
                      onChange={(e) => update({ Description: e.target.value })}
                    />
                  </Field>
                </div>
              )}
            />
          </div>
        )}

        {section === "security" && (
          <div className="space-y-5">
            <Field label="Heading">
              <Input
                value={draft.Security_Heading || ""}
                onChange={(e) => patchDraft({ Security_Heading: e.target.value })}
              />
            </Field>
            <Field label="Description">
              <Textarea
                rows={3}
                value={draft.Security_Description || ""}
                onChange={(e) => patchDraft({ Security_Description: e.target.value })}
              />
            </Field>
            <Field label="CTA text">
              <Input
                value={draft.Security_Cta_Text || ""}
                onChange={(e) => patchDraft({ Security_Cta_Text: e.target.value })}
              />
            </Field>
            <RepeatableCardList<CmsSecurityCard>
              label="Security cards"
              max={4}
              items={draft.Security_Cards || []}
              onChange={(items) => patchDraft({ Security_Cards: items })}
              createItem={() => ({ Title: "", Description: "" })}
              renderItem={(item, _i, update) => (
                <div className="space-y-3">
                  <Field label="Title">
                    <Input value={item.Title || ""} onChange={(e) => update({ Title: e.target.value })} />
                  </Field>
                  <Field label="Description">
                    <Textarea
                      rows={3}
                      value={item.Description || ""}
                      onChange={(e) => update({ Description: e.target.value })}
                    />
                  </Field>
                  <CmsImageUpload
                    label="Image"
                    value={item.Image}
                    onChange={(media) => update({ Image: media })}
                  />
                </div>
              )}
            />
          </div>
        )}

        {section === "benefits" && (
          <div className="space-y-5">
            <Field label="Heading">
              <Input
                value={draft.Benefits_Heading || ""}
                onChange={(e) => patchDraft({ Benefits_Heading: e.target.value })}
              />
            </Field>
            <RepeatableCardList<CmsBenefitCard>
              label="Benefit cards"
              max={10}
              items={draft.Benefits_Cards || []}
              onChange={(items) => patchDraft({ Benefits_Cards: items })}
              createItem={() => ({ Title: "", Description: "", icon: "check-circle" })}
              renderItem={(item, _i, update) => (
                <div className="space-y-3">
                  <Field label="Title">
                    <Input value={item.Title || ""} onChange={(e) => update({ Title: e.target.value })} />
                  </Field>
                  <Field label="Description">
                    <Textarea
                      rows={3}
                      value={item.Description || ""}
                      onChange={(e) => update({ Description: e.target.value })}
                    />
                  </Field>
                  <Field label="Icon" hint="Icon name (e.g. check-circle) or pasted value from migration">
                    <Input
                      value={typeof item.icon === "string" ? item.icon : ""}
                      onChange={(e) => update({ icon: e.target.value })}
                      placeholder="check-circle"
                    />
                  </Field>
                </div>
              )}
            />
          </div>
        )}

        {section === "faq" && (
          <div className="space-y-5">
            <Field label="FAQ heading">
              <Input
                value={draft.FAQ_Heading || ""}
                onChange={(e) => patchDraft({ FAQ_Heading: e.target.value })}
              />
            </Field>
            <RepeatableCardList<CmsFaqItem>
              label="Questions"
              items={draft.Faq_List || []}
              onChange={(items) => patchDraft({ Faq_List: items })}
              createItem={() => ({ Question: "", Answer: "" })}
              addLabel="Add question"
              renderItem={(item, _i, update) => (
                <div className="space-y-3">
                  <Field label="Question">
                    <Input
                      value={item.Question || ""}
                      onChange={(e) => update({ Question: e.target.value })}
                    />
                  </Field>
                  <Field label="Answer">
                    <Textarea
                      rows={4}
                      value={item.Answer || ""}
                      onChange={(e) => update({ Answer: e.target.value })}
                    />
                  </Field>
                </div>
              )}
            />
          </div>
        )}

        {section === "footer" && (
          <div className="space-y-5">
            <CmsImageUpload
              label="Footer logo"
              value={draft.Footer_Logo}
              onChange={(media) => patchDraft({ Footer_Logo: media })}
            />
            <Field label="Copyright text">
              <Input
                value={draft.Copyright_Text || ""}
                onChange={(e) => patchDraft({ Copyright_Text: e.target.value })}
              />
            </Field>
            <Field label="Bottom CTA heading">
              <Input
                value={draft.Bottom_Cta_Heading || ""}
                onChange={(e) => patchDraft({ Bottom_Cta_Heading: e.target.value })}
              />
            </Field>
            <Field label="Bottom CTA description">
              <Textarea
                rows={2}
                value={draft.Bottom_Cta_Description || ""}
                onChange={(e) => patchDraft({ Bottom_Cta_Description: e.target.value })}
              />
            </Field>
            <Field label="Bottom CTA button text">
              <Input
                value={draft.Bottom_Cta_Button_Text || ""}
                onChange={(e) => patchDraft({ Bottom_Cta_Button_Text: e.target.value })}
              />
            </Field>
            <LinkTableEditor
              label="Footer links"
              value={draft.Footer_Nav_Links || []}
              onChange={(links) => patchDraft({ Footer_Nav_Links: links })}
            />
            <LinkTableEditor
              label="Legal links"
              value={draft.Footer_Legal_Links || []}
              onChange={(links) => patchDraft({ Footer_Legal_Links: links })}
            />
            <RepeatableCardList<CmsSocialLink>
              label="Social links"
              items={draft.Footer_Socials || []}
              onChange={(items) => patchDraft({ Footer_Socials: items })}
              createItem={() => ({ Platform: "facebook", URL: "" })}
              addLabel="Add social link"
              renderItem={(item, _i, update) => (
                <div className="grid gap-3 md:grid-cols-2">
                  <Field label="Platform">
                    <Select
                      value={item.Platform || "facebook"}
                      onValueChange={(v) => update({ Platform: v })}
                    >
                      <SelectTrigger>
                        <SelectValue />
                      </SelectTrigger>
                      <SelectContent>
                        <SelectItem value="facebook">Facebook</SelectItem>
                        <SelectItem value="instagram">Instagram</SelectItem>
                        <SelectItem value="youtube">YouTube</SelectItem>
                      </SelectContent>
                    </Select>
                  </Field>
                  <Field label="URL">
                    <Input value={item.URL || ""} onChange={(e) => update({ URL: e.target.value })} />
                  </Field>
                </div>
              )}
            />
          </div>
        )}

        {section === "seo" && (
          <div className="space-y-5">
            <Field
              label="Page title"
              hint={`${(draft.SEO?.Meta_Title || "").length}/60 characters`}
            >
              <Input
                maxLength={60}
                value={draft.SEO?.Meta_Title || ""}
                onChange={(e) =>
                  patchDraft({ SEO: { ...(draft.SEO || {}), Meta_Title: e.target.value } })
                }
              />
            </Field>
            <Field
              label="Meta description"
              hint={`${(draft.SEO?.Meta_Description || "").length}/160 characters`}
            >
              <Textarea
                maxLength={160}
                rows={3}
                value={draft.SEO?.Meta_Description || ""}
                onChange={(e) =>
                  patchDraft({ SEO: { ...(draft.SEO || {}), Meta_Description: e.target.value } })
                }
              />
            </Field>
            <Field label="Canonical URL">
              <Input
                value={draft.SEO?.Canonical_URL || ""}
                onChange={(e) =>
                  patchDraft({ SEO: { ...(draft.SEO || {}), Canonical_URL: e.target.value } })
                }
              />
            </Field>
            <Field label="Social title (Open Graph)">
              <Input
                maxLength={60}
                value={draft.SEO?.OG_Title || ""}
                onChange={(e) =>
                  patchDraft({ SEO: { ...(draft.SEO || {}), OG_Title: e.target.value } })
                }
              />
            </Field>
            <Field label="Social description">
              <Textarea
                maxLength={160}
                rows={2}
                value={draft.SEO?.OG_Description || ""}
                onChange={(e) =>
                  patchDraft({ SEO: { ...(draft.SEO || {}), OG_Description: e.target.value } })
                }
              />
            </Field>
            <CmsImageUpload
              label="Social preview image"
              value={draft.SEO?.OG_Image}
              onChange={(media) =>
                patchDraft({ SEO: { ...(draft.SEO || {}), OG_Image: media } })
              }
            />
            <Field label="Robots">
              <Input
                value={draft.SEO?.Robots || "index, follow"}
                onChange={(e) =>
                  patchDraft({ SEO: { ...(draft.SEO || {}), Robots: e.target.value } })
                }
              />
            </Field>
          </div>
        )}

        {section === "settings" && (
          <div className="space-y-5">
            <p className="text-sm text-[#697584]">
              Site-wide defaults used by the landing page and Learning Hub. Saving here updates live
              settings immediately (and triggers a landing rebuild).
            </p>
            <Field label="Site name">
              <Input
                value={settings.Site_Name || ""}
                onChange={(e) => patchSettings({ Site_Name: e.target.value })}
              />
            </Field>
            <Field label="Landing domain">
              <Input
                value={settings.Landing_Domain || ""}
                onChange={(e) => patchSettings({ Landing_Domain: e.target.value })}
              />
            </Field>
            <Field label="Learning Hub domain">
              <Input
                value={settings.LearningHub_Domain || ""}
                onChange={(e) => patchSettings({ LearningHub_Domain: e.target.value })}
              />
            </Field>
            <Field label="Organization name">
              <Input
                value={settings.Organization_Name || ""}
                onChange={(e) => patchSettings({ Organization_Name: e.target.value })}
              />
            </Field>
            <Field label="Organization URL">
              <Input
                value={settings.Organization_URL || ""}
                onChange={(e) => patchSettings({ Organization_URL: e.target.value })}
              />
            </Field>
            <Field label="Twitter handle">
              <Input
                value={settings.Twitter_Handle || ""}
                onChange={(e) => patchSettings({ Twitter_Handle: e.target.value })}
              />
            </Field>
            <Field label="Google site verification">
              <Input
                value={settings.Google_Site_Verification || ""}
                onChange={(e) => patchSettings({ Google_Site_Verification: e.target.value })}
              />
            </Field>
            <CmsImageUpload
              label="Default social image"
              value={settings.Default_OG_Image}
              onChange={(media) => patchSettings({ Default_OG_Image: media })}
            />
            <CmsImageUpload
              label="Organization logo"
              value={settings.Organization_Logo}
              onChange={(media) => patchSettings({ Organization_Logo: media })}
            />
            <Button
              type="button"
              onClick={() => void handleSaveSettings()}
              disabled={savingSettings || !settingsDirty}
            >
              {savingSettings ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
              Save site settings
            </Button>
          </div>
        )}
      </div>

      {confirmPublish ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
          <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
            <h3 className="text-lg font-semibold text-[#2f3945]">Publish landing page?</h3>
            <p className="mt-2 text-sm leading-6 text-[#697584]">
              This copies your draft to the live API and triggers a rebuild of therapyflow.pro. Unsaved
              draft and settings changes will be saved first.
            </p>
            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setConfirmPublish(false)}>
                Cancel
              </Button>
              <Button type="button" onClick={() => void handlePublish()} disabled={publishing}>
                {publishing ? <ContentLoader variant="inline" size="sm" className="mr-1" /> : null}
                Publish now
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      {toastMessage ? (
        <Toast
          message={toastMessage}
          type={toastType}
          onClose={() => setToastMessage(null)}
        />
      ) : null}
    </SuperAdminPageShell>
  );
};

export default CmsLandingPageEditor;
