
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useMemo, useState } from "react";
import { CheckCircle2, XCircle } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Switch } from "@/components/ui/switch";
import { cn } from "@/lib/utils";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  isMaskedIntegrationSecret,
  useGetSuperAdminIntegrationQuery,
  useTestSuperAdminIntegrationMutation,
  useUpsertSuperAdminIntegrationMutation,
  type IntegrationKey,
  type IntegrationTestResult,
  type IntegrationUpsertRequest,
} from "@/store/api/superAdminApi";
import {
  INTEGRATION_FIELD_HINTS,
  INTEGRATION_FIELD_LABELS,
  SUPER_ADMIN_INTEGRATIONS,
  WEBHOOK_URL_MAX_LENGTH,
  type IntegrationFieldKey,
} from "../integration.config";

interface IntegrationPanelProps {
  integrationKey: IntegrationKey;
}

type SecretFieldKey = Extract<
  IntegrationFieldKey,
  "secret" | "connectClientSecret" | "platformWebhookSecret" | "connectWebhookSecret"
>;

function isSecretField(field: IntegrationFieldKey): field is SecretFieldKey {
  return (
    field === "secret" ||
    field === "connectClientSecret" ||
    field === "platformWebhookSecret" ||
    field === "connectWebhookSecret"
  );
}

function formatTimestamp(value: string | null | undefined): string {
  if (!value) return "Never";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleString();
}

