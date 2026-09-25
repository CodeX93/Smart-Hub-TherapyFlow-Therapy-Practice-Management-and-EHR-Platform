
import { ContentLoader } from "@/components/shared/ContentLoader";
import { useState } from "react";
import { RotateCcw } from "lucide-react";
import { Button } from "@/components/ui/button";
import { getApiErrorMessage } from "@/utils/apiError";
import {
  useGetSuperAdminApiKeysQuery,
  useRotateSuperAdminApiKeyMutation,
  type RotateApiKeyResponse,
} from "@/store/api/superAdminApi";
import RotateApiKeyModal from "./RotateApiKeyModal";

function formatExpiry(value: string | null): string {
  if (!value) return "No expiry";
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return value;
  return parsed.toLocaleDateString();
}

const ApiKeysPanel = () => {
  const { data: apiKeys = [], isLoading, error, refetch } = useGetSuperAdminApiKeysQuery();
  const [rotateApiKey, { isLoading: isRotating }] = useRotateSuperAdminApiKeyMutation();
  const [rotatingKeyId, setRotatingKeyId] = useState<number | null>(null);
  const [rotatedKey, setRotatedKey] = useState<RotateApiKeyResponse | null>(null);
  const [isRotateModalOpen, setIsRotateModalOpen] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleRotate = async (keyId: number) => {
    setActionError(null);
    setRotatingKeyId(keyId);
    setRotatedKey(null);
    setIsRotateModalOpen(true);

    try {
      const response = await rotateApiKey(keyId).unwrap();
      setRotatedKey(response);
      void refetch();
    } catch (rotateError) {
      setIsRotateModalOpen(false);
      setActionError(getApiErrorMessage(rotateError));
    } finally {
      setRotatingKeyId(null);
    }
  };

  return (
    <>
      <div className="border-b border-[#eef2f5] px-6 py-6">
        <h2 className="text-[1.375rem] font-semibold leading-7 text-[#25323e]">Platform API keys</h2>
        <p className="mt-1 text-[0.8125rem] leading-5 text-[#8b96a2]">
          Rotate keys used by platform services. The new key is shown once after rotation.
        </p>
      </div>

      <div className="px-6 py-6">
        {isLoading ? (
          <ContentLoader size="md" className="py-12 text-[#7b8794] mr-2" />
        ) : error ? (
          <div className="rounded-[0.75rem] border border-[#fecaca] bg-[#fef2f2] px-4 py-6 text-[0.875rem] text-[#b42318]">
            {getApiErrorMessage(error)}
          </div>
        ) : apiKeys.length === 0 ? (
          <div className="rounded-[0.75rem] border border-dashed border-[#d9e1e8] bg-[#fafcfd] px-5 py-8 text-[0.8125rem] text-[#8b96a2]">
            No platform API keys found.
          </div>
        ) : (
          <div className="overflow-hidden rounded-[0.875rem] border border-[#edf2f7]">
            <table className="w-full border-collapse text-left">
              <thead className="bg-[#f8fafc]">
                <tr>
                  <th className="px-4 py-3 text-[0.75rem] font-semibold uppercase tracking-wide text-[#8b96a2]">
                    Name
                  </th>
                  <th className="px-4 py-3 text-[0.75rem] font-semibold uppercase tracking-wide text-[#8b96a2]">
                    Prefix
                  </th>
                  <th className="px-4 py-3 text-[0.75rem] font-semibold uppercase tracking-wide text-[#8b96a2]">
                    Status
                  </th>
                  <th className="px-4 py-3 text-[0.75rem] font-semibold uppercase tracking-wide text-[#8b96a2]">
                    Expires
                  </th>
                  <th className="px-4 py-3 text-right text-[0.75rem] font-semibold uppercase tracking-wide text-[#8b96a2]">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#edf2f7]">
                {apiKeys.map((apiKey) => (
                  <tr key={apiKey.id} className="bg-white">
                    <td className="px-4 py-4 text-[0.875rem] font-medium text-[#25323e]">
                      {apiKey.name}
                    </td>
                    <td className="px-4 py-4 font-mono text-[0.8125rem] text-[#5f6d7a]">
                      {apiKey.keyPrefix}
                    </td>
                    <td className="px-4 py-4 text-[0.8125rem] text-[#5f6d7a]">
                      {apiKey.isActive ? "Active" : "Revoked"}
                    </td>
                    <td className="px-4 py-4 text-[0.8125rem] text-[#5f6d7a]">
                      {formatExpiry(apiKey.expiresAt)}
                    </td>
                    <td className="px-4 py-4 text-right">
                      <Button
                        type="button"
                        variant="outline"
                        size="sm"
                        disabled={!apiKey.isActive || isRotating}
                        onClick={() => void handleRotate(apiKey.id)}
                        className="rounded-full border-[#dce5ee]"
                        loading={rotatingKeyId === apiKey.id}
                        loadingLabel="Rotating API key..."
                      >
                        <RotateCcw className="mr-2 h-4 w-4" />
                        Rotate
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {actionError ? (
          <div className="mt-4 rounded-[0.75rem] border border-[#fecaca] bg-[#fef2f2] px-4 py-3 text-[0.8125rem] text-[#b42318]">
            {actionError}
          </div>
        ) : null}
      </div>

      <RotateApiKeyModal
        isOpen={isRotateModalOpen}
        rotatedKey={rotatedKey}
        onClose={() => {
          setIsRotateModalOpen(false);
          setRotatedKey(null);
        }}
      />
    </>
  );
};

export default ApiKeysPanel;
