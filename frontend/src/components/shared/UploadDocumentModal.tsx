import { useEffect, useState, useRef } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { X, Upload, ChevronDown } from "lucide-react";
import { Button } from "../ui/button";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "../ui/form";
import { Select, SelectContent, SelectItem, SelectTrigger } from "../ui/select";
import {
  uploadDocumentSchema,
  type UploadDocumentFormValues,
} from "../../schemas/upload-document.schema";

export interface UploadedFile {
  id: string;
  file: File;
  name: string;
  type: string;
}

interface UploadDocumentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onUpload: (files: UploadedFile[], documentType: string) => void;
  isUploading?: boolean;
}

const getDocumentIcon = (filename: string): string => {
  const extension = filename.split(".").pop()?.toLowerCase();

  if (extension === "pdf") {
    return "/documents/pdf.svg";
  } else if (["doc", "docx"].includes(extension || "")) {
    return "/documents/doc.svg";
  } else if (
    ["jpg", "jpeg", "png", "gif", "svg", "webp", "bmp"].includes(
      extension || "",
    )
  ) {
    return "/documents/img.svg";
  }

  return "/documents/doc.svg";
};

const getFileTypeLabel = (filename: string): string => {
  const extension = filename.split(".").pop()?.toUpperCase();
  return extension || "FILE";
};

const isLegacyDocFile = (file: File): boolean => {
  const lowerName = file.name.toLowerCase();
  return lowerName.endsWith(".doc") && !lowerName.endsWith(".docx");
};