const IntegrationPanelContent = ({ integrationKey }: IntegrationPanelProps) => {
  const meta = useMemo(
    () => SUPER_ADMIN_INTEGRATIONS.find((item) => item.key === integrationKey),
    [integrationKey],
  );

  const {
    currentData: config,
    isLoading,
    isFetching,
    error,
    refetch,
  } = useGetSuperAdminIntegrationQuery(integrationKey);

  const [upsertIntegration, { isLoading: isSaving }] =
    useUpsertSuperAdminIntegrationMutation();
  const [testIntegration, { isLoading: isTesting }] =
    useTestSuperAdminIntegrationMutation();

  const [enabled, setEnabled] = useState(false);
  const [clientId, setClientId] = useState("");
  const [publishableKey, setPublishableKey] = useState("");
  const [secret, setSecret] = useState("");
  const [connectClientSecret, setConnectClientSecret] = useState("");
  const [platformWebhookSecret, setPlatformWebhookSecret] = useState("");
  const [connectWebhookSecret, setConnectWebhookSecret] = useState("");
  const [webhookUrl, setWebhookUrl] = useState("");
  const [secretIsSet, setSecretIsSet] = useState(false);
  const [connectClientSecretIsSet, setConnectClientSecretIsSet] = useState(false);
  const [platformWebhookSecretIsSet, setPlatformWebhookSecretIsSet] = useState(false);
  const [connectWebhookSecretIsSet, setConnectWebhookSecretIsSet] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);
  const [saveMessage, setSaveMessage] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<IntegrationTestResult | null>(null);

  const draftKey = integrationKey;
  const [seededDraftKey, setSeededDraftKey] = useState<unknown>(null);
  if (config && seededDraftKey !== draftKey) {
    setSeededDraftKey(draftKey);
    setEnabled(config.enabled);
    setClientId(config.clientId ?? "");
    setPublishableKey(config.publishableKey ?? "");
    setWebhookUrl(config.webhookUrl ?? "");
    setSecret("");
    setConnectClientSecret("");
    setPlatformWebhookSecret("");
    setConnectWebhookSecret("");
    setSecretIsSet(isMaskedIntegrationSecret(config.secret));
    setConnectClientSecretIsSet(isMaskedIntegrationSecret(config.connectClientSecret));
    setPlatformWebhookSecretIsSet(isMaskedIntegrationSecret(config.platformWebhookSecret));
    setConnectWebhookSecretIsSet(isMaskedIntegrationSecret(config.connectWebhookSecret));
    setFormError(null);
    setSaveMessage(null);
    setTestResult(null);
  }

  const fieldValues: Record<IntegrationFieldKey, string> = {
    clientId,
    publishableKey,
    secret,
    connectClientSecret,
    platformWebhookSecret,
    connectWebhookSecret,
    webhookUrl,
  };

  const secretSetFlags: Record<SecretFieldKey, boolean> = {
    secret: secretIsSet,
    connectClientSecret: connectClientSecretIsSet,
    platformWebhookSecret: platformWebhookSecretIsSet,
    connectWebhookSecret: connectWebhookSecretIsSet,
  };

  const setFieldValue = (field: IntegrationFieldKey, value: string) => {
    switch (field) {
      case "clientId":
        setClientId(value);
        break;
      case "publishableKey":
        setPublishableKey(value);
        break;
      case "secret":
        setSecret(value);
        break;
      case "connectClientSecret":
        setConnectClientSecret(value);
        break;
      case "platformWebhookSecret":
        setPlatformWebhookSecret(value);
        break;
      case "connectWebhookSecret":
        setConnectWebhookSecret(value);
        break;
      case "webhookUrl":
        setWebhookUrl(value);
        break;
      default:
        break;
    }
  };

  const buildPayload = (): IntegrationUpsertRequest => {
    const payload: IntegrationUpsertRequest = { enabled };
    const fields = meta?.fields ?? [];

    fields.forEach((field) => {
      const value = fieldValues[field].trim();
      if (!value) return;
      if (field === "clientId") payload.clientId = value;
      if (field === "publishableKey") payload.publishableKey = value;
      if (field === "secret") payload.secret = value;
      if (field === "connectClientSecret") payload.connectClientSecret = value;
      if (field === "platformWebhookSecret") payload.platformWebhookSecret = value;
      if (field === "connectWebhookSecret") payload.connectWebhookSecret = value;
      if (field === "webhookUrl") payload.webhookUrl = value;
    });

    return payload;
  };

  const handleSave = async () => {
    setFormError(null);
    setSaveMessage(null);
    setTestResult(null);

    try {
      await upsertIntegration({
        integrationKey,
        body: buildPayload(),
      }).unwrap();
      setSecretIsSet(previous => previous || Boolean(secret.trim()));
      setConnectClientSecretIsSet(previous => previous || Boolean(connectClientSecret.trim()));
      setPlatformWebhookSecretIsSet(previous => previous || Boolean(platformWebhookSecret.trim()));
      setConnectWebhookSecretIsSet(previous => previous || Boolean(connectWebhookSecret.trim()));
      setSecret("");
      setConnectClientSecret("");
      setPlatformWebhookSecret("");
      setConnectWebhookSecret("");
      setSaveMessage("Integration settings saved.");
      void refetch();
    } catch (saveError) {
      setFormError(getApiErrorMessage(saveError));
    }
  };

  const handleTest = async () => {
    setFormError(null);
    setTestResult(null);

    try {
      const result = await testIntegration(integrationKey).unwrap();
      setTestResult(result);
    } catch (testError) {
      setFormError(getApiErrorMessage(testError));
    }
  };

  if (!meta) return null;

  if (isLoading) {
    return (
      <ContentLoader size="md" className="min-h-[20rem] text-[#7b8794] mr-2" />
    );
  }

  if (error) {
    return (
      <div className="rounded-[0.75rem] border border-[#fecaca] bg-[#fef2f2] px-4 py-6 text-[0.875rem] text-[#b42318]">
        {getApiErrorMessage(error)}
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="border-b border-[#eef2f5] px-6 py-6">
        <h2 className="text-[1.375rem] font-semibold leading-7 text-[#25323e]">{meta.label}</h2>
        <p className="mt-1 text-[0.8125rem] leading-5 text-[#8b96a2]">{meta.description}</p>
        <p className="mt-2 text-[0.75rem] text-[#9ca7b2]">
          Last configured: {formatTimestamp(config?.lastConfiguredAt)}
        </p>
      </div>

      <div className="space-y-5 px-6 pb-6">
        <div className="flex items-center justify-between rounded-[0.875rem] border border-[#edf2f7] bg-[#fbfcfd] px-4 py-4">
          <div>
            <p className="text-[0.875rem] font-medium text-[#25323e]">Enabled</p>
            <p className="text-[0.75rem] text-[#8b96a2]">
              Required credentials must be configured before enabling.
            </p>
          </div>
          <Switch checked={enabled} onCheckedChange={setEnabled} />
        </div>

        {meta.fields.map((field) => {
          const isSecret = isSecretField(field);
          const alreadySet = isSecret ? secretSetFlags[field] : false;

          return (
            <div key={field} className="space-y-2">
              <label className="text-[0.8125rem] font-medium text-[#435361]">
                {INTEGRATION_FIELD_LABELS[field]}
              </label>
              <Input
                type={isSecret ? "password" : "text"}
                value={fieldValues[field]}
                onChange={(event) => setFieldValue(field, event.target.value)}
                placeholder={
                  isSecret && alreadySet
                    ? "Already configured — enter a new value to replace"
                    : INTEGRATION_FIELD_HINTS[field]
                }
                maxLength={field === "webhookUrl" ? WEBHOOK_URL_MAX_LENGTH : undefined}
                className="h-11 rounded-[0.625rem] border-[#dce5ee] bg-white"
              />
              {isSecret && alreadySet ? (
                <p className="text-[0.75rem] text-[#6b7b88]">A value is stored on the server.</p>
              ) : null}
              {!isSecret && INTEGRATION_FIELD_HINTS[field] ? (
                <p className="text-[0.75rem] text-[#9ca7b2]">{INTEGRATION_FIELD_HINTS[field]}</p>
              ) : null}
            </div>
          );
        })}

        {formError ? (
          <div className="rounded-[0.75rem] border border-[#fecaca] bg-[#fef2f2] px-4 py-3 text-[0.8125rem] text-[#b42318]">
            {formError}
          </div>
        ) : null}

        {saveMessage ? (
          <div className="rounded-[0.75rem] border border-[#bbf7d0] bg-[#f0fdf4] px-4 py-3 text-[0.8125rem] text-[#166534]">
            {saveMessage}
          </div>
        ) : null}

        {testResult ? (
          <div
            className={cn(
              "flex items-start gap-2 rounded-[0.75rem] border px-4 py-3 text-[0.8125rem]",
              testResult.success
                ? "border-[#bbf7d0] bg-[#f0fdf4] text-[#166534]"
                : "border-[#fecaca] bg-[#fef2f2] text-[#b42318]",
            )}
          >
            {testResult.success ? (
              <CheckCircle2 className="mt-0.5 h-4 w-4 shrink-0" />
            ) : (
              <XCircle className="mt-0.5 h-4 w-4 shrink-0" />
            )}
            <div>
              {testResult.success
                ? `Connection successful (${testResult.latencyMs} ms)`
                : testResult.error || "Connection test failed"}
            </div>
          </div>
        ) : null}

        <div className="flex flex-wrap justify-end gap-3 pt-2">
          <Button
            type="button"
            variant="outline"
            onClick={() => void handleTest()}
            disabled={isTesting || isSaving || isFetching}
            className="h-10 rounded-full border-[#dce5ee] px-5"
            loading={isTesting}
            loadingLabel="Testing..."
          >
            Test connection
          </Button>
          <Button
            type="button"
            onClick={() => void handleSave()}
            disabled={isSaving || isFetching}
            className="h-10 rounded-full bg-[#435564] px-6 text-white hover:bg-[#394957]"
            loading={isSaving}
            loadingLabel="Saving..."
          >
            Save changes
          </Button>
        </div>
      </div>
    </div>
  );
};

const IntegrationPanel = (props: IntegrationPanelProps) => <IntegrationPanelContent key={props.integrationKey} {...props} />;

export default IntegrationPanel;
