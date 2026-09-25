import { ContentLoader } from "@/components/shared/ContentLoader";
import { useMemo, useState } from "react";
import { Download, Upload, X } from "lucide-react";
import { Button } from "@/components/ui/button";
import CustomSelect from "@/components/form/CustomSelect";
import {
  useBulkUploadSessionsMutation,
  useLazyDownloadSessionBulkUploadTemplateQuery,
} from "@/store/api/admin/dashboard.api";
import { getApiErrorMessage } from "@/utils/apiError";

type UploadResult = {
  total: number;
  successful: number;
  failed: number;
  errors: Record<string, unknown>[];
};

interface BulkUploadSessionsModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
  onToast: (message: string, type: "success" | "error" | "info") => void;
}

type ParsedData = {
  headers: string[];
  rows: Record<string, string>[];
};

/**
 * Fields match the backend bulk-upload template / single-session booking required data.
 * Headers from the downloaded template auto-map by key (and light aliases).
 */
const SESSION_FIELDS = [
  {
    key: "clientMrn",
    label: "Client MRN",
    required: true,
    aliases: ["client_mrn", "clientmrn", "mrn", "clientid", "client_id"],
  },
  {
    key: "therapistUsername",
    label: "Therapist Email (optional)",
    required: false,
    aliases: [
      "therapist_username",
      "therapistemail",
      "therapist",
      "therapistname",
      "therapistid",
    ],
  },
  {
    key: "sessionDate",
    label: "Session Date (YYYY-MM-DD)",
    required: true,
    aliases: ["session_date", "date"],
  },
  {
    key: "sessionTime",
    label: "Session Time (24h or AM/PM)",
    required: true,
    aliases: ["session_time", "time", "starttime"],
  },
  {
    key: "sessionMode",
    label: "Session Mode (must exist in system)",
    required: true,
    aliases: ["session_mode", "mode"],
  },
  {
    key: "sessionType",
    label: "Session Type (must exist in system)",
    required: true,
    aliases: ["session_type", "clinicalsessiontype"],
  },
  {
    key: "serviceCode",
    label: "Service Code (must exist in system)",
    required: true,
    aliases: ["service_code", "serviceid", "service"],
  },
  {
    key: "roomNumber",
    label: "Room Number (in-person only)",
    required: false,
    aliases: ["room_number", "room", "roomid"],
  },
  {
    key: "notes",
    label: "Notes (optional)",
    required: false,
    aliases: ["note", "comments"],
  },
] as const;

function normalizeHeader(value: string): string {
  return value.trim().toLowerCase().replace(/[\s_-]+/g, "");
}

function findMatchingHeader(
  headers: string[],
  field: (typeof SESSION_FIELDS)[number],
): string | undefined {
  const candidates = new Set<string>([
    normalizeHeader(field.key),
    ...field.aliases.map((alias) => normalizeHeader(alias)),
  ]);
  return headers.find((header) => candidates.has(normalizeHeader(header)));
}

function autoMapHeaders(headers: string[]): Record<string, string> {
  const mappings: Record<string, string> = {};
  const usedHeaders = new Set<string>();

  SESSION_FIELDS.forEach((field) => {
    const match = findMatchingHeader(headers, field);
    if (match && !usedHeaders.has(match)) {
      mappings[field.key] = match;
      usedHeaders.add(match);
    }
  });

  return mappings;
}

function parseCsvLine(line: string): string[] {
  const cells: string[] = [];
  let current = "";
  let insideQuotes = false;
  for (let i = 0; i < line.length; i += 1) {
    const char = line[i];
    if (char === '"') {
      const next = line[i + 1];
      if (insideQuotes && next === '"') {
        current += '"';
        i += 1;
      } else {
        insideQuotes = !insideQuotes;
      }
      continue;
    }
    if (char === "," && !insideQuotes) {
      cells.push(current.trim());
      current = "";
      continue;
    }
    current += char;
  }
  cells.push(current.trim());
  return cells;
}