const UploadDocumentModal = ({
  isOpen,
  onClose,
  onUpload,
  isUploading = false,
}: UploadDocumentModalProps) => {
  const [uploadedFiles, setUploadedFiles] = useState<UploadedFile[]>([]);
  const [isDragging, setIsDragging] = useState(false);
  const [fileError, setFileError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const form = useForm<UploadDocumentFormValues>({
    resolver: zodResolver(uploadDocumentSchema),
    defaultValues: {
      documentType: "",
      description: "",
    },
  });

  useEffect(() => {
    if (!isOpen) return;
    setUploadedFiles([]);
    setIsDragging(false);
    setFileError(null);
    form.reset({
      documentType: "",
      description: "",
    });
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }, [isOpen, form]);

  if (!isOpen) return null;

  const hasSelectedFile = uploadedFiles.length > 0;

  const handleFileSelect = (files: FileList | null) => {
    if (!files || files.length === 0 || hasSelectedFile) return;

    const file = files[0];
    if (isLegacyDocFile(file)) {
      setFileError(".doc files are not supported. Please upload .docx instead.");
      if (fileInputRef.current) {
        fileInputRef.current.value = "";
      }
      return;
    }
    setFileError(null);
    setUploadedFiles([
      {
        id: `${Date.now()}`,
        file,
        name: file.name,
        type: file.type,
      },
    ]);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  };

  const handleFileInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    handleFileSelect(e.target.files);
  };

  const handleDragOver = (e: React.DragEvent) => {
    if (hasSelectedFile) return;
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
  };

  const handleDrop = (e: React.DragEvent) => {
    if (hasSelectedFile) return;
    e.preventDefault();
    setIsDragging(false);
    handleFileSelect(e.dataTransfer.files);
  };

  const handleRemoveFile = (id: string) => {
    setUploadedFiles((prev) => prev.filter((file) => file.id !== id));
  };

  const onSubmit = (values: UploadDocumentFormValues) => {
    if (uploadedFiles.length > 0) {
      onUpload(uploadedFiles, values.documentType);
    }
  };

  const handleClose = () => {
    if (isUploading) return;
    setUploadedFiles([]);
    form.reset();
    onClose();
  };

  const documentTypeOptions = [
    { value: "intake_form", label: "Forms and Intake" },
    { value: "consent", label: "Consent" },
    { value: "insurance_card", label: "Insurance Documents" },
    { value: "id_document", label: "ID Document" },
    { value: "medical_record", label: "Medical Records" },
    { value: "prescription", label: "Prescription" },
    { value: "lab_result", label: "Lab Results" },
    { value: "referral_letter", label: "Referral Letter" },
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center">
      {/* Overlay */}
      <div
        className="app-modal-overlay fixed inset-0 backdrop-blur-[0.125rem]"
        onClick={handleClose}
      />

      {/* Modal */}
      <Form {...form}>
        <div className="app-modal-surface relative rounded-2xl w-full max-w-[37.5rem] mx-4 z-50 overflow-hidden flex flex-col max-h-[90vh]">
          {/* Header */}
          <div className="flex items-start justify-between p-6 pb-2">
            <div>
              <h2 className="text-xl font-bold text-gray-900">
                Upload Document
              </h2>
              <p className="text-sm text-gray-500 mt-1">
                Upload insurance cards, forms, or other documents to share with
                your therapist
              </p>
            </div>
            <button
              onClick={handleClose}
              disabled={isUploading}
              className="text-gray-400 hover:text-gray-600 transition-colors p-1"
            >
              <X size={20} />
            </button>
          </div>

          {/* Content Scrollable Area */}
          <div className="overflow-y-auto px-6 py-4">
            <form
              id="upload-form"
              onSubmit={form.handleSubmit(onSubmit)}
              className="space-y-6"
            >
              {/* Drag and Drop Area */}
              <div
                onDragOver={handleDragOver}
                onDragLeave={handleDragLeave}
                onDrop={handleDrop}
                className={`border border-dashed rounded-[1.25rem] p-8 text-center transition-all ${
                  hasSelectedFile
                    ? "border-gray-200 bg-gray-50 opacity-60 pointer-events-none"
                    : isDragging
                      ? "border-[var(--bg-primary-dark)] bg-[var(--bg-primary-50)]"
                      : "border-gray-200 bg-[#F8FAFC]"
                }`}
              >
                <div className="flex flex-col items-center gap-3">
                  <div className="w-14 h-14 bg-[#E8EDF2] rounded-full flex items-center justify-center mb-1">
                    <Upload className="w-6 h-6 text-gray-600" />
                    {/* Or stick to img if preferred, but lucide used elsewhere. The prompt image has a specific icon though. I'll stick to image if available, else icon. 
                                 The original code used /documents/upload.svg. I'll keep it.
                             */}
                    {/* <img src="/documents/upload.svg" alt="Upload" className="w-6 h-6" /> */}
                  </div>

                  <div>
                    <p className="text-sm text-gray-600 mb-4">
                      {hasSelectedFile
                        ? "Remove the selected file to upload a different one."
                        : (
                          <>
                            Drop a file here or click{" "}
                            <span className="font-semibold text-gray-900">
                              &quot;Select document&quot;
                            </span>
                          </>
                        )}
                    </p>
                    <Button
                      type="button"
                      variant="outline"
                      onClick={() => fileInputRef.current?.click()}
                      disabled={hasSelectedFile}
                      className="h-10 px-6 rounded-full border-gray-200 bg-white hover:bg-gray-50 text-gray-700 font-medium text-sm shadow-sm disabled:cursor-not-allowed disabled:opacity-50"
                    >
                      Select document
                    </Button>
                    <input
                      ref={fileInputRef}
                      type="file"
                      accept=".pdf,.jpg,.jpeg,.png,.docx"
                      onChange={handleFileInputChange}
                      className="hidden"
                    />
                  </div>
                  <p className="text-[0.6875rem] text-gray-400 mt-2">
                    Supported: PDF, Word (.docx), Images (Max 10MB)
                  </p>
                  {fileError ? (
                    <p className="text-[0.6875rem] text-red-500 mt-2">{fileError}</p>
                  ) : null}
                </div>
              </div>

              {/* Uploaded Files List */}
              {uploadedFiles.length > 0 && (
                <div className="space-y-2 flex gap-4 items-center flex-wrap">
                  {uploadedFiles.map((file) => (
                    <div
                      key={file.id}
                      className="flex w-full max-w-full items-center gap-3 rounded-xl border border-gray-100 bg-white py-2 pl-2 pr-3 shadow-sm"
                    >
                      <img
                        src={getDocumentIcon(file.name)}
                        alt={file.name}
                        className="h-8 w-8 shrink-0 rounded"
                        onError={(e) => {
                          // Fallback if image fails
                          (e.target as HTMLImageElement).src =
                            "/documents/doc.svg"; // Fallback
                        }}
                      />
                      <div className="min-w-0 flex-1 pr-2">
                        <p
                          className="truncate text-sm font-medium text-gray-900"
                          title={file.name}
                        >
                          {file.name}
                        </p>
                        <p className="text-[0.625rem] text-gray-500 uppercase">
                          {getFileTypeLabel(file.name)}
                        </p>
                      </div>
                      <button
                        type="button"
                        onClick={() => handleRemoveFile(file.id)}
                        className="flex h-5 w-5 shrink-0 items-center justify-center rounded-full bg-gray-900 text-white transition-colors hover:bg-gray-700"
                      >
                        <X size={12} />
                      </button>
                    </div>
                  ))}
                </div>
              )}

              <div className="space-y-4">
                {/* Document Type Select */}
                <FormField
                  control={form.control}
                  name="documentType"
                  render={({ field }) => (
                    <FormItem>
                      <FormControl>
                        <Select
                          value={field.value || ""}
                          onValueChange={field.onChange}
                        >
                          <SelectTrigger className="group w-full h-[3.5rem] min-h-[3.5rem] py-0 rounded-xl border-gray-200 pt-7 pb-1 relative text-left [&>svg]:hidden bg-white focus:ring-0 items-start">
                            <span
                              className={`absolute left-3 transition-all duration-200 pointer-events-none ${field.value ? "top-3.5 -translate-y-1/2 text-[0.6875rem] text-gray-500" : "top-1/2 -translate-y-1/2 text-gray-400 group-data-[state=open]:top-3.5 group-data-[state=open]:text-[0.6875rem] group-data-[state=open]:text-gray-500"}`}
                            >
                              Type of document{" "}
                              <span className="text-red-500">*</span>
                            </span>
                            <span className="block truncate text-base text-[#101828]">
                              {field.value
                                ? documentTypeOptions.find(
                                    (opt) => opt.value === field.value,
                                  )?.label
                                : ""}
                            </span>
                            <div className="absolute right-3 top-1/2 -translate-y-1/2">
                              <ChevronDown className="h-5 w-5 text-[#8E95A2] opacity-50" />
                            </div>
                          </SelectTrigger>
                          <SelectContent className="rounded-xl border-gray-100 shadow-lg">
                            {documentTypeOptions.map((option) => (
                              <SelectItem
                                key={option.value}
                                value={option.value}
                                className="text-sm py-2.5"
                              >
                                {option.label}
                              </SelectItem>
                            ))}
                          </SelectContent>
                        </Select>
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              </div>
            </form>
          </div>

          {/* Footer */}
          <div className="p-6 pt-2 border-t border-gray-50 mt-auto">
            <div className="flex items-center justify-end gap-3">
              <Button
                type="button"
                variant="secondary"
                size="lg"
                onClick={handleClose}
                disabled={isUploading}
              >
                Cancel
              </Button>
              <Button
                type="submit"
                variant="primary"
                size="lg"
                form="upload-form"
                disabled={
                  uploadedFiles.length === 0 ||
                  !form.watch("documentType") ||
                  isUploading
                }
                loading={isUploading}
                loadingLabel="Uploading..."
              >
                Upload Document
              </Button>
            </div>
          </div>
        </div>
      </Form>
    </div>
  );
};

export default UploadDocumentModal;
