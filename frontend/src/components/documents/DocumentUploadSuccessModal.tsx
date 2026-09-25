import React from "react";
import { X, FileText } from "lucide-react";
import { Button } from "../ui/button";

interface UploadedFileDisplay {
  name: string;
  type: string;
}

interface DocumentUploadSuccessModalProps {
  isOpen: boolean;
  onClose: () => void;
  files: UploadedFileDisplay[];
}

const DocumentUploadSuccessModal: React.FC<DocumentUploadSuccessModalProps> = ({
  isOpen,
  onClose,
  files,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-2 md:p-0">
      <div className="w-146.75 bg-white rounded-xl shadow-xl flex flex-col items-center text-center relative overflow-hidden p-10">
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition-colors cursor-pointer hover:bg-gray-100 rounded-full p-1.5 duration-300"
        >
          <X size={24} />
        </button>

        <div className=" flex flex-col items-center w-full">
          {/* Success Icon */}
          <img
            src="/assets/check-email.png"
            alt="check"
            className="w-16 h-16 mb-5"
          />

          <h2 className="font-semibold text-2xl text-gray-900 mb-2">
            Document uploaded successfully
          </h2>

          <p className="text-gray-500 text-sm mb-8">
            Your document has been shared with your therapist.
          </p>

          {/* Files List - Grid for multiple files, flex for single? Image shows 2 in row. */}
          <div className="flex flex-wrap gap-3 w-full justify-center">
            {files.map((file, index) => (
              <div
                key={index}
                className="flex items-center gap-3 border border-gray-100 rounded-xl p-3 w-full sm:w-[calc(50%-0.375rem)] text-left shadow-sm bg-white"
              >
                <div className="w-10 h-10 bg-red-500 rounded-lg flex items-center justify-center text-white shrink-0">
                  <FileText size={20} />
                </div>
                <div className="min-w-0">
                  <p className="text-sm font-medium text-gray-900 truncate">
                    {file.name}
                  </p>
                  <p className="text-xs text-gray-400 uppercase">{file.type}</p>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Footer */}
        <div className="w-full mt-6">
          <Button
            className="w-fit rounded-full p-7 bg-[#111827] text-white hover:bg-[#111827]/90 font-medium text-base cursor-pointer"
            onClick={onClose}
          >
            Done
          </Button>
        </div>
      </div>
    </div>
  );
};

export default DocumentUploadSuccessModal;
