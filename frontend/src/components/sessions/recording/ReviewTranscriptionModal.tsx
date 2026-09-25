import { useMemo, useState } from "react";
import { X, Copy } from "lucide-react";
import { cn } from "../../../lib/utils";
import { Button } from "../../ui/button";
import type { SessionNoteTranscriptionResponse } from "@/store/api/admin/sessionNotes.api";

interface ReviewTranscriptionModalProps {
  isOpen: boolean;
  onClose: () => void;
  onApply: (data: Partial<TranscribedData>) => void;
  transcriptionData: SessionNoteTranscriptionResponse | null;
}

export interface TranscribedData {
  sessionFocus: string;
  symptoms: string;
  shortTermGoals: string;
  intervention: string;
  remarks: string;
  recommendations: string;
  progress: string;
}

const EMPTY_DATA: TranscribedData = {
  sessionFocus: "",
  symptoms: "",
  shortTermGoals: "",
  intervention: "",
  progress: "",
  remarks: "",
  recommendations: "",
};

const ReviewTranscriptionModalContent = ({
  isOpen,
  onClose,
  onApply,
  transcriptionData,
}: ReviewTranscriptionModalProps) => {
  const [activeTab, setActiveTab] = useState<"structured" | "transcript">("structured");
  const [selectedFields, setSelectedFields] = useState<Record<keyof TranscribedData, boolean>>({
    sessionFocus: true,
    symptoms: true,
    shortTermGoals: true,
    intervention: true,
    progress: true,
    remarks: true,
    recommendations: true,
  });
  const [fieldData, setFieldData] = useState<TranscribedData>(() => ({ ...EMPTY_DATA, ...transcriptionData?.mappedFields }));



  const selectedCount = useMemo(
    () => Object.values(selectedFields).filter(Boolean).length,
    [selectedFields],
  );
  const totalCount = useMemo(() => Object.keys(selectedFields).length, [selectedFields]);

  if (!isOpen) return null;

  const handleApply = () => {
    const dataToApply: Partial<TranscribedData> = {};
    (Object.keys(fieldData) as Array<keyof TranscribedData>).forEach((key) => {
      if (selectedFields[key]) dataToApply[key] = fieldData[key];
    });
    onApply(dataToApply);
  };

  const toggleField = (key: keyof TranscribedData) => {
    setSelectedFields((prev) => ({ ...prev, [key]: !prev[key] }));
  };

  const handleCopyTranscript = () => {
    void navigator.clipboard.writeText(transcriptionData?.rawTranscription ?? "");
  };

  const fieldLabels: Record<keyof TranscribedData, string> = {
    sessionFocus: "Session Focus",
    symptoms: "Symptoms",
    shortTermGoals: "Short-term Goals",
    intervention: "Interventions",
    progress: "Progress",
    remarks: "Clinical Remarks",
    recommendations: "Recommendations",
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-[10000] p-4 font-sans">
      <div className="bg-white rounded-2xl w-full max-w-3xl h-[85vh] flex flex-col shadow-xl animate-in fade-in zoom-in duration-200">
        <div className="p-6 border-b border-gray-100 flex-shrink-0">
          <div className="flex items-center justify-between mb-2">
            <h2 className="text-lg font-semibold text-(--text-primary-dark)">Review Voice Transcription</h2>
            <button onClick={onClose} className="p-1 hover:bg-gray-100 rounded-full text-gray-500">
              <X size={20} />
            </button>
          </div>
          <p className="text-sm text-gray-500">
            Review the AI-generated field mappings and choose which ones to apply to your session note
          </p>

          <div className="flex gap-2 p-1 bg-[#F3F4F6] rounded-full mt-6 w-full">
            <button
              onClick={() => setActiveTab("structured")}
              className={cn(
                "flex-1 px-4 py-2 text-sm font-medium rounded-full transition-all cursor-pointer text-center",
                activeTab === "structured"
                  ? "bg-white text-[#111827] shadow-sm"
                  : "text-[#9CA3AF] hover:text-[#6B7280]",
              )}
            >
              Structured Fields ({selectedCount}/{totalCount} selected)
            </button>
            <button
              onClick={() => setActiveTab("transcript")}
              className={cn(
                "flex-1 px-4 py-2 text-sm font-medium rounded-full transition-all cursor-pointer text-center",
                activeTab === "transcript"
                  ? "bg-white text-[#111827] shadow-sm"
                  : "text-[#9CA3AF] hover:text-[#6B7280]",
              )}
            >
              Full Transcript
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto p-6 bg-white">
          {activeTab === "structured" ? (
            <div className="space-y-6">
              {(Object.keys(fieldData) as Array<keyof TranscribedData>).map((key) => (
                <div key={key} className={cn("transition-opacity duration-200", !selectedFields[key] && "opacity-60")}>
                  <div className="flex items-center gap-2 mb-2">
                    <div
                      onClick={() => toggleField(key)}
                      className={cn(
                        "w-4 h-4 rounded border flex items-center justify-center cursor-pointer transition-colors",
                        selectedFields[key]
                          ? "bg-(--bg-primary-dark) border-(--bg-primary-dark)"
                          : "bg-white border-gray-300",
                      )}
                    >
                      {selectedFields[key] && <Check size={12} className="text-white" />}
                    </div>
                    <label
                      onClick={() => toggleField(key)}
                      className="text-sm font-medium text-gray-700 cursor-pointer select-none"
                    >
                      {fieldLabels[key]}
                    </label>
                  </div>
                  <p className="text-xs text-gray-400 mb-1.5 ml-6">AI suggestion</p>
                  <div className="ml-6">
                    <textarea
                      value={fieldData[key]}
                      onChange={(e) => setFieldData((prev) => ({ ...prev, [key]: e.target.value }))}
                      className="w-full text-sm p-3 border border-gray-200 rounded-xl bg-white focus:ring-1 focus:ring-gray-300 focus:outline-none min-h-[3.75rem] resize-none shadow-xs text-gray-600"
                    />
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <h3 className="text-sm font-medium text-[#111827]">Full Transcription</h3>
                <button
                  onClick={handleCopyTranscript}
                  className="flex items-center gap-1.5 text-sm text-[#3E5C76] hover:text-[#2d4356] transition-colors"
                >
                  <Copy size={16} /> Copy
                </button>
              </div>
              <div className="bg-[#FAFAFA] border border-[#F3F4F6] rounded-xl p-4">
                <p className="text-sm text-[#374151] leading-relaxed whitespace-pre-wrap">
                  {transcriptionData?.rawTranscription || "No transcription available."}
                </p>
              </div>
            </div>
          )}
        </div>

        <div className="p-6 border-t border-gray-100 bg-white flex justify-end gap-3 flex-shrink-0 rounded-b-2xl">
          <Button
            variant="outline"
            onClick={onClose}
            className="h-10 px-6 border-[#E5E7EB] bg-white hover:bg-gray-50 text-[#374151] rounded-full text-sm font-medium"
          >
            Cancel
          </Button>
          <Button
            onClick={handleApply}
            className="h-10 px-6 bg-(--bg-primary-dark) hover:bg-(--bg-primary-dark)/90 text-white rounded-full text-sm font-medium shadow-sm transition-all"
          >
            Apply Selected ({String(selectedCount).padStart(2, "0")})
          </Button>
        </div>
      </div>
    </div>
  );
};

function Check({ size, className }: { size: number; className?: string }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="3"
      strokeLinecap="round"
      strokeLinejoin="round"
      className={className}
    >
      <polyline points="20 6 9 17 4 12" />
    </svg>
  );
}

const ReviewTranscriptionModal = (props: ReviewTranscriptionModalProps) => props.isOpen && props.transcriptionData ? <ReviewTranscriptionModalContent {...props} /> : null;

export default ReviewTranscriptionModal;
