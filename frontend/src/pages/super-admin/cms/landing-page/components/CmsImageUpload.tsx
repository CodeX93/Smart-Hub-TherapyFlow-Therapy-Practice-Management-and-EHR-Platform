import { ContentLoader } from "@/components/shared/ContentLoader";
import { TrashIcon } from "@/components/icons/commonIcons";
import { useRef, useState } from "react";
import { ImagePlus } from "lucide-react";
import { Button } from "@/components/ui/button";
import type { CmsMedia } from "@/store/api/superAdminApi";
import { useUploadCmsMediaMutation } from "@/store/api/superAdminApi";
import { getApiErrorMessage } from "@/utils/apiError";

type Props = {
  label: string;
  value?: CmsMedia | null;
  onChange: (media: CmsMedia | null) => void;
  helpText?: string;
};

export default function CmsImageUpload({ label, value, onChange, helpText }: Props) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [upload, { isLoading }] = useUploadCmsMediaMutation();
  const [error, setError] = useState<string | null>(null);

  const handleFile = async (file: File | undefined) => {
    if (!file) return;
    setError(null);
    try {
      const result = await upload({ file }).unwrap();
      onChange({
        url: result.url,
        alternativeText: result.alternativeText,
        width: result.width,
        height: result.height,
      });
    } catch (err) {
      setError(getApiErrorMessage(err));
    }
  };

  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-[#2f3945]">{label}</label>
      {helpText ? <p className="text-xs text-[#697584]">{helpText}</p> : null}
      {value?.url ? (
        <div className="overflow-hidden rounded-xl border border-[#e6e9ee] bg-[#f7f8fa]">
          <img
            src={value.url}
            alt={value.alternativeText || label}
            className="max-h-48 w-full object-contain bg-white"
          />
          <div className="flex gap-2 p-3">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => inputRef.current?.click()}
              disabled={isLoading}
            >
              Replace image
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onChange(null)}
              disabled={isLoading}
            >
              <TrashIcon size={14} className="mr-1" />
              Remove
            </Button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          onClick={() => inputRef.current?.click()}
          disabled={isLoading}
          className="flex w-full flex-col items-center justify-center gap-2 rounded-xl border border-dashed border-[#c5ccd6] bg-white px-4 py-8 text-sm text-[#697584] transition hover:border-[#97a3b1] hover:bg-[#fafbfc]"
        >
          {isLoading ? <ContentLoader variant="inline" size="md" /> : <ImagePlus size={20} />}
          {isLoading ? "Uploading…" : "Click to upload an image"}
        </button>
      )}
      <input
        ref={inputRef}
        type="file"
        accept="image/*"
        className="hidden"
        onChange={(e) => void handleFile(e.target.files?.[0])}
      />
      {error ? <p className="text-xs text-red-600">{error}</p> : null}
    </div>
  );
}