async function parseCsvFile(file: File): Promise<ParsedData> {
  const text = await file.text();
  const lines = text
    .replace(/^\uFEFF/, "")
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0 && !line.startsWith("#"));

  if (!lines.length) {
    throw new Error("The uploaded file is empty.");
  }

  const headers = parseCsvLine(lines[0]).map((header) => header.trim());
  if (!headers.length) {
    throw new Error("Unable to read CSV headers.");
  }

  const rows = lines.slice(1).map((line) => {
    const values = parseCsvLine(line);
    const row: Record<string, string> = {};
    headers.forEach((header, index) => {
      row[header] = values[index]?.trim() ?? "";
    });
    return row;
  });

  return { headers, rows };
}

const BulkUploadSessionsModal = ({
  isOpen,
  onClose,
  onSuccess,
  onToast,
}: BulkUploadSessionsModalProps) => {
  const [step, setStep] = useState(1);
  const [file, setFile] = useState<File | null>(null);
  const [parsedData, setParsedData] = useState<ParsedData | null>(null);
  const [mappings, setMappings] = useState<Record<string, string>>({});
  const [result, setResult] = useState<UploadResult | null>(null);
  const [isParsingFile, setIsParsingFile] = useState(false);

  const [downloadTemplate, { isFetching: isDownloadingTemplate }] =
    useLazyDownloadSessionBulkUploadTemplateQuery();
  const [bulkUploadSessions, { isLoading: isUploading }] =
    useBulkUploadSessionsMutation();

  const headerOptions = useMemo(
    () =>
      (parsedData?.headers || []).map((header) => ({
        value: header,
        label: header,
      })),
    [parsedData?.headers],
  );

  const requiredFieldKeys = useMemo(
    () => SESSION_FIELDS.filter((field) => field.required).map((field) => field.key),
    [],
  );

  const canGoNextFromMapping = useMemo(
    () => requiredFieldKeys.every((key) => Boolean(mappings[key])),
    [mappings, requiredFieldKeys],
  );

  const mappedSessions = useMemo(() => {
    if (!parsedData) return [];
    return parsedData.rows
      .map((row) => {
        const payload: Record<string, string> = {};
        SESSION_FIELDS.forEach((field) => {
          const selectedHeader = mappings[field.key];
          if (!selectedHeader) return;
          const rawValue = row[selectedHeader];
          if (rawValue && rawValue.trim().length > 0) {
            payload[field.key] = rawValue.trim();
          }
        });
        return payload;
      })
      .filter((payload) => Object.keys(payload).length > 0);
  }, [mappings, parsedData]);

  // Show every mapped row so users can fully review before upload.
  const previewRows = mappedSessions;

  const handleClose = () => {
    setStep(1);
    setFile(null);
    setParsedData(null);
    setMappings({});
    setResult(null);
    setIsParsingFile(false);
    onClose();
  };

  const handleTemplateDownload = async () => {
    try {
      const blob = await downloadTemplate().unwrap();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = url;
      link.download = "session-bulk-upload-template.csv";
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
      onToast("Template downloaded successfully.", "success");
    } catch (error) {
      onToast(getApiErrorMessage(error), "error");
    }
  };

  const handleFileSelection = async (selectedFile: File | null) => {
    if (!selectedFile) return;
    const extension = selectedFile.name.split(".").pop()?.toLowerCase();
    if (!extension || !["csv", "xls", "xlsx"].includes(extension)) {
      onToast("Please upload a CSV or Excel file.", "error");
      return;
    }
    if (extension !== "csv") {
      onToast("Please upload the CSV template file.", "error");
      return;
    }
    setFile(selectedFile);
    setIsParsingFile(true);
    try {
      const parsed = await parseCsvFile(selectedFile);
      setParsedData(parsed);
      const initialMappings = autoMapHeaders(parsed.headers);
      setMappings(initialMappings);
      setStep(2);
      const missingRequired = requiredFieldKeys.filter((key) => !initialMappings[key]);
      if (missingRequired.length === 0) {
        onToast("Columns auto-mapped from the template.", "success");
      } else {
        onToast(
          "Some required columns could not be auto-mapped. Review the mapping below.",
          "info",
        );
      }
    } catch (error) {
      onToast(
        error instanceof Error ? error.message : "Unable to parse uploaded file.",
        "error",
      );
    } finally {
      setIsParsingFile(false);
    }
  };

  const handleUpload = async () => {
    try {
      const response = await bulkUploadSessions({ sessions: mappedSessions }).unwrap();
      setResult({
        total: response.total || mappedSessions.length,
        successful: response.successful || 0,
        failed: response.failed || 0,
        errors: response.errors || [],
      });
      onToast(
        response.failed > 0
          ? `Upload finished: ${response.successful} succeeded, ${response.failed} failed${
              (response.errors || []).some((e) => {
                const t = typeof e.type === "string" ? e.type.toLowerCase() : "";
                const m = typeof e.message === "string" ? e.message.toLowerCase() : "";
                return t === "conflict" || m.includes("conflict");
              })
                ? " (including schedule conflicts)"
                : ""
            }.`
          : "Sessions uploaded successfully.",
        response.failed > 0 ? "info" : "success",
      );
      onSuccess?.();
      setStep(4);
    } catch (error) {
      onToast(getApiErrorMessage(error), "error");
    }
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-[80] flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-4xl rounded-2xl bg-white shadow-xl">
        <div className="flex items-center justify-between border-b border-(--neutral-100) px-6 py-4">
          <div>
            <h2 className="text-xl font-semibold text-(--text-primary-dark)">Bulk Upload Sessions</h2>
            <div className="mt-2 flex items-center gap-2">
              {[1, 2, 3, 4].map((stepIndex) => (
                <div
                  key={stepIndex}
                  className={`h-7 w-7 rounded-full text-xs font-semibold flex items-center justify-center ${
                    stepIndex <= step
                      ? "bg-(--bg-primary-dark) text-white"
                      : "bg-(--neutral-100) text-(--text-neutral-600)"
                  }`}
                >
                  {stepIndex}
                </div>
              ))}
            </div>
          </div>
          <button
            onClick={handleClose}
            className="rounded-full p-2 text-(--text-neutral-600) hover:bg-(--neutral-100) cursor-pointer"
          >
            <X size={18} />
          </button>
        </div>

        <div className="max-h-[70vh] min-w-0 overflow-y-auto overflow-x-hidden px-6 py-5">
          {step === 1 && (
            <div className="space-y-5">
              <div>
                <h3 className="text-lg font-semibold text-(--text-primary-dark)">Upload File</h3>
                <p className="text-sm text-(--text-neutral-600)">
                  Download the template, fill each row like booking a single session, then upload the CSV.
                  All fields are required except therapist email and notes. Therapist email is optional —
                  leave blank to use the client&apos;s assigned therapist; if filled, it must match that
                  assignment.
                </p>
                <ul className="mt-3 list-disc space-y-1.5 pl-5 text-sm text-(--text-neutral-600)">
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Client MRN</span>
                    {" — "}
                    the client medical record number (example:{" "}
                    <span className="font-mono text-(--text-primary-dark)">CL-2026-0001</span>), not
                    the internal database ID. Must exist in the system.
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Session mode</span>
                    {" — "}
                    must match a configured mode in the system (same keys/labels as single session
                    booking, e.g.{" "}
                    <span className="font-mono text-(--text-primary-dark)">in_person</span>,{" "}
                    <span className="font-mono text-(--text-primary-dark)">Online</span>).
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Session type</span>
                    {" — "}
                    must match a configured clinical session type in System Options. Rows with an
                    unknown type fail with a clear error.
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Room number</span>
                    {" — "}
                    required for in-person sessions only (same rule as single booking). Leave blank
                    for online/virtual. If filled, the room must exist in the system.
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Service code</span>
                    {" — "}
                    must exist in the system. Duration is taken from the service, not from the CSV.
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Status</span>
                    {" — "}
                    not in the CSV; every uploaded session is created as{" "}
                    <span className="font-mono text-(--text-primary-dark)">scheduled</span>.
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Date</span>
                    {" — "}
                    use <span className="font-medium">YYYY-MM-DD</span> (example:{" "}
                    <span className="font-mono text-(--text-primary-dark)">2026-08-15</span>).
                  </li>
                  <li>
                    <span className="font-medium text-(--text-primary-dark)">Time</span>
                    {" — "}
                    use either{" "}
                    <span className="font-medium">24-hour</span> format (
                    <span className="font-mono text-(--text-primary-dark)">09:00</span>,{" "}
                    <span className="font-mono text-(--text-primary-dark)">14:30</span>) or{" "}
                    <span className="font-medium">12-hour with AM/PM</span> (
                    <span className="font-mono text-(--text-primary-dark)">9:00 AM</span>,{" "}
                    <span className="font-mono text-(--text-primary-dark)">2:30 PM</span>).
                    Do not use UTC — enter the normal clock time for that therapist&apos;s schedule.
                  </li>
                </ul>
              </div>
              <label className="border border-dashed border-(--neutral-200) rounded-xl p-6 flex flex-col items-center justify-center gap-3 cursor-pointer">
                <Upload className="text-(--text-neutral-600)" size={24} />
                <span className="text-sm text-(--text-primary-dark)">
                  {file ? file.name : "Click to upload CSV file"}
                </span>
                <input
                  type="file"
                  className="hidden"
                  accept=".csv,.xls,.xlsx"
                  onChange={(event) => {
                    void handleFileSelection(event.target.files?.[0] || null);
                  }}
                />
              </label>
              <div>
                <Button
                  variant="outline"
                  className="rounded-full"
                  onClick={() => void handleTemplateDownload()}
                  disabled={isDownloadingTemplate}
                  loading={isDownloadingTemplate}
                  loadingLabel="Downloading template..."
                >
                  <Download className="mr-2 h-4 w-4" />
                  Download Template
                </Button>
              </div>
              {isParsingFile && (
                <div className="text-sm text-(--text-neutral-600) flex items-center gap-2">
                  <ContentLoader variant="inline" size="sm" />
                  Parsing file...
                </div>
              )}
            </div>
          )}

          {step === 2 && parsedData && (
            <div className="space-y-5">
              <div>
                <h3 className="text-lg font-semibold text-(--text-primary-dark)">Map Fields</h3>
                <p className="text-sm text-(--text-neutral-600)">
                  Columns from the template are auto-mapped. Confirm each field before continuing.
                  Session time accepts 24-hour (14:30) or 12-hour with AM/PM (2:30 PM).
                </p>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {SESSION_FIELDS.map((field) => (
                  <CustomSelect
                    key={field.key}
                    label={field.label}
                    required={field.required}
                    placeholder="Select column"
                    value={mappings[field.key] || ""}
                    onChange={(value) =>
                      setMappings((prev) => ({
                        ...prev,
                        [field.key]: value,
                      }))
                    }
                    options={headerOptions}
                    isSearch={false}
                  />
                ))}
              </div>
            </div>
          )}

          {step === 3 && (
            <div className="space-y-5 min-w-0">
              <div>
                <h3 className="text-lg font-semibold text-(--text-primary-dark)">Review Data</h3>
                <p className="text-sm text-(--text-neutral-600)">
                  Review all sessions before uploading. Scroll the table to see every row.
                </p>
                <p className="mt-3 text-sm font-medium text-(--text-primary-dark)">
                  Found {mappedSessions.length} sessions to upload
                </p>
              </div>
              <div className="min-w-0 w-full max-h-[min(50vh,24rem)] overflow-auto border border-(--neutral-100) rounded-xl">
                <table className="w-max min-w-full">
                  <thead className="bg-(--neutral-50) sticky top-0 z-1">
                    <tr className="text-left text-xs text-(--text-neutral-600)">
                      {SESSION_FIELDS.map((field) => (
                        <th key={field.key} className="px-3 py-2 whitespace-nowrap bg-(--neutral-50)">
                          {field.label}
                          {field.required ? " *" : ""}
                        </th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {previewRows.map((row, index) => (
                      <tr key={`preview-${index}`} className="border-t border-(--neutral-100) text-sm">
                        {SESSION_FIELDS.map((field) => (
                          <td key={field.key} className="px-3 py-2 whitespace-nowrap">
                            {row[field.key] || "-"}
                          </td>
                        ))}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {step === 4 && (
            <div className="space-y-5">
              <div>
                <h3 className="text-lg font-semibold text-(--text-primary-dark)">Upload Results</h3>
              </div>
              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div className="rounded-xl border border-(--neutral-100) p-4">
                  <p className="text-sm text-(--text-neutral-600)">Total</p>
                  <p className="text-xl font-semibold text-(--text-primary-dark)">
                    {result?.total ?? 0}
                  </p>
                </div>
                <div className="rounded-xl border border-emerald-100 bg-emerald-50 p-4">
                  <p className="text-sm text-emerald-700">Successful</p>
                  <p className="text-xl font-semibold text-emerald-700">
                    {result?.successful ?? 0}
                  </p>
                </div>
                <div className="rounded-xl border border-red-100 bg-red-50 p-4">
                  <p className="text-sm text-red-700">Failed</p>
                  <p className="text-xl font-semibold text-red-700">
                    {result?.failed ?? 0}
                  </p>
                </div>
              </div>
              {result?.errors && result.errors.length > 0 ? (
                <div className="space-y-3">
                  {(() => {
                    const conflicts = result.errors.filter((entry) => {
                      const type = typeof entry.type === "string" ? entry.type.toLowerCase() : "";
                      const message =
                        typeof entry.message === "string" ? entry.message.toLowerCase() : "";
                      return type === "conflict" || message.includes("conflict");
                    });
                    const otherErrors = result.errors.filter((entry) => {
                      const type = typeof entry.type === "string" ? entry.type.toLowerCase() : "";
                      const message =
                        typeof entry.message === "string" ? entry.message.toLowerCase() : "";
                      return !(type === "conflict" || message.includes("conflict"));
                    });
                    return (
                      <>
                        {conflicts.length > 0 ? (
                          <div className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                            <p className="text-sm font-semibold text-amber-900 mb-2">
                              Scheduling conflicts ({conflicts.length})
                            </p>
                            <p className="text-xs text-amber-800 mb-2">
                              These rows were not created because a session is already scheduled at
                              that date and time (for example after re-uploading the same file).
                            </p>
                            <div className="space-y-1 max-h-48 overflow-y-auto">
                              {conflicts.map((entry, index) => {
                                const row =
                                  typeof entry.row === "number" ? `Row ${entry.row}: ` : "";
                                const message =
                                  typeof entry.message === "string"
                                    ? entry.message
                                    : "Scheduling conflict";
                                return (
                                  <p
                                    key={`upload-conflict-${index}`}
                                    className="text-sm text-amber-900"
                                  >
                                    {row}
                                    {message}
                                  </p>
                                );
                              })}
                            </div>
                          </div>
                        ) : null}
                        {otherErrors.length > 0 ? (
                          <div className="rounded-xl border border-red-100 bg-red-50 p-4">
                            <p className="text-sm font-semibold text-red-700 mb-2">
                              Errors ({otherErrors.length})
                            </p>
                            <div className="space-y-1 max-h-48 overflow-y-auto">
                              {otherErrors.map((entry, index) => {
                                const row =
                                  typeof entry.row === "number" ? `Row ${entry.row}: ` : "";
                                const message =
                                  typeof entry.message === "string"
                                    ? entry.message
                                    : "Upload validation error";
                                return (
                                  <p
                                    key={`upload-error-${index}`}
                                    className="text-sm text-red-700"
                                  >
                                    {row}
                                    {message}
                                  </p>
                                );
                              })}
                            </div>
                          </div>
                        ) : null}
                      </>
                    );
                  })()}
                </div>
              ) : null}
            </div>
          )}
        </div>

        <div className="flex items-center justify-end gap-3 border-t border-(--neutral-100) px-6 py-4">
          {step > 1 && step < 4 && (
            <Button variant="outline" onClick={() => setStep((prev) => prev - 1)} className="rounded-full">
              Back
            </Button>
          )}
          {step === 2 && (
            <Button
              onClick={() => setStep(3)}
              className="rounded-full"
              disabled={!canGoNextFromMapping}
            >
              Next
            </Button>
          )}
          {step === 3 && (
            <Button onClick={() => void handleUpload()} className="rounded-full" disabled={isUploading}
              loading={isUploading}
              loadingLabel="Uploading sessions..."
            >

              Upload Sessions
            </Button>
          )}
          {step === 4 && (
            <Button onClick={handleClose} className="rounded-full">
              Close
            </Button>
          )}
        </div>
      </div>
    </div>
  );
};

export default BulkUploadSessionsModal;
