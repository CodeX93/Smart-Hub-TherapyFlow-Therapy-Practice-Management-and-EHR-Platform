
import { ContentLoader } from "@/components/shared/ContentLoader";
import { Copy, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { RotateApiKeyResponse } from "@/store/api/superAdminApi";

interface RotateApiKeyModalProps {
  isOpen: boolean;
  rotatedKey: RotateApiKeyResponse | null;
  onClose: () => void;
}

const RotateApiKeyModal = ({ isOpen, rotatedKey, onClose }: RotateApiKeyModalProps) => {
  if (!isOpen) return null;

  const handleCopy = async () => {
    if (!rotatedKey?.apiKey) return;
    try {
      await navigator.clipboard.writeText(rotatedKey.apiKey);
    } catch {
      // Clipboard may be unavailable in some browsers.
    }
  };

  return (
    <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/40 p-4 backdrop-blur-[0.125rem]">
      <div className="relative w-full max-w-lg overflow-hidden rounded-[1rem] bg-white p-6 shadow-xl">
        <button
          type="button"
          onClick={onClose}
          className="absolute right-5 top-5 rounded-full p-1.5 text-[#8b96a2] transition-colors hover:bg-[#f3f6f8]"
        >
          <X size={18} />
        </button>

        <div className="pr-8">
          <h2 className="text-[1.25rem] font-semibold text-[#25323e]">New API key created</h2>
          <p className="mt-1 text-[0.8125rem] leading-5 text-[#8b96a2]">
            Copy this key now. It is shown only once — the previous key stops working
            immediately.
          </p>
        </div>

        {rotatedKey ? (
          <div className="mt-5 space-y-3">
            <div className="rounded-[0.75rem] border border-[#edf2f7] bg-[#fbfcfd] px-4 py-3">
              <p className="text-[0.75rem] font-medium uppercase tracking-wide text-[#8b96a2]">
                {rotatedKey.name}
              </p>
              <p className="mt-2 break-all font-mono text-[0.8125rem] text-[#25323e]">
                {rotatedKey.apiKey}
              </p>
            </div>
            <p className="text-[0.75rem] text-[#8b96a2]">
              Prefix: {rotatedKey.keyPrefix}
              {rotatedKey.expiresAt
                ? ` · Expires ${new Date(rotatedKey.expiresAt).toLocaleDateString()}`
                : ""}
            </p>
          </div>
        ) : (
          <ContentLoader size="md" className="py-10 text-[#7b8794]" />
        )}

        <div className="mt-6 flex justify-end gap-3">
          <Button
            type="button"
            variant="outline"
            onClick={() => void handleCopy()}
            disabled={!rotatedKey?.apiKey}
            className="rounded-full"
          >
            <Copy className="mr-2 h-4 w-4" />
            Copy key
          </Button>
          <Button
            type="button"
            onClick={onClose}
            className="rounded-full bg-[#435564] text-white hover:bg-[#394957]"
          >
            Done
          </Button>
        </div>
      </div>
    </div>
  );
};

export default RotateApiKeyModal;
